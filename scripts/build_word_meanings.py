#!/usr/bin/env python3
"""Builds the word-by-word meaning assets for :core:data from the quran.com API v4.

Writes (compact UTF-8 JSON):
  core/data/src/main/assets/quran/words/en/001..114.json - English word meanings
  core/data/src/main/assets/quran/words/bn/001..114.json - Bengali word meanings
      per surah: [[ayah, ["meaning0", "meaning1", ...]], ...] - one meaning per display word of the
      ayah's Arabic text (as the app's ArabicWords splits it), in word order. Surah 1's ayah 1 is
      the basmala; the basmala before other surahs' verse 1 is 1:1, which the app reads at runtime,
      so no surah has an ayah 0 row.

Source: quran.com API v4 (Quran.com / Quran Foundation, https://api.quran.com), word-by-word
translations: GET /verses/by_chapter/{surah}?words=true&language={en|bn}&word_fields=text_uthmani
&per_page=50&page={n}, paginating through pagination.next_page. Each verse's words with
char_type_name "word" (the "end" word is the ayah-number glyph) carry word.translation.text - the
meaning in the request's language.

Alignment: quran.com splits the same Uthmani text the app bundles, pause marks attached to the word
before them, but not identically everywhere. Ayahs whose word counts agree are zipped; the rest are
aligned by letter skeleton (skeleton() from build_quran_text.py, over both texts normalised: the
alef variants ٱأإآ and the dagger alef - however carried, for the sources spell it on ى, on tatweel
or alone - become ا, a hamza above becomes ء, and small waw/yeh, pause marks, tatweel and bidi
control marks go), walking both word lists and matching each skeleton fragment as far as it goes:
several quran.com words making one display word join their meanings with a space, and one
quran.com word spanning several display words repeats its meaning on each of them. Ayahs that still
cannot be aligned, or whose words come back with no meaning, are named and fail the build unless
WORD_GROUPS pins their grouping by hand.

Usage (from the repo root):
  python3 scripts/build_word_meanings.py [--cache DIR]
Raw responses are cached under --cache (default ~/.cache/qit-word-meanings), so reruns don't refetch.
Tests: python3 -m unittest scripts/test_build_word_meanings.py -v
"""
import argparse
import json
import os
import sys
import time
import unicodedata
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_audio_timing import PAUSE_MARKS, VERSES, display_words
from build_quran_text import skeleton

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_DIR = os.path.join(REPO, "core", "data", "src", "main", "assets", "quran")

API = "https://api.quran.com/api/v4"
USER_AGENT = "Qandeel-word-meanings-builder/1.0 (offline asset build for the Qandeel Quran app)"
LANGS = ("en", "bn")
DEFAULT_CACHE = os.path.join(os.path.expanduser("~"), ".cache", "qit-word-meanings")

REQUEST_PAUSE_S = 0.2
RETRIES = 3
BACKOFF_S = 2

ALEF = "ا"
ALEF_VARIANTS = "ٱأإآ"  # the sources disagree on hamza carriers (80:25 أ vs ا) and wasla
DAGGER_ALEF = "ٰ"  # however carried, it is one long a: ours rides it on ى or alone, quran.com on tatweel
HAMZA_ABOVE = "ٔ"  # quran.com writes ء as tatweel + this mark (alquran.cloud as the letter ء)
TATWEEL = "ـ"
SMALL_LETTERS = "ۥۦ"  # small waw/yeh as letters; quran.com marks the same sound with U+06E7, a combining mark
LONG_A = (("ىٰ", ALEF), ("اٰ", ALEF))  # a carrier letter + dagger alef are one long a, not two
HAMZA_LONG_A = (("ٰٔ", "ءا"), ("ٰٔ", "ءا"))  # hamza above + dagger alef, in either mark order

# Ayahs the skeleton alignment can't group (none so far): display word index -> quran.com word
# indices, the grouping the meanings are joined by.
WORD_GROUPS = {}


def word_skeleton(text):
    """The letters of [text] as alignment compares them (see module docstring)."""
    plain = unicodedata.normalize("NFC", text).replace(TATWEEL, "")  # NFC first: ا + ٓ composes to آ, a letter
    for cluster, letters in LONG_A + HAMZA_LONG_A:
        plain = plain.replace(cluster, letters)
    plain = "".join(ALEF if ch in ALEF_VARIANTS or ch == DAGGER_ALEF else "ء" if ch == HAMZA_ABOVE else ch
                    for ch in plain)
    plain = "".join(ch for ch in plain
                    if not ch.isspace() and unicodedata.category(ch) != "Cf"
                    and ch not in PAUSE_MARKS and ch not in SMALL_LETTERS)
    return skeleton(plain)


