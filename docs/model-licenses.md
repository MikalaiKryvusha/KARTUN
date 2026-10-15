# Model licences and provenance

KARTUN runs Real-ESRGAN family models on the device through ncnn. Every model file below comes from its original
author's release; the SHA-256 lets anyone re-check the bytes. All of them are under the BSD 3-Clause licence of
Real-ESRGAN — full text: [licenses/Real-ESRGAN_BSD-3-Clause.txt](licenses/Real-ESRGAN_BSD-3-Clause.txt)
(Copyright (c) 2021, Xintao Wang).

| Model in KARTUN | Role | Source | Files and SHA-256 |
|---|---|---|---|
| `realesr-general-x4v3` | default, fast photo model | weights `realesr-general-x4v3.pth` from [xinntao/Real-ESRGAN release v0.2.5.0](https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0) (sha256 `8dc7edb9ac80ccdc30c3a5dca6616509367f05fbc184ad95b731f05bece96292`), converted to ncnn by `tools/convert-model.py` (pnnx 20260526) | `.param` `2bf1ccee1c8b86ef4cbb23957ac4f4a0f6fbb1fca6bcc20b39caf3d7e28becfb` · `.bin` `8fdd6052f8931091164a570ee7b5c5b2d1dcd991112d464eb9ae1a5b52ece553` |
| `realesrgan-x4plus` | "maximum quality" card | `realesrgan-ncnn-vulkan-20220424-windows.zip` from the same release v0.2.5.0 (sha256 `abc02804e17982a3be33675e4d471e91ea374e65b70167abc09e31acb412802d`), `models/` folder, unchanged | `.param` `35330ececcea33b6c397a72548e788d5d53becee4734c50b7fada36e89f10a86` · `.bin` `713ee713b0353afaa27976f0563a64a5043bd70b9bd8936c2e26e25ebcdbcddf` |
| `realesr-animevideov3-x4` | anime and drawings | same zip, `models/` folder, unchanged | `.param` `850a248e7c14c27e5bd8cf7265113a9441036a7db63963bb8aa5169d788a435e` · `.bin` `548a36f9c3f4ab8da56cd3b13badf23968bee207b396dad14d04b830e5f2ab2d` |

## Conversion check

The converted `realesr-general-x4v3` is compared with the original PyTorch model on the same 256×256 image (the
release's `input2.jpg`): the ncnn output is 47.1 dB PSNR from the PyTorch output, a bicubic 4× resize is 30.1 dB.
Run: `testcases/reports/2026-10-14_F0_own_fast_model.md`.

## Engine

ncnn `20260526` (Tencent, BSD 3-Clause) — prebuilt `ncnn-20260526-android-vulkan.zip`, fetched and hash-checked by
`tools/fetch-ncnn.mjs`.

## Catalogue models (downloaded on demand, not in the APK)

Served from the GitHub release `models` of this repository; the files, their SHA-256 and sizes live in one place —
[model-catalogue.json](model-catalogue.json). **The models belong to their authors**; KARTUN claims no ownership and
passes on no rights beyond each author's licence.

| Model | Author | Licence | Commercial use | Original source |
|---|---|---|---|---|
| Real-ESRGAN General v3 | Xintao Wang | BSD 3-Clause | allowed | xinntao/Real-ESRGAN release v0.2.5.0, our conversion (above) |
| Real-ESRGAN x4plus anime | Xintao Wang | BSD 3-Clause | allowed | xinntao/Real-ESRGAN release v0.2.5.0 |
| Real-ESRGAN AnimeVideo v3 | Xintao Wang | BSD 3-Clause | allowed | xinntao/Real-ESRGAN release v0.2.5.0 |
| 4xNomos8kSC | Philip Hofmann (Phhofm) | CC BY 4.0 | allowed, with credit | [Phhofm/models](https://github.com/Phhofm/models) release 4xNomos8kSC, ncnn fp16 files unchanged |
| 4xHFA2k | Philip Hofmann (Phhofm) | CC BY 4.0 | allowed, with credit | [Phhofm/models](https://github.com/Phhofm/models) release 4xHFA2k, ncnn fp16 files unchanged |
| 4xNomosWebPhoto RealPLKSR | Philip Hofmann (Phhofm) | CC BY 4.0 | allowed, with credit | [Phhofm/models](https://github.com/Phhofm/models) release 4xNomosWebPhoto_RealPLKSR, converted to ncnn fp16 |
| 4x NMKD Superscale | NMKD | WTFPL | allowed | `4x_NMKD-Superscale-SP_178000_G.pth` ([OpenModelDB](https://openmodeldb.info/models/4x-NMKD-Superscale)), converted to ncnn fp16 |
| 4x NMKD Siax | NMKD | WTFPL | allowed | `4x_NMKD-Siax_200k.pth` ([OpenModelDB](https://openmodeldb.info/models/4x-NMKD-Siax-CX)), converted to ncnn fp16 |
| 4x ClearReality V1 | Kim2091 | Apache 2.0 by the author's card; CC BY-NC-SA 4.0 by OpenModelDB | **treated as not allowed** until settled | [huggingface.co/Kim2091/ClearRealityV1](https://huggingface.co/Kim2091/ClearRealityV1), converted to ncnn fp16 |
| 4x Remacri | FoolhardyVEVO | CC BY-NC-SA 4.0 | **not allowed** | `4x_foolhardy_Remacri.pth` ([OpenModelDB](https://openmodeldb.info/models/4x-Remacri)), converted to ncnn fp16 |
| 4x UltraMix Balanced | Kim2091 | CC BY-NC-SA 4.0 | **not allowed** | [huggingface.co/Kim2091/UltraSharp](https://huggingface.co/Kim2091/UltraSharp) `Interpolations/`, converted to ncnn fp16 |
| 4x UltraSharp V2 Lite | Kim2091 | CC BY-NC-SA 4.0 | **not allowed** | [huggingface.co/Kim2091/UltraSharpV2](https://huggingface.co/Kim2091/UltraSharpV2), converted to ncnn fp16 |

Original files, their SHA-256 and the conversion check: `researches/07_best_models_recon.md` §2–§3; the converter is
`tools/convert-model.py`.

CC BY 4.0: https://creativecommons.org/licenses/by/4.0/ · CC BY-NC-SA 4.0: https://creativecommons.org/licenses/by-nc-sa/4.0/ ·
Apache 2.0: https://www.apache.org/licenses/LICENSE-2.0 · WTFPL: http://www.wtfpl.net/
