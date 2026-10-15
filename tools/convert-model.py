"""convert-model.py - convert a super-resolution model's original PyTorch weights (.pth / .safetensors) into the NCNN
.param/.bin pair the KARTUN engine runs (input blob "data", output blob "output"), then CHECK the result.

Any architecture the spandrel library loads is accepted (ESRGAN/RRDB with old or new key names, Real-ESRGAN Compact,
SPAN, RealPLKSR, ...). The engine only runs plain NCNN layers, so the tool:
  1. replaces RealPLKSR's global skip `torch.repeat_interleave` (no NCNN layer) with a 1x1 convolution whose weight is
     one-hot - the same tensor, bit for bit (checked on every run);
  2. refuses (exit 1) when the exported graph still holds a layer NCNN cannot run (pnnx leaves those as pnnx.*, F.*,
     torch.*, Tensor.*, nn.*);
  3. runs the NCNN net (fp32, CPU, the ncnn Python package of the same release as the engine) and the PyTorch model on
     one crop of a real photo and refuses (exit 1) when they differ by less than 45 dB PSNR.
It prints each output file's SHA-256 and size - the lines docs/model-catalogue.json takes.
History: until 2026-10-15 the tool restated Real-ESRGAN's SRVGGNetCompact by hand and converted only
realesr-general-x4v3; the spandrel loader reproduces that model's files byte for byte (checked when the tool changed).
[TESTED: 2026-10-15 · realesr-general-x4v3 re-converted byte-identical to docs/model-licenses.md (param 2bf1ccee…,
bin 8fdd6052…); seven catalogue models converted, 60.3-72.9 dB vs PyTorch on photo1.jpg; red on a copy with the
repeat_interleave patch disabled (REFUSED, exit 1) and on --verify of Remacri weights against the Siax files (22.8 dB,
exit 1). On the device: not yet]

Needs, in the conversion venv: torch, pnnx, spandrel, ncnn (pip names; versions in HOUSE_RULES.md section 6).
Usage (from the project root):
  .venv-convert/Scripts/python.exe -I tools/convert-model.py <weights> <out_dir> [name] [--image <photo>] [--fixed <px>]
  .venv-convert/Scripts/python.exe -I tools/convert-model.py --verify <weights> <param> <bin> [--image <photo>] [--fixed <px>]
DAT (UltraSharp V2 and other DAT2 models) is rewritten by tools/ncnn_dat.py and traced at ONE tile size: --fixed 128
writes `0=128 1=128 2=3` into the Input layer, and the engine feeds such a net 128 px tiles, padding the edge ones.
"""
import hashlib
import math
import sys
from collections import Counter
from pathlib import Path

import numpy as np
import torch

DEFAULT_IMAGE = Path("engine/third_party/testdata/photo1.jpg")
MIN_PSNR_DB = 45.0  # fp16 weights land at 60-73 dB; a wrong graph or wrong weights land far below (bicubic is ~30 dB)
CHECK_CROP = 96  # px; small enough for a 23-block RRDB on the CPU in seconds
# layer types pnnx writes when NCNN has no such layer ("aten::" seen 2026-10-15: F.normalize -> aten::clamp_min)
NCNN_FOREIGN = ("pnnx.", "F.", "torch.", "Tensor.", "nn.", "aten::")


