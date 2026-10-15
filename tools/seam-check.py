# Seam check of a tiled upscale: mean |diff| between neighbour columns/rows AT tile borders vs near them and mid-tile;
# a ratio border/near close to 1 means no visible seam. Usage: python -I tools/seam-check.py <output png> <tile px in output>
# [TESTED: 2026-10-15 · DAT2 photo1 on the Titan, step 432 - researches/08 section 15]
import sys
import numpy as np
from PIL import Image
Image.MAX_IMAGE_PIXELS = None
path, step = sys.argv[1], int(sys.argv[2])
a = np.asarray(Image.open(path).convert("RGB")).astype(np.float32)
h, w, _ = a.shape
print("size", w, h, "step", step)
def col(x): return np.abs(a[:, x, :] - a[:, x - 1, :]).mean()
def row(y): return np.abs(a[y, :, :] - a[y - 1, :, :]).mean()
for name, n, f in (("vertical", w, col), ("horizontal", h, row)):
    border = [f(k) for k in range(step, n, step)]
    mid = [f(k + step // 2) for k in range(0, n - step // 2, step) if k + step // 2 < n]
    near = [f(k + d) for k in range(step, n, step) for d in (-3, -2, 2, 3) if 0 < k + d < n]
    b, m, nr = np.mean(border), np.mean(mid), np.mean(near)
    print(f"{name}: border {b:.3f}  mid-tile {m:.3f}  near-border {nr:.3f}  ratio border/near {b/nr:.3f}  worst border {max(border):.3f} at k={int(np.argmax(border))+1}")
