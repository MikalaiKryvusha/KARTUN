# Seam check of a tiled upscale: mean |diff| between neighbour columns/rows AT tile borders vs near them and mid-tile;
# a ratio border/near close to 1 means no visible seam. Usage: python -I tools/seam-check.py <output image> <tile px in output> [<tile px in output, vertical>]
# The step may be fractional (a result limited by the output size, plans/08 step 9): the borders then stand where the engine
# puts them - output pixel ceil(k * step - 0.5) is the first one owned by tile k (firstOwned in kartun_engine.cpp).
# Kept in uint8 and diffed per column/row, so a 536 MP result fits in memory.
# [TESTED: 2026-10-15 · DAT2 photo1 on the Titan, step 432 - researches/08 section 15]
import math
import sys
import numpy as np
from PIL import Image
Image.MAX_IMAGE_PIXELS = None
path, step = sys.argv[1], float(sys.argv[2])
step_y = float(sys.argv[3]) if len(sys.argv) > 3 else step  # rows: a limited result scales the axes a hair apart
a = np.asarray(Image.open(path).convert("RGB"))
h, w, _ = a.shape
print("size", w, h, "step", step)
def col(x): return np.abs(a[:, x, :].astype(np.int16) - a[:, x - 1, :].astype(np.int16)).mean()
def row(y): return np.abs(a[y, :, :].astype(np.int16) - a[y - 1, :, :].astype(np.int16)).mean()
for name, n, f, step in (("vertical", w, col, step), ("horizontal", h, row, step_y)):
    edges = [math.ceil(k * step - 0.5) for k in range(1, int(n / step) + 1)]
    edges = [e for e in edges if 0 < e < n]
    border = [f(e) for e in edges]
    mid = [f(int(e - step / 2)) for e in edges if 0 < int(e - step / 2) < n]
    near = [f(e + d) for e in edges for d in (-3, -2, 2, 3) if 0 < e + d < n]
    b, m, nr = np.mean(border), np.mean(mid), np.mean(near)
    print(f"{name}: border {b:.3f}  mid-tile {m:.3f}  near-border {nr:.3f}  ratio border/near {b/nr:.3f}  worst border {max(border):.3f} at k={int(np.argmax(border))+1}")
