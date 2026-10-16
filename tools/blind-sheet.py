"""blind-sheet.py - a blind comparison row for the owner: the original + the chosen models' results under numbers only.

Takes the results tools/compare-models.py already wrote (one WebP per model id, same input), shuffles the chosen ids with
a fresh random seed, copies them as 1.webp, 2.webp, ... (file names never name the model) and writes index.html with one
row: the original, then the numbered candidates. The key «number -> model id» goes to key.json beside the page and is
not linked from it (the taste-class comparison contract: same material, blind labels, key stored beside).
One row left to right with a horizontal scrollbar, every picture the same width, the mouse wheel scrolls the row (the owner,
2026-10-16 ≈ 20:4x: «Все картинки результаты в один ряд выстрой слеванаправо со скролбаром горизонтальным»); each picture nearly
the window's height (≈ 20:5x: «увелчиь полотно котиков, чтобы большие картинки были в экране браузера!»). [NOT-TESTED]

Usage (from the project root): python -I tools/blind-sheet.py <compare_dir>[,<compare_dir2>…] <out_dir> <id1,id2,...> [<titles.txt: one row title per line, UTF-8>]
"""
import json
import random
import secrets
import shutil
import sys
from pathlib import Path


def main():
    # Several compare dirs (comma-separated) make several rows; ONE shuffle serves all rows, so a number is the same model
    # in every row (the owner, 2026-10-16 ≈ 20:4x: «найди второго котика - мыльного» · «и ниже вторым рядом»).
    srcs, out, ids = [Path(s) for s in sys.argv[1].split(",")], Path(sys.argv[2]), sys.argv[3].split(",")
    # Row titles come from a UTF-8 file, one per line (text travels through files, not argv - AGENT_GUIDE.md hygiene).
    titles = Path(sys.argv[4]).read_text(encoding="utf-8").splitlines() if len(sys.argv) > 4 else [f"Фото {r + 1}" for r in range(len(srcs))]
    for src in srcs:
        for i in ids:
            if not (src / f"{i}.webp").exists():
                sys.exit(f"no result for {i} in {src} - run tools/compare-models.py first")
    out.mkdir(parents=True, exist_ok=True)
    for old in out.glob("*.webp"):
        old.unlink()
    seed = secrets.randbits(32)
    order = ids[:]
    random.Random(seed).shuffle(order)
    key = {"seed": seed, "sources": [str(s) for s in srcs], "numbers": {str(n): mid for n, mid in enumerate(order, 1)}}
    rows = []
    for r, src in enumerate(srcs):
        # «Оригинал» is what the networks were GIVEN - the small crop, enlarged without smoothing (input.webp of
        # compare-models.py) - never the sharp source photo (the owner, 2026-10-16 ≈ 20:5x: «оригинал - то, с чем работали
        # нейронки, что на вход подавал!!!!!»).
        shutil.copyfile(src / "input.webp", out / f"r{r + 1}_original.webp")
        cells = [f'<figure><img src="r{r + 1}_original.webp"><figcaption>Оригинал (вход нейросетей)</figcaption></figure>']
        for n, mid in enumerate(order, 1):
            shutil.copyfile(src / f"{mid}.webp", out / f"r{r + 1}_{n}.webp")
            cells.append(f'<figure><img src="r{r + 1}_{n}.webp"><figcaption>№ {n}</figcaption></figure>')
        rows.append(f'<h2>{titles[r] if r < len(titles) else ""}</h2><div class="row">{"".join(cells)}</div>')
    (out / "key.json").write_text(json.dumps(key, indent=2) + "\n", encoding="utf-8", newline="\n")
    (out / "index.html").write_text(f"""<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Слепой тест моделей</title><style>
body{{margin:0;padding:16px;background:#0D0D0D;color:#F2F2F2;font:15px/1.4 system-ui,sans-serif}}
h1{{font-size:20px;margin:0 0 4px}} h2{{font-size:16px;margin:10px 0 6px;color:#F2F2F2}} p{{color:#9A9A9A;margin:0 0 12px}}
.row{{display:flex;flex-wrap:nowrap;gap:12px;overflow-x:auto;padding-bottom:14px;scrollbar-color:#FF1A8C #222}}
.row::-webkit-scrollbar{{height:14px}} .row::-webkit-scrollbar-thumb{{background:#FF1A8C;border-radius:7px}} .row::-webkit-scrollbar-track{{background:#222}}
figure{{margin:0;flex:0 0 auto;width:min(calc(100vh - 90px),94vw)}} img{{width:100%;height:auto;display:block;border-radius:6px;cursor:zoom-in}}
figcaption{{text-align:center;margin-top:6px;font-size:18px;font-weight:700;color:#FF1A8C}}
figure:first-child figcaption{{color:#9A9A9A;font-weight:500}}
img.big{{position:fixed;inset:0;width:100vw;height:100vh;object-fit:contain;background:#0D0D0D;z-index:9;cursor:zoom-out;border-radius:0}}
</style></head><body><h1>Слепой тест: какая модель лучше?</h1>
<p>Одно и то же фото, уменьшенное «как из мессенджера» и увеличенное в 4 раза. Слева — то, что получили нейросети, дальше кандидаты под номерами в один ряд: листайте вбок полосой прокрутки внизу или колесом мыши. Нажмите на картинку — откроется на весь экран, ещё раз — закроется.</p>
{''.join(rows)}
<script>document.querySelectorAll('img').forEach(i=>i.onclick=()=>i.classList.toggle('big'));document.querySelectorAll('.row').forEach(r=>r.addEventListener('wheel',e=>{{if(Math.abs(e.deltaY)>Math.abs(e.deltaX)){{r.scrollLeft+=e.deltaY;e.preventDefault()}}}},{{passive:false}}))</script></body></html>""",
                                  encoding="utf-8", newline="\n")
    print(f"written {out / 'index.html'}: {len(srcs)} rows x {len(order)} candidates, key in {out / 'key.json'}")


if __name__ == "__main__":
    main()
