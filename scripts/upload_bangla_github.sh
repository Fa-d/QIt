#!/bin/bash
# Publishes the split Bangla verse files to GitHub Releases so the app can download them per surah.
# Repo: Fa-d/qit-quran-audio (public). GitHub caps a release at 1000 assets, so verses are spread
# over releases bn-1 … bn-7 (bn-k holds ayahs 1000(k-1)+1 … 1000k) plus bn-intro (surah intros).
# The app builds URLs as  https://github.com/Fa-d/qit-quran-audio/releases/download/bn-<k>/<NNNNN>.mp3
# Resumable: assets already on a release are skipped.  Usage: bash scripts/upload_bangla_github.sh
# (no set -u: bash 3.2 treats empty arrays as unbound)
REPO="Fa-d/qit-quran-audio"
SRC="/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio/bangla/bangla-translation-verses"
BATCH=50

if ! gh repo view "$REPO" >/dev/null 2>&1; then
  echo "[$(date)] creating $REPO"
  gh repo create "$REPO" --public \
    --description "Verse-by-verse Bangla Quran translation audio used by the QIt app" || exit 1
  cat > /tmp/qit_readme.md <<'MD'
# QIt Quran audio — Bangla translation, verse by verse

Bangla translation audio (Islamic Foundation translation) for every ayah of the Quran, one MP3 per
verse, used by the QIt Android / Wear OS app.

- `bn-1` … `bn-7` — verse files named by global ayah number (`00001.mp3` … `06236.mp3`);
  release `bn-k` holds ayahs `1000·(k−1)+1 … 1000·k`.
- `bn-intro` — `SSS.mp3`: the basmala and its Bangla translation spoken before verse 1 of surah `SSS`
  (112 surahs; none for 1 and 9).

**Source:** cut from the archive.org item
[AlQuranWithBengaliBanglaTranslation-ReciterMisharyRashidAl-Afasy](https://archive.org/details/AlQuranWithBengaliBanglaTranslation-ReciterMisharyRashidAl-Afasy)
(recitation by Mishary Rashid Alafasy with Bangla translation) by aligning each Arabic verse against
the verse-by-verse Alafasy set and keeping the Bangla between verses. 128 kbps stereo MP3, 44.1 kHz.
MD
  gh api -X PUT "repos/$REPO/contents/README.md" -f message="Add README" \
    -f content="$(base64 < /tmp/qit_readme.md)" >/dev/null || exit 1
fi

ensure_release() { # tag title
  gh release view "$1" -R "$REPO" >/dev/null 2>&1 ||
    gh release create "$1" -R "$REPO" --title "$2" --notes "$2" >/dev/null
}

upload_release() { # tag file...
  local tag="$1"; shift
  local have; have=$(gh release view "$tag" -R "$REPO" --json assets -q '.assets[].name' 2>/dev/null)
  local todo=()
  for f in "$@"; do grep -qxF "$(basename "$f")" <<<"$have" || todo+=("$f"); done
  echo "[$(date)] $tag: ${#todo[@]} of $# to upload"
  local i
  for ((i = 0; i < ${#todo[@]}; i += BATCH)); do
    # Asset uploads hit GitHub's rate limits; back off and retry (the skip list makes retries cheap).
    local tries=0
    until gh release upload "$tag" -R "$REPO" "${todo[@]:i:BATCH}" >/dev/null 2>"/tmp/qit_upload_err.$$"; do
      if grep -q "already_exists" "/tmp/qit_upload_err.$$"; then  # a retry after a partial batch
        upload_release "$tag" "${todo[@]:i:BATCH}"; break
      fi
      tries=$((tries + 1)); [ $tries -ge 12 ] && { echo "$tag: batch at $i failed" >&2; break; }
      echo "[$(date)] $tag: rate limited, retry $tries in $((tries * 60))s"; sleep $((tries * 60))
    done
  done
}

for k in 1 2 3 4 5 6 7; do
  lo=$((1000 * (k - 1) + 1)); hi=$((1000 * k)); [ $hi -gt 6236 ] && hi=6236
  ensure_release "bn-$k" "Bangla verses $lo–$hi"
done
ensure_release "bn-intro" "Bangla surah intros (basmala)"

# One release at a time: parallel uploads only trip the rate limit sooner.
for k in 1 2 3 4 5 6 7; do
  lo=$((1000 * (k - 1) + 1)); hi=$((1000 * k)); [ $hi -gt 6236 ] && hi=6236
  files=(); for n in $(seq $lo $hi); do files+=("$SRC/$(printf %05d $n).mp3"); done
  upload_release "bn-$k" "${files[@]}"
done
upload_release "bn-intro" "$SRC"/intro/*.mp3

echo "[$(date)] verifying"
total=0
for tag in bn-1 bn-2 bn-3 bn-4 bn-5 bn-6 bn-7 bn-intro; do
  c=$(gh release view "$tag" -R "$REPO" --json assets -q '.assets | length'); total=$((total + c))
  echo "$tag: $c assets"
done
echo "total: $total (expected $((6236 + 112)))"
for p in bn-1/00001 bn-1/01000 bn-2/01001 bn-7/06236 bn-intro/002; do
  code=$(curl -sIL -o /dev/null -w '%{http_code}' "https://github.com/$REPO/releases/download/$p.mp3")
  echo "HEAD $p.mp3 -> $code"
done
