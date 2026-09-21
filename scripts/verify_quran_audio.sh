#!/bin/bash
# Verifies the downloaded Quran audio set: counts, sizes, corrupt/empty files.
ROOT="/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio"
AR="$ROOT/arabic/alafasy-128k"; EN="$ROOT/english/saheeh-intl-walk-192k"; BN="$ROOT/bangla/alafasy-bangla-translation"

check() { # dir expected_count label
  local d="$1" want="$2" label="$3"
  local have; have=$(ls "$d" 2>/dev/null | grep -c '\.mp3$' || true)
  local empty; empty=$(find "$d" -name '*.mp3' -size -2k 2>/dev/null | wc -l | tr -d ' ')
  local part; part=$(find "$d" -name '*.part' 2>/dev/null | wc -l | tr -d ' ')
  local sz; sz=$(du -sh "$d" 2>/dev/null | cut -f1)
  local miss=0
  if [ "$label" != "Bangla" ]; then
    for i in $(seq 1 6236); do f=$(printf "%s/%05d.mp3" "$d" "$i"); [ -f "$f" ] || miss=$((miss+1)); done
  fi
  echo "$label: $have/$want mp3s | missing=$miss | empty(<2KB)=$empty | partial=$part | size=$sz"
}

check "$AR" 6236 "Arabic"
check "$EN" 6236 "English"
check "$BN" 114   "Bangla"
check "$ROOT/bangla/bangla-translation-verses" 6236 "Bangla verses"
[ -s "$ROOT/failed.txt" ] && { echo "failed downloads:"; cat "$ROOT/failed.txt"; } || echo "no failed downloads recorded"
# Bangla sha1 verification against archive.org metadata
python3 - "$BN" <<'PY'
import hashlib, json, os, sys, urllib.request
d = sys.argv[1]
meta = json.load(urllib.request.urlopen(
    "https://archive.org/metadata/AlQuranWithBengaliBanglaTranslation-ReciterMisharyRashidAl-Afasy", timeout=60))
bad = ok = 0
for f in meta.get("files", []):
    if not f["name"].lower().endswith(".mp3"): continue
    p = os.path.join(d, f["name"])
    if not os.path.exists(p): print("MISSING:", f["name"]); bad += 1; continue
    h = hashlib.sha1(open(p, "rb").read()).hexdigest()
    if h == f.get("sha1"): ok += 1
    else: print("SHA1 MISMATCH:", f["name"]); bad += 1
print(f"Bangla sha1: {ok} ok, {bad} bad")
PY
