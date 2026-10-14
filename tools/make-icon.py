"""Build the KARTUN launcher icon and the store icon from the owner's pictures in design/icon/.

[NOT-TESTED] - fresh; becomes TESTED after the icon is seen on the Titan's home screen.

Inputs (the owner's originals, never edited): design/icon/icon_v3.png - the launcher picture (redrawn by the owner for
the OS circle crop, 2026-10-14 15:30); design/icon/heart_neon.png - the earlier heart without a plate (verdict 15:11,
superseded by icon_v3); design/icon/icon_v2.png - the store picture.

Outputs:
  app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png  - the picture, scaled so the heart sits
                                                                             inside the adaptive-icon safe zone
  app/src/main/res/mipmap-{...}/ic_launcher_monochrome.png                  - white heart + laser for themed icons
  app/src/main/res/values/ic_launcher_background.xml                       - the picture's own background colour
  app/src/main/res/mipmap-anydpi/ic_launcher.xml, ic_launcher_round.xml     - the adaptive icon (minSdk 36)
  design/icon/store_512.png         - icon_v2 at 512x512 for Google Play and the README
  design/icon/preview_masks.png     - the launcher icon under circle / squircle / rounded-square masks + themed

Geometry - Android adaptive icons (developer.android.com/develop/ui/views/launch/icon_design_adaptive): every layer is
108x108 dp, a launcher mask shows the central 72x72 dp, and only a 66 dp circle in the centre is guaranteed to survive
every mask. So the heart's farthest pixel from the centre is placed on that circle (minus a small margin); the laser
streak above and below the heart may be cut by a mask - it fades out there anyway.

Run from the project root:  python tools/make-icon.py
(needs Pillow only; the system Python of this machine has it - HOUSE_RULES.md section 6)
"""
import argparse
import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# The launcher picture: icon_v3 - the owner's redraw for the OS circle crop (2026-10-14 15:30: "я просил перерисовать
# так, чтобы в круге ОС кропало сердце так, чтобы оно по центру оказывалось"). Used full-bleed (scale 1.0): the circle
# cuts inside the glass plate and the heart sits on the safe circle. heart_neon.png: --src ... --inset 0.17 (auto scale).
SRC_HEART = os.path.join(ROOT, 'design', 'icon', 'icon_v3.png')
DEFAULT_SCALE = 1.0
DEFAULT_INSET = 0.21
SRC_PLATE = os.path.join(ROOT, 'design', 'icon', 'icon_v2.png')
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')

LAYER_DP = 108          # adaptive icon layer side
VISIBLE_DP = 72         # what a launcher mask shows
SAFE_RADIUS_DP = 33     # 66 dp circle that survives every mask
SAFE_MARGIN = 0.96      # keep the heart's outermost pixel a hair inside the safe circle
DENSITIES = {'mdpi': 1.0, 'hdpi': 1.5, 'xhdpi': 2.0, 'xxhdpi': 3.0, 'xxxhdpi': 4.0}
STORE_SIDE = 512        # Google Play hi-res icon: 512x512 32-bit PNG

# What counts as "the heart" in the owner's picture: saturated pink (red far above green). The background is
# near-black and the glow around the heart is dim pink, both well under these thresholds (measured below).
BODY_PINK_MIN = 120     # R - G for the heart body
LASER_LUMA_MIN = 200    # the white core of the laser line
LASER_BAND = 30         # px either side of the laser line ignored when measuring the heart (its glow is pink too)
OPEN_PX = 9             # odd px window that opens away the thin spark strokes (heart pixels are ~35 px squares)
EDGE_STRIP = 24         # px of the picture's border averaged for the background colour
MEASURE_INSET = 0.17    # fraction of the side ignored at each edge when looking for the heart: icon_v2/v3 carry a
                        # pink neon rim around the plate at ~0.14 of the side; the hearts start at ~0.19


def laser_column(rgb):
    """x of the laser line: the column with the most near-white pixels."""
    luma = rgb.convert('L').point(lambda v: 255 if v > LASER_LUMA_MIN else 0)
    w, h = luma.size
    best, best_x = -1, w // 2
    for x in range(w):
        n = luma.crop((x, 0, x + 1, h)).histogram()[255]
        if n > best:
            best, best_x = n, x
    return best_x


def body_mask(rgb, laser_x, inset=MEASURE_INSET):
    """Hard mask of the heart BODY: saturated pink, with the laser band blanked (its pink glow runs far above and
    below the heart and would otherwise set the size) and the thin spark strokes around the laser opened away."""
    r, g, _ = rgb.split()
    body = ImageChops.subtract(r, g).point(lambda v: 255 if v > BODY_PINK_MIN else 0)
    draw = ImageDraw.Draw(body)
    draw.rectangle((laser_x - LASER_BAND, 0, laser_x + LASER_BAND, rgb.size[1]), fill=0)
    w, h = rgb.size
    m = round(w * inset)                          # the plate's neon rim (icon_v2/v3) lies outside this box
    for box in ((0, 0, w, m), (0, h - m, w, h), (0, 0, m, h), (w - m, 0, w, h)):
        draw.rectangle(box, fill=0)
    return body.filter(ImageFilter.MinFilter(OPEN_PX)).filter(ImageFilter.MaxFilter(OPEN_PX))


