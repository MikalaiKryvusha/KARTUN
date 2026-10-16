"""compare-models.py - one photo through every KARTUN model, side by side: a comparison page for the owner's eye.

The photo is shrunk to a "messenger" copy (width --input-width, JPEG --jpeg quality), a square crop around the chosen
point is taken from it, and every model enlarges that crop 4x with ncnn on the PC CPU (fp32; the same ncnn release as
the engine - tools/convert-model.py checks each net against its PyTorch weights). Next to the results stand the
original photo at the same scale (what the picture really looked like), a bicubic 4x (no neural network) and the input
itself. Output: <out>/index.html + one WebP per row (HOUSE_RULES.md R4). Speed on the PC only compares models with each
other; the device's speed is measured on the Titan.
Models: the two built into the APK + every entry of docs/model-catalogue.json, files looked up in engine/third_party/.
[TESTED: 2026-10-15 · 14 models on engine/third_party/testdata/hathaway.jpg --at 0.550,0.220; page rendered headless at
1040 px wide with no horizontal scroll; original, input and three model results read by eye (face in frame, real
enlargements); opened in the owner's browser]

Usage (from the project root, conversion venv - needs ncnn, numpy, Pillow):
  .venv-convert/Scripts/python.exe -I tools/compare-models.py <photo> <out_dir> --at <x>,<y> [--input-width 600]
      [--jpeg 80] [--crop 200]
  --crop 0: the whole shrunk copy, no crop (--at is then ignored).
  --at: the crop centre as fractions of the photo's width and height (0.55,0.22 = right of centre, upper fifth).
  <photo>.credit.txt (UTF-8, optional): the photo's author and licence, printed under the table.
"""
import html
import io
import json
import os
import sys
import time
from pathlib import Path

import ncnn
import numpy as np
from PIL import Image

SCALE = 4
THIRD = Path("engine/third_party")
SEARCH = [THIRD / "converted", THIRD / "realesrgan-win" / "models", THIRD / "catalogue"]
BUILT_IN = [  # the APK's own models (docs/model-licenses.md), not in the download catalogue
    {"id": "realesr-general-x4v3", "name": "Real-ESRGAN General v3 («Быстрая»)", "author": "Xintao Wang",
     "licence": "BSD-3-Clause", "commercial": True, "purpose": "photos, fast"},
    {"id": "realesrgan-x4plus", "name": "Real-ESRGAN x4plus («Детальная»)", "author": "Xintao Wang",
     "licence": "BSD-3-Clause", "commercial": True, "purpose": "photos, fine detail"},
]
NEW_TODAY = {"4x-NMKD-Superscale-fp16", "4x-NMKD-Siax-fp16", "4xNomosWebPhoto-RealPLKSR-fp16", "4x-ClearRealityV1-fp16",
             "4x-Remacri-fp16", "4x-UltraMix-Balanced-fp16", "4x-UltraSharpV2-Lite-fp16"}


def arg(name, default=None):
    a = sys.argv
    return a[a.index(name) + 1] if name in a else default


def find(name):
    for d in SEARCH:
        if (d / name).exists():
            return d / name
    sys.exit(f"model file {name} not found in {[str(d) for d in SEARCH]}")


