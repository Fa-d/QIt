#!/usr/bin/env python3
"""Splits the Bangla surah files (Alafasy Arabic + Bangla translation, interleaved per verse)
into Bangla-only verse files named by global ayah number (00001.mp3 … 06236.mp3).

Each Arabic verse is located inside the surah file by waveform cross-correlation against the
verse-by-verse Alafasy set (same recording), so the audio between Arabic verse k and k+1 is the
Bangla translation of verse k. Verses recorded with a different take (all of surah 75, 3:1) don't
correlate; runs of those are placed by a gapped DTW alignment over MFCCs and snapped to the
near-silent splices between clips (flag "dtw"). Resumable: finished surahs are skipped (--force redoes).

Needs ffmpeg + numpy/scipy:  quran_audio/.venv/bin/python scripts/split_bangla_verses.py [--surahs 1,2] [--jobs 6]

--voice toha / --voice baezeed split the recordings of the extra Bangla voices, which read the
translation of a group of verses after the group's Arabic: each group's translation is cut among its
verses by speech recognition and forced alignment (scripts/bangla_asr_split.py), where the words
tell the verses apart; the others share the file of the group's last verse. Afterwards,
--shared-table regenerates :core:domain's SharedTranslationsData.kt from their TSVs.
"""
import argparse, glob, json, os, subprocess, sys, tempfile
from multiprocessing import Pool

import numpy as np
from scipy.fft import dct
from scipy.signal import fftconvolve, stft

ROOT = "/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio"
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Each voice: its surah files (Arabic + Bangla), the verse-by-verse recording of the same Arabic, and
# where the verse files go. "if" reads every verse's translation after it; the others read a group's.
VOICES = {
    "if": dict(src="bangla/alafasy-bangla-translation", pattern="{:03d} - *.mp3", arabic="arabic/alafasy-128k",
               out="bangla/bangla-translation-verses", work="bangla/.work", name="bangla", grouped=False),
    "toha": dict(src="bangla/toha-src", pattern="{:03d}.mp3", arabic="arabic/abdul-basit-mujawwad-128k",
                 out="bangla/toha-verses", work="bangla/.work-toha", name="toha", grouped=True, track="BANGLA_TOHA"),
    # Baezeed's Sudais is another recording than everyayah's: his Arabic is found by voice, not waveform.
    "baezeed": dict(src="bangla/baezeed-src", pattern="{:03d}.mp3", arabic="arabic/sudais-192k",
                    out="bangla/baezeed-verses", work="bangla/.work-baezeed", name="baezeed", grouped=True,
                    track="BANGLA_BAEZEED", by_voice=True),
}
# The voice comes from the environment so that Pool workers (spawned, not forked, on macOS) see it too.
if __name__ == "__main__" and "--voice" in sys.argv[:-1]:
    os.environ["QIT_BANGLA_VOICE"] = sys.argv[sys.argv.index("--voice") + 1]
VOICE = os.environ.get("QIT_BANGLA_VOICE", "if")
_CFG = VOICES[VOICE]
AR = f"{ROOT}/{_CFG['arabic']}"
BN_SRC = f"{ROOT}/{_CFG['src']}"
OUT = f"{ROOT}/{_CFG['out']}"
WORK = f"{ROOT}/{_CFG['work']}"
ALIGN_TSV = f"{ROOT}/bangla/{_CFG['name']}_alignment.tsv"
REPORT = f"{ROOT}/bangla/{_CFG['name']}_split_report.txt"
GROUPED = _CFG["grouped"]

VERSES = [7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
          112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
          89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
          12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
          30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6]
assert len(VERSES) == 114 and sum(VERSES) == 6236
FIRST_AYAH = [1 + sum(VERSES[:i]) for i in range(114)]  # global ayah number of each surah's verse 1

SR = 8000          # analysis rate (mono)
OUT_SR = 44100     # output rate (stereo, from the original source)
FRAME = SR // 100  # 10 ms energy frames
SILENT_DB = -55    # edge trimming: splice points between clips are near-digital silence
PAD = 0.10         # seconds of silence kept around trimmed segments
MIN_SEG = 0.40     # shorter Bangla gaps mean the translation is grouped with the next verse
LOW_CONF = 0.80    # true matches score ~0.98+
HOP = 160          # 20 ms MFCC hop for the DTW fallback
GAP_BN, GAP_AR = 0.15, 1.5  # DTW cost of leaving a frame unmatched when it sounds Bangla / Arabic
                            # (matching speech costs ~0.25, a different take ~0.4, non-matching ~0.45)
SNAP = 2.5         # seconds a DTW boundary may move to reach a splice


