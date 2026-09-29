#!/usr/bin/env python3
"""Generates Qandeel's logo, the launcher and notification icons, and the Play Store graphics.

The mark is a qandeel, the lamp that hangs in a mosque: its glass is the rub el hizb (the octagram
the app draws around surah and ayah numbers), with a flame at its heart, hanging from a chain.
One geometry, in the adaptive icon's 108-unit grid, feeds every output:

  app|wear/src/main/res/drawable/ic_launcher_foreground.xml  the gold lamp (also the monochrome layer)
  app|wear/src/main/res/drawable/ic_launcher_background.xml  green, with a soft glow behind the flame
  core/data/src/main/res/drawable/ic_notification.xml        white silhouette for notifications
  docs/store/logo.svg                                        master vector
  docs/store/icon-512.png                                    Play icon (full bleed; Play rounds it)
  docs/store/feature-graphic-1024x500.png                    Play feature graphic

Everything the launcher shows stays inside the adaptive icon's 66-unit safe circle, so circle,
squircle and the watch's round mask never clip it. The PNGs are screenshots of generated HTML taken
with headless Chrome, which draws the gradients and shapes the Arabic wordmark (Amiri Quran, the
app's bundled OFL font).

Usage: python3 scripts/build_logo.py [--chrome PATH] [--preview DIR]
  --preview DIR also writes a page of the mark at launcher, notification and store sizes.
"""

import argparse
import math
import os
import pathlib
import subprocess
import tempfile

REPO = pathlib.Path(__file__).resolve().parent.parent
FONT = REPO / "core/designsystem/src/main/res/font/amiri_quran.ttf"
DEFAULT_CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# The app's mushaf palette, as in res/values/colors.xml: QandeelPalettes primary 30 (green), 35 (the
# glow behind the flame), 25 (the edges) and tertiary 80 (gold).
GREEN = "#195039"
GREEN_GLOW = "#275C44"
GREEN_EDGE = "#09442E"
GOLD = "#EFC055"

CENTER = 54.0
SAFE_RADIUS = 33.0
KAPPA = 0.5522847498  # cubic Bezier handle length for a quarter circle


