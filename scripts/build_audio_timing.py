#!/usr/bin/env python3
"""Builds the recitation timing assets for :core:data.

Writes (compact UTF-8 JSON):
  core/data/src/main/assets/quran/timing/{ar.alafasy,ar.basit-mujawwad,ar.sudais}/001..114.json
      per surah: [[ayah, [start0, end0, start1, end1, ...]], ...] - where each display word of the
      ayah's Arabic text is recited (ms) in the reciter's verse-by-verse file (Alafasy 128 kbps,
      Abdul Basit mujawwad 128 kbps, Sudais 192 kbps).
  core/data/src/main/assets/quran/audio/durations.json
      {"verses": {"ar": [6236], "en": [6236], "bn": [6236], ...}, "intros": {"bn": [114]}} - the length
      (ms) of every audio file the app plays, by track code: verses by global ayah, a track's own
      basmala files by surah (0 for surahs 1 and 9, which have none).

Word timings come from quran-align by Collin Fair (https://github.com/cpfair/quran-align), release
2016-11-24, files Alafasy_128kbps.json, Abdul_Basit_Mujawwad_128kbps.json and
Abdurrahmaan_As-Sudais_192kbps.json, licensed CC BY 4.0. The islamic.network files the app plays
are byte-identical to the everyayah.com files it was aligned against, so the timings apply as-is.

Display words are the Arabic text split on whitespace, with a token made only of pause marks
(U+06D6-U+06DC, U+06DE, U+06E9) attached to the word before it - the app's ArabicWords splits the
same way. quran-align splits Tanzil's Uthmani text alike, except that it counts the pause mark after
the muqatta'at of 10:1 and 13:1 as a word. The aligner leaves a few words unplaced, squeezes ten to
nothing and joins a few into one segment: joined words share their segment in proportion to their
letters, and unplaced words get a share of the gap they fall in (or of the previous word, when there
is no gap).

Durations are measured with ffprobe over the local quran_audio/ folder (see quran_audio/README.md),
which mirrors the CDN and Hugging Face files byte for byte.

Usage (from the repo root; needs ffprobe):
  python3 scripts/build_audio_timing.py [--align-zip quran-align-data-2016-11-24.zip] [--audio quran_audio]
Tests: python3 -m unittest scripts/test_build_audio_timing.py -v
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unicodedata
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_DIR = os.path.join(REPO, "core", "data", "src", "main", "assets", "quran")

ALIGN_URL = ("https://github.com/cpfair/quran-align/releases/download/"
             "release-2016-11-24/quran-align-data-2016-11-24.zip")
# Each reciter with word timings: its timing asset folder, its quran-align file and that file's sha1
# (from the release's README). The app plays the same everyayah recordings the files were aligned on.
RECITERS = {
    "ar.alafasy": ("ar", "Alafasy_128kbps.json", "167bfc53255152aaf8c9a2e5e79c1160134c77a4"),
    "ar.basit-mujawwad": ("ar.basit", "Abdul_Basit_Mujawwad_128kbps.json", "d0c5d63917c0a2d59f0d86ce27936251b6d76f90"),
    "ar.sudais": ("ar.sudais", "Abdurrahmaan_As-Sudais_192kbps.json", "4315194bc88db14192201426fb4f6617bffac4ac"),
}

VERSES = [7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
          112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
          89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
          12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
          30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6]
assert len(VERSES) == 114 and sum(VERSES) == 6236
FIRST_AYAH = [1 + sum(VERSES[:i]) for i in range(114)]  # global number of each surah's verse 1

PAUSE_MARKS = {chr(c) for c in range(0x06D6, 0x06DD)} | {"۞", "۩"}

# Ayahs where quran-align counts the pause mark after the first word (the muqatta'at) as a word.
MARK_COUNTED_AFTER_FIRST_WORD = {(10, 1), (13, 1)}

# Words shorter than this share of a gap are stretched by borrowing from the previous word.
MIN_WORD_MS = 120

AUDIO_TRACKS = {
    "ar": os.path.join("arabic", "alafasy-128k"),
    "en": os.path.join("english", "saheeh-intl-walk-192k"),
    "bn": os.path.join("bangla", "bangla-translation-verses"),
    "ar.basit": os.path.join("arabic", "abdul-basit-mujawwad-128k"),
    "bn.toha": os.path.join("bangla", "toha-verses"),
    "ar.sudais": os.path.join("arabic", "sudais-192k"),
    "bn.baezeed": os.path.join("bangla", "baezeed-verses"),
}
# Tracks whose basmala is a file of its own per surah (the others reuse 1:1).
INTRO_TRACKS = {
    "bn": os.path.join("bangla", "bangla-translation-verses", "intro"),
}


def is_pause_mark(token):
    """A token made only of pause marks (and the combining marks that may ride on them)."""
    return (any(ch in PAUSE_MARKS for ch in token)
            and all(ch in PAUSE_MARKS or unicodedata.category(ch).startswith("M") for ch in token))


def display_words(text):
    """The words of an ayah as the app highlights them: whitespace tokens that aren't pause marks."""
    return [token for token in text.split() if not is_pause_mark(token)]


