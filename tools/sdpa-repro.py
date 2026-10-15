"""sdpa-repro.py - minimal ncnn nets with ONE SDPA layer, to reproduce on a device what DAT2's channel attention does.

Why: on the Titan's Mali-G615 the layer-by-layer comparison (tools/dat-blob-stats.py) found the first wrong layer of
UltraSharp V2 to be sdpa_4671 - SDPA over channels: 6 heads (groups) x 30 channels (sequence) x 16384 pixels (embed dim),
no mask, scale 1 - while its inputs matched the PC exactly. A net of 3x3 conv -> reshape -> SDPA -> reshape -> 3x3 conv
lets the debug probe run that one layer with chosen shapes; the variants change one axis at a time.

Writes <out>/sdpa_<name>.param/.bin for each variant, and sdpa_<name>_f with a fixed 128x128 input (fp32 in the engine). Input: any w x h x 3 picture (the probe feeds 0.5 everywhere to
`blobs` runs); blob "sdpa" is the SDPA output. Compare the device with the PC by tools/dat-blob-stats.py --blobs sdpa.
Usage: .venv-convert/Scripts/python.exe -I tools/sdpa-repro.py <out dir>
[NOT-TESTED]
"""
import os
import sys

import numpy as np

# name: (channels C, heads G) -> SDPA sees (G groups, C/G sequence, w*h embed)
VARIANTS = {
    "s30g6": (180, 6),    # DAT2's channel attention: sequence 30
    "s5g6": (30, 6),      # last session's test: sequence 5 (was correct on the Mali)
    "s30g1": (30, 1),     # sequence 30, one group
    "s16g1": (16, 1),     # sequence 16
    "s8g1": (8, 1),       # sequence 8
    "s4g1": (4, 1),       # sequence 4
}


def conv_weights(rng, cout, cin):
    w = (rng.standard_normal((cout, cin, 3, 3)) * 0.3).astype(np.float32)
    b = (rng.standard_normal(cout) * 0.1).astype(np.float32)
    return w, b


def write(out, name, c, g, fixed=False):
    rng = np.random.default_rng(7)
    w0, b0 = conv_weights(rng, c, 3)
    w1, b1 = conv_weights(rng, 3, c)
    seq = c // g
    lines = [
        "7767517",
        "7 9",
        "Input data 0 1 data" + (" 0=128 1=128 2=3" if fixed else ""),
        f"Convolution conv_0 1 1 data c0 0={c} 1=3 11=3 12=1 13=1 14=1 2=1 3=1 4=1 5=1 6={w0.size}",
        f"Reshape r0 1 1 c0 qkv 0=-1 1={seq} 2={g}",
        "Split sp 1 3 qkv q k v",
        # scale 1/sqrt(embed) is set by the run size; a fixed small scale keeps softmax away from one-hot here
        "SDPA sdpa 3 1 q k v sdpa 5=0 6=0.001",
        "Reshape r1 1 1 sdpa r1o 0=-1 1=1 2=" + str(c),
        "Noop keep 1 1 r1o kept",
        f"Convolution conv_1 1 1 kept output 0=3 1=3 11=3 12=1 13=1 14=1 2=1 3=1 4=1 5=1 6={w1.size}",
    ]
    # ncnn layer count: 8 layers, blob count: data c0 qkv q k v sdpa r1o kept output = 10
    lines[1] = "8 10"
    with open(os.path.join(out, f"sdpa_{name}.param"), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(out, f"sdpa_{name}.bin"), "wb") as f:
        for w, b in ((w0, b0), (w1, b1)):
            f.write(np.zeros(1, dtype=np.uint32).tobytes())  # weight tag 0 = raw fp32
            f.write(w.tobytes())
            f.write(b.tobytes())


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, (c, g) in VARIANTS.items():
        write(out, name, c, g)
        # "_f": a fixed 128x128 input - the engine then keeps blobs in fp32 storage, as it does for DAT2
        write(out, name + "_f", c, g, fixed=True)
        print("wrote", name, name + "_f")


if __name__ == "__main__":
    main()
