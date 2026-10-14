# Test run report — Phase 0: our own ncnn conversion of realesr-general-x4v3 (the default fast model)

**Created:** 2026-10-14 18:16 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** `tools/convert-model.py`
(this commit), pnnx 20260526, torch 2.14.1, numpy 2.5.4; desktop `realesrgan-ncnn-vulkan.exe` from the xinntao zip v0.2.5.0

## 1. Work

`plans/02_epic01_F0_engine_spike.md` step 4: KARTUN ships its own ncnn conversion of `realesr-general-x4v3` from the
original BSD-3 weights (the default model — `[OWNER]` interview #001, Q2 = A), not Upscayl's file. Basis: the converted
net must reproduce the original PyTorch model; `plans/01_EPIC_mvp.md` §2.4 (real network, not interpolation).

## 2. Contour

PC (Windows 11, the owner's machine) for runs 1–6; the HEADWOLF Titan 1 (Mali-G615 MC6, Vulkan 1.3) for runs 7–13,
debug APK `com.kriniks.kartun.debug` 0.1 (1) with the phase 0 engine (unchanged since `testcases/reports/2026-10-14_F0_engine_probe.md`),
over adb `100.99.111.48:42661` (the Titan was offline 18:13–18:17; the owner woke it and gave the new port ≈ 18:20).
The converted model pushed to the app's files dir, sha256 on the device equal to the PC file (`8fdd6052…e553`). Weights: `engine/third_party/realesr-general-x4v3.pth`, sha256
`8dc7edb9…6292`, equal to a fresh download from the xinntao release v0.2.5.0 (18:15). Input: `test256.png` (the release's
`input2.jpg`, 256×256). Ground truth: the PyTorch model with the original weights on the same input.

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-14 18:13 +03:00 | `.venv-convert/Scripts/python.exe -I tools/convert-model.py engine/third_party/realesr-general-x4v3.pth engine/third_party/converted` | ❌ `ModuleNotFoundError: numpy` in pnnx's own check step (ncnn files already written) → `pip install numpy` |
| 2 | ≈ 2026-10-14 18:13 +03:00 | same | ❌ `OSError: Invalid argument: 'engine\third_party\\converted\realesr_…'` — the Windows path inside pnnx's generated .py read `\t`/`\r` as escapes → script passes an absolute forward-slash path |
| 3 | ≈ 2026-10-14 18:13 +03:00 | same | ok · `realesr-general-x4v3.param` (6 400 B) + `.bin` (2 435 272 B) |
| 4 | 2026-10-14 18:14:02 +03:00 | `.venv-convert/Scripts/python.exe -I <scratchpad>/torch_ref.py tools/convert-model.py engine/third_party/realesr-general-x4v3.pth test256.png torch_gx4v3_256.png` | ok · 1024×1024 PyTorch output |
| 5 | 2026-10-14 18:14:10 +03:00 | `realesrgan-ncnn-vulkan.exe -i test256.png -o pc_own_gx4v3_256.png -m <scratchpad>/models -n realesr-general-x4v3 -s 4 -t 200` | ok · 1024×1024 |
| 6 | ≈ 2026-10-14 18:14 +03:00 | `magick compare -metric PSNR pc_own_gx4v3_256.png torch_gx4v3_256.png null:` and the same for `bicubic256x4.png` | 47.11 dB · bicubic 30.09 dB |
| 7 | 2026-10-14 18:21:30 +03:00 | `node tools/titan-probe.mjs --in test256.png --model realesr-general-x4v3 --gpu true --tile 200 --ref engine/third_party/testdata/torch_gx4v3_256.png --bicubic engine/third_party/testdata/bicubic256x4.png` | ok · GPU · 1024×1024 · 399 ms · **52.30 dB to PyTorch** · 31.43 dB to bicubic |
| 8 | 2026-10-14 18:21:38 +03:00 | same, `--gpu false` | ok · CPU · 4 811 ms · 52.30 dB · 31.43 dB |
| 9 | ≈ 2026-10-14 18:21:56 +03:00 | `node tools/titan-probe.mjs --in test1mp.png --model realesr-general-x4v3 --gpu true --tile 200` | ok · GPU · 4096×4096 · **5 777 ms** |
| 10 | ≈ 2026-10-14 18:22:16 +03:00 | `node tools/titan-probe.mjs --in photo1.jpg --model realesr-general-x4v3 --gpu true --tile 200` | ok · GPU · 960×1280 → 3840×5120 · **6 648 ms** |
| 11 | ≈ 2026-10-14 18:23:58 +03:00 | `node tools/titan-probe.mjs --in test1mp.png --model realesr-general-x4v3 --gpu false --tile 200` | ok · CPU · 4096×4096 · 84 806 ms |
| 12 | 2026-10-14 18:24:02 +03:00 | `node tools/titan-probe.mjs --in test256.png --model realesr-animevideov3-x4 --gpu true --tile 200 --bicubic engine/third_party/testdata/bicubic256x4.png` | ok · GPU · 237 ms · 34.04 dB to bicubic |
| 13 | 2026-10-14 18:24:08 +03:00 | same, `--gpu false` | ok · CPU · 2 709 ms · 34.04 dB to bicubic |

Moments of runs 4–5, 7–8 and 12–13 are the output files' mtimes on the PC (pulled at the end of the run); runs 9–11 did not
pull a file and carry the `date` printed after them, with `≈`. Run 14 (18:24:15): `logcat -d -t 2000 | grep -cE 'FATAL|SIGSEGV'`
printed 1 — the one hit is adbd logging that very grep command; no crash line.

## 4. Checks

Hygiene: conversion script runs to exit 0; no unit tests.
Functional run: the converted model walked through our engine on the Titan by the probe activity (phase 0 has no UI, so
there is no user path yet); output PNGs pulled and READ by `magick compare`, the probe's JSON lines READ for every run.

| Case | Status | Observation |
|---|---|---|
| C1 conversion reproduces the original net | pass | ncnn (desktop, Vulkan) vs PyTorch: 47.1 dB PSNR |
| C2 not an interpolation | pass | bicubic vs PyTorch: 30.1 dB — 17 dB below the ncnn result |
| C3 blob names the engine expects | pass | `.param` has `Input data` and the last blob `output` |
| C4 our engine on the Titan matches the original net | pass | GPU and CPU both 52.3 dB to PyTorch (run 7, 8); GPU vs CPU output 63.6 dB to each other |
| C5 time per 1 MP, default model | pass | GPU 5.8 s (test 1024×1024), 6.6 s (photo 960×1280); CPU 84.8 s — 15× slower |
| C6 combinations left open by plan 02 step 8 | pass | animevideov3 at 256 on GPU (237 ms) and CPU (2.7 s), outputs 66.2 dB to each other; a model on CPU at 1 MP (run 11) |
| C7 no crash | pass | run 14: 0 crash lines |

## 5. Found

- The x4plus model on CPU at 1 MP is not run: by the 256×256 CPU time (93.9 s) it would take ≈ 25 min; the fallback ladder
  of phase 1 decides whether x4plus on CPU is offered at all.
- `tools/convert-model.py` never ran end to end before: pnnx writes its files under the `.pt` stem with `-` → `_`, and
  needs numpy for its check step; and a relative Windows path breaks pnnx's generated Python (EXP-0003 class). Fixed in
  the script; numpy installed in `.venv-convert`.

## 6. Traces

`engine/third_party/converted/` (gitignored), `engine/third_party/testdata/{torch_gx4v3_256,pc_own_gx4v3_256}.png`
(gitignored); provenance and hashes — `docs/model-licenses.md`.

## 7. Verdict

partial — our own conversion of the default model reproduces the original network on the PC and on the Titan (GPU and
CPU), 1 MP in 5.8 s on the GPU; `partial` only because phase 0 has no user path to walk (the probe activity stands in).
