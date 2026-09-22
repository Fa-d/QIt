#!/usr/bin/env python3
"""Tests for the pure transform/validate/write functions of build_quran_text.py.

No network: everything here runs against small synthetic editions built from VERSES.
Run from the repo root:  python3 -m unittest scripts/test_build_quran_text.py -v
"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_quran_text as bqt

BOM = "﻿"
BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
BASMALA_WORD = "بِسْمِ"


def synthetic_surahs():
    return [
        {
            "number": i,
            "nameArabic": f"سُورَةُ {i}",
            "nameEnglish": f"Surah {i}",
            "meaningEnglish": f"Meaning {i}",
            "ayahCount": count,
            "revelation": "MECCAN" if i % 2 else "MEDINAN",
        }
        for i, count in enumerate(bqt.VERSES, 1)
    ]


def synthetic_rows():
    rows, g = [], 1
    for s, count in enumerate(bqt.VERSES, 1):
        surah_rows = []
        for n in range(1, count + 1):
            surah_rows.append(
                {"n": n, "g": g, "ar": f"ar{s}:{n}", "en": f"en{s}:{n}", "bn": f"bn{s}:{n}"}
            )
            g += 1
        rows.append(surah_rows)
    return rows


class StripBomTest(unittest.TestCase):
    def test_strips_leading_bom_and_trims(self):
        self.assertEqual(bqt.strip_bom(BOM + " text "), "text")

    def test_text_without_bom_unchanged_but_trimmed(self):
        self.assertEqual(bqt.strip_bom(" plain "), "plain")


class StripBasmalaTest(unittest.TestCase):
    def test_exact_prefix_removed(self):
        self.assertEqual(
            bqt.strip_basmala(BASMALA + " الٓمٓ", BASMALA, surah=2, ayah=1), "الٓمٓ"
        )

    def test_tolerant_fallback_drops_first_four_words(self):
        variant = "بِسْمِ ٱللَّهِ ٱلرَّحْمَنِ ٱلرَّحِيمِ"  # small diacritic difference
        self.assertEqual(
            bqt.strip_basmala(variant + " قٓ وَٱلْقُرْآنِ", BASMALA, surah=50, ayah=1),
            "قٓ وَٱلْقُرْآنِ",
        )

    def test_shadda_variant_removed(self):
        # 95:1 and 97:1 spell the first word with a shadda: "بِّسْمِ"
        variant = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        self.assertEqual(
            bqt.strip_basmala(variant + " وَٱلتِّينِ وَٱلزَّيْتُونِ", BASMALA, surah=95, ayah=1),
            "وَٱلتِّينِ وَٱلزَّيْتُونِ",
        )

    def test_other_opening_words_untouched(self):
        text = "بِٱسْمِ رَبِّكَ ٱلَّذِى خَلَقَ"  # letters differ from the basmala
        self.assertEqual(bqt.strip_basmala(text, BASMALA, surah=96, ayah=1), text)

    def test_surah_1_verse_1_kept_intact(self):
        self.assertEqual(bqt.strip_basmala(BASMALA, BASMALA, surah=1, ayah=1), BASMALA)

    def test_surah_9_untouched(self):
        text = "بَرَآءَةٌ مِّنَ ٱللَّهِ"
        self.assertEqual(bqt.strip_basmala(text, BASMALA, surah=9, ayah=1), text)

    def test_non_first_verse_untouched(self):
        # 27:30 quotes the basmala mid-surah; it must survive untouched
        text = "إِنَّهُ مِن سُلَيْمَٰنَ وَإِنَّهُ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        self.assertEqual(bqt.strip_basmala(text, BASMALA, surah=27, ayah=30), text)

    def test_no_basmala_prefix_unchanged(self):
        self.assertEqual(bqt.strip_basmala("قٓ", BASMALA, surah=2, ayah=1), "قٓ")

    def test_basmala_only_yields_empty_for_later_checks(self):
        self.assertEqual(bqt.strip_basmala(BASMALA, BASMALA, surah=2, ayah=1), "")


class BuildSurahsTest(unittest.TestCase):
    def test_shape_from_meta_references(self):
        surahs = bqt.build_surahs(
            [
                {
                    "number": 1,
                    "name": "سُورَةُ ٱلْفَاتِحَةِ",
                    "englishName": "Al-Faatiha",
                    "englishNameTranslation": "The Opening",
                    "numberOfAyahs": 7,
                    "revelationType": "Meccan",
                },
                {
                    "number": 2,
                    "name": "سُورَةُ البَقَرَةِ",
                    "englishName": "Al-Baqara",
                    "englishNameTranslation": "The Cow",
                    "numberOfAyahs": 286,
                    "revelationType": "Medinan",
                },
            ]
        )
        self.assertEqual(
            surahs[0],
            {
                "number": 1,
                "nameArabic": "سُورَةُ ٱلْفَاتِحَةِ",
                "nameEnglish": "Al-Faatiha",
                "meaningEnglish": "The Opening",
                "ayahCount": 7,
                "revelation": "MECCAN",
            },
        )
        self.assertEqual(surahs[1]["revelation"], "MEDINAN")


class BuildRowsTest(unittest.TestCase):
    @staticmethod
    def editions():
        # surah 1 (2 ayahs) + surah 2 (2 ayahs): enough to exercise basmala stripping
        ar = [
            {"number": 1, "ayahs": [
                {"number": 1, "numberInSurah": 1, "text": BOM + BASMALA},
                {"number": 2, "numberInSurah": 2, "text": " ٱلْحَمْدُ لِلَّهِ "},
            ]},
            {"number": 2, "ayahs": [
                {"number": 3, "numberInSurah": 1, "text": BOM + BASMALA + " الٓمٓ"},
                {"number": 4, "numberInSurah": 2, "text": "ذَٰلِكَ"},
            ]},
        ]
        en = [
            {"number": 1, "ayahs": [
                {"number": 1, "numberInSurah": 1, "text": " In the name of Allah "},
                {"number": 2, "numberInSurah": 2, "text": "All praise is due to Allah"},
            ]},
            {"number": 2, "ayahs": [
                {"number": 3, "numberInSurah": 1, "text": "Alif, Meem"},
                {"number": 4, "numberInSurah": 2, "text": "This is the Book"},
            ]},
        ]
        bn = [
            {"number": 1, "ayahs": [
                {"number": 1, "numberInSurah": 1, "text": "শুরু করছি আল্লাহর নামে"},
                {"number": 2, "numberInSurah": 2, "text": "সকল প্রশংসা"},
            ]},
            {"number": 2, "ayahs": [
                {"number": 3, "numberInSurah": 1, "text": "আলিফ, মীম"},
                {"number": 4, "numberInSurah": 2, "text": "এই কিতাব"},
            ]},
        ]
        return ar, en, bn

    def test_rows_shape_and_cleanup(self):
        rows = bqt.build_rows(*self.editions())
        self.assertEqual(rows[0][0], {
            "n": 1, "g": 1, "ar": BASMALA, "en": "In the name of Allah",
            "bn": "শুরু করছি আল্লাহর নামে",
        })
        self.assertEqual(rows[0][1]["ar"], "ٱلْحَمْدُ لِلَّهِ")
        # surah 2 verse 1: BOM and basmala stripped, translations left alone
        self.assertEqual(rows[1][0], {
            "n": 1, "g": 3, "ar": "الٓمٓ", "en": "Alif, Meem", "bn": "আলিফ, মীম",
        })
        self.assertEqual(rows[1][1]["g"], 4)

    def test_editions_out_of_sync_rejected(self):
        ar, en, bn = self.editions()
        en[1]["ayahs"][0]["number"] = 99  # global number disagrees with the arabic edition
        with self.assertRaisesRegex(ValueError, "out of sync"):
            bqt.build_rows(ar, en, bn)


class ValidateTest(unittest.TestCase):
    def test_valid_data_passes(self):
        bqt.validate(synthetic_surahs(), synthetic_rows())

    def test_wrong_surah_count_rejected(self):
        rows = synthetic_rows()
        rows.pop()
        with self.assertRaisesRegex(ValueError, "114"):
            bqt.validate(synthetic_surahs()[:-1], rows)

    def test_wrong_ayah_count_rejected(self):
        rows = synthetic_rows()
        rows[0].pop()
        with self.assertRaisesRegex(ValueError, "surah 1"):
            bqt.validate(synthetic_surahs(), rows)

    def test_meta_count_disagreeing_with_editions_rejected(self):
        surahs = synthetic_surahs()
        surahs[0]["ayahCount"] = 6
        with self.assertRaisesRegex(ValueError, "surah 1"):
            bqt.validate(surahs, synthetic_rows())

    def test_global_number_gap_rejected(self):
        rows = synthetic_rows()
        rows[1][0]["g"] += 1  # hole after surah 1's last ayah
        with self.assertRaisesRegex(ValueError, "global"):
            bqt.validate(synthetic_surahs(), rows)

    def test_empty_text_rejected(self):
        rows = synthetic_rows()
        rows[113][2]["bn"] = ""
        with self.assertRaisesRegex(ValueError, "empty bn"):
            bqt.validate(synthetic_surahs(), rows)


class CheckBasmalaStrippedTest(unittest.TestCase):
    def test_clean_rows_pass(self):
        rows = synthetic_rows()
        rows[0][0]["ar"] = BASMALA          # 1:1 keeps it
        rows[1][0]["ar"] = "الٓمٓ"           # 2:1 stripped
        rows[8][0]["ar"] = "بَرَآءَةٌ"        # 9:1 never had it
        bqt.check_basmala_stripped(rows)

    def test_remaining_basmala_prefix_rejected(self):
        rows = synthetic_rows()
        rows[2][0]["ar"] = BASMALA + " الم"
        with self.assertRaisesRegex(ValueError, "surah 3"):
            bqt.check_basmala_stripped(rows)

    def test_remaining_shadda_variant_rejected(self):
        rows = synthetic_rows()
        rows[0][0]["ar"] = BASMALA
        rows[96][0]["ar"] = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ إِنَّآ أَنزَلْنَٰهُ"
        with self.assertRaisesRegex(ValueError, "surah 97"):
            bqt.check_basmala_stripped(rows)

    def test_empty_first_verse_rejected(self):
        rows = synthetic_rows()
        rows[4][0]["ar"] = ""
        with self.assertRaisesRegex(ValueError, "surah 5"):
            bqt.check_basmala_stripped(rows)


class WriteAssetsTest(unittest.TestCase):
    def test_writes_compact_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            bqt.write_assets(tmp, synthetic_surahs(), synthetic_rows())
            qdir = os.path.join(tmp, "quran")
            self.assertTrue(os.path.isfile(os.path.join(qdir, "surahs.json")))
            files = sorted(os.listdir(os.path.join(qdir, "text")))
            self.assertEqual(len(files), 114)
            self.assertEqual(files[0], "001.json")
            self.assertEqual(files[-1], "114.json")
            with open(os.path.join(qdir, "surahs.json"), encoding="utf-8") as f:
                raw = f.read()
            self.assertEqual(raw, json.dumps(json.loads(raw), ensure_ascii=False,
                                             separators=(",", ":")))
            with open(os.path.join(qdir, "text", "001.json"), encoding="utf-8") as f:
                first = json.load(f)
            self.assertEqual(len(first), bqt.VERSES[0])
            self.assertEqual(set(first[0]), {"n", "g", "ar", "en", "bn"})


if __name__ == "__main__":
    unittest.main()
