# Test run report — Phase 0: our own ncnn conversion of realesr-general-x4v3 (the default fast model)

**Created:** 2026-10-14 18:16 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** `tools/convert-model.py`
(this commit), pnnx 20260526, torch 2.14.1, numpy 2.5.4; desktop `realesrgan-ncnn-vulkan.exe` from the xinntao zip v0.2.5.0

## 1. Work

`plans/02_epic01_F0_engine_spike.md` step 4: KARTUN ships its own ncnn conversion of `realesr-general-x4v3` from the
original BSD-3 weights (the default model — `[OWNER]` interview #001, Q2 = A), not Upscayl's file. Basis: the converted
net must reproduce the original PyTorch model; `plans/01_EPIC_mvp.md` §2.4 (real network, not interpolation).

## 2. Contour

PC only (Windows 11, the owner's machine); the Titan was offline (Tailscale: "offline, last seen 2h ago", 18:13 and
18:16), so the device part of the plan is not run here. Weights: `engine/third_party/realesr-general-x4v3.pth`, sha256
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

Moments of runs 4–5 are the output files' mtimes; the others carry `≈`.

## 4. Checks

Hygiene: conversion script runs to exit 0; no unit tests.
Functional run: NONE — the converted model has not been run by our engine on the Titan yet (device offline).

| Case | Status | Observation |
|---|---|---|
| C1 conversion reproduces the original net | pass | ncnn (desktop, Vulkan) vs PyTorch: 47.1 dB PSNR |
| C2 not an interpolation | pass | bicubic vs PyTorch: 30.1 dB — 17 dB below the ncnn result |
| C3 blob names the engine expects | pass | `.param` has `Input data` and the last blob `output` |
| C4 our engine on the Titan, GPU and CPU, time per 1 MP | blocked | Titan offline |

## 5. Found

- `tools/convert-model.py` never ran end to end before: pnnx writes its files under the `.pt` stem with `-` → `_`, and
  needs numpy for its check step; and a relative Windows path breaks pnnx's generated Python (EXP-0003 class). Fixed in
  the script; numpy installed in `.venv-convert`.

## 6. Traces

`engine/third_party/converted/` (gitignored), `engine/third_party/testdata/{torch_gx4v3_256,pc_own_gx4v3_256}.png`
(gitignored); provenance and hashes — `docs/model-licenses.md`.

## 7. Verdict

partial — the conversion is correct on the PC (C1–C3); the device run C4 is blocked until the Titan is back online.
