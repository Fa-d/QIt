"""Splits one Bangla translation chunk that covers several verses into one piece per verse, where the
words say where one verse ends.

The recordings of the extra Bangla voices read the translation of a group of verses in one go, with
the same short breath between phrases as between verses, so pauses alone can't tell where a verse
ends. Instead:
1. a Bangla speech recognizer (Bengali.AI's fine-tuned Whisper medium, "tugstugi", on whisper.cpp)
   transcribes the chunk, in windows of at most 28 s;
2. forced alignment (torchaudio's MMS_FA, scripts/bangla_align.py) places each word in the audio;
3. each word is matched against the verse's text in six published Bangla translations (the
   narrators' own translations aren't published as text);
4. a dynamic programme puts one cut per verse boundary in a gap between words, scored by the words on
   either side, a prior from the translations' lengths and the gap's length.
A cut that the words don't clearly decide is dropped: those verses keep one piece, played after the
last of them (see the app's SharedTranslations). A cut is never placed inside a word.
"""
import json, os, re, subprocess, tempfile, time, unicodedata, urllib.request

import numpy as np
from scipy.ndimage import maximum_filter1d, uniform_filter1d

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS = os.path.expanduser("~/.cache/whisper-cpp")
WHISPER_MODEL = f"{MODELS}/ggml-tugstugi-medium-bn.bin"  # bengaliAI/tugstugi_bengaliai-asr_whisper-medium
ALIGN_PYTHON = f"{MODELS}/conv/bin/python"                 # Python 3.12 with torch, torchaudio, uroman
REFS = "/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio/bangla/.ref/refs.json"
PORT = 8179
SERVER = f"http://127.0.0.1:{PORT}"

WINDOW = 28.0         # seconds: Whisper hears 30 s at a time
EDGE_PAD = 0.5        # seconds of silence around each window
MIN_PAUSE = 0.10      # seconds of quiet that count as a pause (for window cuts)
PAUSE_DROP = 14.0     # dB below the surrounding loudness that counts as quiet
MATCH_MIN = 0.5       # bigram Dice below this is no match
LENGTH_WEIGHT = 6.0   # cost of a piece whose share of the chunk is off by 100% of the chunk
PAUSE_WEIGHT = 1.0    # bonus per second of gap (capped at 1 s) at a cut
MIN_PIECE = 0.6       # seconds: shortest piece a verse can get
CONFIDENT = 1.0       # margin (about one telling word) a cut needs to stand on its own
EVIDENCE_LENGTH = 0.5  # length prior weight while judging a cut: there, the words must decide
EVIDENCE_PAUSE = 0.3
TELLING = 0.25        # how much more a word belongs to one of two neighbouring verses than the other

_refs = None
_aligner = None


def refs():
    """Reference translations: global ayah -> list of texts."""
    global _refs
    if _refs is None:
        with open(REFS, encoding="utf-8") as f:
            _refs = {int(g): texts for g, texts in json.load(f).items()}
    return _refs


_FOLD = str.maketrans({"ী": "ি", "ূ": "ু", "ঈ": "ই", "ঊ": "উ", "ণ": "ন", "ষ": "স", "শ": "স", "য": "জ",
                       "ৎ": "ত", "ঁ": None, "‌": None, "‍": None, "়": None, "ঃ": None})


def norm_words(text):
    """Bangla words, folded so spelling variants still meet."""
    text = unicodedata.normalize("NFC", text).translate(_FOLD)
    return [w for w in re.findall(r"[ঀ-৿]+", text) if len(w) >= 2]


def bigrams(w):
    return {w[i:i + 2] for i in range(len(w) - 1)} or {w}


def dice(a, b):
    return 2 * len(a & b) / (len(a) + len(b))


# ---- recognition and alignment ------------------------------------------------------------------

