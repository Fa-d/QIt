#!/usr/bin/env python3
"""Tests for the pure functions of build_audio_timing.py. No network, no audio files.

Run from the repo root:  python3 -m unittest scripts/test_build_audio_timing.py -v
"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_audio_timing as bat

# 10:1 - the muqatta'at, a pause mark, then four words.
YUNUS_1 = "الٓر ۚ تِلْكَ ءَايَٰتُ ٱلْكِتَٰبِ ٱلْحَكِيمِ"
# 2:2 - two standalone pause marks between the words.
BAQARAH_2 = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"


class DisplayWordsTest(unittest.TestCase):
    def test_pause_marks_are_not_words(self):
        self.assertEqual(len(bat.display_words(BAQARAH_2)), 7)

    def test_every_pause_mark_is_recognised(self):
        for mark in ["ۖ", "ۗ", "ۘ", "ۙ", "ۚ", "ۛ", "ۜ", "۞", "۩"]:
            self.assertTrue(bat.is_pause_mark(mark), mark)

    def test_letters_are_not_pause_marks(self):
        self.assertFalse(bat.is_pause_mark("لَا"))

    def test_letters_ignore_combining_marks(self):
        self.assertEqual(bat.letters("لَا"), 2)


class AlignerMappingTest(unittest.TestCase):
    def test_pause_marks_are_skipped(self):
        self.assertEqual(bat.aligner_to_display(2, 2, BAQARAH_2), [0, 1, 2, 3, 4, 5, 6])

    def test_mark_after_muqattaat_counts_in_10_1(self):
        self.assertEqual(bat.aligner_to_display(10, 1, YUNUS_1), [0, None, 1, 2, 3, 4])

    def test_other_ayahs_skip_that_mark(self):
        self.assertEqual(bat.aligner_to_display(15, 1, YUNUS_1), [0, 1, 2, 3, 4])


class WordSpansTest(unittest.TestCase):
    def test_one_segment_per_word(self):
        segments = [[i, i + 1, i * 100, i * 100 + 90] for i in range(7)]
        spans = bat.word_spans(2, 2, BAQARAH_2, segments, duration_ms=1000)
        self.assertEqual(spans[3], (300, 390))

    def test_joined_words_split_by_letters(self):
        text = "ab abcd"
        spans = bat.word_spans(1, 1, text, [[0, 2, 0, 600]], duration_ms=700)
        self.assertEqual(spans, [(0, 200), (200, 600)])

    def test_counted_pause_mark_is_dropped_and_gaps_filled(self):
        # 10:1: aligner index 1 is the mark, index 3 (the second display word) was never placed.
        segments = [[0, 1, 0, 1000], [2, 3, 1100, 1500], [4, 5, 2000, 2400], [5, 6, 2400, 3000]]
        spans = bat.word_spans(10, 1, YUNUS_1, segments, duration_ms=3200)
        self.assertEqual(spans[0], (0, 1000))
        self.assertEqual(spans[1], (1100, 1500))
        self.assertEqual(spans[2], (1500, 2000))  # the gap between its neighbours
        self.assertEqual(spans[4], (2400, 3000))

    def test_unplaced_tail_runs_to_the_end_of_the_file(self):
        text = "aa bb cc dd"
        spans = bat.word_spans(1, 1, text, [[0, 1, 0, 500], [1, 2, 500, 1000]], duration_ms=2000)
        self.assertEqual(spans[2:], [(1000, 1500), (1500, 2000)])

    def test_run_without_a_gap_borrows_from_the_previous_word(self):
        text = "aa bb cc"
        spans = bat.word_spans(1, 1, text, [[0, 1, 0, 800], [2, 3, 800, 1200]], duration_ms=1300)
        self.assertEqual(spans, [(0, 400), (400, 800), (800, 1200)])

    def test_word_squeezed_to_nothing_is_placed_like_an_unplaced_one(self):
        # 2:164 has [21, 22, 20770, 20760]: the word ends before it starts.
        segments = [[0, 1, 0, 800], [1, 2, 810, 800], [2, 3, 810, 1200]]
        spans = bat.word_spans(1, 1, "aa bb cc", segments, duration_ms=1300)
        self.assertEqual(spans, [(0, 405), (405, 810), (810, 1200)])

    def test_unplaced_leading_word_starts_at_zero(self):
        spans = bat.word_spans(1, 1, "aa bb", [[1, 2, 600, 900]], duration_ms=1000)
        self.assertEqual(spans[0], (0, 600))


class ValidateSpansTest(unittest.TestCase):
    def test_count_mismatch_rejected(self):
        with self.assertRaisesRegex(ValueError, "2:2"):
            bat.validate_spans("2:2", [(0, 1)], 2)

    def test_words_out_of_order_rejected(self):
        with self.assertRaisesRegex(ValueError, "word 1"):
            bat.validate_spans("1:1", [(500, 600), (100, 200)], 2)

    def test_word_ending_before_it_starts_rejected(self):
        with self.assertRaisesRegex(ValueError, "word 0"):
            bat.validate_spans("1:1", [(500, 400)], 1)


class BuildTimingsTest(unittest.TestCase):
    def test_every_ayah_gets_flat_start_end_pairs(self):
        texts, alignments = {}, {}
        for surah, count in enumerate(bat.VERSES, 1):
            for ayah in range(1, count + 1):
                texts[(surah, ayah)] = "aa bb"
                alignments[(surah, ayah)] = [[0, 1, 0, 100], [1, 2, 100, 250]]
        per_surah = bat.build_timings(texts, alignments, [300] * 6236)
        self.assertEqual(len(per_surah), 114)
        self.assertEqual(per_surah[1][254], [255, [0, 100, 100, 250]])


class ProbeTest(unittest.TestCase):
    def test_a_missing_file_is_zero_ms(self):
        # A verse that shares its translation's file with the next verse has none of its own.
        self.assertEqual(bat.probe_ms("/nonexistent/00001.mp3"), 0)


class WriteAssetsTest(unittest.TestCase):
    def test_writes_compact_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            per_surah = [[[1, [0, 10]]]] * 114
            durations = {"verses": {"ar": [1], "en": [2], "bn": [3]}, "intros": {"bn": [0]}}
            bat.write_assets(tmp, {"ar.alafasy": per_surah}, durations)
            timing = sorted(os.listdir(os.path.join(tmp, "timing", "ar.alafasy")))
            self.assertEqual((len(timing), timing[0], timing[-1]), (114, "001.json", "114.json"))
            with open(os.path.join(tmp, "audio", "durations.json"), encoding="utf-8") as f:
                self.assertEqual(f.read(), '{"verses":{"ar":[1],"en":[2],"bn":[3]},"intros":{"bn":[0]}}')
            with open(os.path.join(tmp, "timing", "ar.alafasy", "001.json"), encoding="utf-8") as f:
                self.assertEqual(json.load(f), [[1, [0, 10]]])


if __name__ == "__main__":
    unittest.main()