def decode(path, sr, channels, fmt="f32le", out=None):
    cmd = ["ffmpeg", "-v", "error", "-i", path, "-ac", str(channels), "-ar", str(sr), "-f", fmt]
    if out:
        subprocess.run(cmd + ["-y", out], check=True)
        return None
    raw = subprocess.run(cmd + ["-"], capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def frame_db(x):
    n = len(x) // FRAME
    return 20 * np.log10(np.sqrt(np.mean(x[: n * FRAME].reshape(n, FRAME) ** 2, axis=1)) + 1e-9)


def find_verse(x, cum2, v, lo, hi):
    """Best normalized cross-correlation offset of template v in x[lo:hi+len(v)] (earliest strong peak)."""
    hi = min(hi, len(x) - len(v))
    if hi <= lo:
        return lo, 0.0
    c = fftconvolve(x[lo: hi + len(v)], v[::-1], mode="valid")
    vv = float(np.dot(v, v))
    e = cum2[lo + len(v): hi + len(v) + 1] - cum2[lo: hi + 1]
    ncc = c / np.sqrt(np.maximum(e, 1e-4 * vv) * vv)
    best = float(ncc.max())
    first = int(np.argmax(ncc >= 0.9 * best))  # repeated verses (e.g. Ar-Rahman refrain): take the earliest
    a, b = max(0, first - SR // 2), min(len(ncc), first + SR // 2)
    k = a + int(np.argmax(ncc[a:b]))
    return lo + k, float(ncc[k])


def trim(db, s, e):
    """Trim near-silent 10 ms frames from both ends of [s, e) (seconds), keep PAD, stay inside [s, e)."""
    fs, fe = int(np.ceil(s * 100)), min(int(e * 100), len(db))
    while fs < fe and db[fs] < SILENT_DB:
        fs += 1
    while fe > fs and db[fe - 1] < SILENT_DB:
        fe -= 1
    if fe <= fs:
        return s, s
    return max(s, fs / 100 - PAD), min(e, fe / 100 + PAD)


def melbank(n_fft=512, n_mels=40, fmin=60, fmax=3800):
    mel, imel = (lambda f: 2595 * np.log10(1 + f / 700)), (lambda m: 700 * (10 ** (m / 2595) - 1))
    bins = np.floor((n_fft + 1) * imel(np.linspace(mel(fmin), mel(fmax), n_mels + 2)) / SR).astype(int)
    fb = np.zeros((n_mels, n_fft // 2 + 1))
    for m in range(1, n_mels + 1):
        l, c, r = bins[m - 1], bins[m], bins[m + 1]
        fb[m - 1, l:c] = (np.arange(l, c) - l) / max(c - l, 1)
        fb[m - 1, c:r] = (r - np.arange(c, r)) / max(r - c, 1)
    return fb


MEL = melbank()


def raw_mfcc(x):
    """MFCCs c1..c19, one row per 20 ms frame."""
    _, _, Z = stft(x, fs=SR, nperseg=400, noverlap=400 - HOP, nfft=512, boundary=None, padded=False)
    return dct(np.log(MEL @ (np.abs(Z) ** 2) + 1e-8), type=2, axis=0, norm="ortho")[1:20].T


def mfcc(x):
    """Unit-length, per-utterance normalized MFCCs (c1..c13) for DTW."""
    C = raw_mfcc(x)[:, :13]
    C = (C - C.mean(axis=0)) / (C.std(axis=0) + 1e-6)
    return C / (np.linalg.norm(C, axis=1, keepdims=True) + 1e-9)


def win_feats(F, win=50):
    """Mean + std of MFCCs over a centered 1 s window around every frame (voice features)."""
    c1 = np.vstack([np.zeros((1, F.shape[1])), np.cumsum(F, axis=0)])
    c2 = np.vstack([np.zeros((1, F.shape[1])), np.cumsum(F ** 2, axis=0)])
    idx = np.arange(len(F))
    a, b = np.clip(idx - win // 2, 0, len(F)), np.clip(idx + win // 2, 0, len(F))
    cnt = (b - a).clip(1)[:, None]
    mu = (c1[b] - c1[a]) / cnt
    return np.hstack([mu, np.sqrt(np.maximum((c2[b] - c2[a]) / cnt - mu ** 2, 0))])


def voice_model():
    """LDA separating Alafasy's Arabic from the Bangla narrator, trained on surahs that align exactly.
    Its score is a log-odds that a frame is Arabic. Cached in WORK."""
    path = f"{WORK}/voice_lda.npz"
    if os.path.exists(path):
        m = np.load(path)
        return m["mu"], m["sd"], m["w"], float(m["b0"])
    X, Y = [], []
    for surah in (1, 19, 36, 55, 67, 78, 97, 112, 113, 114):
        first, n = FIRST_AYAH[surah - 1], VERSES[surah - 1]
        x = decode(source_file(surah), SR, 1)
        cum2 = np.concatenate([[0.0], np.cumsum(x * x)])
        anc, cur = [], 0
        for k in range(n):
            v = decode(f"{AR}/{first + k:05d}.mp3", SR, 1)
            pos, conf = find_verse(x, cum2, v, cur, cur + int(max(180.0, 6 * len(v) / SR) * SR))
            if conf >= LOW_CONF:
                anc.append((pos / SR, (pos + len(v)) / SR))
                cur = pos + len(v)
        F = win_feats(raw_mfcc(x))
        y = -np.ones(len(F), dtype=int)  # 1 Arabic, 0 Bangla, -1 unknown; 0.5 s margins at the edges
        for k, (s, e) in enumerate(anc):
            nxt = anc[k + 1][0] if k + 1 < len(anc) else len(x) / SR
            y[int(s * 50) + 25: int(e * 50) - 25] = 1
            y[int(e * 50) + 25: int(nxt * 50) - 25] = 0
        X.append(F[y >= 0][::5])
        Y.append(y[y >= 0][::5])
    X, Y = np.vstack(X), np.concatenate(Y)
    mu, sd = X.mean(axis=0), X.std(axis=0) + 1e-6
    Z = (X - mu) / sd
    ma, mb = Z[Y == 1].mean(axis=0), Z[Y == 0].mean(axis=0)
    w = np.linalg.solve(np.cov(Z[Y == 1].T) + np.cov(Z[Y == 0].T) + 1e-3 * np.eye(Z.shape[1]), ma - mb)
    b0 = float(-w @ (ma + mb) / 2)
    os.makedirs(WORK, exist_ok=True)
    np.savez(path, mu=mu, sd=sd, w=w, b0=b0)
    return mu, sd, w, b0


def arabic_prob(x):
    mu, sd, w, b0 = voice_model()
    return 1 / (1 + np.exp(-np.clip(((win_feats(raw_mfcc(x)) - mu) / sd) @ w + b0, -30, 30)))


def align_gapped(R, Qs, gap):
    """Place templates Qs, in order, inside R. A frame left outside every template costs gap[j]
    (cheap where the voice is Bangla, expensive where it is Arabic). DTW steps (1,1), (1,2), (2,1).
    Returns [(start_frame, end_frame_inclusive)] per template."""
    m, INF = len(R), 1e18
    cols = np.arange(m)
    G = np.concatenate([[0.0], np.cumsum(gap)])  # G[j] = gap cost of frames 0..j-1
    A = G[:-1].copy()  # cost of starting the next template at column j
    starts, links = [], []
    for Q in Qs:
        D1, S1 = A + (1 - R @ Q[0]), cols.copy()
        D2, S2 = np.full(m, INF), cols.copy()
        for i in range(1, len(Q)):
            c = 1 - R @ Q[i]
            o = np.full((3, m), INF)
            s = np.zeros((3, m), dtype=np.int64)
            o[0, 1:], s[0, 1:] = D1[:-1] + c[1:], S1[:-1]
            o[1, 2:], s[1, 2:] = D1[:-2] + 2 * c[2:], S1[:-2]
            o[2, 1:], s[2, 1:] = D2[:-1] + c[1:], S2[:-1]
            arg = o.argmin(axis=0)
            D2, S2, D1, S1 = D1, S1, o[arg, cols], s[arg, cols]
        starts.append(S1)
        T = D1 - G[1:]  # ending at j' then gap frames j'+1..j-1 costs D1[j'] + G[j] - G[j'+1]
        cm = np.minimum.accumulate(T)
        links.append(np.maximum.accumulate(np.where(T <= cm, cols, 0)))  # best previous end per column
        A = np.full(m, INF)
        A[1:] = cm[:-1] + G[1:-1]
        E = D1
    j = int(np.argmin(E + G[-1] - G[1:]))
    out = []
    for k in range(len(Qs) - 1, -1, -1):
        s = int(starts[k][j])
        out.append((s, j))
        if k:
            j = int(links[k - 1][s - 1])
    return out[::-1]


def splices(db, lo, hi):
    """(start, end) seconds of near-silent runs (clip boundaries) inside [lo, hi]."""
    quiet = np.concatenate([[False], db[int(lo * 100): int(hi * 100)] < SILENT_DB, [False]])
    edges = np.flatnonzero(np.diff(quiet.astype(np.int8)))
    return [(lo + a / 100, lo + b / 100) for a, b in zip(edges[::2], edges[1::2])]


def snap(t, points, lo, hi):
    c = [p for p in points if max(lo, t - SNAP) <= p <= min(hi, t + SNAP)]
    return min(c, key=lambda p: abs(p - t)) if c else t


def place_by_dtw(x, db, verses, lo, hi):
    """Arabic (start, end) seconds for consecutive verse templates inside [lo, hi] of the surah."""
    region = x[int(lo * SR): int(hi * SR)]
    Qs = []
    for v in verses:
        loud = np.flatnonzero(frame_db(v) > -45)
        Qs.append(mfcc(v[loud[0] * FRAME: (loud[-1] + 1) * FRAME] if len(loud) else v))
    gap = GAP_BN + (GAP_AR - GAP_BN) * arabic_prob(region)
    raw = [(lo + s * HOP / SR, lo + (e * HOP + 400) / SR) for s, e in align_gapped(mfcc(region), Qs, gap)]
    sp = splices(db, lo, hi)
    out = []
    for k, (s, e) in enumerate(raw):
        prev_e = raw[k - 1][1] if k else lo
        next_s = raw[k + 1][0] if k + 1 < len(raw) else hi
        s2 = snap(s, [b for a, b in sp], prev_e - 0.3, e)
        e2 = snap(e, [a for a, b in sp], s, next_s + 0.3)
        out.append((s2, e2) if e2 - s2 > 0.5 else (s, e))
    return out


def split_group(db, s, e, weights):
    """Split one translation chunk [s, e) covering several verses at pauses nearest proportional targets."""
    fs, fe = int(s * 100), min(int(e * 100), len(db))
    seg = db[fs:fe]
    thr = SILENT_DB if np.sum(seg < SILENT_DB) >= len(weights) - 1 else np.percentile(seg, 10)
    quiet = seg < thr
    pauses, i = [], 0
    while i < len(quiet):  # centers of quiet runs >= 150 ms (or deep splices of any length)
        if quiet[i]:
            j = i
            while j < len(quiet) and quiet[j]:
                j += 1
            if j - i >= 15 or thr == SILENT_DB:
                pauses.append((fs + (i + j) / 2) / 100)
            i = j
        else:
            i += 1
    w = np.cumsum(weights) / np.sum(weights)
    cuts, prev = [s], s
    for t in (s + (e - s) * w[:-1]):
        cands = [p for p in pauses if p > prev + 0.5]
        c = min(cands, key=lambda p: abs(p - t)) if cands else t
        cuts.append(c)
        prev = c
    cuts.append(e)
    return list(zip(cuts[:-1], cuts[1:]))


def encode(pcm, s, e, out_path):
    a, b = int(round(s * OUT_SR)), int(round(e * OUT_SR))
    seg = pcm[a:b].astype(np.float32)
    fi, fo = min(len(seg), int(0.010 * OUT_SR)), min(len(seg), int(0.020 * OUT_SR))
    seg[:fi] *= np.linspace(0, 1, fi, dtype=np.float32)[:, None]
    seg[len(seg) - fo:] *= np.linspace(1, 0, fo, dtype=np.float32)[:, None]
    tmp = out_path + ".part"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "s16le", "-ar", str(OUT_SR), "-ac", "2", "-i", "-",
                    "-c:a", "libmp3lame", "-b:a", "128k", "-f", "mp3", tmp],
                   input=np.clip(seg, -32768, 32767).astype("<i2").tobytes(), check=True)
    os.replace(tmp, out_path)


def process(surah):
    try:
        if _CFG.get("by_voice"):
            return split_surah_by_voice(surah)
        return split_surah_grouped(surah) if GROUPED else split_surah(surah)
    except Exception:
        import traceback
        return surah, None, traceback.format_exc()


def source_file(surah):
    found = glob.glob(f"{BN_SRC}/{_CFG['pattern'].format(surah)}")
    return found[0] if len(found) == 1 else None


def locate_arabic(surah, x, cum2, db):
    """Where each Arabic verse of [surah] is inside the surah file x: [(start, end, confidence)], the verse
    templates and per-verse flags."""
    n, first = VERSES[surah - 1], FIRST_AYAH[surah - 1]
    dur = len(x) / SR
    # 1) anchors: Arabic verse positions, in order. A verse that doesn't correlate (different take)
    #    leaves the cursor where it was, so it can't drag the following verses off course.
    anchors, verses, cursor = [], [], 0
    flags = [[] for _ in range(n)]
    for k in range(n):
        v = decode(f"{AR}/{first + k:05d}.mp3", SR, 1)
        verses.append(v)
        win = int(max(180.0, 6 * len(v) / SR) * SR)
        pos, conf = find_verse(x, cum2, v, cursor, cursor + win)
        if conf < LOW_CONF:  # widen to the rest of the file before giving up
            pos2, conf2 = find_verse(x, cum2, v, cursor, len(x))
            if conf2 > conf:
                pos, conf = pos2, conf2
        if conf < LOW_CONF and k == 0:  # muqatta'at: the file may reuse another surah's recording
            for other in range(1, 115):   # (3:1 "Alif-Lam-Mim" is 2:1's audio)
                if other in (1, surah):  # 1:1 is the basmala, which opens every intro
                    continue
                v2 = decode(f"{AR}/{FIRST_AYAH[other - 1]:05d}.mp3", SR, 1)
                pos2, conf2 = find_verse(x, cum2, v2, 0, int(min(90.0, dur) * SR))
                if conf2 >= LOW_CONF:
                    pos, conf, v = pos2, conf2, v2
                    flags[k].append(f"reused:{FIRST_AYAH[other - 1]}")
                    break
        anchors.append((pos / SR, (pos + len(v)) / SR, conf))
        if conf >= LOW_CONF:
            cursor = pos + len(v)

    # 2) runs of unmatched verses: gapped DTW between the neighbouring anchors
    k = 0
    while k < n:
        if anchors[k][2] >= LOW_CONF:
            k += 1
            continue
        j = k
        while j + 1 < n and anchors[j + 1][2] < LOW_CONF:
            j += 1
        lo = anchors[k - 1][1] if k else 0.0
        hi = anchors[j + 1][0] if j + 1 < n else dur
        if k == 0 and surah != 1:  # skip the intro basmala (= verse 1:1 audio) so it isn't taken for verse 1
            basmala = decode(f"{AR}/00001.mp3", SR, 1)
            pos, conf = find_verse(x, cum2, basmala, 0, int(min(60.0, hi) * SR))
            if conf >= LOW_CONF:
                lo = (pos + len(basmala)) / SR
        for i, (s, e) in enumerate(place_by_dtw(x, db, verses[k: j + 1], lo, hi)):
            anchors[k + i] = (s, e, anchors[k + i][2])
            flags[k + i].append("dtw")
        k = j + 1

    return anchors, verses, flags


def split_surah(surah):
    n, first = VERSES[surah - 1], FIRST_AYAH[surah - 1]
    tsv = f"{WORK}/{surah:03d}.tsv"
    src = source_file(surah)
    if src is None:
        return surah, None, "source file not found"

    x = decode(src, SR, 1)
    dur = len(x) / SR
    cum2 = np.concatenate([[0.0], np.cumsum(x * x)])
    db = frame_db(x)

    anchors, verses, flags = locate_arabic(surah, x, cum2, db)

    # 3) Bangla segments between anchors (Arabic verse k, then its translation)
    raw = [(anchors[k][1], max(anchors[k][1], anchors[k + 1][0]) if k + 1 < n else dur) for k in range(n)]
    segs = [trim(db, s, e) for s, e in raw]
    group = []
    for k in range(n):
        group.append(k)
        if segs[k][1] - segs[k][0] < MIN_SEG and k + 1 < n:
            continue  # no translation after this Arabic verse: grouped with the next one
        if len(group) > 1 and segs[k][1] - segs[k][0] >= MIN_SEG:
            weights = [anchors[g][1] - anchors[g][0] for g in group]
            for g, part in zip(group, split_group(db, *segs[k], weights)):
                segs[g] = trim(db, *part)
                flags[g].append("approx")
        group = []
    while True:  # an Arabic clip after the last translation is an outro (e.g. surah 110), not translation
        s, e = segs[n - 1]
        sp = [c for c in splices(db, s, e) if c[1] <= e - 1.0]
        if not sp or np.median(arabic_prob(x[int(sp[-1][1] * SR): int(e * SR)])) < 0.9:
            break
        segs[n - 1] = trim(db, s, sp[-1][0])
        flags[n - 1].append("outro-dropped")
    for k in range(n):
        if segs[k][1] - segs[k][0] < MIN_SEG:
            flags[k].append("missing")
    intro = trim(db, 0.0, anchors[0][0])
    has_intro = intro[1] - intro[0] >= 0.5

    # 4) encode from the original 44.1 kHz stereo audio
    os.makedirs(f"{OUT}/intro", exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        rawpcm = f"{td}/s.raw"
        decode(src, OUT_SR, 2, fmt="s16le", out=rawpcm)
        pcm = np.memmap(rawpcm, dtype="<i2", mode="r").reshape(-1, 2)
        for k in range(n):
            if segs[k][1] > segs[k][0]:
                encode(pcm, *segs[k], f"{OUT}/{first + k:05d}.mp3")
        if has_intro:
            encode(pcm, *intro, f"{OUT}/intro/{surah:03d}.mp3")
        del pcm

    rows = []
    for k in range(n):
        a0, a1, conf = anchors[k]
        rows.append(f"{first + k}\t{surah}\t{k + 1}\t{a0:.3f}\t{a1:.3f}\t{segs[k][0]:.3f}\t{segs[k][1]:.3f}"
                    f"\t{conf:.3f}\t{','.join(flags[k])}")
    os.makedirs(WORK, exist_ok=True)
    with open(tsv + ".part", "w") as f:
        f.write(f"#surah\t{surah}\tduration\t{dur:.3f}\tintro\t"
                f"{f'{intro[0]:.3f}-{intro[1]:.3f}' if has_intro else '-'}\n")
        f.write("\n".join(rows) + "\n")
    os.replace(tsv + ".part", tsv)
    return surah, tsv, None


# ---- voices that read a group's translation after its Arabic ----------------------------------------

TRANSLATION_MIN = 8.0   # a gap after an Arabic verse this long needs one word of the verses' meaning to be translation
TRANSLATION_WORDS = 3   # words of the verses' meaning any gap with that many is translation (not a breath or the crowd)
LOUD_DROP = 30          # dB below a template's loudest frame that counts as its silent edge


def loud_span(v):
    """(start, end) seconds of the audible part of verse template v: the everyayah files carry a
    second or more of silence, while the surah files already go on with the translation there."""
    db = frame_db(v)
    loud = np.flatnonzero(db > db.max() - LOUD_DROP)
    if len(loud) == 0:
        return 0.0, len(v) / SR
    return loud[0] / 100, (loud[-1] + 1) / 100


def split_surah_grouped(surah):
    import bangla_asr_split as asr

    n, first = VERSES[surah - 1], FIRST_AYAH[surah - 1]
    tsv = f"{WORK}/{surah:03d}.tsv"
    src = source_file(surah)
    if src is None:
        return surah, None, "source file not found"
    asr.start_server()
    x = decode(src, SR, 1)
    dur = len(x) / SR
    cum2 = np.concatenate([[0.0], np.cumsum(x * x)])
    db = frame_db(x)
    pcm16 = (decode(src, 16000, 1) * 32767).clip(-32768, 32767).astype(np.int16)

    anchors, verses, flags = locate_arabic(surah, x, cum2, db)
    arabic = []  # the audible Arabic of each verse
    for k, (s0, e0, conf) in enumerate(anchors):
        if "dtw" in flags[k]:
            arabic.append((s0, e0))
        else:
            a, b = loud_span(verses[k])
            arabic.append((s0 + a, min(s0 + b, e0)))

    # translation chunks: the gap after an Arabic verse that holds speech in Bangla
    chunks = {}  # k -> (start, end, words): the translation of the group ending at verse k
    group_start = 0
    for k in range(n):
        g0 = arabic[k][1]
        g1 = arabic[k + 1][0] if k + 1 < n else dur
        s, e = trim(db, g0, max(g0, g1))
        if e - s < 1.0:
            continue
        words = asr.words_in(pcm16, db, s, e)
        matched = asr.matching_words(words, [first + j for j in range(group_start, k + 1)])
        if matched >= TRANSLATION_WORDS or (e - s >= TRANSLATION_MIN and matched >= 1):
            chunks[k] = (s, e, words)
            group_start = k + 1
    if n - 1 not in chunks:
        return surah, None, "no translation after the last verse"

    # split each chunk among the verses of its group
    pieces = []  # (start, end, [global ayahs it holds])
    margins = {}
    group_start = 0
    for k in sorted(chunks):
        s, e, words = chunks[k]
        group = list(range(group_start, k + 1))
        parts, ms = asr.split_chunk(db, s, e, [first + j for j in group], words)
        for j, m in zip(group, ms):
            margins[j] = m
        for a, b, held in parts:
            pieces.append((a, b, [first + group[h] for h in held]))
        group_start = k + 1

    # encode from the original audio (stereo, 44.1 kHz)
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        rawpcm = f"{td}/s.raw"
        decode(src, OUT_SR, 2, fmt="s16le", out=rawpcm)
        pcm = np.memmap(rawpcm, dtype="<i2", mode="r").reshape(-1, 2)
        for a, b, held in pieces:
            encode(pcm, a, b, f"{OUT}/{held[-1]:05d}.mp3")
        del pcm
    # a verse that now shares a file mustn't keep one from an earlier run
    owner = {g: held[-1] for _, _, held in pieces for g in held}
    for g in owner:
        if owner[g] != g and os.path.exists(f"{OUT}/{g:05d}.mp3"):
            os.remove(f"{OUT}/{g:05d}.mp3")

    span = {held[-1]: (a, b) for a, b, held in pieces}
    rows = []
    for k in range(n):
        g = first + k
        a0, a1 = arabic[k]
        b0, b1 = span.get(g, (0.0, 0.0))
        f = list(flags[k])
        if owner[g] != g:
            f.append(f"shared:{owner[g]}")
        m = margins.get(k)
        if m is not None:
            f.append(f"margin:{m:.2f}")
        rows.append(f"{g}\t{surah}\t{k + 1}\t{a0:.3f}\t{a1:.3f}\t{b0:.3f}\t{b1:.3f}\t{anchors[k][2]:.3f}\t{','.join(f)}")
    os.makedirs(WORK, exist_ok=True)
    with open(tsv + ".part", "w") as fh:
        fh.write(f"#surah\t{surah}\tduration\t{dur:.3f}\tintro\t-\n")
        fh.write("\n".join(rows) + "\n")
    os.replace(tsv + ".part", tsv)
    with open(f"{WORK}/{surah:03d}.words.json", "w", encoding="utf-8") as fh:
        json.dump({str(k): [[w, round(a, 2), round(b, 2)] for w, a, b in c[2]] for k, c in chunks.items()},
                  fh, ensure_ascii=False)
    return surah, tsv, None


# ---- a voice whose Arabic is another recording than the verse-by-verse set -------------------------

BLOCK_MIN = 2.0       # seconds: shortest run of Arabic that counts as a block of verses
CHUNK_MIN = 1.5       # seconds: shortest run of Bangla that counts as a translation chunk
BASELINE = 0.15       # coverage a verse's meaning needs in its chunk to be worth placing there
TEMPO_WEIGHT = 1.0    # cost per unit of |log(block length / expected length)|


def other_voice_model():
    """LDA telling the reciter's Arabic (the verse-by-verse set: same reciter, other recording) from Bangla
    narration (the Islamic Foundation and Toha verse files: other narrators). Cached in WORK."""
    path = f"{WORK}/voice_lda.npz"
    if os.path.exists(path):
        m = np.load(path)
        return m["mu"], m["sd"], m["w"], float(m["b0"])
    import random
    rng = random.Random(1)

    def feats(f):
        x = decode(f, SR, 1)
        F = win_feats(raw_mfcc(x))
        db = frame_db(x)[::2][:len(F)]
        return F[db > np.percentile(db, 30)][::3] if len(F) else F

    ar = rng.sample(sorted(glob.glob(f"{AR}/*.mp3")), 120)
    bn = rng.sample(sorted(glob.glob(f"{ROOT}/bangla/bangla-translation-verses/0*.mp3")), 120)
    XA, XB = np.vstack([feats(f) for f in ar]), np.vstack([feats(f) for f in bn])
    X = np.vstack([XA, XB])
    mu, sd = X.mean(axis=0), X.std(axis=0) + 1e-6
    ZA, ZB = (XA - mu) / sd, (XB - mu) / sd
    ma, mb = ZA.mean(axis=0), ZB.mean(axis=0)
    w = np.linalg.solve(np.cov(ZA.T) + np.cov(ZB.T) + 1e-3 * np.eye(X.shape[1]), ma - mb)
    b0 = float(-w @ (ma + mb) / 2)
    os.makedirs(WORK, exist_ok=True)
    np.savez(path, mu=mu, sd=sd, w=w, b0=b0)
    return mu, sd, w, b0


def voice_runs(x):
    """[(start, end, is_arabic)] seconds: the file cut into runs of Arabic and of Bangla voice."""
    mu, sd, w, b0 = other_voice_model()
    p = 1 / (1 + np.exp(-np.clip(((win_feats(raw_mfcc(x)) - mu) / sd) @ w + b0, -30, 30)))
    arabic = np.convolve(p, np.ones(25) / 25, mode="same") > 0.5  # 0.5 s smoothing, 20 ms frames
    edges = np.flatnonzero(np.diff(np.r_[False, arabic, False].astype(np.int8)))
    runs = [(a * HOP / SR, b * HOP / SR) for a, b in zip(edges[::2], edges[1::2])]
    blocks = [(a, b) for a, b in runs if b - a >= BLOCK_MIN]
    merged = []  # Arabic blocks closer than CHUNK_MIN hold no translation between them
    for a, b in blocks:
        if merged and a - merged[-1][1] < CHUNK_MIN:
            merged[-1] = (merged[-1][0], b)
        else:
            merged.append((a, b))
    return merged


def assign_groups(n, first, chunk_words, block_lengths, verse_lengths):
    """Consecutive verses for each (block, chunk): [(first_index, last_index)] per chunk, or None.
    Scores how much of each verse's meaning its chunk holds, and how well the block's length matches
    its verses in the verse-by-verse recording (at the surah's own tempo)."""
    import bangla_asr_split as asr
    m = len(chunk_words)
    if m > n:
        return None
    toks = []
    for words in chunk_words:
        t = {u for w, _, _ in words for u in asr.norm_words(w)}
        toks.append((t, {u[:4] for u in t if len(u) >= 5}))
    tempo = sum(block_lengths) / max(sum(verse_lengths), 1e-6)
    cum = np.concatenate([[0.0], np.cumsum(verse_lengths)])
    cov = np.zeros((m, n))
    expected = np.concatenate([[0.0], np.cumsum(block_lengths)]) / max(sum(block_lengths), 1e-6) * cum[-1]
    for i in range(m):
        for v in range(n):  # only verses near where the block's share of the recitation puts them
            if expected[i] - 0.25 * cum[-1] - 60 <= cum[v] <= expected[i + 1] + 0.25 * cum[-1] + 60:
                cov[i, v] = asr.coverage(first + v, *toks[i]) - BASELINE
            else:
                cov[i, v] = -1.0
    cc = np.concatenate([np.zeros((m, 1)), np.cumsum(cov, axis=1)], axis=1)
    neg = -1e18
    best = np.full((m + 1, n + 1), neg)
    back = np.zeros((m + 1, n + 1), dtype=int)
    best[0, 0] = 0.0
    for i in range(1, m + 1):
        for b in range(i, n - (m - i) + 1):
            for a in range(i - 1, b):
                if best[i - 1, a] == neg:
                    continue
                length = tempo * (cum[b] - cum[a])
                v = (best[i - 1, a] + cc[i - 1, b] - cc[i - 1, a]
                     - TEMPO_WEIGHT * abs(np.log(max(block_lengths[i - 1], 0.1) / max(length, 0.1))))
                if v > best[i, b]:
                    best[i, b], back[i, b] = v, a
    if best[m, n] == neg:
        return None
    out, b = [], n
    for i in range(m, 0, -1):
        a = back[i, b]
        out.append((a, b - 1))
        b = a
    return out[::-1]


BASMALA_WORDS = ("পরম", "করুণাময়", "দয়ালু", "আল্লাহর", "নামে", "শুরু", "করছি")


def basmala_meaning(out_path, surah=112):
    """Cuts the basmala's meaning from the head of [surah] (between the Arabic basmala and the first
    verses) into out_path, from the first to the last of its words heard there. Returns the surah it
    came from, or None if it isn't found there."""
    import bangla_asr_split as asr
    src = source_file(surah)
    x = decode(src, SR, 1)
    db = frame_db(x)
    head_end = voice_runs(x)[0][0]
    pcm16 = (decode(src, 16000, 1) * 32767).clip(-32768, 32767).astype(np.int16)
    words = asr.words_in(pcm16, db, 0.0, head_end)
    keys = [asr.norm_words(k)[0] for k in BASMALA_WORDS]
    hits = [i for i, (w, _, _) in enumerate(words)
            if any(t[:4] == k[:4] for t in asr.norm_words(w) for k in keys)]
    if len(hits) < 2:
        return None
    s, e = words[hits[0]][1], words[hits[-1]][2]
    s, e = max(0.0, s - 0.15), min(head_end, e + 0.25)
    with tempfile.TemporaryDirectory() as td:
        rawpcm = f"{td}/s.raw"
        decode(src, OUT_SR, 2, fmt="s16le", out=rawpcm)
        pcm = np.memmap(rawpcm, dtype="<i2", mode="r").reshape(-1, 2)
        encode(pcm, s, e, out_path)
        del pcm
    return surah


def split_surah_by_voice(surah):
    import bangla_asr_split as asr

    n, first = VERSES[surah - 1], FIRST_AYAH[surah - 1]
    tsv = f"{WORK}/{surah:03d}.tsv"
    src = source_file(surah)
    if src is None:
        return surah, None, "source file not found"
    asr.start_server()
    x = decode(src, SR, 1)
    dur = len(x) / SR
    db = frame_db(x)
    pcm16 = (decode(src, 16000, 1) * 32767).clip(-32768, 32767).astype(np.int16)

    blocks = voice_runs(x)
    if not blocks:
        return surah, None, "no Arabic found"
    # a translation chunk follows each block, up to the next one; a chunk without the verses' meaning
    # (recognition making things up over a pause in the Arabic) joins the blocks around it
    chunks = []
    for i, (a, b) in enumerate(blocks):
        s, e = trim(db, b, blocks[i + 1][0] if i + 1 < len(blocks) else dur)
        words = asr.words_in(pcm16, db, s, e) if e - s >= CHUNK_MIN else []
        chunks.append([a, b, s, e, words])
    everything = range(first, first + n)
    kept = []
    for c in chunks:
        if kept and kept[-1][3] - kept[-1][2] < 8.0 and asr.matching_words(kept[-1][4], everything) < 2:
            kept[-1] = [kept[-1][0], c[1], c[2], c[3], c[4]]  # that was a pause in the Arabic, not translation
        else:
            kept.append(c)
    if kept and asr.matching_words(kept[-1][4], everything) < 1:
        kept.pop()
    if not kept:
        return surah, None, "no translation found"

    verse_lengths = [len(decode(f"{AR}/{first + k:05d}.mp3", SR, 1)) / SR for k in range(n)]
    groups = assign_groups(n, first, [c[4] for c in kept], [c[1] - c[0] for c in kept], verse_lengths)
    if groups is None:
        return surah, None, f"could not assign {n} verses to {len(kept)} chunks"

    pieces, margins, block_of = [], {}, {}
    for (a, b, s, e, words), (v0, v1) in zip(kept, groups):
        group = list(range(v0, v1 + 1))
        parts, ms = asr.split_chunk(db, s, e, [first + j for j in group], words)
        for j, mg in zip(group, ms):
            margins[j] = mg
        for j in group:
            block_of[j] = (a, b)
        for pa, pb, held in parts:
            pieces.append((pa, pb, [first + group[h] for h in held]))

    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        rawpcm = f"{td}/s.raw"
        decode(src, OUT_SR, 2, fmt="s16le", out=rawpcm)
        pcm = np.memmap(rawpcm, dtype="<i2", mode="r").reshape(-1, 2)
        for pa, pb, held in pieces:
            encode(pcm, pa, pb, f"{OUT}/{held[-1]:05d}.mp3")
        del pcm
    owner = {g: held[-1] for _, _, held in pieces for g in held}
    basmala_from = None
    if surah == 1 and owner[1] != 1:
        # Al-Fatiha goes from the Arabic straight to the meaning of 1:2; the basmala's meaning is read at
        # the head of the other surahs, after its Arabic: 1:1 (every surah's basmala) is cut from there.
        basmala_from = basmala_meaning(f"{OUT}/00001.mp3")
        if basmala_from is not None:
            pieces = [(pa, pb, [g for g in held if g != 1]) for pa, pb, held in pieces]
            owner[1] = 1
    for g in owner:
        if owner[g] != g and os.path.exists(f"{OUT}/{g:05d}.mp3"):
            os.remove(f"{OUT}/{g:05d}.mp3")

    span = {held[-1]: (pa, pb) for pa, pb, held in pieces if held}
    rows = []
    for k in range(n):
        g = first + k
        a0, a1 = block_of[k]
        b0, b1 = span.get(g, (0.0, 0.0))
        f = ["block"]
        if g == 1 and basmala_from is not None:
            f.append(f"basmala-from:{basmala_from}")
        if owner[g] != g:
            f.append(f"shared:{owner[g]}")
        if margins.get(k) is not None:
            f.append(f"margin:{margins[k]:.2f}")
        rows.append(f"{g}\t{surah}\t{k + 1}\t{a0:.3f}\t{a1:.3f}\t{b0:.3f}\t{b1:.3f}\t0.000\t{','.join(f)}")
    os.makedirs(WORK, exist_ok=True)
    with open(tsv + ".part", "w") as fh:
        fh.write(f"#surah\t{surah}\tduration\t{dur:.3f}\tintro\t-\n")
        fh.write("\n".join(rows) + "\n")
    os.replace(tsv + ".part", tsv)
    with open(f"{WORK}/{surah:03d}.words.json", "w", encoding="utf-8") as fh:
        json.dump({f"{c[2]:.2f}": [[w, round(a, 2), round(b, 2)] for w, a, b in c[4]] for c in kept},
                  fh, ensure_ascii=False)
    return surah, tsv, None


def shared_verses():
    """Global ayahs that share their file with a later verse, from the voice's TSVs."""
    out = []
    for surah in range(1, 115):
        p = f"{WORK}/{surah:03d}.tsv"
        if not os.path.exists(p):
            continue
        for line in open(p).read().strip().split("\n")[1:]:
            cols = line.split("\t")
            if len(cols) > 8 and "shared:" in cols[8]:
                out.append(int(cols[0]))
    return sorted(out)


def as_ranges(numbers):
    """[3, 4, 5, 9] -> "3-5,9"."""
    runs = []
    for g in numbers:
        if runs and g == runs[-1][1] + 1:
            runs[-1][1] = g
        else:
            runs.append([g, g])
    return ",".join(f"{a}-{b}" if a != b else f"{a}" for a, b in runs)


def write_shared_table():
    """Regenerates :core:domain's SharedTranslationsData.kt from every grouped voice's TSVs."""
    global WORK
    entries = []
    for name, cfg in VOICES.items():
        if not cfg["grouped"]:
            continue
        WORK = f"{ROOT}/{cfg['work']}"
        entries.append(f'    Track.{cfg["track"]} to "{as_ranges(shared_verses())}",')
    path = f"{REPO}/core/domain/src/main/kotlin/dev/sadakat/qit/core/domain/audio/SharedTranslationsData.kt"
    with open(path, "w") as f:
        f.write("// Generated by scripts/split_bangla_verses.py: do not edit.\n"
                "package dev.sadakat.qit.core.domain.audio\n\n"
                "import dev.sadakat.qit.core.domain.model.Track\n\n"
                "/** See [SharedTranslations]. */\n"
                "internal val SHARED_TRANSLATIONS: Map<Track, String> = mapOf(\n"
                + "\n".join(entries) + "\n)\n")
    print(f"wrote {path}")


def done(surah):
    first, n = FIRST_AYAH[surah - 1], VERSES[surah - 1]
    tsv = f"{WORK}/{surah:03d}.tsv"
    if not os.path.exists(tsv):
        return False
    shared = {int(r.split("\t")[0]) for r in open(tsv).read().strip().split("\n")[1:] if "shared:" in r}
    return all(os.path.exists(f"{OUT}/{first + k:05d}.mp3") for k in range(n) if first + k not in shared)


def report():
    rows, lines = [], []
    lines.append("surah\tverses\tduration\tarabic\tbangla\tintro\tcoverage\tmin_conf\tflagged")
    for s in range(1, 115):
        p = f"{WORK}/{s:03d}.tsv"
        if not os.path.exists(p):
            lines.append(f"{s}\tNOT PROCESSED")
            continue
        head, *body = open(p).read().strip().split("\n")
        h = head.split("\t")
        dur, intro = float(h[3]), h[5]
        intro_d = 0.0 if intro == "-" else float(intro.split("-")[1]) - float(intro.split("-")[0])
        r = [b.split("\t") for b in body]
        ar = sum(float(x[4]) - float(x[3]) for x in r)
        bn = sum(float(x[6]) - float(x[5]) for x in r)
        flagged = [f"{x[1]}:{x[2]}({x[8]})" for x in r if len(x) > 8 and x[8]]
        lines.append(f"{s}\t{len(r)}\t{dur:.1f}\t{ar:.1f}\t{bn:.1f}\t{intro_d:.1f}\t{(ar + bn + intro_d) / dur:.3f}"
                     f"\t{min(float(x[7]) for x in r):.3f}\t{' '.join(flagged)}")
        rows += body
    with open(ALIGN_TSV, "w") as f:
        f.write("ayah\tsurah\tverse\tarabic_start\tarabic_end\tbangla_start\tbangla_end\tconfidence\tflags\n")
        f.write("\n".join(rows) + "\n")
    open(REPORT, "w").write("\n".join(lines) + "\n")
    print("\n".join(lines))
    print(f"\n{len(rows)} verses in {ALIGN_TSV}\nreport: {REPORT}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--surahs", help="comma list, e.g. 1,2,112 (default: all)")
    ap.add_argument("--jobs", type=int, default=6)
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--voice", choices=sorted(VOICES), default="if",
                    help="which recording to split (default: if, the Islamic Foundation translation)")
    ap.add_argument("--shared-table", action="store_true",
                    help="only regenerate :core:domain's table of verses that share a file")
    a = ap.parse_args()
    if a.shared_table:
        write_shared_table()
        return
    surahs = [int(s) for s in a.surahs.split(",")] if a.surahs else list(range(1, 115))
    todo = [s for s in surahs if a.force or not done(s)]
    todo.sort(key=lambda s: -os.path.getsize(source_file(s)))  # biggest first
    print(f"{len(todo)} surah(s) to process, {len(surahs) - len(todo)} already done", flush=True)
    failed = []
    with Pool(a.jobs) as pool:
        for surah, tsv, err in pool.imap_unordered(process, todo):
            print(f"surah {surah:3d}: {'OK' if not err else 'FAILED: ' + err}", flush=True)
            if err:
                failed.append(surah)
    report()
    if failed:
        print(f"FAILED surahs: {failed}")
        sys.exit(1)


if __name__ == "__main__":
    main()