def start_server():
    """Starts whisper-server with the Bangla model unless it is listening already. No temperature
    fallback (-nf): on some windows it made one request decode for many minutes, while the others queued.
    Greedy decoding: the Bangla model transcribes these recordings as well as with beam search, 3x faster."""
    def up():
        try:
            urllib.request.urlopen(SERVER, timeout=2)
            return True
        except OSError:
            return False
    if up():
        return
    import fcntl
    with open(os.path.join(tempfile.gettempdir(), "qit-whisper-server.lock"), "w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)  # parallel workers: only the first starts a server
        if up():
            return
        _launch_server()
        for _ in range(180):
            time.sleep(1)
            if up():
                return
    raise RuntimeError("whisper-server did not start")


def _launch_server():
    subprocess.Popen(["whisper-server", "-m", WHISPER_MODEL, "-l", "bn", "--port", str(PORT), "-t", "8",
                      "-bs", "1", "-bo", "1", "-nf", "-nt"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                     start_new_session=True)


def _transcribe(wav):
    """The window's text, or "" if the server can't give it (a window it chokes on is left untranscribed:
    its verses then just share a file, rather than failing the surah)."""
    for _ in range(2):
        done = subprocess.run(["curl", "-s", "--max-time", "1800", f"{SERVER}/inference", "-F", f"file=@{wav}",
                               "-F", "response_format=json", "-F", "language=bn"], capture_output=True)
        if done.returncode == 0:
            try:
                return json.loads(done.stdout.decode("utf-8", "replace")).get("text", "")
            except ValueError:
                pass
        start_server()  # it may have died
    return ""


def _align(wav, words):
    global _aligner
    if _aligner is None:
        _aligner = subprocess.Popen([ALIGN_PYTHON, os.path.join(HERE, "bangla_align.py")], stdin=subprocess.PIPE,
                                    stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, bufsize=1)
    _aligner.stdin.write(json.dumps({"wav": wav, "words": words}, ensure_ascii=False) + "\n")
    reply = json.loads(_aligner.stdout.readline())
    return reply.get("times")


def pauses(db, s, e):
    """[(center_s, length_s)] of pauses inside [s, e]: runs of at least MIN_PAUSE where the level drops
    PAUSE_DROP dB below the loudest of the surrounding 1.5 s. Relative, because the recordings keep
    a floor of music or reverb under the voice (about -40 dB) and are never silent between phrases."""
    fs, fe = int(s * 100), min(int(e * 100), len(db))
    if fe - fs < 3:
        return []
    seg = uniform_filter1d(db[fs:fe], 5)
    quiet = np.concatenate([[False], seg < maximum_filter1d(seg, 150) - PAUSE_DROP, [False]])
    edges = np.flatnonzero(np.diff(quiet.astype(np.int8)))
    return [((fs + (a + b) / 2) / 100, (b - a) / 100) for a, b in zip(edges[::2], edges[1::2])
            if (b - a) / 100 >= MIN_PAUSE]


def windows(db, s, e):
    """[s, e) cut into windows of at most WINDOW seconds, each ending at the longest pause of its last 40%."""
    out, a = [], s
    while a < e - 0.3:
        b = e
        if e - a > WINDOW:
            ps = pauses(db, a + WINDOW * 0.6, a + WINDOW)
            b = max(ps, key=lambda p: p[1])[0] if ps else a + WINDOW
        out.append((a, b))
        a = b
    return out


def words_in(pcm16k, db, s, e):
    """[(word, start_s, end_s)] said in [s, e) of the source (pcm16k: 16 kHz mono int16)."""
    result = []
    pad = np.zeros(int(EDGE_PAD * 16000), dtype=np.int16)
    for a, b in windows(db, s, e):
        clip = np.concatenate([pad, pcm16k[int(a * 16000): int(b * 16000)], pad])
        with tempfile.TemporaryDirectory() as td:
            wav = os.path.join(td, "w.wav")
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "s16le", "-ar", "16000", "-ac", "1", "-i", "-", wav],
                           input=clip.tobytes(), check=True)
            said = re.findall(r"[ঀ-৿]+", _transcribe(wav))
            times = _align(wav, said) if said else None
        if not times:
            continue
        for w, t in zip(said, times):
            if t is not None:
                result.append((w, a - EDGE_PAD + t[0], a - EDGE_PAD + t[1]))
    return result


# ---- matching and cutting -----------------------------------------------------------------------