def load_ncnn_dat():
    """tools/ncnn_dat.py, imported by path: `python -I` keeps the script's own directory off sys.path."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("ncnn_dat", Path(__file__).with_name("ncnn_dat.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def load(weights, fixed=None):
    """Load the weights with spandrel and make the graph NCNN-runnable; returns spandrel's model descriptor."""
    import spandrel

    desc = spandrel.ModelLoader().load_from_file(str(weights))
    model = desc.model.eval()
    if desc.architecture.name == "DAT":  # 6-D windows, 2-axis roll, F.normalize -> ncnn-safe ops (researches/08)
        if not fixed:
            sys.exit("DAT is traced at one tile size: pass --fixed <px> (a multiple of 32, e.g. 128)")
        print(f"DAT: {load_ncnn_dat().patch(model)} modules rewritten for ncnn")
    if hasattr(model, "repeat_op"):  # RealPLKSR: x = feats(x) + repeat_interleave(x, s*s, dim=1)
        s2 = desc.scale * desc.scale
        probe = torch.rand(1, 3, 40, 36)
        with torch.no_grad():
            before = model(probe)
        conv = torch.nn.Conv2d(3, 3 * s2, 1, bias=False)
        with torch.no_grad():
            conv.weight.zero_()
            for o in range(3 * s2):
                conv.weight[o, o // s2, 0, 0] = 1.0  # output channel o repeats input channel o // s2
        model.repeat_op = conv
        with torch.no_grad():
            diff = float((model(probe) - before).abs().max())
        if diff != 0.0:
            sys.exit(f"repeat_interleave replacement changed the output (max abs {diff})")
    print(f"loaded: {desc.architecture.name}, scale {desc.scale}")
    return desc


def psnr_vs_torch(desc, param, binf, image, size=CHECK_CROP):
    """Run the NCNN net and the PyTorch model on the centre crop of `image`; return PSNR in dB."""
    import ncnn
    from PIL import Image

    img = Image.open(image).convert("RGB")
    w, h = img.size
    x0, y0 = (w - size) // 2, (h - size) // 2
    crop = np.asarray(img.crop((x0, y0, x0 + size, y0 + size)), dtype=np.float32) / 255.0
    chw = np.ascontiguousarray(crop.transpose(2, 0, 1))  # the engine feeds RGB 0..1, CHW
    with torch.no_grad():
        ref = desc.model(torch.from_numpy(chw)[None]).clamp(0, 1)[0].numpy()

    net = ncnn.Net()
    # A fixed-size net (DAT) is checked on the GPU: ncnn's CPU SDPA crashes on the channel attention's long vectors
    # (2026-10-15, 6 x 30 x 4096: access violation; the Vulkan kernel is exact), and the phone runs DAT on the GPU anyway.
    net.opt.use_vulkan_compute = size != CHECK_CROP and ncnn.get_gpu_count() > 0
    net.opt.use_fp16_packed = net.opt.use_fp16_storage = net.opt.use_fp16_arithmetic = False
    net.opt.use_bf16_storage = False
    if net.load_param(str(param)) != 0 or net.load_model(str(binf)) != 0:
        sys.exit(f"ncnn cannot load {param} / {binf}")
    ex = net.create_extractor()
    ex.input("data", ncnn.Mat(chw))
    rc, out = ex.extract("output")
    if rc != 0:
        sys.exit(f"ncnn extract failed: {rc}")
    got = np.clip(np.array(out), 0, 1)
    if net.opt.use_vulkan_compute:  # release the GPU before exit: without it the process segfaults at shutdown
        del ex, out
        net.clear()
        del net
        ncnn.destroy_gpu_instance()
    if got.shape != ref.shape:
        sys.exit(f"ncnn output {got.shape} != PyTorch output {ref.shape}")
    mse = float(((got - ref) ** 2).mean())
    return 99.0 if mse == 0 else 10 * math.log10(1.0 / mse)


def check(desc, param, binf, image, fixed=None):
    db = psnr_vs_torch(desc, param, binf, image, fixed or CHECK_CROP)
    print(f"ncnn vs PyTorch on {Path(image).name}: {db:.1f} dB (refuse below {MIN_PSNR_DB})")
    if db < MIN_PSNR_DB:
        sys.exit(f"REFUSED: {param} does not compute what the weights compute")


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def take_image(args):
    if "--image" in args:
        i = args.index("--image")
        image = Path(args[i + 1])
        del args[i:i + 2]
        return image
    return DEFAULT_IMAGE


def take_fixed(args):
    if "--fixed" in args:
        i = args.index("--fixed")
        fixed = int(args[i + 1])
        del args[i:i + 2]
        return fixed
    return None


def convert(weights, out_dir, name, image, fixed=None):
    out_dir.mkdir(parents=True, exist_ok=True)
    desc = load(weights, fixed)
    import pnnx  # imported late: only the export needs it

    pt_path = (out_dir / f"{name}.pt").resolve()
    # Two input shapes mark H and W as dynamic, so the NCNN net accepts any tile size; a --fixed net is traced at one
    # size. The path goes in with forward slashes: pnnx writes it into a generated .py, where a Windows "\t" or "\r"
    # would turn into an escape (EXP-0003).
    with torch.no_grad():
        if fixed:
            pnnx.export(desc.model, pt_path.as_posix(), inputs=torch.rand(1, 3, fixed, fixed))
        else:
            m = desc.size_requirements.multiple_of if desc.size_requirements else 1
            h1, w1, h2, w2 = [((v + m - 1) // m) * m for v in (64, 64, 96, 80)]
            pnnx.export(desc.model, pt_path.as_posix(), inputs=torch.rand(1, 3, h1, w1), inputs2=torch.rand(1, 3, h2, w2))

    # pnnx names its outputs after the .pt stem with "-" turned into "_" and the blobs in0/out0; the engine reads "data"
    # and writes "output" (like the official models).
    stem = pt_path.stem.replace("-", "_")
    text = (out_dir / f"{stem}.ncnn.param").read_text(encoding="utf-8")
    text = text.replace(" in0", " data").replace(" out0", " output")
    if fixed:  # the Input layer carries the size (w=0, h=1, c=2): the engine reads it and pads edge tiles up to it
        lines = text.splitlines(keepends=True)
        i = next(k for k, ln in enumerate(lines) if ln.startswith("Input"))
        lines[i] = lines[i].rstrip("\r\n") + f" 0={fixed} 1={fixed} 2=3\n"
        text = "".join(lines)
    foreign = Counter(t for t in (ln.split()[0] for ln in text.splitlines()[2:] if ln.strip()) if t.startswith(NCNN_FOREIGN))
    if foreign:
        sys.exit(f"REFUSED: layers NCNN cannot run: {dict(foreign)}")
    param, binf = out_dir / f"{name}.param", out_dir / f"{name}.bin"
    param.write_text(text, encoding="utf-8")
    binf.write_bytes((out_dir / f"{stem}.ncnn.bin").read_bytes())
    check(desc, param, binf, image, fixed)
    for f in (param, binf):
        print(f"{f.name}  sha256 {sha(f)}  bytes {f.stat().st_size}")


def main():
    args = sys.argv[1:]
    image = take_image(args)
    fixed = take_fixed(args)
    if args and args[0] == "--verify":
        weights, param, binf = map(Path, args[1:4])
        check(load(weights, fixed), param, binf, image, fixed)
        return
    weights, out_dir = Path(args[0]), Path(args[1])
    convert(weights, out_dir, args[2] if len(args) > 2 else weights.stem, image, fixed)


if __name__ == "__main__":
    main()