class Path:
    """A path kept both as SVG/VectorDrawable path data and as flattened points (for checks)."""

    def __init__(self):
        self.commands = []
        self.points = []
        self._pen = (0.0, 0.0)

    def move(self, x, y):
        self.commands.append(f"M{fmt(x)},{fmt(y)}")
        self.points.append((x, y))
        self._pen = (x, y)
        return self

    def line(self, x, y):
        self.commands.append(f"L{fmt(x)},{fmt(y)}")
        self.points.append((x, y))
        self._pen = (x, y)
        return self

    def cubic(self, x1, y1, x2, y2, x, y):
        self.commands.append(f"C{fmt(x1)},{fmt(y1)} {fmt(x2)},{fmt(y2)} {fmt(x)},{fmt(y)}")
        x0, y0 = self._pen
        for i in range(1, 9):
            t = i / 8
            u = 1 - t
            self.points.append((
                u**3 * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t**3 * x,
                u**3 * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t**3 * y,
            ))
        self._pen = (x, y)
        return self

    def close(self):
        self.commands.append("Z")
        return self

    def polygon(self, points):
        self.move(*points[0])
        for p in points[1:]:
            self.line(*p)
        return self.close()

    def circle(self, cx, cy, r):
        k = r * KAPPA
        self.move(cx, cy - r)
        self.cubic(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        self.cubic(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        self.cubic(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        self.cubic(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        return self.close()

    @property
    def data(self):
        return " ".join(self.commands)


def fmt(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def octagram(cx, cy, r):
    """The rub el hizb as one outline: two squares of circumradius r, a point straight up."""
    inner = r * math.cos(math.radians(45)) / math.cos(math.radians(22.5))
    pts = []
    for i in range(16):
        angle = math.radians(-90 + i * 22.5)
        radius = r if i % 2 == 0 else inner
        pts.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    return pts


def bar(x0, y0, x1, y1, width):
    """A straight stroke from (x0, y0) to (x1, y1) as a filled quadrilateral."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    nx, ny = -dy / length * width / 2, dx / length * width / 2
    return [(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)]


# The lamp. Body centre, frame radii and the parts above and below, in the 108 grid.
BODY_Y = 57.0
BODY_R = 20.5
WINDOW_R = 15.2
CAP_TOP = 30.2
CAP_BOTTOM = 39.0
RING_Y = 25.2


def lamp_paths():
    """(path, fill rule) pairs: the lamp, all in one colour. evenOdd paths carry their holes."""
    frame = Path().polygon(octagram(CENTER, BODY_Y, BODY_R)).polygon(octagram(CENTER, BODY_Y, WINDOW_R))

    # The flame: a point on top, a round belly, centred in the window.
    top, belly_y, belly_r = BODY_Y - 13.0, BODY_Y + 3.4, 5.3
    k = belly_r * KAPPA
    flame = (
        Path()
        .move(CENTER, top)
        .cubic(CENTER + 1.8, top + 4.0, CENTER + belly_r, belly_y - 5.0, CENTER + belly_r, belly_y)
        .cubic(CENTER + belly_r, belly_y + k, CENTER + k, belly_y + belly_r, CENTER, belly_y + belly_r)
        .cubic(CENTER - k, belly_y + belly_r, CENTER - belly_r, belly_y + k, CENTER - belly_r, belly_y)
        .cubic(CENTER - belly_r, belly_y - 5.0, CENTER - 1.8, top + 4.0, CENTER, top)
        .close()
    )

    # The cap: an onion dome over the star's top point, on a lip, rising to a point into the chain.
    lip = CAP_BOTTOM - 1.5
    cap = (
        Path()
        .move(CENTER - 7.6, CAP_BOTTOM)
        .line(CENTER - 7.6, lip)
        .line(CENTER - 6.0, lip)
        .cubic(CENTER - 7.4, lip - 3.2, CENTER - 2.4, CAP_TOP + 2.6, CENTER, CAP_TOP)
        .cubic(CENTER + 2.4, CAP_TOP + 2.6, CENTER + 7.4, lip - 3.2, CENTER + 6.0, lip)
        .line(CENTER + 7.6, lip)
        .line(CENTER + 7.6, CAP_BOTTOM)
        .close()
    )

    # The chain: the ring the lamp hangs from, and a short stem down to the knob.
    ring = Path().circle(CENTER, RING_Y, 2.8).circle(CENTER, RING_Y, 1.4)
    strands = Path().polygon(bar(CENTER, RING_Y + 2.4, CENTER, CAP_TOP + 1.2, 1.6))

    # The finial: a drop under the star's bottom point.
    drop_y, drop_r = BODY_Y + BODY_R + 2.6, 2.3
    k = drop_r * KAPPA
    finial = (
        Path()
        .move(CENTER - drop_r, drop_y)
        .cubic(CENTER - drop_r, drop_y - k, CENTER - k, drop_y - drop_r, CENTER, drop_y - drop_r)
        .cubic(CENTER + k, drop_y - drop_r, CENTER + drop_r, drop_y - k, CENTER + drop_r, drop_y)
        .cubic(CENTER + drop_r, drop_y + 1.8, CENTER + 0.8, drop_y + 3.6, CENTER, drop_y + 5.2)
        .cubic(CENTER - 0.8, drop_y + 3.6, CENTER - drop_r, drop_y + 1.8, CENTER - drop_r, drop_y)
        .close()
    )
    return [
        (frame, "evenOdd"),
        (flame, "nonZero"),
        (cap, "nonZero"),
        (ring, "evenOdd"),
        (strands, "nonZero"),
        (finial, "nonZero"),
    ]


def all_points():
    return [p for path, _ in lamp_paths() for p in path.points]


def bounds():
    pts = all_points()
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return min(xs), min(ys), max(xs), max(ys)


# --- Android vector drawables ---------------------------------------------------------------------

HEADER = '<?xml version="1.0" encoding="utf-8"?>\n'
GENERATED = "<!-- Generated by scripts/build_logo.py; edit the script, not this file. -->\n"


def vector_paths(color_attr, indent="    "):
    return "".join(
        f'{indent}<path\n{indent}    android:fillColor="{color_attr}"\n'
        + (f'{indent}    android:fillType="evenOdd"\n' if rule == "evenOdd" else "")
        + f'{indent}    android:pathData="{path.data}" />\n'
        for path, rule in lamp_paths()
    )


def foreground_xml():
    return (
        HEADER
        + "<!-- The qandeel: a lamp whose glass is the rub el hizb, a flame at its heart, hung from a chain.\n"
        + "     One colour, so it doubles as the themed-icon (monochrome) layer; inside the 66dp safe zone.\n"
        + "     Generated by scripts/build_logo.py; edit the script, not this file. -->\n"
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:width="108dp"\n    android:height="108dp"\n'
        + '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        + vector_paths("@color/qandeel_icon_foreground")
        + "</vector>\n"
    )


def background_xml():
    return (
        HEADER
        + "<!-- Deep green with a soft glow where the flame sits.\n"
        + "     Generated by scripts/build_logo.py; edit the script, not this file. -->\n"
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        + '    android:width="108dp"\n    android:height="108dp"\n'
        + '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + f'            <gradient\n                android:type="radial"\n'
        + f'                android:centerX="{fmt(CENTER)}"\n                android:centerY="{fmt(BODY_Y)}"\n'
        + '                android:gradientRadius="62">\n'
        + f'                <item android:offset="0" android:color="@color/qandeel_icon_glow" />\n'
        + f'                <item android:offset="0.55" android:color="@color/qandeel_icon_background" />\n'
        + f'                <item android:offset="1" android:color="@color/qandeel_icon_edge" />\n'
        + "            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n"
    )


def notification_xml():
    """The lamp cropped to its own bounds, centred in a 24dp status-bar icon (white, alpha only)."""
    x0, y0, x1, y1 = bounds()
    side = max(x1 - x0, y1 - y0) / 0.92  # a little padding, as the status bar icon grid asks
    tx = side / 2 - (x0 + x1) / 2
    ty = side / 2 - (y0 + y1) / 2
    return (
        HEADER
        + "<!-- The qandeel for the status bar: the launcher lamp's silhouette, white.\n"
        + "     Generated by scripts/build_logo.py; edit the script, not this file. -->\n"
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:width="24dp"\n    android:height="24dp"\n'
        + f'    android:viewportWidth="{fmt(side)}"\n    android:viewportHeight="{fmt(side)}"\n'
        + '    android:tint="?android:attr/colorControlNormal">\n'
        + f'    <group\n        android:translateX="{fmt(tx)}"\n        android:translateY="{fmt(ty)}">\n'
        + vector_paths("@android:color/white", indent="        ")
        + "    </group>\n</vector>\n"
    )


# --- SVG and store graphics ---------------------------------------------------------------------


def svg_paths(fill):
    return "".join(
        f'<path fill="{fill}" fill-rule="{"evenodd" if rule == "evenOdd" else "nonzero"}" d="{path.data}"/>'
        for path, rule in lamp_paths()
    )


def glow_defs(ident):
    return (
        f'<radialGradient id="{ident}" gradientUnits="userSpaceOnUse" cx="{CENTER}" cy="{BODY_Y}" r="62">'
        f'<stop offset="0" stop-color="{GREEN_GLOW}"/><stop offset="0.55" stop-color="{GREEN}"/>'
        f'<stop offset="1" stop-color="{GREEN_EDGE}"/></radialGradient>'
    )


def logo_svg(size=108, crop=0.0, background=True):
    """The icon as SVG; crop trims the 108 grid's edges (0 = the full adaptive-icon canvas)."""
    view = f"{fmt(crop)} {fmt(crop)} {fmt(108 - 2 * crop)} {fmt(108 - 2 * crop)}"
    bg = f'<defs>{glow_defs("glow")}</defs><rect width="108" height="108" fill="url(#glow)"/>' if background else ""
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="{view}">'
        f"{bg}{svg_paths(GOLD)}</svg>"
    )


def font_face():
    return f"@font-face{{font-family:Amiri;src:url('{FONT.as_uri()}')}}"


def icon_html():
    # Play icons are full bleed; Play applies the rounded mask. The lamp takes about two thirds.
    return (
        "<html><body style='margin:0'>"
        + logo_svg(size=512, crop=12)
        + "</body></html>"
    )


def feature_html():
    x0, y0, x1, y1 = bounds()
    lamp_view = f"{fmt(x0 - 2)} {fmt(y0 - 2)} {fmt(x1 - x0 + 4)} {fmt(y1 - y0 + 4)}"
    return f"""<html><head><style>
{font_face()}
body{{margin:0;width:1024px;height:500px;overflow:hidden;display:flex;align-items:center;justify-content:center;
  gap:64px;background:radial-gradient(circle at 330px 250px,{GREEN_GLOW} 0,{GREEN} 320px,{GREEN_EDGE} 780px)}}
.lamp{{height:380px}}
.words{{font-family:Amiri;color:{GOLD};display:flex;flex-direction:column}}
.name{{font-size:120px;line-height:1}}
.arabic{{font-size:84px;line-height:1.25;direction:rtl;align-self:flex-start;margin-top:4px}}
.tag{{font-size:38px;line-height:1.3;color:#F6EBD2;letter-spacing:.5px;margin-top:22px}}
</style></head><body>
<svg class="lamp" viewBox="{lamp_view}">{svg_paths(GOLD)}</svg>
<div class="words"><div class="name">Qandeel</div><div class="arabic">قنديل</div>
<div class="tag">Quran &amp; Hadith Audio</div></div>
</body></html>"""


def preview_html():
    sizes = [192, 108, 72, 48, 36]
    cells = []
    for s in sizes:
        cells.append(
            f"<figure><div style='width:{s}px;height:{s}px;border-radius:50%;overflow:hidden'>"
            f"{logo_svg(size=s)}</div><figcaption>{s}px circle</figcaption></figure>"
        )
    cells.append(
        f"<figure><div style='width:108px;height:108px;border-radius:28%;overflow:hidden'>{logo_svg(size=108)}</div>"
        "<figcaption>squircle</figcaption></figure>"
    )
    mono = (
        "<figure><div style='width:108px;height:108px;border-radius:50%;overflow:hidden;background:#D8E6DD'>"
        f"<svg width='108' height='108' viewBox='0 0 108 108'>{svg_paths('#1F3A2C')}</svg></div>"
        "<figcaption>themed (monochrome)</figcaption></figure>"
    )
    x0, y0, x1, y1 = bounds()
    notif = "".join(
        f"<figure><div style='background:#222;padding:6px'><svg width='{s}' height='{s}' "
        f"viewBox='{fmt(x0 - 2)} {fmt(y0 - 2)} {fmt(max(x1 - x0, y1 - y0) + 4)} {fmt(max(x1 - x0, y1 - y0) + 4)}'>"
        f"{svg_paths('#fff')}</svg></div><figcaption>{s}px notification</figcaption></figure>"
        for s in (24, 36)
    )
    safe = (
        "<figure><svg width='216' height='216' viewBox='0 0 108 108'>"
        f"<defs>{glow_defs('g2')}</defs><rect width='108' height='108' fill='url(#g2)'/>"
        f"<circle cx='54' cy='54' r='{SAFE_RADIUS}' fill='none' stroke='#f55' stroke-width='0.4'/>"
        f"{svg_paths(GOLD)}</svg><figcaption>66dp safe zone</figcaption></figure>"
    )
    return (
        "<html><body style='margin:0;background:#EEE;font:12px sans-serif;display:flex;flex-wrap:wrap;"
        "align-items:end;gap:16px;padding:16px;width:1000px'>"
        + safe + "".join(cells) + mono + notif + "</body></html>"
    )


def screenshot(chrome, html, out, width, height):
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as f:
        f.write(html)
        page = f.name
    try:
        subprocess.run(
            [chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--allow-file-access-from-files",
             f"--screenshot={out}", f"--window-size={width},{height}", "--default-background-color=00000000",
             pathlib.Path(page).as_uri()],
            check=True, capture_output=True,
        )
    finally:
        os.unlink(page)


def write(path, text):
    path = REPO / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    print("wrote", path.relative_to(REPO))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--chrome", default=DEFAULT_CHROME, help="Chrome binary for the PNGs")
    parser.add_argument("--preview", help="also write preview.png (the mark at every size) into this directory")
    args = parser.parse_args()

    for module in ("app", "wear"):
        write(f"{module}/src/main/res/drawable/ic_launcher_foreground.xml", foreground_xml())
        write(f"{module}/src/main/res/drawable/ic_launcher_background.xml", background_xml())
    write("core/data/src/main/res/drawable/ic_notification.xml", notification_xml())
    write("docs/store/logo.svg", logo_svg(size=512) + "\n")

    screenshot(args.chrome, icon_html(), str(REPO / "docs/store/icon-512.png"), 512, 512)
    print("wrote docs/store/icon-512.png")
    screenshot(args.chrome, feature_html(), str(REPO / "docs/store/feature-graphic-1024x500.png"), 1024, 500)
    print("wrote docs/store/feature-graphic-1024x500.png")
    if args.preview:
        out = os.path.join(args.preview, "preview.png")
        screenshot(args.chrome, preview_html(), out, 1032, 560)
        print("wrote", out)


if __name__ == "__main__":
    main()
