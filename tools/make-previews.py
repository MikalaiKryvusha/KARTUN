"""make-previews.py - one before/after picture per catalogue model for the «Все модели» cards (plans/05 step 3).

The card shows it the way Upscayl's model dialog does (the owner, chat 2026-10-16 ≈ 00:30 +03:00: «у апскейли они
красивые»): the same whole scene twice side by side - on the left the small picture enlarged 4x bicubically (no
network), on the right the model's own 4x result. Recipe, the same for every model: the subject photo of the model
(SUBJECTS below, sources in engine/third_party/preview-src/ with their credit files) is centre-cropped to a square and
shrunk to 128 x 128 - the small picture both halves start from (128 is also the DAT2 net's fixed input, so no model needs
tiling); the model runs 4x with ncnn on the PC CPU (the engine's ncnn release, compare-models.py's run()); the two 512 x
512 halves are joined into one 1024 x 512 WebP at docs/model-previews/<id>.webp. The «До» / «После» labels and the line
between the halves are drawn by the app, so they speak the user's language.
[NOT-TESTED: the 2:1 recipe of 2026-10-16]

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

SRC_DIR = Path("engine/third_party/preview-src")
FALLBACK = Path("engine/third_party/realesrgan-win/input2.jpg")  # the BSD-3 beach sample of Real-ESRGAN
SMALL = 128      # the small picture's side; also DAT2's fixed input
QUALITY = 84
OUT = Path("docs/model-previews")
# model id -> subject file in SRC_DIR (missing -> the beach sample). Filled when the subject photos are chosen.
SUBJECTS = {}

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


def small_picture(path):
    """The subject centre-cropped to a square and shrunk to SMALL x SMALL (high-quality downscale)."""
    img = Image.open(path).convert("RGB")
    side = min(img.size)
    left, top = (img.width - side) // 2, (img.height - side) // 2
    return np.asarray(img.crop((left, top, left + side, top + side)).resize((SMALL, SMALL), Image.LANCZOS))


def main():
    models = json.loads(Path("docs/model-catalogue.json").read_text(encoding="utf-8"))["models"]
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = []
    for m in models:
        src = SRC_DIR / SUBJECTS[m["id"]] if m["id"] in SUBJECTS else FALLBACK
        small = small_picture(src)
        param = cm.find(next(f["name"] for f in m["files"] if f["name"].endswith(".param")))
        binf = cm.find(next(f["name"] for f in m["files"] if f["name"].endswith(".bin")))
        size = fixed_input(param)
        rgb = small if not size else np.pad(small, ((0, size[1] - SMALL), (0, size[0] - SMALL), (0, 0)), mode="edge")
        after, dt = cm.run(param, binf, rgb)
        after = Image.fromarray(after[: SMALL * cm.SCALE, : SMALL * cm.SCALE])
        before = Image.fromarray(small).resize(after.size, Image.BICUBIC)
        pair = Image.new("RGB", (after.width * 2, after.height))
        pair.paste(before, (0, 0))
        pair.paste(after, (after.width, 0))
        pair.save(OUT / f"{m['id']}.webp", quality=QUALITY)
        sheet.append(pair)
        print(f"{m['id']}: {src.name} -> {pair.width}x{pair.height} in {dt:.1f} s")
    if "--sheet" in sys.argv:
        w, h = sheet[0].width // 2, sheet[0].height // 2
        cols = 2
        rows = (len(sheet) + cols - 1) // cols
        page = Image.new("RGB", (cols * (w + 8), rows * (h + 8)), (13, 13, 13))
        for i, p in enumerate(sheet):
            page.paste(p.resize((w, h), Image.LANCZOS), ((i % cols) * (w + 8), (i // cols) * (h + 8)))
        page.save(sys.argv[sys.argv.index("--sheet") + 1], quality=82)


if __name__ == "__main__":
    main()
