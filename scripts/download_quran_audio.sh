#!/bin/bash
# Downloads the full Quran audio (Arabic recitation, English translation, Bangla translation)
# Resumable: completed files are skipped; partial .part files are resumed.
set -u
ROOT="/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio"
AR="$ROOT/arabic/alafasy-128k"
EN="$ROOT/english/saheeh-intl-walk-192k"
BN="$ROOT/bangla/alafasy-bangla-translation"
mkdir -p "$AR" "$EN" "$BN"
FAIL="$ROOT/failed.txt"; : > "$FAIL"

dl() { # $1=url $2=outfile
  local url="$1" out="$2" tmp="${2}.part"
  if [ -f "$out" ]; then return 0; fi
  if curl -fsSL --retry 6 --retry-delay 3 --retry-all-errors -C - \
       --connect-timeout 20 --max-time 600 -o "$tmp" "$url"; then
    mv -f "$tmp" "$out"
  else
    rm -f "$tmp"; echo "$url" >> "$FAIL"
  fi
}
export -f dl; export ROOT FAIL

echo "[$(date)] Arabic: Alafasy 128k, verses 1..6236"
seq 1 6236 | xargs -P 8 -I{} bash -c 'dl "https://cdn.islamic.network/quran/audio/128/ar.alafasy/{}.mp3" "$ROOT/arabic/alafasy-128k/$(printf %05d {}).mp3"'
echo "[$(date)] Arabic done: $(ls "$AR" | wc -l | tr -d ' ') files"

echo "[$(date)] English: Saheeh Intl by Ibrahim Walk 192k, verses 1..6236"
seq 1 6236 | xargs -P 8 -I{} bash -c 'dl "https://cdn.islamic.network/quran/audio/192/en.walk/{}.mp3" "$ROOT/english/saheeh-intl-walk-192k/$(printf %05d {}).mp3"'
echo "[$(date)] English done: $(ls "$EN" | wc -l | tr -d ' ') files"

echo "[$(date)] Bangla: Alafasy + Bangla translation (archive.org, 114 surahs)"
ITEM="AlQuranWithBengaliBanglaTranslation-ReciterMisharyRashidAl-Afasy"
python3 - "$ITEM" "$BN" > "$ROOT/bangla_manifest.tsv" <<'PY'
import json, sys, urllib.parse, urllib.request
item, outdir = sys.argv[1], sys.argv[2]
meta = json.load(urllib.request.urlopen(f"https://archive.org/metadata/{item}", timeout=60))
for f in meta.get("files", []):
    n = f["name"]
    if n.lower().endswith(".mp3"):
        print(f"https://archive.org/download/{item}/{urllib.parse.quote(n)}\t{outdir}/{n}")
PY
python3 - "$ROOT" <<'PY'
import sys
root = sys.argv[1]
lines = open(f"{root}/bangla_manifest.tsv").read().strip().split("\n")
open(f"{root}/bangla_aria_input.txt", "w").write(
    "\n".join(f"{u}\n  out={p.split('/')[-1]}" for u, p in (l.split("\t") for l in lines)) + "\n")
PY
if command -v aria2c >/dev/null 2>&1; then
  aria2c -i "$ROOT/bangla_aria_input.txt" -d "$BN" -j3 -x16 -s16 -k1M -c \
        --file-allocation=none --max-tries=10 --retry-wait=5 \
        --console-log-level=warn --summary-interval=0 || echo "bangla aria2 finished with errors" >> "$FAIL"
else
  p=0
  while IFS=$'\t' read -r url out; do
    dl "$url" "$out" & p=$((p+1)); [ $((p%3)) -eq 0 ] && wait
  done < "$ROOT/bangla_manifest.tsv"
  wait
fi
echo "[$(date)] Bangla done: $(ls "$BN" | wc -l | tr -d ' ') files"
echo "[$(date)] FINISHED. failures: $(wc -l < "$FAIL" | tr -d ' ')"