def verse_scores(words, ayahs):
    """score[i][k]: how much word i belongs to verse k more than to the other verses of the chunk."""
    vocab = [set(w for text in refs()[g] for w in norm_words(text)) for g in ayahs]
    grams = [[bigrams(u) for u in v] for v in vocab]
    raw = np.zeros((len(words), len(ayahs)))
    for i, (word, _, _) in enumerate(words):
        for token in norm_words(word):
            b = bigrams(token)
            for k, gs in enumerate(grams):
                best = max((dice(b, g) for g in gs), default=0.0)
                if best >= MATCH_MIN:
                    raw[i, k] = max(raw[i, k], best)
    if len(ayahs) > 1:
        return raw - (raw.sum(axis=1, keepdims=True) - raw) / (len(ayahs) - 1)
    return raw


REAL_MEANING = 4  # words of the verses' meaning that show a repetitive chunk is real translation

# Words too common in any translation to show that a gap holds the meaning of particular verses.
COMMON = set(norm_words("মধ্যে একটি এবং তারা তাদের আমরা আমাদের তোমরা তোমাদের তিনি তিনিই আল্লাহ আল্লাহর জন্য "
                        "জন্যে থেকে কোন কোনো করে করেন হয় হবে হয়েছে যারা যাদের এই সেই এর তার তাঁর তাকে না কি আর "
                        "ও যে যখন তখন যদি তবে কিন্তু সব সকল তোমার আমার"))


def matching_words(words, ayahs):
    """How many different words (3+ letters, not among the most common) are words of the verses'
    reference translations (exactly, or by their first four letters), or 0 when the words mostly repeat
    themselves. Speech recognition makes up Bangla over breaths and the crowd between Arabic verses,
    often a phrase over and over, and rarely one that means what the verses do."""
    tokens = [t for w, _, _ in words for t in norm_words(w)]
    if not tokens:
        return 0
    vocab = set().union(*(c for g in ayahs for c in content_words(g))) if ayahs else set()
    prefixes = {u[:4] for u in vocab if len(u) >= 5}
    matched = sum(1 for t in set(tokens)
                  if len(t) >= 3 and t not in COMMON and (t in vocab or (len(t) >= 5 and t[:4] in prefixes)))
    # A made-up phrase repeats and means little; a repetitive meaning (Al-Kafirun) still means a lot.
    if matched < REAL_MEANING and len(set(tokens)) < 0.5 * len(tokens):
        return 0
    return matched


def content_words(g):
    """Per reference translation of global ayah g: its distinct content words (3+ letters, not common)."""
    return [{u for u in norm_words(t) if len(u) >= 3 and u not in COMMON} for t in refs()[g]]


def coverage(g, tokens, prefixes):
    """How much of verse g's meaning a chunk's words hold: the best share, over the reference
    translations, of a translation's content words found in the chunk (exactly or by their first four
    letters, which absorbs inflection and recognition slips)."""
    best = 0.0
    for words in content_words(g):
        if words:
            hit = sum(1 for u in words if u in tokens or (len(u) >= 5 and u[:4] in prefixes))
            best = max(best, hit / len(words))
    return best


def text_shares(ayahs):
    """Each verse's share of the group, by the mean length of its reference translations."""
    lengths = np.array([np.mean([len(norm_words(t)) for t in refs()[g]]) + 1.0 for g in ayahs])
    return lengths / lengths.sum()


VERBATIM = 0.6       # share of a translation's words heard, in order, for it to be the text being read
EDGE_WORDS = 2       # a boundary counts when a word within this many of it, on each side, is matched


