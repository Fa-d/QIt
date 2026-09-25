#!/usr/bin/env python3
"""Re-cuts a Bangla voice's verse files from a better copy of the same recording.

Toha's verse files were cut from a 16 kHz, 20 kbps archive.org copy. The same narration, Bangla only,
is on YouTube per juz at 44.1 kHz, 128 kbps (the "Bangla Audio Quran" channel; ids in
quran_audio/toha-youtube/juz_ids.txt). Each existing verse file is found in its juz's audio by
waveform cross-correlation (same recording: matches score ~0.9) and the same span is cut from the
better copy, so the verse boundaries and the verses that share a file stay exactly as split.

  quran_audio/.venv/bin/python scripts/recut_from_better_copy.py --pieces DIR --juz DIR --out DIR
"""
import argparse, glob, os, subprocess, sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import split_bangla_verses as S  # noqa: E402  (decode, find_verse, VERSES, FIRST_AYAH)

MIN_CONF = 0.5  # below this a verse wasn't found in the better copy: its old file is kept
AHEAD = 900     # seconds past the previous verse to look for the next one


def juz_of(global_ayah):
    """The juz (1..30) that starts at or before global ayah [global_ayah]."""
    starts = [S.FIRST_AYAH[s - 1] + a - 1 for s, a in JUZ_STARTS]
    return max(i + 1 for i, g in enumerate(starts) if g <= global_ayah)


JUZ_STARTS = [(1, 1), (2, 142), (2, 253), (3, 93), (4, 24), (4, 148), (5, 82), (6, 111), (7, 88), (8, 41),
              (9, 93), (11, 6), (12, 53), (15, 1), (17, 1), (18, 75), (21, 1), (23, 1), (25, 21), (27, 56),
              (29, 46), (33, 31), (36, 28), (39, 32), (41, 47), (46, 1), (51, 31), (58, 1), (67, 1), (78, 1)]


EDGE = 3.0  # seconds matched at each end of a verse


def locate(y, cum2, v, cursor):
    """(start, end) seconds of verse audio v inside y, or None. The copies run at very slightly different
    speeds (about 0.3%), so a long verse doesn't match as a whole: its first and last EDGE seconds are
    found separately; one found end places the other by the verse's length."""
    n = min(len(v), int(EDGE * S.SR))
    head, tail = v[:n], v[-n:]

    def find(t, lo, hi):
        lo, hi = max(0, lo), min(len(y) - len(t), hi)
        if hi <= lo:
            return None, 0.0
        return S.find_verse(y, cum2, t, lo, hi)

    ph, ch = find(head, cursor, cursor + int(AHEAD * S.SR))
    if ch < MIN_CONF:
        ph, ch = find(head, 0, len(y))
    if ch >= MIN_CONF:
        expected = ph + len(v) - n
        pt, ct = find(tail, expected - int(2 * S.SR), expected + int(2 * S.SR))
        end = (pt + n) if ct >= MIN_CONF else ph + len(v)
        return ph / S.SR, end / S.SR
    pt, ct = find(tail, cursor, cursor + int(AHEAD * S.SR) + len(v))
    if ct >= MIN_CONF:
        return max(0, pt + n - len(v)) / S.SR, (pt + n) / S.SR
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pieces", required=True, help="the voice's current verse files (00001.mp3 ...)")
    ap.add_argument("--juz", required=True, help="folder with juz01.m4a ... juz30.m4a")
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    pieces = sorted(glob.glob(f"{a.pieces}/*.mp3"))
    by_juz = {}
    for p in pieces:
        by_juz.setdefault(juz_of(int(os.path.basename(p)[:5])), []).append(p)
    kept = []
    for juz, files in sorted(by_juz.items()):
        todo = [f for f in files if not os.path.exists(f"{a.out}/{os.path.basename(f)}")]
        if not todo:
            continue
        src = f"{a.juz}/juz{juz:02d}.m4a"
        y = S.decode(src, S.SR, 1)
        cum2 = np.concatenate([[0.0], np.cumsum(y * y)])
        # pass 1: find each verse; pass 2: a verse this edit recorded in another take (not found) is the
        # gap between its found neighbours, the stream being the verses back to back, when the gap is
        # about as long as the verse
        cursor, spans, lengths = 0, [], []
        for f in files:
            v = S.decode(f, S.SR, 1)
            span = locate(y, cum2, v, cursor)
            spans.append(span)
            lengths.append(len(v) / S.SR)
            if span is not None:
                cursor = int(span[1] * S.SR)
        db = S.frame_db(y)
        i = 0
        while i < len(spans):
            if spans[i] is not None:
                i += 1
                continue
            j = i  # the run of verses not found: i..j
            while j + 1 < len(spans) and spans[j + 1] is None:
                j += 1
            before = spans[i - 1][1] if i > 0 else None
            after = spans[j + 1][0] if j + 1 < len(spans) else None
            want = sum(lengths[i:j + 1])
            # One missing verse only: in a longer run the edit may use other takes with other wording,
            # and a split by length put 84:2-3 where 84:4 belongs. Those keep the old copy's files.
            if i == j and before is not None and after is not None and 0.6 * want <= after - before <= 1.6 * want + 1.0:
                # split the gap among the run by the verses' lengths, each cut at the quietest point near it
                cut, acc = before, 0.0
                for k in range(i, j + 1):
                    acc += lengths[k]
                    if k == j:
                        end = after
                    else:
                        target = before + (after - before) * acc / want
                        lo, hi = int((target - 1.0) * 100), int((target + 1.0) * 100)
                        end = (lo + int(np.argmin(db[lo:hi]))) / 100 if hi > lo else target
                    spans[k] = (cut, end)
                    cut = end
            i = j + 1
        for f, span in zip(files, spans):
            name = os.path.basename(f)
            if os.path.exists(f"{a.out}/{name}"):
                continue
            if span is None:
                kept.append(name)
                subprocess.run(["cp", f, f"{a.out}/{name}"], check=True)
                continue
            start, end = span
            part = f"{a.out}/{name}.part"
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", f"{start:.3f}", "-t", f"{end - start:.3f}", "-i", src,
                            "-ac", "1", "-c:a", "libmp3lame", "-b:a", "128k", "-f", "mp3", part], check=True)
            os.replace(part, f"{a.out}/{name}")
        print(f"juz {juz}: {len(files)} verse files", flush=True)
    print(f"kept from the old copy (not found in the better one): {len(kept)} {kept[:20]}")


if __name__ == "__main__":
    sys.exit(main())
