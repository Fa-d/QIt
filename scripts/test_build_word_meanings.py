#!/usr/bin/env python3
"""Tests for the pure functions of build_word_meanings.py. No network, no bundled assets.

Run from the repo root:  python3 -m unittest scripts/test_build_word_meanings.py -v
"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_audio_timing as bat
import build_word_meanings as bwm

# 2:2 - two standalone pause marks between the words (they belong to the word before them).
BAQARAH_2 = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"
BAQARAH_2_WORDS = bat.display_words(BAQARAH_2)
# The same ayah as quran.com returns it: pause marks ride inside the word strings.
BAQARAH_2_QC = ["ذَٰلِكَ", "ٱلْكِتَٰبُ", "لَا", "رَيْبَ ۛ", "فِيهِ ۛ", "هُدًى", "لِّلْمُتَّقِينَ"]


class WordSkeletonTest(unittest.TestCase):
    def test_harakat_and_small_marks_are_dropped(self):
        self.assertEqual(bwm.word_skeleton("ٱلْحَمْدُ"), "الحمد")

    def test_alef_wasla_counts_as_alef(self):
        self.assertEqual(bwm.word_skeleton("ٱلرَّحْمَـٰنِ"), bwm.word_skeleton("الرَّحْمَٰنِ"))

    def test_pause_marks_and_tatweel_are_dropped(self):
        self.assertEqual(bwm.word_skeleton("وُسْعَهَا ۚ"), bwm.word_skeleton("وُسْعَهَا"))
        self.assertEqual(bwm.word_skeleton("الرحـمـن"), "الرحمن")

    def test_the_sources_long_a_spellings_agree(self):
        # ours writes the long a of these words differently than quran.com (11:13, 12:39)
        self.assertEqual(bwm.word_skeleton("ٱفْتَرَىٰهُ"), bwm.word_skeleton("افْتَرَاهُ"))
        self.assertEqual(bwm.word_skeleton("يَٰصَىٰحِبَىِ"), bwm.word_skeleton("يَـٰصَـٰحِبَىِ"))

    def test_hamza_written_as_a_letter_or_as_a_mark_agrees(self):
        # ours has the letter ء where quran.com has tatweel + hamza above (2:4)
        self.assertEqual(bwm.word_skeleton("وَبِٱلْءَاخِرَةِ"), bwm.word_skeleton("وَبِٱلْـَٔاخِرَةِ"))

    def test_hamza_and_dagger_alef_agree_in_either_mark_order(self):
        # the sources order the two marks differently once NFC reorders them (2:71, 7:161)
        self.assertEqual(bwm.word_skeleton("ٱلْـَٰٔنَ"), bwm.word_skeleton("ٱلْـَٔـٰنَ"))
        self.assertEqual(bwm.word_skeleton("خَطِيٓـَٰٔتِكُمْ"), bwm.word_skeleton("خَطِيٓـَٔـٰتِكُمْ"))

    def test_small_yeh_written_as_a_letter_or_as_a_mark_agrees(self):
        # ours has U+06E6 (a letter) where quran.com has tatweel + U+06E7 (a mark) (2:61)
        self.assertEqual(bwm.word_skeleton("ٱلنَّبِيِّۦنَ"), bwm.word_skeleton("ٱلنَّبِيِّـۧنَ"))

    def test_alef_hamza_variants_and_control_marks_agree(self):
        self.assertEqual(bwm.word_skeleton("أَنَّا"), bwm.word_skeleton("اَنَّا"))  # 80:25
        self.assertEqual(bwm.word_skeleton("سَآئِلٌ"), bwm.word_skeleton("سَآئِلٌ"))  # 70:1, madda composes
        self.assertEqual(bwm.word_skeleton("ٱلْعَظِيمِ ۩\u200f"), "العظيم")  # 27:26, sajda mark + RLM


class GroupWordsTest(unittest.TestCase):
    def test_equal_counts_zip(self):
        groups = bwm.group_words("2:2", BAQARAH_2_WORDS, list(zip(BAQARAH_2_QC, "abcdefg")))
        self.assertEqual(groups, [[i] for i in range(7)])

    def test_two_quran_com_words_merge_into_one_display_word(self):
        display = ["قُلْ", "الْحَمِيدُ"]
        qc = [("قُلْ", "say"), ("الْحَمِيدُ", "praiseworthy"), ("ۙ", "a bare mark")]  # the mark is its own word
        groups = bwm.group_words("1:1", display, qc)
        self.assertEqual(groups, [[0], [1, 2]])

    def test_one_quran_com_word_spans_two_display_words(self):
        display = ["ٱلرَّحْمَٰ", "نِ"]  # our text splits where quran.com has one word
        qc = [("ٱلرَّحْمَٰنِ", "merciful")]
        groups = bwm.group_words("1:1", display, qc)
        self.assertEqual(groups, [[0], [0]])  # the meaning repeats on both

    def test_words_left_over_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "left over"):
            bwm.group_words("2:2", ["ذَٰلِكَ"], list(zip(BAQARAH_2_QC, "abcdefg")))

    def test_parting_skeletons_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "part ways"):
            bwm.group_words("2:2", ["ذَٰلِكَ", "ٱلْكِتَٰبُ"], [("لَا", "no"), ("رَيْبَ", "doubt")])


class AssignMeaningsTest(unittest.TestCase):
    def test_one_to_one(self):
        qc = list(zip(BAQARAH_2_QC, ["this", "the book", "no", "doubt", "in it", "guidance", "for the cautious"]))
        meanings = bwm.assign_meanings("2:2", BAQARAH_2_WORDS, qc)
        self.assertEqual(meanings[2], "no")
        self.assertEqual(len(meanings), 7)

    def test_merged_words_join_with_a_space(self):
        display = ["قُلْ", "الْحَمِيدُ"]
        qc = [("قُلْ", "say"), ("الْحَمِيدُ", "praiseworthy"), ("ۙ", "a bare mark")]
        self.assertEqual(bwm.assign_meanings("1:1", display, qc), ["say", "praiseworthy a bare mark"])

    def test_split_word_repeats_its_meaning(self):
        display = ["ٱلرَّحْمَٰ", "نِ"]
        qc = [("ٱلرَّحْمَٰنِ", "merciful")]
        self.assertEqual(bwm.assign_meanings("1:1", display, qc), ["merciful", "merciful"])

    def test_an_override_grouping_wins(self):
        saved = dict(bwm.WORD_GROUPS)
        try:
            bwm.WORD_GROUPS["9:1"] = [[0], [1, 2]]
            meanings = bwm.assign_meanings("9:1", ["عَذَابٌ", "أَلِيمٌ"], [("x", "a"), ("y", "b"), ("z", "c")])
            self.assertEqual(meanings, ["a", "b c"])
        finally:
            bwm.WORD_GROUPS.clear()
            bwm.WORD_GROUPS.update(saved)

    def test_empty_meanings_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "no meaning"):
            bwm.assign_meanings("1:1", ["قُلْ"], [("قُلْ", "")])
        with self.assertRaisesRegex(ValueError, "no meaning"):
            bwm.assign_meanings("1:1", ["قُلْ"], [("قُلْ", "  ")])

    def test_a_group_beyond_the_words_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "beyond"):
            bwm.join_meanings("1:1", 1, [[0, 7]], [("قُلْ", "say")])


class VerseWordsTest(unittest.TestCase):
    def test_the_ayah_number_glyph_is_dropped(self):
        verse = {"verse_key": "1:1", "words": [
            {"char_type_name": "word", "text_uthmani": "بِسْمِ", "translation": {"text": " In (the) name "}},
            {"char_type_name": "end", "text_uthmani": "١", "translation": {"text": "(1)"}},
            {"char_type_name": "word", "text_uthmani": "ٱللَّهِ", "translation": {"text": "(of) Allah"}},
        ]}
        self.assertEqual(bwm.verse_words(verse), [("بِسْمِ", "In (the) name"), ("ٱللَّهِ", "(of) Allah")])


class CollectWordsTest(unittest.TestCase):
    def test_pages_key_by_ayah_and_reject_repeats(self):
        pages = [
            {"verses": [{"verse_key": "112:1"}, {"verse_key": "112:2"}]},
            {"verses": [{"verse_key": "112:2"}]},
        ]
        for payload in pages:
            for verse in payload["verses"]:
                verse["words"] = [{"char_type_name": "word", "text_uthmani": "قُلْ",
                                   "translation": {"text": "say"}}]
        with self.assertRaisesRegex(ValueError, "appears twice"):
            bwm.collect_words(112, pages)


class BuildRowsTest(unittest.TestCase):
    def test_both_languages_in_ayah_order(self):
        text_rows = [
            {"surah": 112, "n": 1, "ar": "قُلْ هُوَ ٱللَّهُ أَحَدٌ"},
            {"surah": 112, "n": 2, "ar": "ٱللَّهُ ٱلصَّمَدُ"},
        ]
        words = {lang: {(112, 1): [("قُلْ", f"say {lang}"), ("هُوَ", "he"), ("ٱللَّهُ", "god"), ("أَحَدٌ", "one")],
                        (112, 2): [("ٱللَّهُ", "god"), ("ٱلصَّمَدُ", "eternal")]}
                 for lang in bwm.LANGS}
        per_lang, aligned = bwm.build_rows(112, text_rows, words)
        self.assertEqual(per_lang["en"][0], [1, ["say en", "he", "god", "one"]])
        self.assertEqual(per_lang["bn"][1], [2, ["god", "eternal"]])
        self.assertEqual(aligned, set())

    def test_ayahs_needing_alignment_are_reported(self):
        text_rows = [{"surah": 112, "n": 1, "ar": "قُلْ هُوَ"}]
        words = {lang: {(112, 1): [("قُلْ هُوَ", "say he")]} for lang in bwm.LANGS}
        _, aligned = bwm.build_rows(112, text_rows, words)
        self.assertEqual(aligned, {(112, 1)})

    def test_a_missing_verse_fails(self):
        with self.assertRaisesRegex(ValueError, "no en verse"):
            bwm.build_rows(112, [{"surah": 112, "n": 1, "ar": "قُلْ"}],
                           {lang: {} for lang in bwm.LANGS})


class ValidateTest(unittest.TestCase):
    ROWS = [{"surah": 1, "n": 1, "ar": "قُلْ هُوَ"}]

    def test_accepts_one_meaning_per_word(self):
        bwm.validate({lang: [[1, ["say", "he"]]] for lang in bwm.LANGS}, self.ROWS)

    def test_a_missing_row_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "en rows"):
            bwm.validate({lang: [] for lang in bwm.LANGS}, self.ROWS)

    def test_a_row_out_of_order_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "out of order"):
            bwm.validate({lang: [[2, ["say", "he"]]] for lang in bwm.LANGS}, self.ROWS)

    def test_a_wrong_meaning_count_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "1 en meanings for 2 words"):
            bwm.validate({"en": [[1, ["say"]]], "bn": [[1, ["say", "he"]]]}, self.ROWS)

    def test_a_blank_meaning_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "empty bn meaning"):
            bwm.validate({"en": [[1, ["say", "he"]]], "bn": [[1, ["say", ""]]]}, self.ROWS)


class WriteAssetsTest(unittest.TestCase):
    def test_writes_compact_files_per_language(self):
        with tempfile.TemporaryDirectory() as tmp:
            per_surah = [{lang: [[1, ["say", "he"]]] for lang in bwm.LANGS}] * 114
            bwm.write_assets(tmp, per_surah)
            for lang in ("en", "bn"):
                self.assertEqual(len(os.listdir(os.path.join(tmp, "words", lang))), 114)
            with open(os.path.join(tmp, "words", "bn", "001.json"), encoding="utf-8") as f:
                self.assertEqual(f.read(), '[[1,["say","he"]]]')
            with open(os.path.join(tmp, "words", "en", "114.json"), encoding="utf-8") as f:
                self.assertEqual(json.load(f), [[1, ["say", "he"]]])
