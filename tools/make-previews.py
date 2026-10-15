"""make-previews.py - one preview per catalogue model for the «Все модели» cards (plans/05 step 3).

Every model enlarges the SAME 80 x 80 crop of the BSD-3 beach sample (Real-ESRGAN's `input2.jpg`, the cliff on the left)
4x with ncnn on the PC CPU - the same ncnn release as the engine, through compare-models.py's run() - and the result is
written as a 320 x 320 WebP to docs/model-previews/<id>.webp. The same crop enlarged bicubically (no network) goes to
docs/model-previews/_bicubic.webp: the card's thumbnail shows it as the «before» half (mockup .mthumb). A net traced at one input size (DAT2: `0=128 1=128` in its
Input line, the engine's fixed-size path) gets the crop padded by repeating the edge, and the padding is cut off again.
The recipe is written down here so a new model's preview stands beside the old ones on equal terms (the first previews
of 2026-10-14 came from a crop nobody recorded, so all of them were rebuilt with this script).
[TESTED: 2026-10-16 · 14 previews written; the sheet of all 14 read by eye: the same cliff in every one, real
enlargements, no blank or shifted picture]

Usage (from the project root, conversion venv):
  .venv-convert/Scripts/python.exe -I tools/make-previews.py [--sheet <out.webp>]
"""
import importlib.util
import json
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

SAMPLE = Path("engine/third_party/realesrgan-win/input2.jpg")
CROP = (8, 52, 88, 132)  # left, top, right, bottom in the 256 x 256 sample: the cliff, the sea and the sand
OUT = Path("docs/model-previews")
QUALITY = 82

spec = importlib.util.spec_from_file_location("compare_models", "tools/compare-models.py")
cm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cm)


def fixed_input(param):
    """(w, h) from `Input ... 0=<w> 1=<h>` of a fixed-size net, else None."""
    for line in Path(param).read_text(encoding="utf-8").splitlines():
        if line.startswith("Input"):
            w, h = re.search(r"\b0=(\d+)", line), re.search(r"\b1=(\d+)", line)
            return (int(w.group(1)), int(h.group(1))) if w and h else None
    return None


def main():
    crop = np.asarray(Image.open(SAMPLE).convert("RGB").crop(CROP))
    models = json.loads(Path("docs/model-catalogue.json").read_text(encoding="utf-8"))["models"]
    OUT.mkdir(parents=True, exist_ok=True)
    side = (crop.shape[1] * cm.SCALE, crop.shape[0] * cm.SCALE)
    Image.fromarray(crop).resize(side, Image.BICUBIC).save(OUT / "_bicubic.webp", quality=QUALITY)
    shots = []
    for m in models:
        param = cm.find(next(f["name"] for f in m["files"] if f["name"].endswith(".param")))
        binf = cm.find(next(f["name"] for f in m["files"] if f["name"].endswith(".bin")))
        rgb, size = crop, fixed_input(param)
        if size:
            rgb = np.pad(crop, ((0, size[1] - crop.shape[0]), (0, size[0] - crop.shape[1]), (0, 0)), mode="edge")
        img, dt = cm.run(param, binf, rgb)
        img = img[: crop.shape[0] * cm.SCALE, : crop.shape[1] * cm.SCALE]
        Image.fromarray(img).save(OUT / f"{m['id']}.webp", quality=QUALITY)
        shots.append(img)
        print(f"{m['id']}: {img.shape[1]}x{img.shape[0]} in {dt:.1f} s")
    if "--sheet" in sys.argv:
        cols = 7
        rows = (len(shots) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * 324, rows * 324), (13, 13, 13))
        for i, s in enumerate(shots):
            sheet.paste(Image.fromarray(s), ((i % cols) * 324, (i // cols) * 324))
        sheet.save(sys.argv[sys.argv.index("--sheet") + 1], quality=80)


if __name__ == "__main__":
    main()
