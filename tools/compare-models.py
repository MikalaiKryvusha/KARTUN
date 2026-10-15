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
    chw = np.ascontiguousarray((rgb.astype(np.float32) / 255.0).transpose(2, 0, 1))
    t0 = time.perf_counter()
    ex = net.create_extractor()
    ex.input("data", ncnn.Mat(chw))
    rc, out = ex.extract("output")
    dt = time.perf_counter() - t0
    if rc != 0:
        sys.exit(f"ncnn extract failed for {param}: {rc}")
    img = (np.clip(np.array(out), 0, 1).transpose(1, 2, 0) * 255.0 + 0.5).astype(np.uint8)
    return img, dt


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
    cx, cy = round(ax * in_w), round(ay * in_h)
    x0, y0 = max(0, min(in_w - crop, cx - crop // 2)), max(0, min(in_h - crop, cy - crop // 2))
    inp = np.asarray(small.crop((x0, y0, x0 + crop, y0 + crop)))
    side = crop * SCALE

    # The truth at the output's scale: the same region of the original, resized to side x side.
    k = ow / in_w
    truth = orig.crop((round(x0 * k), round(y0 * k), round((x0 + crop) * k), round((y0 + crop) * k))).resize((side, side), Image.LANCZOS)
    rows = [
        ("original", "Оригинал фото", "как было на самом деле, тот же масштаб", truth, None, None, None),
        ("bicubic", "Без нейросети", "обычное увеличение 4× (бикубическое)", Image.fromarray(inp).resize((side, side), Image.BICUBIC), None, None, None),
        ("input", "Что получила модель", f"кусок {crop}×{crop} из копии {in_w}×{in_h}, JPEG {q}", Image.fromarray(inp).resize((side, side), Image.NEAREST), None, None, None),
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
<p>Фото уменьшено до {in_w}×{in_h} и сохранено в JPEG {q} — как из мессенджера. Каждая модель увеличила кусок {crop}×{crop} в 4 раза ({side}×{side}).
Картинка в таблице — в половину размера; нажмите, чтобы открыть в полном. Время — на ПК, только чтобы сравнить модели между собой; скорость на планшете меряется на Титане.</p>
<table><tr><th>№</th><th>Модель · вес · где · время · лицензия</th><th>Результат</th></tr>
{''.join(trs)}</table><p style="margin-top:16px">{credit}</p></body></html>"""
    (out / "index.html").write_text(page, encoding="utf-8", newline="\n")
    print(f"written {out / 'index.html'}: {len(rows) - 3} models")


if __name__ == "__main__":
    main()