def letters(word):
    """How many letters [word] has (combining marks don't count); at least 1."""
    return max(1, sum(1 for ch in word if not unicodedata.category(ch).startswith("M")))


def aligner_to_display(surah, ayah, text):
    """For each word index quran-align uses in this ayah, the display word it is (None for a pause
    mark it counts as a word)."""
    mapping, display = [], 0
    for i, token in enumerate(text.split()):
        if not is_pause_mark(token):
            mapping.append(display)
            display += 1
        elif i == 1 and (surah, ayah) in MARK_COUNTED_AFTER_FIRST_WORD:
            mapping.append(None)
    return mapping


def split_by_letters(start, end, words):
    """Splits [start, end) among [words] in proportion to their letters: [(start, end), ...]."""
    weights = [letters(w) for w in words]
    total = sum(weights)
    spans, cursor, acc = [], start, 0
    for weight in weights:
        acc += weight
        boundary = start + round((end - start) * acc / total)
        spans.append((cursor, boundary))
        cursor = boundary
    return spans


def word_spans(surah, ayah, text, segments, duration_ms):
    """[(start, end)] for every display word of the ayah, from quran-align's segments."""
    words = display_words(text)
    mapping = aligner_to_display(surah, ayah, text)
    spans = [None] * len(words)
    for first, last, start, end in segments:
        if end <= start:
            continue  # A word the aligner squeezed to nothing (10 in all): place it like an unplaced one.
        targets = [mapping[i] for i in range(first, last) if i < len(mapping) and mapping[i] is not None]
        for index, span in zip(targets, split_by_letters(start, end, [words[t] for t in targets])):
            spans[index] = span
    fill_gaps(spans, words, duration_ms)
    return spans


def fill_gaps(spans, words, duration_ms):
    """Gives every run of unplaced words the time between its neighbours (the file's start or end at
    the edges). A run with too little time borrows the tail of the word before it."""
    i = 0
    while i < len(spans):
        if spans[i] is not None:
            i += 1
            continue
        run_end = i
        while run_end < len(spans) and spans[run_end] is None:
            run_end += 1
        start = spans[i - 1][1] if i > 0 else 0
        end = spans[run_end][0] if run_end < len(spans) else max(duration_ms, start)
        run = words[i:run_end]
        if end - start < MIN_WORD_MS * len(run) and i > 0:
            # Share the previous word's time with the run.
            prev_start = spans[i - 1][0]
            shared = split_by_letters(prev_start, end, [words[i - 1]] + run)
            spans[i - 1] = shared[0]
            spans[i:run_end] = shared[1:]
        else:
            spans[i:run_end] = split_by_letters(start, end, run)
        i = run_end


def validate_spans(key, spans, word_count):
    if len(spans) != word_count:
        raise ValueError(f"{key}: {len(spans)} timed words for {word_count} words")
    previous = 0
    for index, (start, end) in enumerate(spans):
        if start < previous or end < start:
            raise ValueError(f"{key}: word {index} runs {start}..{end} after {previous}")
        previous = start


