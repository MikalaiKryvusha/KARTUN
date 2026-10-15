"""blind-sheet.py - a blind comparison row for the owner: the original + the chosen models' results under numbers only.

Takes the results tools/compare-models.py already wrote (one WebP per model id, same input), shuffles the chosen ids with
a fresh random seed, copies them as 1.webp, 2.webp, ... (file names never name the model) and writes index.html with one
row: the original, then the numbered candidates. The key «number -> model id» goes to key.json beside the page and is
not linked from it (the taste-class comparison contract: same material, blind labels, key stored beside).
[TESTED: 2026-10-15 · blind-1 from design/model-compare (HFA2k, NMKD Siax, NomosWebPhoto): one row at 1040 px with no
horizontal scroll, the page's HTML names no model (checked headless), files 1-3.webp + key.json; opened for the owner]

Usage (from the project root): python -I tools/blind-sheet.py <compare_dir> <out_dir> <id1,id2,...>
"""
import json
import random
import secrets
import shutil
import sys
from pathlib import Path


def main():
    src, out, ids = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3].split(",")
    for i in ids:
        if not (src / f"{i}.webp").exists():
            sys.exit(f"no result for {i} in {src} - run tools/compare-models.py first")
    out.mkdir(parents=True, exist_ok=True)
    for old in out.glob("*.webp"):
        old.unlink()
    seed = secrets.randbits(32)
    order = ids[:]
    random.Random(seed).shuffle(order)
    shutil.copyfile(src / "original.webp", out / "original.webp")
    key = {"seed": seed, "source": str(src), "numbers": {}}
    cells = ['<figure><img src="original.webp"><figcaption>Оригинал</figcaption></figure>']
    for n, mid in enumerate(order, 1):
        shutil.copyfile(src / f"{mid}.webp", out / f"{n}.webp")
        key["numbers"][str(n)] = mid
        cells.append(f'<figure><img src="{n}.webp"><figcaption>№ {n}</figcaption></figure>')
    (out / "key.json").write_text(json.dumps(key, indent=2) + "\n", encoding="utf-8", newline="\n")
    cols = len(cells)
    (out / "index.html").write_text(f"""<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Слепой тест моделей</title><style>
body{{margin:0;padding:16px;background:#0D0D0D;color:#F2F2F2;font:15px/1.4 system-ui,sans-serif}}
h1{{font-size:20px;margin:0 0 4px}} p{{color:#9A9A9A;margin:0 0 12px}}
.row{{display:grid;grid-template-columns:repeat({cols},minmax(0,1fr));gap:10px}}
figure{{margin:0}} img{{width:100%;height:auto;display:block;border-radius:6px;cursor:zoom-in}}
figcaption{{text-align:center;margin-top:6px;font-size:18px;font-weight:700;color:#FF1A8C}}
figure:first-child figcaption{{color:#9A9A9A;font-weight:500}}
img.big{{position:fixed;inset:0;width:100vw;height:100vh;object-fit:contain;background:#0D0D0D;z-index:9;cursor:zoom-out;border-radius:0}}
</style></head><body><h1>Слепой тест: какая модель лучше?</h1>
<p>Одно и то же фото, уменьшенное «как из мессенджера» и увеличенное в 4 раза. Слева — оригинал. Нажмите на картинку — откроется на весь экран, ещё раз — закроется.</p>
<div class="row">{''.join(cells)}</div>
<script>document.querySelectorAll('img').forEach(i=>i.onclick=()=>i.classList.toggle('big'))</script></body></html>""",
                                  encoding="utf-8", newline="\n")
    print(f"written {out / 'index.html'}: {len(order)} candidates, key in {out / 'key.json'}")


if __name__ == "__main__":
    main()
