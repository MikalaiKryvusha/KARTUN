"""dat-blob-stats.py - the PC half of the layer-by-layer comparison of an ncnn net between the PC and the Titan.

Runs the net on the same flat input the engine's debug probe uses (w x h x 3, 0.5 everywhere - kartun_engine.cpp
nativeTimeToBlob), extracts each named blob with a fresh extractor and prints one JSON line per blob with the numbers
logBlobStats writes to logcat on the device: shape, sum, mean |x|, NaN/inf count, first four values.
With --device <logcat file> it also reads the device's `blobstat` lines and prints, per blob, PC vs device and the
relative difference of mean |x| - the first blob that differs is the layer to look at (researches/08 section 9).

Usage (from the project root):
  .venv-convert/Scripts/python.exe -I tools/dat-blob-stats.py <param> <bin> --blobs 5,10,21 [--cpu] [--size 128]
      [--device <logcat.txt>]
[NOT-TESTED]
"""
import argparse
import json
import re

import numpy as np
import ncnn


def stats(name, a):
    flat = a.astype(np.float64).ravel()
    ok = np.isfinite(flat)
    good = flat[ok]
    return {"blob": name, "shape": list(a.shape), "sum": float(good.sum()),
            "mabs": float(np.abs(good).mean()) if good.size else 0.0, "bad": int((~ok).sum()),
            "v": [float(x) for x in flat[:4]]}


def run(param, binf, blobs, cpu, size):
    net = ncnn.Net()
    net.opt.use_vulkan_compute = not cpu
    # the engine keeps fixed-size (DAT) nets in fp32 storage - same here
    net.opt.use_fp16_storage = False
    net.opt.use_fp16_packed = False
    net.opt.use_fp16_arithmetic = False
    net.load_param(param)
    net.load_model(binf)
    # ncnn.Mat(array) borrows the array's memory: keep the array alive, or the input is freed garbage (seen 2026-10-15)
    arr = np.full((3, size, size), 0.5, dtype=np.float32)
    inp = ncnn.Mat(arr).clone()
    out = []
    for b in blobs:
        ex = net.create_extractor()
        ex.input("data", inp)
        rc, m = ex.extract(b)
        out.append(stats(b, np.array(m)) if rc == 0 else {"blob": b, "error": rc})
    return out


def read_device(path):
    dev = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = re.search(r'(\{"blobstat".*\})', line)
            if m:
                d = json.loads(m.group(1))
                dev[d["blobstat"]] = d
    return dev


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("param")
    ap.add_argument("bin")
    ap.add_argument("--blobs", required=True)
    ap.add_argument("--cpu", action="store_true")
    ap.add_argument("--size", type=int, default=128)
    ap.add_argument("--device")
    a = ap.parse_args()
    blobs = [b.strip() for b in a.blobs.split(",") if b.strip()]
    pc = run(a.param, a.bin, blobs, a.cpu, a.size)
    dev = read_device(a.device) if a.device else {}
    for s in pc:
        if a.device:
            d = dev.get(s["blob"])
            if d is None or "mabs" not in s:
                print(s["blob"], "pc", s.get("mabs"), "device", None)
                continue
            rel = abs(d["mabs"] - s["mabs"]) / max(abs(s["mabs"]), 1e-12)
            flag = "DIFF" if rel > 1e-3 or d["bad"] else "ok"
            print(f'{s["blob"]:>6} {flag:4} pc mabs={s["mabs"]:.6g} sum={s["sum"]:.6g} v={[round(x, 4) for x in s["v"]]} | '
                  f'dev mabs={d["mabs"]:.6g} sum={d["sum"]:.6g} bad={d["bad"]} v={[round(x, 4) for x in d["v"]]} '
                  f'shape pc={s["shape"]} dev=({d["c"]},{d["d"]},{d["h"]},{d["w"]})')
        else:
            print(json.dumps(s))


if __name__ == "__main__":
    main()