def run(param, binf, rgb):
    """rgb: HxWx3 uint8 -> 4x HxWx3 uint8, ncnn fp32 on the CPU; returns (image, seconds)."""
    net = ncnn.Net()
    net.opt.use_vulkan_compute = False
    net.opt.use_fp16_packed = net.opt.use_fp16_storage = net.opt.use_fp16_arithmetic = False
    net.opt.use_bf16_storage = False
    net.opt.num_threads = os.cpu_count() or 4
    if net.load_param(str(param)) != 0 or net.load_model(str(binf)) != 0:
        sys.exit(f"ncnn cannot load {param}")
    def forward(a):
        chw = np.ascontiguousarray((a.astype(np.float32) / 255.0).transpose(2, 0, 1))
        ex = net.create_extractor()
        ex.input("data", ncnn.Mat(chw))
        rc, out = ex.extract("output")
        if rc != 0:
            sys.exit(f"ncnn extract failed for {param}: {rc}")
        return (np.clip(np.array(out), 0, 1).transpose(1, 2, 0) * 255.0 + 0.5).astype(np.uint8)

    t0 = time.perf_counter()
    fixed = fixed_input(param)
    if not fixed:
        img = forward(rgb)
    else:
        # A net traced at one input size (DAT2 - UltraSharp V2: `Input … 0=128`) takes exactly that window: cut the picture
        # like the app's engine (FIXED_PAD of context each side, reflect at the borders) and keep each window's core. Fed
        # the whole crop at once it returned noise (the cat test, 2026-10-16 ≈ 20:4x).
        core = fixed - 2 * FIXED_PAD
        h, w, _ = rgb.shape
        ty, tx = (h + core - 1) // core, (w + core - 1) // core
        padded = np.pad(rgb, ((FIXED_PAD, FIXED_PAD + ty * core - h), (FIXED_PAD, FIXED_PAD + tx * core - w), (0, 0)), mode="reflect")
        img = np.zeros((ty * core * SCALE, tx * core * SCALE, 3), np.uint8)
        for y in range(ty):
            for x in range(tx):
                o = forward(padded[y * core:y * core + fixed, x * core:x * core + fixed])
                img[y * core * SCALE:(y + 1) * core * SCALE, x * core * SCALE:(x + 1) * core * SCALE] =                     o[FIXED_PAD * SCALE:(FIXED_PAD + core) * SCALE, FIXED_PAD * SCALE:(FIXED_PAD + core) * SCALE]
        img = img[:h * SCALE, :w * SCALE]
    return img, time.perf_counter() - t0


FIXED_PAD = 10  # the engine's PREPADDING (kartun_engine.cpp)


def fixed_input(param):
    """`0=<w>` of the .param's Input line when the net was traced at one size (DAT2), else 0."""
    for line in Path(param).read_text(encoding="utf-8").splitlines():
        if line.startswith("Input"):
            for tok in line.split():
                if tok.startswith("0="):
                    return int(tok[2:])
    return 0


def mb(nbytes):
    return f"{nbytes / 1e6:.1f}".replace(".", ",") + " МБ"


