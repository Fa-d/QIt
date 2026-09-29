#!/usr/bin/env python3
"""Tests for build_logo.py's geometry and generated drawables. No Chrome, writes nothing.

Run from the repo root:  python3 -m unittest scripts/test_build_logo.py -v
"""
import math
import os
import sys
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_logo as bl

ANDROID = "{http://schemas.android.com/apk/res/android}"


class GeometryTest(unittest.TestCase):
    def test_the_lamp_stays_inside_the_adaptive_icon_safe_circle(self):
        # Launchers mask the 108dp canvas down to at least a 66dp circle; nothing may reach past it.
        worst = max(math.hypot(x - bl.CENTER, y - bl.CENTER) for x, y in bl.all_points())
        self.assertLessEqual(worst, bl.SAFE_RADIUS)

    def test_the_lamp_hangs_straight(self):
        x0, _, x1, _ = bl.bounds()
        self.assertAlmostEqual(x0 + x1, 2 * bl.CENTER, places=6)

    def test_the_octagram_is_the_rub_el_hizb(self):
        star = bl.octagram(0, 0, 10)
        self.assertEqual(16, len(star))
        radii = [math.hypot(x, y) for x, y in star]
        self.assertTrue(all(math.isclose(r, 10) for r in radii[::2]))
        # Two squares of circumradius 10 cross at 10 * cos 45° / cos 22.5° from the centre.
        self.assertTrue(all(math.isclose(r, 7.6537, rel_tol=1e-4) for r in radii[1::2]))
        self.assertTrue(math.isclose(star[0][0], 0, abs_tol=1e-9))  # a point straight up
        self.assertLess(star[0][1], 0)

    def test_the_window_leaves_a_frame(self):
        self.assertGreater(bl.BODY_R - bl.WINDOW_R, 4)


class DrawableTest(unittest.TestCase):
    def test_the_foreground_is_one_colour_so_it_can_be_the_themed_icon(self):
        root = ET.fromstring(bl.foreground_xml())
        colors = {p.get(f"{ANDROID}fillColor") for p in root.iter("path")}
        self.assertEqual({"@color/qandeel_icon_foreground"}, colors)
        self.assertEqual("108", root.get(f"{ANDROID}viewportWidth"))

    def test_paths_with_holes_are_even_odd(self):
        root = ET.fromstring(bl.foreground_xml())
        rules = [p.get(f"{ANDROID}fillType") for p in root.iter("path")]
        self.assertEqual(len(bl.lamp_paths()), len(rules))
        self.assertEqual("evenOdd", rules[0])  # the frame and its window

    def test_the_background_glows_from_the_flame(self):
        root = ET.fromstring(bl.background_xml())
        gradient = next(root.iter("gradient"))
        self.assertEqual("radial", gradient.get(f"{ANDROID}type"))
        self.assertEqual(bl.fmt(bl.BODY_Y), gradient.get(f"{ANDROID}centerY"))

    def test_the_notification_icon_fits_the_whole_lamp(self):
        root = ET.fromstring(bl.notification_xml())
        side = float(root.get(f"{ANDROID}viewportWidth"))
        group = next(root.iter("group"))
        tx, ty = float(group.get(f"{ANDROID}translateX")), float(group.get(f"{ANDROID}translateY"))
        x0, y0, x1, y1 = bl.bounds()
        for x, y in ((x0, y0), (x1, y1)):
            self.assertTrue(0 <= x + tx <= side and 0 <= y + ty <= side)
        self.assertEqual("24dp", root.get(f"{ANDROID}width"))

    def test_the_svg_parses(self):
        ET.fromstring(bl.logo_svg())


if __name__ == "__main__":
    unittest.main()
