"""convert-model.py - convert the official realesr-general-x4v3 PyTorch weights (xinntao/Real-ESRGAN, BSD-3)
into NCNN .param/.bin with pnnx, with the blob names the KARTUN engine expects (input "data", output "output").

The SRVGGNetCompact architecture below follows Real-ESRGAN's realesrgan/archs/srvgg_arch.py
(BSD-3-Clause, (c) 2021 Xintao Wang) - restated here so no Real-ESRGAN package is needed. [NOT-TESTED]

Usage (from the project root, with the conversion venv):
  .venv-convert/Scripts/python.exe -I tools/convert-model.py <weights.pth> <out_dir> [name]
"""
import sys
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F


class SRVGGNetCompact(nn.Module):
    """Compact VGG-style SR net: conv + PReLU body, pixel-shuffle upsampler, nearest-neighbour skip."""

    def __init__(self, num_in_ch=3, num_out_ch=3, num_feat=64, num_conv=32, upscale=4):
        super().__init__()
        self.upscale = upscale
        self.body = nn.ModuleList()
        self.body.append(nn.Conv2d(num_in_ch, num_feat, 3, 1, 1))
        self.body.append(nn.PReLU(num_parameters=num_feat))
        for _ in range(num_conv):
            self.body.append(nn.Conv2d(num_feat, num_feat, 3, 1, 1))
            self.body.append(nn.PReLU(num_parameters=num_feat))
        self.body.append(nn.Conv2d(num_feat, num_out_ch * upscale * upscale, 3, 1, 1))
        self.upsampler = nn.PixelShuffle(upscale)

    def forward(self, x):
        out = x
        for layer in self.body:
            out = layer(out)
        out = self.upsampler(out)
        return out + F.interpolate(x, scale_factor=self.upscale, mode="nearest")


def main():
    weights, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    name = sys.argv[3] if len(sys.argv) > 3 else weights.stem
    out_dir.mkdir(parents=True, exist_ok=True)

    model = SRVGGNetCompact()
    state = torch.load(weights, map_location="cpu", weights_only=True)
    model.load_state_dict(state.get("params", state), strict=True)
    model.eval()

    import pnnx  # imported late: it is only needed for the export step

    pt_path = (out_dir / f"{name}.pt").resolve()
    # Two input shapes mark H and W as dynamic, so the NCNN net accepts any tile size.
    # The path goes in with forward slashes: pnnx writes it into a generated .py, where a Windows "\t" or "\r"
    # would turn into an escape (EXP-0003 class).
    pnnx.export(model, pt_path.as_posix(), inputs=torch.rand(1, 3, 64, 64), inputs2=torch.rand(1, 3, 96, 80))

    # pnnx names its outputs after the .pt stem with "-" turned into "_" (realesr_general_x4v3.ncnn.param).
    stem = pt_path.stem.replace("-", "_")
    # pnnx names the blobs in0/out0; the engine reads "data" and writes "output" (like the official models).
    param = out_dir / f"{stem}.ncnn.param"
    text = param.read_text(encoding="utf-8")
    text = text.replace(" in0", " data").replace(" out0", " output")
    (out_dir / f"{name}.param").write_text(text, encoding="utf-8")
    (out_dir / f"{name}.bin").write_bytes((out_dir / f"{stem}.ncnn.bin").read_bytes())
    print(f"written {out_dir / (name + '.param')} and .bin")


if __name__ == "__main__":
    main()
