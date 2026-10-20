#!/usr/bin/env python3
"""Generate the Lideri Chat app icons and the iOS launch logo from the brand kit.

    python3 design/lideri/generate-icons.py        # run from the repo root

Source (never modified): design/lideri/kit/logos/lideri-negativa.png
Only the LIDERI wordmark (first opaque pixel-row block) is used for icons; the
five icons below it are left out. The wordmark is not recoloured or stretched.

Writes:
    design/lideri/icon-master-1024.png                       (RGB, no alpha)
    app/src/main/res/mipmap-*/ic_launcher{,_foreground,_monochrome,_round}.png
    iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/*.png   (sizes from Contents.json)
    iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/*      (full signature, 480/720 px)
    iosApp/iosApp/Assets.xcassets/LaunchBackground.colorset/Contents.json

Requires Pillow and NumPy. Deterministic: running it twice changes nothing.
"""
import hashlib
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent.parent
SRC = ROOT / 'design' / 'lideri' / 'kit' / 'logos' / 'lideri-negativa.png'
MASTER = ROOT / 'design' / 'lideri' / 'icon-master-1024.png'
RES = ROOT / 'app' / 'src' / 'main' / 'res'
XCASSETS = ROOT / 'iosApp' / 'iosApp' / 'Assets.xcassets'

BG_HEX = '0424D9'
BG = tuple(int(BG_HEX[i:i + 2], 16) for i in (0, 2, 4))
CANVAS_DP, SAFE_DIAM_DP, WORD_DP = 108.0, 66.0, 60.0
LEGACY_WORD_FRACTION = 0.62
IOS_WORD_FRACTION = 0.70
LAUNCH_WIDTH_PT = 240
DENSITIES = [('mdpi', 108), ('hdpi', 162), ('xhdpi', 216), ('xxhdpi', 324), ('xxxhdpi', 432)]
LEGACY = [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]


def downscale(img, tw, th):
    """Resize through premultiplied alpha so the colour under transparent
    pixels does not bleed into the edges as a halo."""
    a = np.array(img).astype(np.float64)
    pre = np.concatenate([a[..., :3] * (a[..., 3:4] / 255.0), a[..., 3:4]], axis=2)
    small = np.array(Image.fromarray(np.clip(pre, 0, 255).astype(np.uint8), 'RGBA')
                     .resize((tw, th), Image.LANCZOS)).astype(np.float64)
    oa = small[..., 3:4]
    rgb = np.divide(small[..., :3] * 255.0, np.maximum(oa, 1e-6))
    return Image.fromarray(
        np.clip(np.concatenate([rgb, oa], axis=2), 0, 255).astype(np.uint8), 'RGBA')


def load_wordmark(full):
    alpha = np.array(full)[..., 3]
    rows = (alpha > 0).any(axis=1)
    blocks, start = [], None
    for y, on in enumerate(rows):
        if on and start is None:
            start = y
        elif not on and start is not None:
            blocks.append((start, y))
            start = None
    if start is not None:
        blocks.append((start, len(rows)))
    if len(blocks) != 2:
        sys.exit(f'expected exactly 2 pixel-row blocks in {SRC}, found {len(blocks)}')
    y0, y1 = blocks[0]
    cols = (alpha[y0:y1] > 0).any(axis=0)
    xs = np.where(cols)[0]
    return full.crop((int(xs.min()), y0, int(xs.max()) + 1, y1))


def word_at_width(word, width):
    w, h = word.size
    return downscale(word, max(1, round(width)), max(1, round(width * h / w)))