def group_words(key, display, qc):
    """For each display word, the quran.com words (by index) it covers.

    Walks both word lists over their skeletons, matching each fragment as far as it goes: a
    quran.com word that ends inside a display word feeds it, and a display word that ends inside a
    quran.com word takes that word (and the next, and so on, until it is covered). Word counts that
    agree therefore zip; elsewhere the merge falls where the skeletons say it does. Raises
    ValueError when the letter streams part ways or words are left over on either side.
    """
    d_skels = [word_skeleton(word) for word in display]
    q_skels = [word_skeleton(word) for word, _ in qc]
    groups = [[] for _ in display]
    i, j = 0, 0
    d_rest = d_skels[0] if d_skels else ""
    q_rest = q_skels[0] if q_skels else ""
    while i < len(d_skels) or j < len(q_skels):
        if q_rest == "" and j < len(q_skels):
            host = i if i < len(d_skels) else len(d_skels) - 1
            if host < 0:
                raise ValueError(f"{key}: words left over at display word {i} / quran.com word {j}")
            groups[host].append(j)  # a quran.com word of bare marks rides on the word being covered
            j += 1
            q_rest = q_skels[j] if j < len(q_skels) else ""
            continue
        if not d_rest or not q_rest:
            raise ValueError(f"{key}: words left over at display word {i} / quran.com word {j}")
        if d_rest == q_rest:
            groups[i].append(j)
            i, j = i + 1, j + 1
            d_rest = d_skels[i] if i < len(d_skels) else ""
            q_rest = q_skels[j] if j < len(q_skels) else ""
        elif len(d_rest) < len(q_rest) and q_rest.startswith(d_rest):
            groups[i].append(j)  # the display word ends inside the quran.com word
            i += 1
            q_rest = q_rest[len(d_rest):]
            d_rest = d_skels[i] if i < len(d_skels) else ""
        elif d_rest.startswith(q_rest):
            groups[i].append(j)  # the quran.com word ends inside the display word
            j += 1
            d_rest = d_rest[len(q_rest):]
            q_rest = q_skels[j] if j < len(q_skels) else ""
        else:
            raise ValueError(f"{key}: skeletons part ways at display word {i} / quran.com word {j}")
    return groups


def join_meanings(key, display_count, groups, qc):
    """The meanings of [groups] (one group of quran.com word indices per display word); a group of
    several joins with a space, and a quran.com word in several groups repeats on each."""
    if len(groups) != display_count:
        raise ValueError(f"{key}: {len(groups)} word groups for {display_count} display words")
    meanings = []
    for word, indices in enumerate(groups):
        if any(index >= len(qc) for index in indices):
            raise ValueError(f"{key}: word {word}'s group {indices} is beyond the {len(qc)} quran.com words")
        meaning = " ".join(m for m in (qc[index][1].strip() for index in indices) if m)
        if not meaning:
            raise ValueError(f"{key}: word {word} has no meaning")
        meanings.append(meaning)
    return meanings


def assign_meanings(key, display, qc):
    """One meaning per display word, from the override table when it has this ayah (see docstring)."""
    groups = WORD_GROUPS[key] if key in WORD_GROUPS else group_words(key, display, qc)
    return join_meanings(key, len(display), groups, qc)


def verse_words(verse):
    """One API verse -> [(text, meaning), ...] for its real words (the ayah-number glyph dropped)."""
    return [
        (word["text_uthmani"], (word["translation"]["text"] or "").strip())
        for word in verse["words"]
        if word["char_type_name"] == "word"
    ]


def collect_words(surah, pages):
    """One surah's API pages -> {ayah number: [(text, meaning), ...]}."""
    words = {}
    for payload in pages:
        for verse in payload["verses"]:
            ayah = int(verse["verse_key"].split(":")[1])
            if ayah in words:
                raise ValueError(f"surah {surah}: verse {verse['verse_key']} appears twice")
            words[ayah] = verse_words(verse)
    return words


def build_rows(surah, text_rows, words):
    """One surah's text rows and its languages' words -> {lang: [[ayah, [meanings]], ...]}, plus the
    set of ayah keys whose words needed aligning (counts disagreed or an override applied)."""
    per_lang, aligned = {}, set()
    for lang in LANGS:
        rows = []
        for row in text_rows:
            key = (surah, row["n"])
            if key not in words[lang]:
                raise ValueError(f"{key}: no {lang} verse from the API")
            display = display_words(row["ar"])
            qc = words[lang][key]
            if key in WORD_GROUPS or len(display) != len(qc):
                aligned.add(key)
            rows.append([row["n"], assign_meanings(key, display, qc)])
        per_lang[lang] = rows
    return per_lang, aligned


