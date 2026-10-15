# The engine's tiling replayed in PyTorch (108 px tiles on the global grid, <context> px around each, as the engine's
# fixed-size DAT2 net does with context 10): tells apart "the device computes the tiles right" (device vs tiled torch)
# from "tiling itself changes the picture" (tiled vs untiled torch) on the 2 x 2 tiles around input corner (540, 972).
# Usage: .venv-convert/Scripts/python.exe -I -W ignore tools/tile-replay.py <weights> <input> <device output png> <context px>
# [NOT-TESTED as a general tool: written for researches/08 section 15, photo1.jpg 960x1280]
import sys
import numpy as np, torch, spandrel
from PIL import Image
Image.MAX_IMAGE_PIXELS = None
weights, src, dev_out = sys.argv[1], sys.argv[2], sys.argv[3]
T, S = 108, 4
P = int(sys.argv[4]); N = T + 2 * P
inp = np.asarray(Image.open(src).convert("RGB"))
H, W, _ = inp.shape
model = spandrel.ModelLoader().load_from_file(weights).eval()
def run(a):
    t = torch.from_numpy(np.ascontiguousarray(a)).permute(2, 0, 1)[None].float() / 255
    with torch.no_grad(): return np.rint(model(t)[0].clamp(0, 1).permute(1, 2, 0).numpy() * 255)
X, Y = 432, 864  # 2 x 2 tiles: input 432..648 x 864..1080, the corner (540, 972) in the middle
tiled = np.zeros((2 * T * S, 2 * T * S, 3), np.float32)
for j in range(2):
    for i in range(2):
        tx, ty = X + i * T, Y + j * T
        x0, y0, x1, y1 = max(tx - P, 0), max(ty - P, 0), min(tx + T + P, W), min(ty + T + P, H)
        a = inp[y0:y1, x0:x1]
        if a.shape[0] < N or a.shape[1] < N: a = np.pad(a, ((0, N - a.shape[0]), (0, N - a.shape[1]), (0, 0)), mode="edge")
        o = run(a)
        ox, oy = (tx - x0) * S, (ty - y0) * S
        tiled[j * T * S:(j + 1) * T * S, i * T * S:(i + 1) * T * S] = o[oy:oy + T * S, ox:ox + T * S]
dev = np.asarray(Image.open(dev_out).convert("RGB")).astype(np.float32)[Y * S:(Y + 2 * T) * S, X * S:(X + 2 * T) * S]
whole = run(inp[Y - 40:Y + 2 * T + 40, X - 40:X + 2 * T + 40])[160:-160, 160:-160]
def psnr(a, b): return 10 * np.log10(255 ** 2 / np.mean((a - b) ** 2))
print(f"device vs tiled torch   {psnr(dev, tiled):.2f} dB  (does the phone compute the same tiles?)")
print(f"tiled vs untiled torch  {psnr(tiled, whole):.2f} dB  (what tiling itself costs)")
print(f"device vs untiled torch {psnr(dev, whole):.2f} dB")