def body_extent(body):
    """Bounding box of the heart body and its farthest pixel from the picture's centre."""
    w, h = body.size
    cx, cy = (w - 1) / 2, (h - 1) / 2
    far = 0.0
    px = body.load()
    bbox = body.getbbox()
    x0, y0, x1, y1 = bbox
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y]:
                d = math.hypot(x - cx, y - cy)
                if d > far:
                    far = d
    return bbox, far


def silhouette(body, laser_x):
    """The monochrome layer's shape: one solid heart without the laser. The blanked laser band is bridged row by row
    only where the heart is on BOTH sides of it (so the notch between the humps stays open and no streak sticks out
    above or below), then every enclosed hole - the glossy highlight - is filled."""
    shape = body.copy()
    px = shape.load()
    w, h = shape.size
    left, right = laser_x - LASER_BAND - 1, laser_x + LASER_BAND + 1
    for y in range(h):
        if px[left, y] and px[right, y]:
            for x in range(left, right + 1):
                px[x, y] = 255
    outside = shape.copy()
    ImageDraw.floodfill(outside, (0, 0), 128)              # everything reachable from the corner is outside
    filled = outside.point(lambda v: 0 if v == 128 else 255)
    return filled.filter(ImageFilter.GaussianBlur(1.5))


def border_colour(rgb):
    """Average colour of the picture's outer strip - the background layer, so the seam is invisible."""
    w, h = rgb.size
    strips = [rgb.crop((0, 0, w, EDGE_STRIP)), rgb.crop((0, h - EDGE_STRIP, w, h)),
              rgb.crop((0, 0, EDGE_STRIP, h)), rgb.crop((w - EDGE_STRIP, 0, w, h))]
    total, n = [0, 0, 0], 0
    for s in strips:
        for c in s.get_flattened_data():
            total[0] += c[0]; total[1] += c[1]; total[2] += c[2]
            n += 1
    return tuple(round(t / n) for t in total)


def feathered(img, side):
    """The scaled picture with its outer ring faded to transparent, so its square edge never shows on the
    background layer even where the colours differ by a unit or two."""
    alpha = Image.new('L', (side, side), 0)
    draw = ImageDraw.Draw(alpha)
    steps = 64
    r_full, r_zero = side * 0.40, side * 0.5          # opaque inside 80 % of the side, zero at the edge circle
    for i in range(steps, -1, -1):
        rr = r_zero - (r_zero - r_full) * i / steps
        draw.ellipse((side / 2 - rr, side / 2 - rr, side / 2 + rr, side / 2 + rr), fill=int(255 * i / steps))
    draw.ellipse((side / 2 - r_full, side / 2 - r_full, side / 2 + r_full, side / 2 + r_full), fill=255)
    out = img.convert('RGBA')
    out.putalpha(ImageChops.multiply(alpha, img.convert('RGBA').split()[3]))
    return out


