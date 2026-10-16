# Model licences and provenance

KARTUN runs neural upscaling models on the device through ncnn. Two of them ship inside the app; the others are
downloaded on demand (below). Every file comes from its author's release or is our conversion of the author's original
weights; the SHA-256 lets anyone re-check the bytes. **The models belong to their authors**; KARTUN claims no ownership.

## Inside the APK

| Model in KARTUN | Role | Author · licence | Source | Files and SHA-256 |
|---|---|---|---|---|
| `4xNomosWebPhoto-RealPLKSR-fp16` | default model: photos from the web, compression and blur | Philip Hofmann (Phhofm) · [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) | weights `4xNomosWebPhoto_RealPLKSR.pth` (sha256 `a9db66c9b674c6a5025b6ef3bee71a57c33b8605d8a2de0980470f89002efbbe`) from [Phhofm/models](https://github.com/Phhofm/models) release 4xNomosWebPhoto_RealPLKSR, converted to ncnn fp16 by `tools/convert-model.py` | `.param` `ca7f9eda7ff09094d18f5ad1672fddb4a71c29cb0adfe4e083ed29bb42e0cd0a` · `.bin` `1b8659c1ab6858326f0b3c1cc8f5747cf80052edd71b1d3274a06599dd23366a` |
| `4x-NMKD-Siax-fp16` | second card: universal, clean or slightly compressed pictures | NMKD · [WTFPL](http://www.wtfpl.net/) | weights `4x_NMKD-Siax_200k.pth` (sha256 `560424d9f68625713fc47e9e7289a98aabe1d744e1cd6a9ae5a35e9957fd127e`, [OpenModelDB](https://openmodeldb.info/models/4x-NMKD-Siax-CX)), converted to ncnn fp16 by `tools/convert-model.py` | `.param` `3203034dfbcb3f1b26b7766598f2c78b809bea4e27c150f93dff66d841c29974` · `.bin` `01f54cc3b06e13ee49c1ac601f005fffe8cb8d7a1dde98cfa8ef7541b1896f52` |

The owner's pick of 2026-10-15 (blind test, MASTER_PLAN decision log 14:12-14:21). Until then the APK carried
Real-ESRGAN General v3 and x4plus; both are in the catalogue now. The credit CC BY 4.0 asks for stands in the app's
«О приложении» → licences and on the model's card.

## Real-ESRGAN models (catalogue)

Under the BSD 3-Clause licence of Real-ESRGAN — full text: [licenses/Real-ESRGAN_BSD-3-Clause.txt](licenses/Real-ESRGAN_BSD-3-Clause.txt)
(Copyright (c) 2021, Xintao Wang).

| Model in KARTUN | Role | Source | Files and SHA-256 |
|---|---|---|---|
| `realesr-general-x4v3` | fast photo model | weights `realesr-general-x4v3.pth` from [xinntao/Real-ESRGAN release v0.2.5.0](https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0) (sha256 `8dc7edb9ac80ccdc30c3a5dca6616509367f05fbc184ad95b731f05bece96292`), converted to ncnn by `tools/convert-model.py` (pnnx 20260526) | `.param` `2bf1ccee1c8b86ef4cbb23957ac4f4a0f6fbb1fca6bcc20b39caf3d7e28becfb` · `.bin` `8fdd6052f8931091164a570ee7b5c5b2d1dcd991112d464eb9ae1a5b52ece553` |
| `realesrgan-x4plus` | detailed photo model | `realesrgan-ncnn-vulkan-20220424-windows.zip` from the same release v0.2.5.0 (sha256 `abc02804e17982a3be33675e4d471e91ea374e65b70167abc09e31acb412802d`), `models/` folder, unchanged | `.param` `35330ececcea33b6c397a72548e788d5d53becee4734c50b7fada36e89f10a86` · `.bin` `713ee713b0353afaa27976f0563a64a5043bd70b9bd8936c2e26e25ebcdbcddf` |
| `realesr-animevideov3-x4` | anime and drawings | same zip, `models/` folder, unchanged | `.param` `850a248e7c14c27e5bd8cf7265113a9441036a7db63963bb8aa5169d788a435e` · `.bin` `548a36f9c3f4ab8da56cd3b13badf23968bee207b396dad14d04b830e5f2ab2d` |

## Conversion check

The converted `realesr-general-x4v3` is compared with the original PyTorch model on the same 256×256 image (the
release's `input2.jpg`): the ncnn output is 47.1 dB PSNR from the PyTorch output, a bicubic 4× resize is 30.1 dB.
Run: `testcases/reports/2026-10-14_F0_own_fast_model.md`.

## Engine

[MikalaiKryvusha/ncnn](https://github.com/MikalaiKryvusha/ncnn/tree/20260526-kartun.1) `20260526-kartun.1` — custom modified
and improved fork of ncnn `20260526` (Tencent, BSD 3-Clause), built from source by `tools/build-ncnn.mjs` (tag and commit
pinned; the build checks that the fixed shader is inside).

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
