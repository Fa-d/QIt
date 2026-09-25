#!/usr/bin/env python3
"""Tests for the pure functions of build_palettes.py. No materialyoucolor, no repo files.

Run from the repo root:  python3 -m unittest scripts/test_build_palettes.py -v
"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_palettes as bp


class TonesTest(unittest.TestCase):
    def test_tones_run_from_black_to_white_in_order(self):
        self.assertEqual(list(bp.TONES), sorted(bp.TONES))
        self.assertEqual((bp.TONES[0], bp.TONES[-1]), (0, 100))
        self.assertEqual(bp.TONES.count(40), 1)

    def test_sepia_adds_the_container_tones_between_80_and_90(self):
        self.assertEqual(bp.sepia_tones(), tuple(sorted(set(bp.TONES) | set(bp.SEPIA_EXTRA_TONES))))
        self.assertEqual(bp.SEPIA_EXTRA_TONES, (84, 85, 86, 88))


class KotlinPaletteTest(unittest.TestCase):
    def test_one_val_block_in_the_style_of_the_brand_tables(self):
        block = bp.kotlin_palette("sepiaNeutral", "Sepia paper (hue 95).", [(0, 0xFF000000), (94, 0xFFF6EDDA)])
        self.assertEqual(
            block,
            "\n".join(
                [
                    "    /** Sepia paper (hue 95). */",
                    "    val sepiaNeutral = TonalPalette(",
                    "        mapOf(",
                    "            0 to Color(0xFF000000),",
                    "            94 to Color(0xFFF6EDDA),",
                    "        ),",
                    "    )",
                ]
            ),
        )

    def test_the_region_wraps_the_blocks_between_its_markers(self):
        region = bp.generated_region(["block-one", "block-two"])
        self.assertTrue(region.startswith("    " + bp.REGION_START + "\n"))
        self.assertTrue(region.endswith("\n    " + bp.REGION_END))
        self.assertIn("block-one\n\nblock-two", region)


class ReplaceRegionTest(unittest.TestCase):
    def test_replaces_between_the_markers_and_keeps_the_rest_byte_identical(self):
        source = "before\n    %s\nstale\n    %s\nafter" % (bp.REGION_START, bp.REGION_END)
        region = "    %s\nfresh\n    %s" % (bp.REGION_START, bp.REGION_END)
        self.assertEqual(bp.replace_region(source, region), "before\n" + region + "\nafter")

    def test_missing_markers_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "region"):
            bp.replace_region("no markers here", "x")

    def test_unpaired_markers_are_rejected(self):
        source = "    %s\n    %s" % (bp.REGION_START, bp.REGION_START)
        with self.assertRaisesRegex(ValueError, "region"):
            bp.replace_region(source, "x")

    def test_replacing_is_idempotent(self):
        source = "before\n    %s\nstale\n    %s\nafter" % (bp.REGION_START, bp.REGION_END)
        region = "    %s\nfresh\n    %s" % (bp.REGION_START, bp.REGION_END)
        once = bp.replace_region(source, region)
        self.assertEqual(bp.replace_region(once, region), once)


class GenerateTest(unittest.TestCase):
    """Needs materialyoucolor; skipped when it isn't installed (it isn't a project dependency)."""

    def setUp(self):
        try:
            from materialyoucolor.palettes.core_palette import CorePalette
            from materialyoucolor.palettes.tonal_palette import TonalPalette
        except ImportError:
            self.skipTest("materialyoucolor is not installed")
        self.palettes = bp.generate(TonalPalette, CorePalette)

    def test_the_baseline_seed_is_its_own_primary(self):
        primary = dict((name, tones) for name, _, tones in self.palettes)["baselinePrimary"]
        self.assertEqual(dict(primary)[40], bp.BASELINE_SEED)

    def test_the_sepia_paper_tone_reads_like_a_sepia_e_reader(self):
        sepia = dict((name, tones) for name, _, tones in self.palettes)["sepiaNeutral"]
        paper = dict(sepia)[bp.SEPIA_PAPER_TONE]
        anchor = bp.SEPIA_PAPER_ANCHOR
        for shift in (16, 8, 0):  # r, g and b stay near the anchor
            channel = abs(((paper >> shift) & 0xFF) - ((anchor >> shift) & 0xFF))
            self.assertLessEqual(channel, 8, hex(paper))

    def test_every_palette_carries_every_promised_tone(self):
        for name, _, tones in self.palettes:
            expected = bp.sepia_tones() if name.startswith("sepia") else bp.TONES
            self.assertEqual([tone for tone, _ in tones], list(expected), name)


if __name__ == "__main__":
    unittest.main()
