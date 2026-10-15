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
SUBJECTS = {  # the owner's themes, 2026-10-16 ≈ 00:30: «природа, греция, пантеры, коты, цветки, луга, альпы, фолрды, снег» ·
    # «новогодняя елка» · «цветки, пчелы»; photos and licences: engine/third_party/preview-src/*.credit.txt
    "realesr-general-x4v3": "nature.jpg",
    "realesrgan-x4plus": "alps.jpg",
    "realesrgan-x4plus-anime": "anime1.jpg",
    "realesr-animevideov3-x4": "anime2.jpg",
    "4xNomos8kSC_fp16": "fjord.jpg",
    "4xHFA2k_fp16": "anime2.jpg",
    "4x-NMKD-Superscale-fp16": "autumn.jpg",  # the owner: «осенний лес бы ещё вместо того белого здания»
    "4x-NMKD-Siax-fp16": ("panther.jpg", 0.18, 0.42, 0.55),  # the owner: «гепард не влез» · «нужно мордовку чтобы было видно»
    "4xNomosWebPhoto-RealPLKSR-fp16": "cat.jpg",
    "4x-ClearRealityV1-fp16": "flowers_bee.jpg",  # portrait.jpg waits for the owner (an identifiable person)
    "4x-Remacri-fp16": "meadow.jpg",
    "4x-UltraMix-Balanced-fp16": "winter.jpg",  # the owner: «зиму чуть другую, тут много белого» · «сосновый зимний лес да, но другой»
    "4x-UltraSharpV2-Lite-fp16": ("black_panther.jpg", 0.5, 0.47, 0.95),  # «черную пантеру вместо той на полке»
    "4x-UltraSharpV2-fp16": "christmas_tree.jpg",
}

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


def subject(model_id):
    """(file, cx, cy, size): the subject photo, the crop's centre as fractions of width and height, and the square's side
    as a fraction of the photo's shorter side; a bare file name means the centre and the whole shorter side."""
    v = SUBJECTS.get(model_id)
    if v is None:
        return FALLBACK, 0.5, 0.5, 1.0
    if isinstance(v, str):
        return SRC_DIR / v, 0.5, 0.5, 1.0
    return SRC_DIR / v[0], v[1], v[2], v[3]


def small_picture(path, cx=0.5, cy=0.5, frac=1.0):
    """A square around (cx, cy) - kept inside the photo - shrunk to SMALL x SMALL (high-quality downscale)."""
    img = Image.open(path).convert("RGB")
    side = int(min(img.size) * frac)
    left = min(max(int(img.width * cx) - side // 2, 0), img.width - side)
    top = min(max(int(img.height * cy) - side // 2, 0), img.height - side)
    return np.asarray(img.crop((left, top, left + side, top + side)).resize((SMALL, SMALL), Image.LANCZOS))


def main():
    models = json.loads(Path("docs/model-catalogue.json").read_text(encoding="utf-8"))["models"]
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = []
    credits = {}
    for m in models:
        src, cx, cy, frac = subject(m["id"])
        credit = src.with_suffix(".credit.txt")  # nature.jpg -> nature.credit.txt
        if src != FALLBACK:
            # A subject photo ships only with its author line (CC BY / CC BY-SA ask for it; the app shows it in «О приложении»).
            if not credit.exists():
                sys.exit(f"no credit file {credit}")
            credits[credit.read_text(encoding="utf-8").strip()] = True
        small = small_picture(src, cx, cy, frac)
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
    credits["input2.jpg — Real-ESRGAN sample — BSD-3-Clause — https://github.com/xinntao/Real-ESRGAN"] = any(
        subject(m["id"])[0] == FALLBACK for m in models)
    lines = sorted(k for k, used in credits.items() if used)
    (OUT / "credits.json").write_text(json.dumps(lines, ensure_ascii=False, indent=1) + "\n", encoding="utf-8", newline="\n")
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
