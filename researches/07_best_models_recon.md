# Research 07 — The best upscaling models for KARTUN: what the community praises, what 2026 brought, what runs on Vulkan

> **Создан:** 2026-10-15 (agent; `plans/05_model_catalogue.md` step 0)
> **Родитель:** `plans/05_model_catalogue.md` step 0 — the owner, chat 2026-10-14 ≈ 22:33 +03:00: «да, чем больше моделей, тем лучше. Смотрим на ЛУЧШИЕ, которые ОЧЕНЬ хвалят в интернете и на хаггинг фейс»; 2026-10-15 ≈ 14:14: «какие ещё новые суппер качественные модели появились в 2026 году? и движки под них, которые на Вулкан работают?» · «разведка в интернете»
> **Статус:** ✅ written 2026-10-15 14:17 +03:00 — §1–§4 feed the catalogue (13 → 12 models after the owner's cuts) and the APK bundle; §5 opens the DAT2 work
> **Вовне:** the owner — verdicts already taken in chat (journal rows 2026-10-15 13:57–14:15 in `MASTER_PLAN.md`); §6 forks → the owner

## 1. What the community praises — measured, not retold

Popularity has no single ranking; three countable signals were read on 2026-10-15 13:33:

| Signal | Source | How read |
|---|---|---|
| All-time downloads of upscalers | Civitai API `models?types=Upscaler&sort=Most Downloaded` | `stats.downloadCount` |
| Likes | Hugging Face API `models/<repo>` | `likes` (downloads are not tracked for most SR repos — 0) |
| Curation | Upscayl 2.15.0 `resources/models` (local install) and `upscayl/custom-models` README | presence |

Civitai top (downloads): 4x-UltraSharp 264 041 · Remacri 181 568 · 4x NMKD Superscale 78 546 · RealESRGAN x4plus anime 6B 45 476 ·
NMKD Siax "CX" 42 487 · 8x NMKD Superscale 25 718 · RealESRGAN x4plus 22 528 · UltraMix 15 256 · RealisticRescaler 15 188 ·
Nomos8kHAT-L 10 981 · 4x NMKD Siax 200k 10 490 · … LSDIR 6 052 · Nomos8kSC 2 348 · HFA2k 1 481.
Hugging Face likes: Kim2091/UltraSharpV2 212 · Kim2091/UltraSharp 71 · Kim2091/2x-AnimeSharpV4 59 · Kim2091/ClearRealityV1 44 ·
Phips/4xRealWebPhoto_v4_dat2 22 · Phips/4xNomosWebPhoto_RealPLKSR 7.
Upscayl bundles (observed in `C:\Program Files\Upscayl\resources\models`, 7 models, 175 MB of a 584 MB install, nothing is
downloaded): x4plus, General v3, x4plus anime, HFA2k, Remacri, UltraMix Balanced, UltraSharp. Its `custom-models` repo adds
NMKD Siax 200k, NMKD Superscale SP_178000_G, General WDN v3, LSDIR / LSDIRplusC / LSDIRCompactC3, Nomos8kSC, AnimeVideo v3 x2–x4.

Model metadata (author, licence, architecture, hashes): the OpenModelDB data repository, shallow clone at commit
`782aac0` (2026-08-14), 671 models — `data/models/<id>.json`.

## 2. Licences and sources of the models taken (hashes verified against OpenModelDB or the author's repo)

| Model | Author | Licence | Architecture | Original file (sha256 prefix) | Source |
|---|---|---|---|---|---|
| 4x NMKD Superscale SP_178000_G | NMKD | WTFPL | ESRGAN 23 blocks | `1d1b0078fe71` | HF `gemasai/…` = OpenModelDB hash |
| 4x NMKD Siax 200k (OpenModelDB "CX") | NMKD | WTFPL | ESRGAN 23 blocks | `560424d9f686` | HF `uwg/upscaler` = OpenModelDB hash |
| 4xNomosWebPhoto RealPLKSR | Philip Hofmann | CC BY 4.0 | RealPLKSR | `a9db66c9b674` | Phhofm/models release |
| 4x ClearReality V1 | Kim2091 | **conflict**: author's HF card Apache-2.0, OpenModelDB CC BY-NC-SA 4.0 | SPAN | `a4cd3a25b00e` (HF; OpenModelDB lists a different Mega file `92b6d4f5…`) | HF `Kim2091/ClearRealityV1` |
| 4x Remacri | FoolhardyVEVO | CC BY-NC-SA 4.0 | ESRGAN 23 blocks | `e1a73bd89c2d` | HF `FacehugmanIII/…` = OpenModelDB hash |
| 4x UltraMix Balanced | Kim2091 | CC BY-NC-SA 4.0 | ESRGAN 23 blocks | `e23ca000107a` | HF `Kim2091/UltraSharp/Interpolations` |
| 4x UltraSharp V2 Lite | Kim2091 | CC BY-NC-SA 4.0 | RealPLKSR | `7a330ef6b7c6` (safetensors) | HF `Kim2091/UltraSharpV2` |
| 4x UltraSharp V2 | Kim2091 | CC BY-NC-SA 4.0 | DAT2 (transformer) | `2b5db674aa8f` (safetensors), ONNX `6c0201e34037` | HF `Kim2091/UltraSharpV2` |

The author's own ClearReality NCNN files do NOT load in ncnn 20260526: `find_blob_index_by_name /Constant_output_0 failed`
→ "parameter file has inconsistent content" (a dangling blob from an old onnx2ncnn). Our own conversion of the author's
weights loads and matches (below).

## 3. Conversion to the engine's NCNN — what runs on our engine (observed on the PC)

Tool: `tools/convert-model.py` (spandrel 0.4.2 → pnnx 20260526 → ncnn; checks itself against PyTorch on `photo1.jpg`).

| Architecture | Result | Note |
|---|---|---|
| ESRGAN 23 blocks (Remacri, UltraMix, Superscale, Siax) | ✅ 60.3–63.7 dB vs PyTorch | `.bin` 33 424 520 bytes for every one — same as the authors' own fp16 files |
| Compact (General v3) | ✅ byte-identical to the shipped files | the tool's golden |
| SPAN (ClearReality) | ✅ 71.4 dB, 0.85 MB | training-time Conv3XC branches fold into one conv: 9.0 → 0.85 MB |
| RealPLKSR (NomosWebPhoto, UltraSharp V2 Lite) | ✅ 72.9 / 68.8 dB, 14.8 MB | needs one substitution: `torch.repeat_interleave` (no NCNN layer) → an exact one-hot 1×1 conv |
| DAT2 (UltraSharp V2) | ❌ attempt 1: pnnx dies silently in `pass_level0` (TorchScript path; DAT checks the input size in Python — TracerWarnings); attempt 2: pnnx on the author's ONNX segfaults in `fold_constants_dynamic_shape` | §5 |
| RealPLKSR + DySample | not tried | needs `GridSample` — exists in ncnn, not proven on our path |

None of the new models has run on the Titan yet (the port was unknown all session) — speed on the device is unmeasured.
PC CPU, fp32, 200×200 → 800×800 (relative only): Compact 0.2–0.3 s · SPAN 0.2 s · RealPLKSR 4.3 s · ESRGAN 23 blocks 5.9–6.0 s;
PyTorch CPU: UltraSharp V2 Lite 3.0 s, UltraSharp V2 (DAT2) 12.9 s.

## 4. What 2026 brought (web sweep 2026-10-15)

**Community models (OpenModelDB, models dated 2025-07…2026-08):** 22 entries, almost all 1×/2× restoration for games and
video. The only new general family is **StarSample V2** (2026-02-12): 2× only, CC BY-NC-SA, in ESRGAN / SPAN Lite / HAT HQ
variants. **No new 4× photo model of note appeared in 2026.** Phhofm (Nomos, HFA2k, BHI) has no 2026 release; his latest
4× photo work is the BHI series (2024-12): `4xBHI_dat2_real` / `_otf` / `_multiblurjpg` (DAT2) and `4xBHI_realplksr_dysample_*`
(RealPLKSR + DySample, 29.7 MB) — the release note says: "I recommend using the dat2 models over these, since they give
better results quality wise." Kim2091's latest 4× is UltraSharp V2 (2025-05-23, DAT2 + a RealPLKSR Lite).
→ The best recent 4× photo models are **DAT2 transformers**: UltraSharp V2, Nomos2 hq dat2 (CC BY 4.0), BHI dat2 (CC BY 4.0),
RealWebPhoto v4 dat2.

**Research — NTIRE 2026 (CVPR 2026 workshops):**
- *Mobile Real-World Image SR* (first edition; arXiv 2604.17306): speed measured on **MediaTek Dimensity 8400, FP16**,
  128×128 → 512×512 — the class of our Titan (Dimensity 8300 class). Rank 1 VIPSL — CNN, PLKSR-Rep backbone (dim 64, 12
  blocks), 113.85× faster than OSEDiff; rank 2 Antman — RRDBNet (ESRGAN 23 blocks, initialised from Real-ESRGAN x4plus);
  rank 3 SamsungAICamera — OSEDiff distilled into a pruned U-Net + TAESD decoder (720 ms U-Net on a Dimensity 9500, FP32).
  Report's conclusion: "Knowledge distillation and architectural compression are mandatory for mobile deployment."
  Weights: the challenge repo `jiatongli2024/NTIRE2026_Mobile_RealWorld_ImageSR` (code MIT) gives only the DAT baseline via
  Google Drive; team weights are not confirmed; **no licence for the weights is stated** → not usable until a team publishes one.
- *Efficient SR* (11th, arXiv 2604.03198): winner XiaomiMM "SPANV2" (0.139 M params, 5.256 ms); bicubic DIV2K degradation,
  PSNR-ranked — clean-image models, not for real photos; weights "will be available … later".
- One-step diffusion SR (OSEDiff, TODSR, PocketSR arXiv 2510.03012, TOC-SR 2605.02767): generative, "several seconds … on a
  mobile accelerator … hundreds of megabytes" (NTIRE mobile report) — needs an NPU path, not our Vulkan engine.

**Engines on Vulkan (Android):**
| Engine | Vulkan state | SR relevance |
|---|---|---|
| ncnn (ours, 20260526) | mature; has `MultiHeadAttention` with a Vulkan kernel (`src/layer/vulkan/multiheadattention_vulkan.cpp`) | all CNN SR (ESRGAN, Compact, SPAN, RealPLKSR) proven here; DAT2 blocked at pnnx conversion, not at the runtime |
| ExecuTorch Vulkan delegate (1.x, Android AAR) | "under active development"; partitioner runs unsupported ops on CPU | Arm's learning path exports Swin2SR through ExecuTorch + VGF — host-side, no phone numbers |
| stable-diffusion.cpp (ggml, Vulkan) | Vulkan backend; ESRGAN upscaling | Android only via Termux; no transformer SR |
| MNN (Vulkan backend) | not found for SR in this sweep | — |

## 5. Conclusions for KARTUN

1. The models we chose sit on the 2026 frontier for phones: the NTIRE 2026 mobile track was won by a PLKSR CNN and an RRDB
   — exactly the architectures of the owner's APK picks (NomosWebPhoto RealPLKSR, NMKD Siax).
2. The next quality step is **DAT2 on ncnn Vulkan**: it unlocks UltraSharp V2 (the owner accepted it, 14:13) and the
   commercially clean Nomos2 hq dat2 and BHI dat2 real. The runtime has attention on Vulkan; the blocker is the conversion.
   Attempt 3 (and the 3-attempts stop, `BUG_FIXING_FRAMEWORK.md`): trace at a FIXED tile size (DAT's Python size checks are
   what break dynamic tracing) and let the engine pad edge tiles to that size; measure the Titan's time per tile before
   promising it — DAT2 was 4.3× slower than RealPLKSR on the PC.
3. DySample variants (Nomos2 realplksr dysample, BHI realplksr dysample) — one conversion try each; ncnn has GridSample.
4. Diffusion SR is out of scope for the Vulkan engine.

## 6. Forks for the owner

- ClearReality V1 licence conflict (Apache-2.0 by the author vs CC BY-NC-SA by OpenModelDB) — kept non-commercial in the
  catalogue until the owner decides (or the author is asked).
- DAT2 work (§5.2) is engine work of unknown size — the owner's word on its priority vs «Все модели».
