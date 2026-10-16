"""pc-upscale.py - upscale pictures 4x on THIS PC's graphics card with a KARTUN ncnn model (Vulkan), keeping transparency.

Made for the Krinik Tribit session's task (2026-10-16 ≈ 20:14, the owner's words relayed by it: «улучшил по качеству» ·
«чтобы по контуру убрал белое свечение» · «пусть лучшей моделью ультрашарп в2 улучшает на ГПУ компа»).

- Colour: the model over 128 px windows with 10 px of context each side - the cut of the app's engine (kartun_engine.cpp,
  PREPADDING 10); fixed-input nets (DAT2: `0=128` in the .param) take exactly that window.
- Alpha: Lanczos 4x (a mask needs no invented detail).
- Defringe (`--defringe N`, default 3): cut-outs made on a white background keep white in their edge pixels, which glow on
  a dark screen. Before the upscale, the colour of every pixel within N px of the edge (and of the transparent area) is
  replaced by the colour bleeding out from deeper inside the object - Photoshop's «Defringe»; alpha stays as it was.

Usage: .venv-convert/Scripts/python.exe -I tools/pc-upscale.py <in.png|dir> <out dir> [--model <param base>] [--defringe N]
[NOT-TESTED]
"""
import os
import sys
import time

import numpy as np
from PIL import Image, ImageFilter

import ncnn

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_MODEL = os.path.join(ROOT, "engine", "third_party", "catalogue", "4x-UltraSharpV2-fp16")
SCALE = 4
WINDOW = 128          # the net's fixed input
PAD = 10              # context each side, as the engine's PREPADDING
CORE = WINDOW - 2 * PAD
FAR_BLUR = 48         # radius that fills the far transparent area with the object's average colour


def arg(name, default=None):
    if name in sys.argv:
        return sys.argv[sys.argv.index(name) + 1]
    return default


def defringe(rgba: np.ndarray, width: int) -> np.ndarray:
    """Colour of the edge band and of the transparent area := colour bled from the object's inside; alpha untouched."""
    rgb = rgba[..., :3].astype(np.float32)
    alpha = rgba[..., 3]
    inner = Image.fromarray(((alpha >= 250) * 255).astype(np.uint8))
    if width > 0:
        inner = inner.filter(ImageFilter.MinFilter(2 * width + 1))
    known = np.asarray(inner) > 0
    # Far fill first: a wide blur of the known colours (normalised by the known weight) for the whole picture.
    k = known.astype(np.float32)
    def blur(a):  # a wide smooth average: box-shrink by FAR_BLUR, then bilinear back (PIL blurs no float images)
        img = Image.fromarray(a.astype(np.float32), "F")
        small = img.resize((max(1, a.shape[1] // FAR_BLUR), max(1, a.shape[0] // FAR_BLUR)), Image.BOX)
        return np.asarray(small.resize((a.shape[1], a.shape[0]), Image.BILINEAR), dtype=np.float32)
    w = blur(k)
    far = np.stack([blur(rgb[..., c] * k) for c in range(3)], -1) / np.maximum(w, 1e-6)[..., None]
    out = np.where(known[..., None], rgb, far)
    # Near fill: grow the known area ring by ring, each new pixel = mean of its known 3x3 neighbours (exact local colour).
    filled = known.copy()
    for _ in range(width + 2 * PAD):
        acc = np.zeros_like(out)
        cnt = np.zeros(filled.shape, np.float32)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                f = np.roll(np.roll(filled, dy, 0), dx, 1)
                acc += np.roll(np.roll(out * filled[..., None], dy, 0), dx, 1)
                cnt += f
        new = (~filled) & (cnt > 0)
        if not new.any():
            break
        out[new] = acc[new] / cnt[new][:, None]
        filled |= new
    res = rgba.copy()
    res[..., :3] = np.clip(out + 0.5, 0, 255).astype(np.uint8)
    return res


def upscale_rgb(net: ncnn.Net, rgb: np.ndarray) -> np.ndarray:
    h, w, _ = rgb.shape
    ty, tx = (h + CORE - 1) // CORE, (w + CORE - 1) // CORE
    padded = np.pad(rgb, ((PAD, PAD + ty * CORE - h), (PAD, PAD + tx * CORE - w), (0, 0)), mode="reflect")
    out = np.zeros((ty * CORE * SCALE, tx * CORE * SCALE, 3), np.uint8)
    t0 = time.time()
    for y in range(ty):
        for x in range(tx):
            win = np.ascontiguousarray(padded[y * CORE:y * CORE + WINDOW, x * CORE:x * CORE + WINDOW])
            m = ncnn.Mat.from_pixels(win, ncnn.Mat.PixelType.PIXEL_RGB, WINDOW, WINDOW)
            m.substract_mean_normalize([], [1 / 255.0] * 3)
            ex = net.create_extractor()
            ex.input("data", m)
            _, o = ex.extract("output")
            a = np.clip(np.array(o).transpose(1, 2, 0) * 255 + 0.5, 0, 255).astype(np.uint8)
            c = a[PAD * SCALE:(PAD + CORE) * SCALE, PAD * SCALE:(PAD + CORE) * SCALE]
            out[y * CORE * SCALE:(y + 1) * CORE * SCALE, x * CORE * SCALE:(x + 1) * CORE * SCALE] = c
        print(f"  row {y + 1}/{ty} · {time.time() - t0:.0f} s", flush=True)
    return out[:h * SCALE, :w * SCALE]


def main():
    src, dst = sys.argv[1], sys.argv[2]
    model = arg("--model", DEFAULT_MODEL)
    width = int(arg("--defringe", "3"))
    files = [os.path.join(src, f) for f in sorted(os.listdir(src)) if f.lower().endswith(".png")] if os.path.isdir(src) else [src]
    os.makedirs(dst, exist_ok=True)
    net = ncnn.Net()
    net.opt.use_vulkan_compute = True
    # DAT2 in fp16 storage overflows to inf in its first Gemm (researches/08 §9; the engine turns it off the same way).
    net.opt.use_fp16_storage = False
    net.opt.use_fp16_packed = False
    net.load_param(model + ".param")
    net.load_model(model + ".bin")
    for f in files:
        name = os.path.basename(f)
        im = Image.open(f).convert("RGBA")
        rgba = np.asarray(im)
        t0 = time.time()
        print(f"{name}: {im.width} x {im.height}", flush=True)
        clean = defringe(rgba, width)
        rgb = upscale_rgb(net, clean[..., :3])
        alpha = im.getchannel("A").resize((im.width * SCALE, im.height * SCALE), Image.LANCZOS)
        out = Image.fromarray(rgb, "RGB")
        out.putalpha(alpha)
        out.save(os.path.join(dst, name), optimize=False)
        print(f'{{"file":"{name}","in":[{im.width},{im.height}],"out":[{out.width},{out.height}],"seconds":{time.time() - t0:.0f}}}', flush=True)
    net.clear()
    ncnn.destroy_gpu_instance()


if __name__ == "__main__":
    main()