def layer(img_rgba, scale, layer_px):
    """Place the picture (RGBA) centred on a transparent layer, its side = scale * layer side."""
    side = round(layer_px * scale)
    canvas = Image.new('RGBA', (layer_px, layer_px), (0, 0, 0, 0))
    canvas.alpha_composite(img_rgba.resize((side, side), Image.LANCZOS), ((layer_px - side) // 2,) * 2)
    return canvas


def squircle_mask(size, n=4.0):
    m = Image.new('L', (size, size), 0)
    px = m.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            if abs((x - c) / c) ** n + abs((y - c) / c) ** n <= 1:
                px[x, y] = 255
    return m


def main():
    ap = argparse.ArgumentParser(description='Build the KARTUN launcher icon from an owner picture in design/icon/.')
    ap.add_argument('--src', default=SRC_HEART, help='owner picture for the launcher icon (default icon_v3.png)')
    ap.add_argument('--scale', type=float, default=DEFAULT_SCALE,
                    help='picture side / layer side; 0 = auto: the heart fits the safe circle')
    ap.add_argument('--preview', default=os.path.join(ROOT, 'design', 'icon', 'preview_masks.png'),
                    help='where to write the mask preview sheet')
    ap.add_argument('--inset', type=float, default=DEFAULT_INSET,
                    help='edge fraction ignored when looking for the heart (0.21 for icon_v3: its rim glows inward)')
    ap.add_argument('--preview-only', action='store_true', help='write only the preview, leave app resources alone')
    args = ap.parse_args()

    heart = Image.open(args.src).convert('RGB')
    w, h = heart.size
    if w != h:
        sys.exit(f'heart picture is not square: {w}x{h}')
    laser_x = laser_column(heart)
    body = body_mask(heart, laser_x, args.inset)
    bbox, far = body_extent(body)
    bg = border_colour(heart)
    # scale = picture side / layer side, chosen so the farthest heart pixel lands on the safe circle.
    scale = SAFE_MARGIN * (SAFE_RADIUS_DP / LAYER_DP) * w / far
    scale = min(scale, 1.0)
    if args.scale:
        scale = args.scale
    print(f'source {os.path.relpath(args.src, ROOT)}; heart farthest pixel = '
          f'{far / w * scale * LAYER_DP:.1f} dp from the centre (safe circle {SAFE_RADIUS_DP} dp, visible circle '
          f'{VISIBLE_DP // 2} dp)')
    print(f'laser x {laser_x}; heart body bbox {bbox}, farthest pixel {far:.1f} px of half-side {w / 2}; '
          f'background #{bg[0]:02X}{bg[1]:02X}{bg[2]:02X}; picture scale {scale:.3f} of the layer '
          f'(heart width {(bbox[2] - bbox[0]) / w * scale * LAYER_DP:.1f} dp of {VISIBLE_DP} dp visible)')

    fg_src = feathered(heart, w)
    mono_src = Image.new('RGBA', (w, h), (255, 255, 255, 0))
    mono_src.putalpha(silhouette(body, laser_x))

    write_preview(fg_src, mono_src, scale, bg, args.preview)
    if args.preview_only:
        print('preview only:', args.preview)
        return
    src_name = os.path.relpath(args.src, ROOT).replace('\\', '/')

    for name, k in DENSITIES.items():
        px = round(LAYER_DP * k)
        d = os.path.join(RES, f'mipmap-{name}')
        os.makedirs(d, exist_ok=True)
        layer(fg_src, scale, px).save(os.path.join(d, 'ic_launcher_foreground.png'), optimize=True)
        layer(mono_src, scale, px).save(os.path.join(d, 'ic_launcher_monochrome.png'), optimize=True)

    os.makedirs(os.path.join(RES, 'values'), exist_ok=True)
    with open(os.path.join(RES, 'values', 'ic_launcher_background.xml'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                f'<!-- Generated by tools/make-icon.py: the border colour of {src_name}. -->\n'
                '<resources>\n'
                f'    <color name="ic_launcher_background">#{bg[0]:02X}{bg[1]:02X}{bg[2]:02X}</color>\n'
                '</resources>\n')
    adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
                '<!-- Generated by tools/make-icon.py - edit the script, not this file. -->\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@color/ic_launcher_background" />\n'
                '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
                '    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />\n'
                '</adaptive-icon>\n')
    anydpi = os.path.join(RES, 'mipmap-anydpi')
    os.makedirs(anydpi, exist_ok=True)
    for fname in ('ic_launcher.xml', 'ic_launcher_round.xml'):
        with open(os.path.join(anydpi, fname), 'w', encoding='utf-8', newline='\n') as f:
            f.write(adaptive)

    Image.open(SRC_PLATE).convert('RGB').resize((STORE_SIDE, STORE_SIDE), Image.LANCZOS).save(
        os.path.join(ROOT, 'design', 'icon', 'store_512.png'), optimize=True)
    print('written: mipmaps for', ', '.join(DENSITIES), '+ store_512.png + preview')


def write_preview(fg_src, mono_src, scale, bg, path):
    """The launcher icon as a phone shows it - 192 px of visible area under three mask shapes,
    then the themed (monochrome) icon tinted pink on a dark tonal disc."""
    vis = 192
    lay = round(vis * LAYER_DP / VISIBLE_DP)
    full = Image.new('RGBA', (lay, lay), bg + (255,))
    full.alpha_composite(layer(fg_src, scale, lay))
    off = (lay - vis) // 2
    visible = full.crop((off, off, off + vis, off + vis))
    mono = layer(mono_src, scale, lay).crop((off, off, off + vis, off + vis))
    themed = Image.new('RGBA', (vis, vis), (0x3A, 0x1A, 0x2A, 255))
    tint = Image.new('RGBA', (vis, vis), (0xFF, 0x9C, 0xC8, 255))
    tint.putalpha(mono.split()[3])
    themed.alpha_composite(tint)

    circle = Image.new('L', (vis, vis), 0)
    ImageDraw.Draw(circle).ellipse((0, 0, vis - 1, vis - 1), fill=255)
    rounded = Image.new('L', (vis, vis), 0)
    ImageDraw.Draw(rounded).rounded_rectangle((0, 0, vis - 1, vis - 1), radius=round(vis * 0.22), fill=255)
    masks = [circle, squircle_mask(vis), rounded, circle]
    tiles = [visible, visible, visible, themed]
    gap = 32
    sheet = Image.new('RGBA', (gap + len(tiles) * (vis + gap), vis + 2 * gap), (0x2B, 0x2B, 0x30, 255))
    for i, (t, m) in enumerate(zip(tiles, masks)):
        cell = Image.new('RGBA', (vis, vis), (0, 0, 0, 0))
        cell.paste(t, (0, 0), m)
        sheet.alpha_composite(cell, (gap + i * (vis + gap), gap))
    sheet.save(path, optimize=True)


if __name__ == '__main__':
    main()