def centered(canvas_px, word_img):
    out = Image.new('RGBA', (canvas_px, canvas_px), (0, 0, 0, 0))
    out.paste(word_img, ((canvas_px - word_img.width) // 2, (canvas_px - word_img.height) // 2), word_img)
    return out


def silhouette(img):
    a = np.array(img)
    a[..., :3] = 255
    return Image.fromarray(a, 'RGBA')


def safe_circle_fraction(word):
    """Farthest wordmark corner from the layer centre, over the safe radius."""
    w, h = word.size
    scale = WORD_DP / w
    return float(np.hypot(WORD_DP / 2, h * scale / 2) / (SAFE_DIAM_DP / 2))


def write_android(word):
    for name, px in DENSITIES:
        fg = centered(px, word_at_width(word, WORD_DP / CANVAS_DP * px))
        fg.save(RES / f'mipmap-{name}' / 'ic_launcher_foreground.png')
        silhouette(fg).save(RES / f'mipmap-{name}' / 'ic_launcher_monochrome.png')
    for name, px in LEGACY:
        ss = px * 4
        base = Image.new('RGBA', (ss, ss), BG + (255,))
        base = Image.alpha_composite(base, centered(ss, word_at_width(word, LEGACY_WORD_FRACTION * ss)))
        for filename, shape in (('ic_launcher.png', 'sq'), ('ic_launcher_round.png', 'round')):
            mask = Image.new('L', (ss, ss), 0)
            d = ImageDraw.Draw(mask)
            if shape == 'round':
                d.ellipse((0, 0, ss - 1, ss - 1), fill=255)
            else:
                d.rounded_rectangle((0, 0, ss - 1, ss - 1), radius=int(ss * 0.20), fill=255)
            out = Image.new('RGBA', (ss, ss), (0, 0, 0, 0))
            out.paste(base, (0, 0), mask)
            out.resize((px, px), Image.LANCZOS).save(RES / f'mipmap-{name}' / filename)


def write_ios(word):
    bg = Image.new('RGBA', (1024, 1024), BG + (255,))
    master = Image.alpha_composite(bg, centered(1024, word_at_width(word, IOS_WORD_FRACTION * 1024))).convert('RGB')
    master.save(MASTER)
    icon_dir = XCASSETS / 'AppIcon.appiconset'
    contents = json.loads((icon_dir / 'Contents.json').read_text())
    for entry in contents['images']:
        side = float(entry['size'].split('x')[0]) * int(entry.get('scale', '1x').rstrip('x'))
        px = round(side)
        img = master if px == 1024 else master.resize((px, px), Image.LANCZOS)
        img.save(icon_dir / entry['filename'])
    return master


def write_launch(full):
    d = XCASSETS / 'LaunchLogo.imageset'
    d.mkdir(parents=True, exist_ok=True)
    w, h = full.size
    images = []
    for scale in (2, 3):
        tw = LAUNCH_WIDTH_PT * scale
        downscale(full, tw, round(tw * h / w)).save(d / f'LaunchLogo@{scale}x.png')
        images.append({'filename': f'LaunchLogo@{scale}x.png', 'idiom': 'universal', 'scale': f'{scale}x'})
    (d / 'Contents.json').write_text(json.dumps(
        {'images': images, 'info': {'author': 'xcode', 'version': 1}}, indent=2) + '\n')
    c = XCASSETS / 'LaunchBackground.colorset'
    c.mkdir(parents=True, exist_ok=True)
    comp = {'alpha': '1.000', 'blue': f'0x{BG[2]:02X}', 'green': f'0x{BG[1]:02X}', 'red': f'0x{BG[0]:02X}'}
    (c / 'Contents.json').write_text(json.dumps({
        'colors': [{'color': {'color-space': 'srgb', 'components': comp}, 'idiom': 'universal'}],
        'info': {'author': 'xcode', 'version': 1}}, indent=2) + '\n')


def main():
    full = Image.open(SRC).convert('RGBA')
    word = load_wordmark(full)
    print(f'wordmark {word.size[0]}x{word.size[1]} px')
    print(f'safe-circle fraction used by the wordmark: {safe_circle_fraction(word):.3f} (must be <= 1.0)')
    write_android(word)
    write_ios(word)
    write_launch(full)
    print('icon-master-1024.png sha256:', hashlib.sha256(MASTER.read_bytes()).hexdigest())


if __name__ == '__main__':
    main()