def _align_text(heard, ref):
    """Monotonic alignment of heard words to reference words (Needleman-Wunsch, fuzzy matches):
    [(heard index, ref index)] of the matched pairs."""
    hb = [bigrams(w) for w in heard]
    rb = [bigrams(w) for w in ref]
    n, m = len(hb), len(rb)
    score = np.zeros((n + 1, m + 1))
    move = np.zeros((n + 1, m + 1), dtype=np.int8)  # 1 diagonal, 2 skip heard, 3 skip ref
    score[1:, 0] = -0.2 * np.arange(1, n + 1)
    score[0, 1:] = -0.2 * np.arange(1, m + 1)
    move[1:, 0], move[0, 1:] = 2, 3
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            d = dice(hb[i - 1], rb[j - 1])
            options = (score[i - 1, j - 1] + (d if d >= 0.6 else -0.3), score[i - 1, j] - 0.2, score[i, j - 1] - 0.2)
            k = int(np.argmax(options))
            score[i, j], move[i, j] = options[k], k + 1
    pairs, i, j = [], n, m
    while i > 0 or j > 0:
        mv = move[i, j]
        if mv == 1:
            if dice(hb[i - 1], rb[j - 1]) >= 0.6:
                pairs.append((i - 1, j - 1))
            i, j = i - 1, j - 1
        elif mv == 2:
            i -= 1
        else:
            j -= 1
    return pairs[::-1]


def verbatim_cuts(words, ayahs):
    """When the narrator reads one of the reference translations (nearly) word for word: for each verse
    boundary, the index of the first heard word of the next verse, or None where the text doesn't
    show it. None overall when no translation is read verbatim."""
    heard = [norm_words(w)[0] if norm_words(w) else "" for w, _, _ in words]
    best = None
    for t in range(len(refs()[ayahs[0]])):
        ref, verse = [], []
        for k, g in enumerate(ayahs):
            ws = norm_words(refs()[g][t])
            ref += ws
            verse += [k] * len(ws)
        if not ref:
            continue
        pairs = _align_text(heard, ref)
        ratio = len(pairs) / len(ref)
        if ratio >= VERBATIM and (best is None or ratio > best[0]):
            best = (ratio, pairs, verse)
    if best is None:
        return None
    _, pairs, verse = best
    cuts = []
    for k in range(1, len(ayahs)):
        last = [(h, r) for h, r in pairs if verse[r] == k - 1]
        nxt = [(h, r) for h, r in pairs if verse[r] == k]
        ref_end = max((r for r in range(len(verse)) if verse[r] == k - 1), default=None)
        if not last or not nxt or ref_end is None:
            cuts.append(None)
            continue
        h0, r0 = last[-1]
        h1, r1 = nxt[0]
        # both sides of the boundary are matched close to it, and nothing heard in between is unmatched
        if ref_end - r0 < EDGE_WORDS and r1 - (ref_end + 1) < EDGE_WORDS and h1 == h0 + 1:
            cuts.append(h1)
        else:
            cuts.append(None)
    return cuts


def _best(times, gaps, word_sum, shares, total, n, length_w, pause_w, allowed=None):
    """Best cuts into n pieces at candidates (times; gaps = pause length at each). allowed(k, j): may
    piece k end at candidate j. Returns (score, candidate index per boundary incl. both ends) or (None, None)."""
    m = len(times)
    neg = -1e18
    best = np.full((n + 1, m), neg)
    back = np.zeros((n + 1, m), dtype=int)
    best[0, 0] = 0.0
    for k in range(1, n + 1):
        for b in range(1, m):
            if (k == n) != (b == m - 1):
                continue
            if allowed is not None and k < n and not allowed(k, b):
                continue
            bonus = pause_w * min(gaps[b], 1.0) if k < n else 0.0
            for a in range(k - 1, b):
                if best[k - 1, a] == neg or times[b] - times[a] < MIN_PIECE:
                    continue
                v = (best[k - 1, a] + word_sum[b, k - 1] - word_sum[a, k - 1]
                     - length_w * abs((times[b] - times[a]) / total - shares[k - 1]) + bonus)
                if v > best[k, b]:
                    best[k, b], back[k, b] = v, a
    if best[n, m - 1] == neg:
        return None, None
    idx, b = [m - 1], m - 1
    for k in range(n, 0, -1):
        b = back[k, b]
        idx.append(b)
    return float(best[n, m - 1]), idx[::-1]


def quietest(db, a, b):
    """The quietest 10 ms frame between a and b (seconds): where a cut between two words goes."""
    fa, fb = int(a * 100), min(int(b * 100), len(db))
    return (fa + int(np.argmin(db[fa:fb]))) / 100 if fb > fa else (a + b) / 2