def validate(per_lang, text_rows):
    """Fail loudly on anything that would corrupt the app's data."""
    for lang in LANGS:
        rows = per_lang[lang]
        if len(rows) != len(text_rows):
            raise ValueError(f"expected {len(text_rows)} {lang} rows, got {len(rows)}")
        for row, text_row in zip(rows, text_rows):
            key = f"{text_row['surah']}:{text_row['n']}"
            if row[0] != text_row["n"]:
                raise ValueError(f"{key}: {lang} row out of order ({row[0]})")
            expected = len(display_words(text_row["ar"]))
            if len(row[1]) != expected:
                raise ValueError(f"{key}: {len(row[1])} {lang} meanings for {expected} words")
            for word, meaning in enumerate(row[1]):
                if not meaning:
                    raise ValueError(f"{key}: word {word} has an empty {lang} meaning")


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def write_assets(out_root, per_surah):
    for lang in LANGS:
        lang_dir = os.path.join(out_root, "words", lang)
        os.makedirs(lang_dir, exist_ok=True)
        for surah, per_lang in enumerate(per_surah, 1):
            with open(os.path.join(lang_dir, f"{surah:03d}.json"), "w", encoding="utf-8") as f:
                f.write(dump(per_lang[lang]))


def read_text_rows():
    """The bundled text assets -> per-surah [{surah, n, ar}, ...] in ayah order."""
    per_surah = []
    for surah, count in enumerate(VERSES, 1):
        with open(os.path.join(ASSETS_DIR, "text", f"{surah:03d}.json"), encoding="utf-8") as f:
            rows = [{"surah": surah, "n": row["n"], "ar": row["ar"]} for row in json.load(f)]
        if [row["n"] for row in rows] != list(range(1, count + 1)):
            raise ValueError(f"surah {surah}: text rows are not ayahs 1..{count}")
        per_surah.append(rows)
    return per_surah


def fetch_page(lang, surah, page, cache_dir):
    """One API page, from the disk cache when possible, else fetched politely and cached."""
    cache_path = os.path.join(cache_dir, f"{lang}-{surah:03d}-{page:02d}.json")
    if os.path.exists(cache_path):
        try:
            with open(cache_path, encoding="utf-8") as f:
                return json.load(f)
        except json.JSONDecodeError:
            pass  # a truncated write: fetch the page again
    url = (f"{API}/verses/by_chapter/{surah}?words=true&language={lang}"
           f"&word_fields=text_uthmani&per_page=50&page={page}")
    for attempt in range(RETRIES + 1):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=60) as response:
                payload = json.loads(response.read().decode("utf-8"))
            break
        except (urllib.error.URLError, json.JSONDecodeError) as e:
            if attempt == RETRIES:
                raise ValueError(f"{url}: {e}") from e
            time.sleep(BACKOFF_S * 2 ** attempt)
    time.sleep(REQUEST_PAUSE_S)
    with open(cache_path, "w", encoding="utf-8") as f:
        json.dump(payload, f)
    return payload


def fetch_words(cache_dir):
    """{(surah, ayah): [(text, meaning), ...]} per language for the whole Quran, via the cache."""
    words = {}
    for lang in LANGS:
        verses = {}
        for surah, count in enumerate(VERSES, 1):
            pages, page = [], 1
            while page is not None:
                payload = fetch_page(lang, surah, page, cache_dir)
                pages.append(payload)
                page = payload["pagination"]["next_page"]
            if len(pages) != (count + 49) // 50:
                raise ValueError(f"surah {surah}: {len(pages)} {lang} pages for {count} ayahs")
            verses.update({(surah, ayah): words for ayah, words in collect_words(surah, pages).items()})
        if len(verses) != sum(VERSES):
            raise ValueError(f"{lang}: expected {sum(VERSES)} verses, got {len(verses)}")
        words[lang] = verses
    return words


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--cache", default=DEFAULT_CACHE, help="directory for cached API responses")
    args = parser.parse_args()
    os.makedirs(args.cache, exist_ok=True)

    print(f"Fetching quran.com word translations ({', '.join(LANGS)}; cache {args.cache})…")
    words = fetch_words(args.cache)
    per_surah, aligned = [], set()
    for surah, text_rows in enumerate(read_text_rows(), 1):
        per_lang, surah_aligned = build_rows(surah, text_rows, words)
        validate(per_lang, text_rows)
        per_surah.append(per_lang)
        aligned |= surah_aligned
    write_assets(ASSETS_DIR, per_surah)
    print(f"Wrote {len(LANGS)} languages x 114 surahs, {sum(VERSES)} ayahs each, "
          f"to {os.path.join(ASSETS_DIR, 'words')}")
    overridden = sorted(key for key in WORD_GROUPS if key in aligned)
    print(f"{len(aligned)} ayahs aligned beyond a plain zip "
          f"({len(overridden)} via WORD_GROUPS): "
          f"{', '.join(f'{surah}:{ayah}' for surah, ayah in sorted(aligned)) or 'none'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