def build_timings(texts, alignments, durations_ar):
    """texts: {(surah, ayah): arabic}; alignments: {(surah, ayah): segments}; durations_ar by global
    ayah - 1. Returns per-surah rows [[ayah, [s0, e0, ...]], ...]."""
    per_surah = []
    for surah, count in enumerate(VERSES, 1):
        rows = []
        for ayah in range(1, count + 1):
            key = (surah, ayah)
            if key not in alignments:  # the aligner gave up on it: no word pointer there
                continue
            text = texts[key]
            spans = word_spans(surah, ayah, text, alignments[key],
                               durations_ar[FIRST_AYAH[surah - 1] + ayah - 2])
            validate_spans(f"{surah}:{ayah}", spans, len(display_words(text)))
            rows.append([ayah, [ms for span in spans for ms in span]])
        per_surah.append(rows)
    return per_surah


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def write_assets(out_root, timings, durations):
    """timings: {reciter folder: per-surah rows}."""
    audio_dir = os.path.join(out_root, "audio")
    os.makedirs(audio_dir, exist_ok=True)
    for folder, per_surah in timings.items():
        timing_dir = os.path.join(out_root, "timing", folder)
        os.makedirs(timing_dir, exist_ok=True)
        for surah, rows in enumerate(per_surah, 1):
            with open(os.path.join(timing_dir, f"{surah:03d}.json"), "w", encoding="utf-8") as f:
                f.write(dump(rows))
    with open(os.path.join(audio_dir, "durations.json"), "w", encoding="utf-8") as f:
        f.write(dump(durations))


def read_texts():
    texts = {}
    for surah in range(1, 115):
        with open(os.path.join(ASSETS_DIR, "text", f"{surah:03d}.json"), encoding="utf-8") as f:
            for row in json.load(f):
                texts[(surah, row["n"])] = row["ar"]
    return texts


def read_alignments(zip_path, name="Alafasy_128kbps.json", sha1="167bfc53255152aaf8c9a2e5e79c1160134c77a4"):
    with zipfile.ZipFile(zip_path) as archive:
        raw = archive.read(name)
    digest = hashlib.sha1(raw).hexdigest()
    if digest != sha1:
        raise ValueError(f"{name}: sha1 {digest}, expected {sha1}")
    # The Sudais file opens with the log of an aligner crash (on 26:69) before its JSON.
    text = raw.decode("utf-8")
    rows = json.loads(text[text.index("\n[") + 1:] if not text.startswith("[") else text)
    return {(row["surah"], row["ayah"]): row["segments"] for row in rows}


def probe_ms(path):
    """The file's length in ms; 0 for a file that doesn't exist (a verse that shares its translation's file)."""
    if not os.path.exists(path):
        return 0
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path],
        check=True, capture_output=True, text=True,
    ).stdout
    return round(float(out.strip()) * 1000)


def measure_durations(audio_root):
    """{"verses": {track code: [ms by global ayah - 1]}, "intros": {track code: [ms by surah - 1, 0 = none]}}."""
    jobs = {}
    for track, folder in AUDIO_TRACKS.items():
        for g in range(1, 6237):
            jobs[(track, g)] = os.path.join(audio_root, folder, f"{g:05d}.mp3")
    for track, folder in INTRO_TRACKS.items():
        for surah in range(1, 115):
            if surah not in (1, 9):
                jobs[(track + "/intro", surah)] = os.path.join(audio_root, folder, f"{surah:03d}.mp3")
    with ThreadPoolExecutor(max_workers=os.cpu_count() or 4) as pool:
        results = dict(zip(jobs, pool.map(probe_ms, jobs.values())))
    return {
        "verses": {track: [results[(track, g)] for g in range(1, 6237)] for track in AUDIO_TRACKS},
        "intros": {track: [results.get((track + "/intro", s), 0) for s in range(1, 115)] for track in INTRO_TRACKS},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--align-zip", help="quran-align release zip (downloaded when omitted)")
    parser.add_argument("--audio", default=os.path.join(REPO, "quran_audio"), help="local quran_audio folder")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as tmp:
        zip_path = args.align_zip
        if zip_path is None:
            zip_path = os.path.join(tmp, "quran-align.zip")
            print(f"Downloading {ALIGN_URL}…")
            urllib.request.urlretrieve(ALIGN_URL, zip_path)
        alignments = {folder: read_alignments(zip_path, name, sha1) for folder, (_, name, sha1) in RECITERS.items()}

    print(f"Measuring audio durations under {args.audio} (ffprobe)…")
    durations = measure_durations(args.audio)
    texts = read_texts()
    timings = {folder: build_timings(texts, alignments[folder], durations["verses"][track])
               for folder, (track, _, _) in RECITERS.items()}
    write_assets(ASSETS_DIR, timings, durations)
    words = sum(len(row[1]) // 2 for per_surah in timings.values() for rows in per_surah for row in rows)
    count = sum(len(v) for group in durations.values() for v in group.values())
    print(f"Wrote timings for {words} words and {count} durations to {ASSETS_DIR}")


if __name__ == "__main__":
    sys.exit(main())
