#!/usr/bin/env python3
"""Builds the Quran text assets for :core:data from the alquran.cloud API.

Writes (compact UTF-8 JSON):
  core/data/src/main/assets/quran/surahs.json         - per-surah metadata
  core/data/src/main/assets/quran/text/001..114.json  - per-surah verse rows
                                                          {"n","g","ar","en","bn"}

The Arabic edition (quran-uthmani) prefixes verse 1 of every surah except 1 and 9
with the basmala; the app shows/plays it separately, so it is stripped here
(the basmala is the BOM-stripped text of 1:1, which itself is kept intact).
Translations are only BOM-stripped and trimmed.

Network fetching is kept separate from the pure transform/validate/write
functions (see scripts/test_build_quran_text.py).
"""
import json
import os
import unicodedata
import urllib.request

BASE = "https://api.alquran.cloud/v1"
ASSETS_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "core", "data", "src", "main", "assets",
)

# Canonical per-surah ayah counts (copied from scripts/split_bangla_verses.py).
VERSES = [7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
          112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
          89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
          12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
          30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6]
assert len(VERSES) == 114 and sum(VERSES) == 6236

BOM = "﻿"
BASMALA_WORD = "بِسْمِ"
LANGS = ("ar", "en", "bn")


def strip_bom(text):
    """Remove a leading BOM and surrounding whitespace."""
    return (text[1:] if text.startswith(BOM) else text).strip()


def skeleton(text):
    """The letters of [text] without harakat, shadda or other combining marks, for tolerant comparison."""
    return "".join(ch for ch in unicodedata.normalize("NFC", text) if not unicodedata.category(ch).startswith("M"))


def starts_with_basmala(text, basmala):
    """Whether [text] opens with the basmala's four words, whatever their diacritics.

    quran-uthmani spells the basmala differently in places (95:1 and 97:1 read "بِّسْمِ", with a
    shadda), so comparing letters only is what catches every variant.
    """
    expected = [skeleton(word) for word in basmala.split()]
    return [skeleton(word) for word in text.split()[:len(expected)]] == expected


def strip_basmala(text, basmala, surah, ayah):
    """Strip the basmala prefix from a surah-opening verse of quran-uthmani.

    Surah 1's opening is the basmala itself and surah 9 has none, so both stay
    untouched; only verse 1 is considered. Falls back to dropping the first
    four words when an exact prefix match fails on diacritic differences.
    """
    if surah in (1, 9) or ayah != 1:
        return text
    if text.startswith(basmala):
        return text[len(basmala):].strip()
    if starts_with_basmala(text, basmala):
        return " ".join(text.split()[len(basmala.split()):]).strip()
    return text


def build_surahs(references):
    """meta.surahs.references[] -> surahs.json rows."""
    return [
        {
            "number": ref["number"],
            "nameArabic": ref["name"],
            "nameEnglish": ref["englishName"],
            "meaningEnglish": ref["englishNameTranslation"],
            "ayahCount": ref["numberOfAyahs"],
            "revelation": ref["revelationType"].upper(),
        }
        for ref in references
    ]


def build_rows(ar_surahs, en_surahs, bn_surahs):
    """Three full-edition payloads -> per-surah lists of {"n","g","ar","en","bn"} rows."""
    counts = (len(ar_surahs), len(en_surahs), len(bn_surahs))
    if len(set(counts)) != 1:
        raise ValueError(f"editions disagree on surah count: ar/en/bn = {counts}")
    basmala = strip_bom(ar_surahs[0]["ayahs"][0]["text"])
    per_surah = []
    for ar_s, en_s, bn_s in zip(ar_surahs, en_surahs, bn_surahs):
        if not ar_s["number"] == en_s["number"] == bn_s["number"]:
            raise ValueError(f"surah numbers out of sync: {ar_s['number']}/{en_s['number']}/{bn_s['number']}")
        rows = []
        for ar_a, en_a, bn_a in zip(ar_s["ayahs"], en_s["ayahs"], bn_s["ayahs"]):
            if not ar_a["number"] == en_a["number"] == bn_a["number"]:
                raise ValueError(f"surah {ar_s['number']} ayah {ar_a['numberInSurah']}: "
                                 f"global numbers out of sync: "
                                 f"{ar_a['number']}/{en_a['number']}/{bn_a['number']}")
            if not ar_a["numberInSurah"] == en_a["numberInSurah"] == bn_a["numberInSurah"]:
                raise ValueError(f"surah {ar_s['number']}: ayah-in-surah numbers out of sync")
            rows.append({
                "n": ar_a["numberInSurah"],
                "g": ar_a["number"],
                "ar": strip_basmala(strip_bom(ar_a["text"]), basmala,
                                    ar_s["number"], ar_a["numberInSurah"]),
                "en": strip_bom(en_a["text"]),
                "bn": strip_bom(bn_a["text"]),
            })
        per_surah.append(rows)
    return per_surah