def main():
    photo, out = Path(sys.argv[1]), Path(sys.argv[2])
    ax, ay = (float(v) for v in arg("--at").split(","))
    in_w, q, crop = int(arg("--input-width", "600")), int(arg("--jpeg", "80")), int(arg("--crop", "200"))
    out.mkdir(parents=True, exist_ok=True)

    orig = Image.open(photo).convert("RGB")
    ow, oh = orig.size
    in_h = round(oh * in_w / ow)
    buf = io.BytesIO()
    orig.resize((in_w, in_h), Image.LANCZOS).save(buf, "JPEG", quality=q)  # the "messenger" copy
    small = Image.open(io.BytesIO(buf.getvalue())).convert("RGB")
    # --crop 0: the WHOLE shrunk picture goes to the models, no piece cut out (the owner, 2026-10-16 ≈ 21:2x: «можешь не
    # кропать, а уменьшать размер найденных картинок» · «так будет честнее»).
    if crop > 0:
        cx, cy = round(ax * in_w), round(ay * in_h)
        x0, y0 = max(0, min(in_w - crop, cx - crop // 2)), max(0, min(in_h - crop, cy - crop // 2))
        cw = ch = crop
    else:
        x0 = y0 = 0
        cw, ch = in_w, in_h
    inp = np.asarray(small.crop((x0, y0, x0 + cw, y0 + ch)))
    sw, sh = cw * SCALE, ch * SCALE
    side = max(sw, sh)

    # The truth at the output's scale: the same region of the original, resized to the output size.
    k = ow / in_w
    truth = orig.crop((round(x0 * k), round(y0 * k), round((x0 + cw) * k), round((y0 + ch) * k))).resize((sw, sh), Image.LANCZOS)
    rows = [
        ("original", "Оригинал фото", "как было на самом деле, тот же масштаб", truth, None, None, None),
        ("bicubic", "Без нейросети", "обычное увеличение 4× (бикубическое)", Image.fromarray(inp).resize((sw, sh), Image.BICUBIC), None, None, None),
        ("input", "Что получила модель", f"{'кусок ' + str(cw) + '×' + str(ch) + ' из копии' if crop > 0 else 'вся копия'} {in_w}×{in_h}, JPEG {q}", Image.fromarray(inp).resize((sw, sh), Image.NEAREST), None, None, None),
    ]
    cat = json.loads(Path("docs/model-catalogue.json").read_text(encoding="utf-8"))["models"]
    built = {m["id"] for m in BUILT_IN}  # a model still in the APK and already in the catalogue is listed once, as in the APK
    models = [dict(m, place="в APK") for m in BUILT_IN] + [
        dict(m, place="новая, 15.10" if m["id"] in NEW_TODAY else "в каталоге") for m in cat if m["id"] not in built]
    for m in models:
        param, binf = find(m["id"] + ".param"), find(m["id"] + ".bin")
        img, dt = run(param, binf, inp)
        print(f"{m['id']}: {dt:.1f} s")
        rows.append((m["id"], m["name"], f"{m['author']} · {m['purpose']}", Image.fromarray(img), m, binf.stat().st_size + param.stat().st_size, dt))

    trs = []
    for i, (rid, name, sub, img, m, size, dt) in enumerate(rows):
        img.save(out / f"{rid}.webp", quality=92)
        if m is None:
            lic, place, sz, t = "", "", "", ""
            num = ""
        else:
            ok = m["commercial"]
            lic = f'<span class="b {"ok" if ok else "nc"}">{"можно для коммерции" if ok else "не для коммерции"}</span><div class="s">{html.escape(m["licence"])}</div>'
            place, sz, t = m["place"], mb(size), f"{dt:.1f} с".replace(".", ",")
            num = str(i - 2)
        facts = "" if m is None else f'<div class="f"><b>{sz}</b> · {place} · {t} на ПК</div>{lic}'
        trs.append(f'<tr class="{"ref" if m is None else ""}"><td class="n">{num}</td><td class="i"><div class="m">{html.escape(name)}</div>'
                   f'<div class="s">{html.escape(sub)}</div>{facts}</td>'
                   f'<td><a href="{rid}.webp" target="_blank"><img src="{rid}.webp" alt="{html.escape(name)}" loading="lazy"></a></td></tr>')

    title = "Модели KARTUN на одном фото"
    # The photo's author and licence come from a UTF-8 file beside it (text travels through files, not argv).
    credit_file = photo.with_name(photo.name + ".credit.txt")
    credit = html.escape(credit_file.read_text(encoding="utf-8").strip()) if credit_file.exists() else ""
    page = f"""<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title><style>
:root{{--bg:#0D0D0D;--card:#1A1A1A;--fg:#F2F2F2;--dim:#9A9A9A;--acc:#FF1A8C;--ok:#3DDC84}}
body{{margin:0;padding:24px;background:var(--bg);color:var(--fg);font:15px/1.4 system-ui,sans-serif}}
h1{{font-size:22px;margin:0 0 6px}} p{{color:var(--dim);margin:0 0 16px;max-width:900px}}
table{{border-collapse:collapse}} td,th{{padding:10px 12px;border-bottom:1px solid #2A2A2A;vertical-align:middle;text-align:left}}
th{{color:var(--dim);font-weight:500;font-size:13px;position:sticky;top:0;background:var(--bg)}}
tr.ref td{{background:#141414}} .n{{color:var(--acc);font-weight:700}} .r{{text-align:right;white-space:nowrap}}
.m{{font-weight:600}} .s{{color:var(--dim);font-size:13px}} .f{{margin:8px 0 6px;font-size:14px}} .i{{width:300px}}
img{{display:block;border-radius:6px;width:min({side // 2}px,calc(100vw - 420px));height:auto}}
.b{{display:inline-block;padding:2px 8px;border-radius:10px;font-size:12px;white-space:nowrap}} .b+.s{{margin-top:4px}}
.ok{{background:rgba(61,220,132,.15);color:var(--ok)}} .nc{{background:rgba(255,26,140,.15);color:var(--acc)}}
</style></head><body><h1>{html.escape(title)}</h1>
<p>Фото уменьшено до {in_w}×{in_h} и сохранено в JPEG {q} — как из мессенджера. Каждая модель увеличила {"кусок" if crop > 0 else "всю копию"} {cw}×{ch} в 4 раза ({sw}×{sh}).
Картинка в таблице — в половину размера; нажмите, чтобы открыть в полном. Время — на ПК, только чтобы сравнить модели между собой; скорость на планшете меряется на Титане.</p>
<table><tr><th>№</th><th>Модель · вес · где · время · лицензия</th><th>Результат</th></tr>
{''.join(trs)}</table><p style="margin-top:16px">{credit}</p></body></html>"""
    (out / "index.html").write_text(page, encoding="utf-8", newline="\n")
    print(f"written {out / 'index.html'}: {len(rows) - 3} models")


if __name__ == "__main__":
    main()
