#!/bin/bash
# Downloads the sources of the extra Bangla voices (resumable: rerun to fill gaps).
# - Surah files with the Arabic and its Bangla translation, from archive.org, saved as NNN.mp3:
#     Sayed Ismat Toha (with Abdul Basit, mujawwad)  -> quran_audio/bangla/toha-src/
#     Shareef Baezeed Mahmood (with Sudais)          -> quran_audio/bangla/baezeed-src/
#   The Baezeed item lacks surahs 104 and 111 and has 14, 29, 46 and 69 cut short;
#   `alquranwithbanglaaudio` is the same production (mislabelled there as Toha) and has them whole,
#   so it is fetched first for those. Every copy of 34 stops after verse 31.
# - The matching verse-by-verse Arabic from everyayah, saved by global ayah number (00001.mp3 …):
#     Abdul Basit mujawwad 128k -> quran_audio/arabic/abdul-basit-mujawwad-128k/
#     Sudais 192k               -> quran_audio/arabic/sudais-192k/
set -u
ROOT="/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio"
FAIL="$ROOT/failed_voices.txt"; : > "$FAIL"

# $1 = archive.org item, $2 = output dir, $3 = optional comma list of surahs to take (default: all)
archive_item() {
  mkdir -p "$2"
  python3 - "$1" "$2" "${3:-}" > "$2/aria_input.txt" <<'PY'
import json, os, re, sys, urllib.parse, urllib.request
item, outdir, only = sys.argv[1], sys.argv[2], sys.argv[3]
want = {int(s) for s in only.split(",")} if only else set(range(1, 115))
meta = json.load(urllib.request.urlopen(f"https://archive.org/metadata/{item}", timeout=60))
for f in meta.get("files", []):
    name = f["name"]
    m = re.match(r"\s*(\d+)", name)
    if not name.lower().endswith(".mp3") or not m or int(m.group(1)) not in want:
        continue
    out = f"{int(m.group(1)):03d}.mp3"
    if os.path.exists(os.path.join(outdir, out)) and not os.path.exists(os.path.join(outdir, out + ".aria2")):
        continue
    print(f"https://archive.org/download/{item}/{urllib.parse.quote(name)}\n  out={out}")
PY
  aria2c -i "$2/aria_input.txt" -d "$2" -j6 -x8 -s8 -k1M -c --auto-file-renaming=false \
        --file-allocation=none --max-tries=10 --retry-wait=5 \
        --console-log-level=warn --summary-interval=0 || echo "$1: aria2 finished with errors" >> "$FAIL"
}

# $1 = everyayah folder, $2 = output dir
everyayah_set() {
  mkdir -p "$2"
  python3 - "$1" "$2" > "$2.aria_input.txt" <<'PY'
import os, sys
folder, outdir = sys.argv[1], sys.argv[2]
verses = [7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
          112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
          89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
          12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
          30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6]
g = 0
for s, n in enumerate(verses, 1):
    for a in range(1, n + 1):
        g += 1
        out = f"{g:05d}.mp3"
        if os.path.exists(os.path.join(outdir, out)) and not os.path.exists(os.path.join(outdir, out + ".aria2")):
            continue
        print(f"https://everyayah.com/data/{folder}/{s:03d}{a:03d}.mp3\n  out={out}")
PY
  aria2c -i "$2.aria_input.txt" -d "$2" -j16 -x1 -c --auto-file-renaming=false \
        --file-allocation=none --max-tries=10 --retry-wait=5 \
        --console-log-level=warn --summary-interval=0 || echo "$1: aria2 finished with errors" >> "$FAIL"
  rm -f "$2.aria_input.txt"
}

echo "[$(date)] everyayah: Abdul Basit mujawwad, Sudais"
everyayah_set Abdul_Basit_Mujawwad_128kbps "$ROOT/arabic/abdul-basit-mujawwad-128k" &
everyayah_set Abdurrahmaan_As-Sudais_192kbps "$ROOT/arabic/sudais-192k" &
echo "[$(date)] archive.org: Toha, Baezeed"
archive_item Al-QuranArabicToBanglaAudio "$ROOT/bangla/toha-src" &
archive_item alquranwithbanglaaudio "$ROOT/bangla/baezeed-src" 14,29,46,69,104,111 &
wait
archive_item bangla-quran-audio-shareef-baezid "$ROOT/bangla/baezeed-src"  # skips the ones already there
for d in arabic/abdul-basit-mujawwad-128k arabic/sudais-192k bangla/toha-src bangla/baezeed-src; do
  echo "$d: $(ls "$ROOT/$d" | grep -c '\.mp3$') mp3, $(ls "$ROOT/$d" | grep -c '\.aria2$') partial"
done
echo "[$(date)] FINISHED. failures: $(wc -l < "$FAIL" | tr -d ' ')"