def validate(surahs, per_surah_rows):
    """Fail loudly on anything that would corrupt the app's data."""
    if len(surahs) != 114:
        raise ValueError(f"expected 114 surahs, got {len(surahs)}")
    if len(per_surah_rows) != 114:
        raise ValueError(f"expected 114 surah row lists, got {len(per_surah_rows)}")
    total, g = 0, 1
    for i, (meta, rows) in enumerate(zip(surahs, per_surah_rows), 1):
        expected = VERSES[i - 1]
        if len(rows) != expected:
            raise ValueError(f"surah {i}: expected {expected} ayahs, got {len(rows)}")
        if meta["number"] != i:
            raise ValueError(f"surah {i}: metadata numbered {meta['number']}")
        if meta["ayahCount"] != expected:
            raise ValueError(f"surah {i}: metadata ayahCount {meta['ayahCount']} "
                             f"!= expected {expected}")
        total += len(rows)
        for row in rows:
            if row["g"] != g:
                raise ValueError(f"surah {i} ayah {row['n']}: expected global number "
                                 f"{g}, got {row['g']}")
            g += 1
            for lang in LANGS:
                if not row[lang]:
                    raise ValueError(f"surah {i} ayah {row['n']}: empty {lang} text")
    if total != 6236:
        raise ValueError(f"expected 6236 ayahs total, got {total}")


def check_basmala_stripped(per_surah_rows):
    """Post-condition: verse 1 of surahs 2-114 (except 9) carries no basmala and is non-empty."""
    basmala = per_surah_rows[0][0]["ar"]
    for i, rows in enumerate(per_surah_rows, 1):
        if i in (1, 9):
            continue
        text = rows[0]["ar"]
        if not text:
            raise ValueError(f"surah {i}: verse 1 empty after basmala strip")
        if text.startswith(BASMALA_WORD) or starts_with_basmala(text, basmala):
            raise ValueError(f"surah {i}: verse 1 still starts with the basmala: {text[:30]!r}")


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def write_assets(out_root, surahs, per_surah_rows):
    text_dir = os.path.join(out_root, "quran", "text")
    os.makedirs(text_dir, exist_ok=True)
    with open(os.path.join(out_root, "quran", "surahs.json"), "w", encoding="utf-8") as f:
        f.write(dump(surahs))
    for i, rows in enumerate(per_surah_rows, 1):
        with open(os.path.join(text_dir, f"{i:03d}.json"), "w", encoding="utf-8") as f:
            f.write(dump(rows))


def api_get(path):
    url = BASE + path
    with urllib.request.urlopen(url, timeout=120) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    if payload.get("code") != 200:
        raise ValueError(f"{url}: API error {payload.get('code')} {payload.get('status')}")
    return payload["data"]


def main():
    print("Fetching alquran.cloud metadata and editions (quran-uthmani, en.sahih, bn.bengali)…")
    meta = api_get("/meta")["surahs"]["references"]
    editions = [api_get(f"/quran/{ed}")["surahs"]
                for ed in ("quran-uthmani", "en.sahih", "bn.bengali")]
    surahs = build_surahs(meta)
    per_surah_rows = build_rows(*editions)
    validate(surahs, per_surah_rows)
    check_basmala_stripped(per_surah_rows)
    write_assets(ASSETS_DIR, surahs, per_surah_rows)
    print(f"Wrote {len(surahs)} surahs and {sum(len(r) for r in per_surah_rows)} ayahs "
          f"to {os.path.join(ASSETS_DIR, 'quran')}")


if __name__ == "__main__":
    main()