def split_chunk(db, s, e, ayahs, words, keep_apart=()):
    """Cut [s, e) into pieces between words, one per verse where the words tell the verses apart.

    words: (word, start, end) with absolute times, in order. keep_apart: indexes k whose boundary with
    k + 1 is always cut (1:1, the basmala every surah reuses). Returns [(start, end, [indexes of the
    verses it holds])], each piece belonging to the last verse it holds, and the margin of every
    boundary (None where there was nothing to judge)."""
    n = len(ayahs)
    if n == 1:
        return [(s, e, [0])], []
    words = [w for w in words if s <= (w[1] + w[2]) / 2 < e]
    verbatim = verbatim_cuts(words, ayahs) if len(words) > 1 else None
    if verbatim is not None:
        pieces, held, start = [], [], s
        for k in range(n):
            held.append(k)
            if k == n - 1:
                pieces.append((start, e, held))
            elif verbatim[k] is not None:
                h = verbatim[k]
                cut = quietest(db, words[h - 1][2], words[h][1]) if words[h][1] > words[h - 1][2] else words[h][1]
                pieces.append((start, cut, held))
                held, start = [], cut
        return pieces, [None if c is None else float("inf") for c in verbatim]
    # candidates: the chunk's ends and every gap between consecutive words
    times, gaps = [s], [0.0]
    for (_, _, end0), (_, start1, _) in zip(words, words[1:]):
        if start1 > end0:
            times.append(quietest(db, end0, start1))
            gaps.append(start1 - end0)
    times.append(e)
    gaps.append(0.0)
    times = np.array(times)
    score = verse_scores(words, ayahs) if words else np.zeros((0, n))
    per_interval = np.zeros((len(times), n))
    for i, (_, a, b) in enumerate(words):
        j = int(np.searchsorted(times, (a + b) / 2, side="right")) - 1
        if 0 <= j < len(times) - 1:
            per_interval[j] += score[i]
    word_sum = np.vstack([np.zeros((1, n)), np.cumsum(per_interval, axis=0)])
    shares, total = text_shares(ayahs), e - s
    _, idx = _best(times, gaps, word_sum, shares, total, n, LENGTH_WEIGHT, PAUSE_WEIGHT)
    if idx is None:  # too few gaps between words for this many verses
        return [(s, e, list(range(n)))], [None] * (n - 1)
    # Judge each cut against the best placement that moves it past a word telling the two verses
    # apart: within the stretch of neutral words around it ("and", "he", ...) all gaps are equally
    # good by the words, so the cut goes at the longest gap there.
    mids = np.array([(a + b) / 2 for _, a, b in words])
    margins = []
    for k in range(1, n):
        chosen = idx[k]
        telling = np.flatnonzero(np.abs(score[:, k - 1] - score[:, k]) > TELLING) \
            if len(words) else np.zeros(0, dtype=int)
        before = [mids[i] for i in telling if mids[i] < times[chosen]]
        after = [mids[i] for i in telling if mids[i] > times[chosen]]
        lo = max(before) if before else s
        hi = min(after) if after else e
        stretch = [j for j in range(1, len(times) - 1) if lo < times[j] < hi]
        if stretch:
            idx[k] = max(stretch, key=lambda j: gaps[j])
        here, _ = _best(times, gaps, word_sum, shares, total, n, EVIDENCE_LENGTH, EVIDENCE_PAUSE,
                        lambda kk, j, k=k: kk != k or j == idx[k])
        elsewhere, _ = _best(times, gaps, word_sum, shares, total, n, EVIDENCE_LENGTH, EVIDENCE_PAUSE,
                             lambda kk, j, k=k: kk != k or not lo < times[j] < hi)
        margins.append(None if here is None else (here - elsewhere if elsewhere is not None else float("inf")))
    pieces, held, start = [], [], s
    for k in range(n):
        held.append(k)
        if k == n - 1:
            pieces.append((start, e, held))
        elif k in keep_apart or (margins[k] is not None and margins[k] >= CONFIDENT):
            cut = float(times[idx[k + 1]])
            pieces.append((start, cut, held))
            held, start = [], cut
    return pieces, margins
