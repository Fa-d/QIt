#!/usr/bin/env python3
"""Restores the narrow-band Bangla voices (Toha, Baezeed) so they still sound like the narrator.

enhance_bangla_voice.py ran VoiceFixer on each verse file alone, which (a) re-synthesised the whole
voice (it sounds artificial), (b) levelled each file on its own (adjacent verses 6 dB apart) and
(c) cut VoiceFixer's hard 30 s segments into long verses. Here:

  - the verses of a surah are enhanced as one stream, in blocks of whole verses (a verse file never
    plays next to another: the Arabic comes between), each block in 12 s windows that crossfade 1 s;
  - only the rebuilt highs are used: the original voice is kept below CROSSOVER, where the recording
    has it, and VoiceFixer's output is kept above it, scaled per window to the original's level;
  - the level is the original's, verse by verse (it sounded even; one gain per surah pushed its loud
    verses into the limiter), with a limiter only for the few peaks the new highs lift above -1 dBFS;
  - each verse is cut back at exactly its old boundaries, with short fades at both ends (--fade-ms).

A noisy source (Baezeed: its hiss is only ~20 dB under the voice, and rebuilt highs make it sizzle)
takes --denoise (DeepFilterNet first; ffmpeg's afftdn left it watery and thin), --highs-db (the rebuilt highs quieter), --gain-db (one gain
for the whole voice, e.g. to sit at the level of the Arabic it plays between) and longer --fade-ms.

A voice whose recording has quiet stretches (Toha: 10% of its verses 6+ dB under the rest) takes a
second pass over the finished folder, --lift-quiet: a verse more than QUIET_DB under the voice's median
loudness is raised to LIFT_TO_DB under it; the others are left alone.

DeepFilterNet is its prebuilt binary (github.com/Rikorose/DeepFilterNet releases, v0.5.6), at
$DEEP_FILTER or ~/.cache/deepfilter/deep-filter.

Runs in the Python 3.12 venv with torch and voicefixer:
  ~/.cache/whisper-cpp/conv/bin/python scripts/smooth_bangla_voice.py --src DIR --out DIR [--surahs 1,112-114]
"""
import argparse, os, subprocess, sys, tempfile, time

import numpy as np
from scipy.ndimage import maximum_filter1d, uniform_filter1d
from scipy.signal import butter, sosfiltfilt

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from split_bangla_verses import FIRST_AYAH, VERSES  # noqa: E402

SR = 44100
WINDOW, OVERLAP = 12 * SR, 1 * SR  # VoiceFixer takes up to 30 s, but 30 s needs ~5 GB
BLOCK = 300 * SR                   # verses enhanced together, at most (memory on a small machine)
CROSSOVER = 4500                   # Hz: both recordings carry the voice up to about 4.7 kHz
MATCH = (3000, 4300)               # Hz: the band both copies have, to level the rebuilt highs
PEAK = 10 ** (-1 / 20)             # -1 dBFS
FADE_MS = (10, 10)                 # in, out
BITRATE = "128k"
QUIET_DB, LIFT_TO_DB = 4.0, 2.0    # --lift-quiet: verses this far under the median, raised to this far

LOW = butter(8, CROSSOVER, "lowpass", fs=SR, output="sos")
HIGH = butter(8, CROSSOVER, "highpass", fs=SR, output="sos")


def decode(path):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, np.float32).astype(np.float64)


def band_energy(x, lo, hi):
    spec = np.abs(np.fft.rfft(x)) ** 2
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return spec[(f >= lo) & (f < hi)].sum()


def windows(n):
    """Starts of the WINDOW-long windows over n samples; the last one ends at n (never a stub of a few ms)."""
    if n <= WINDOW:
        return [0]
    starts = list(range(0, n - WINDOW, WINDOW - OVERLAP))
    return starts + [n - WINDOW]


DEEP_FILTER = os.environ.get("DEEP_FILTER", os.path.expanduser("~/.cache/deepfilter/deep-filter"))


def denoise(x, strength):
    """x through DeepFilterNet (it runs at 48 kHz), at most `strength` dB off the noise: a limit keeps
    some of the room in, and the voice its body."""
    with tempfile.TemporaryDirectory() as td:
        wav = f"{td}/x.wav"
        subprocess.run(["ffmpeg", "-v", "error", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-", "-ar", "48000",
                        "-c:a", "pcm_f32le", wav], input=x.astype(np.float32).tobytes(), check=True)
        subprocess.run([DEEP_FILTER, "-D", "-a", str(strength), "-o", f"{td}/out", wav], check=True,
                       capture_output=True)
        y = decode(f"{td}/out/x.wav")
    return np.pad(y, (0, max(0, len(x) - len(y))))[:len(x)]


def blend(vf, x, highs_db=0.0):
    """x with its highs rebuilt: x below CROSSOVER, VoiceFixer's output (levelled to x, then highs_db) above it."""
    acc, weight = np.zeros(len(x)), np.zeros(len(x))
    for s in windows(len(x)):
        seg = x[s:s + WINDOW]
        e = np.asarray(vf.restore_inmem(seg.astype(np.float32), cuda=False, mode=0), np.float64).reshape(-1)
        e = np.pad(e, (0, max(0, len(seg) - len(e))))[:len(seg)]
        want, got = band_energy(seg, *MATCH), band_energy(e, *MATCH)
        e *= np.sqrt(want / got) * 10 ** (highs_db / 20) if got > 0 else 0.0
        w = np.ones(len(seg))  # linear ramps where windows overlap, none at the stream's ends
        ramp = min(OVERLAP, len(seg) // 2)
        if s > 0:
            w[:ramp] = np.linspace(0, 1, ramp)
        if s + len(seg) < len(x):
            w[-ramp:] = np.linspace(1, 0, ramp)
        acc[s:s + len(seg)] += e * w
        weight[s:s + len(seg)] += w
    rebuilt = acc / np.maximum(weight, 1e-9)
    return sosfiltfilt(LOW, x) + sosfiltfilt(HIGH, rebuilt)


def limit(y):
    """y with the peaks above PEAK pulled down smoothly (10 ms either side), no delay."""
    size = int(0.010 * SR)
    env = uniform_filter1d(maximum_filter1d(np.abs(y), 2 * size), size)
    return y * np.minimum(1.0, PEAK / np.maximum(env, 1e-9))


def write_mp3(y, path, fade_ms=FADE_MS):
    y = y.copy()
    fin, fout = (min(int(ms / 1000 * SR), len(y) // 2) for ms in fade_ms)
    y[:fin] *= 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, fin))
    y[len(y) - fout:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, fout))
    part = path + ".part"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-",
                    "-c:a", "libmp3lame", "-b:a", BITRATE, "-f", "mp3", part],
                   input=np.clip(y, -1, 1).astype(np.float32).tobytes(), check=True)
    os.replace(part, path)


def surah_blocks(files):
    """The surah's verse files in runs of at most BLOCK samples (a longer verse is a block alone)."""
    blocks, cur, size = [], [], 0
    for f, x in files:
        if cur and size + len(x) > BLOCK:
            blocks.append(cur)
            cur, size = [], 0
        cur.append((f, x))
        size += len(x)
    return blocks + ([cur] if cur else [])


def smooth_surah(vf, src, out, surah, opts):
    first = FIRST_AYAH[surah - 1]
    names = [f"{g:05d}.mp3" for g in range(first, first + VERSES[surah - 1])]
    files = [(n, decode(f"{src}/{n}")) for n in names if os.path.exists(f"{src}/{n}")]
    if not files or all(os.path.exists(f"{out}/{n}") for n, _ in files):
        return 0
    for block in surah_blocks(files):
        x = np.concatenate([x for _, x in block])
        if opts.denoise:
            x = denoise(x, opts.denoise)
        y = limit(blend(vf, x, opts.highs_db) * 10 ** (opts.gain_db / 20))
        at = 0
        for name, x in block:
            write_mp3(y[at:at + len(x)], f"{out}/{name}", opts.fade_ms)
            at += len(x)
    return len(files)


def loudness(path):
    """Integrated loudness (LUFS) of an audio file."""
    err = subprocess.run(["ffmpeg", "-hide_banner", "-i", path, "-af", "ebur128", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    return float([ln for ln in err.splitlines() if ln.strip().startswith("I:")][-1].split()[1])


def lift_quiet(folder):
    """Raises the verses of [folder] that are much quieter than its median (see QUIET_DB)."""
    names = sorted(n for n in os.listdir(folder) if n.endswith(".mp3"))
    level = {n: loudness(f"{folder}/{n}") for n in names}
    median = float(np.median(list(level.values())))
    quiet = [n for n in names if level[n] < median - QUIET_DB]
    for n in quiet:
        gain = median - LIFT_TO_DB - level[n]
        y = limit(decode(f"{folder}/{n}") * 10 ** (gain / 20))
        write_mp3(y, f"{folder}/{n}", (0, 0))  # already faded
        print(f"{n}: {level[n]:.1f} LUFS, +{gain:.1f} dB", flush=True)
    print(f"median {median:.1f} LUFS; raised {len(quiet)} of {len(names)}")


def parse_surahs(spec):
    if not spec:
        return list(range(1, 115))
    out = []
    for part in spec.split(","):
        lo, _, hi = part.partition("-")
        out += range(int(lo), int(hi or lo) + 1)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", help="the voice's original verse files (00001.mp3 ...)")
    ap.add_argument("--out", required=True)
    ap.add_argument("--surahs", help="e.g. 1,112-114 (default: all)")
    ap.add_argument("--denoise", type=float, default=0, help="dB of hiss to take off first (0: none)")
    ap.add_argument("--highs-db", type=float, default=0.0, help="the rebuilt highs louder (+) or quieter (-)")
    ap.add_argument("--gain-db", type=float, default=0.0, help="one gain for the whole voice")
    ap.add_argument("--fade-ms", type=lambda v: tuple(int(t) for t in v.split(",")), default=FADE_MS,
                    help="fade in,out at each file's ends (default 10,10)")
    ap.add_argument("--lift-quiet", action="store_true", help="only raise the quiet verses already in --out")
    a = ap.parse_args()
    if a.lift_quiet:
        return lift_quiet(a.out)
    if not a.src:
        ap.error("--src is required")
    os.makedirs(a.out, exist_ok=True)
    from voicefixer import VoiceFixer
    vf = VoiceFixer()
    surahs = parse_surahs(a.surahs)
    start = time.time()
    for i, surah in enumerate(surahs, 1):
        n = smooth_surah(vf, a.src, a.out, surah, a)
        left = (time.time() - start) / i * (len(surahs) - i) / 3600
        print(f"surah {surah}: {n} file(s)  [{i}/{len(surahs)}, ~{left:.1f} h left]", flush=True)


if __name__ == "__main__":
    sys.exit(main())
