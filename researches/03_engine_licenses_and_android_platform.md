# Research 03 — Engine lineage, licences, models and the Android 16 platform for KARTUN

> **Создан:** 2026-10-14 (agent: Claude Opus 5.5 via a research subagent; web + GitHub API)
> **Родитель:** `GOAL.md` §2 «Необходимо изучить исходный код и лицензию движка Upscayl/NCNN до начала интеграции» and the owner's «МИТ лицензия по возможности»
> **Статус:** 🔬 living reference — sections A–C verified 2026-10-14 (LICENSE files, commits, git blob hashes, release assets); D–E partly from documentation, re-check before acting
> **Вовне:** licence-sensitive forks (models, INTERNET permission) → the MVP interview in `interviews/`

## A. Engine lineage and licences (VERIFIED 2026-10-14)

| Component | Licence | Source | Use in KARTUN |
|---|---|---|---|
| upscayl/upscayl (Electron app) | **AGPL-3.0** | https://github.com/upscayl/upscayl/blob/main/LICENSE | ideas/UX only — no code, strings, icons, preview images |
| upscayl/upscayl-ncnn ("upscayl-bin") | **AGPL-3.0** since commit `3b65512` (2024-01-28, MIT → AGPL) | https://github.com/upscayl/upscayl-ncnn/commit/3b65512e164b031fbe47a4817b1625d0e5b37d1f | **do not copy** (would make KARTUN AGPL); no Android target anyway |
| xinntao/Real-ESRGAN-ncnn-vulkan | **MIT** (© 2021 Xintao Wang + © 2019 nihui) | https://github.com/xinntao/Real-ESRGAN-ncnn-vulkan/blob/master/LICENSE | the GPU tiling reference; unmaintained since 2022-04, no Android, no CPU path |
| nihui/realsr-ncnn-vulkan | **MIT** | https://github.com/nihui/realsr-ncnn-vulkan | reference for `process_cpu()` (CPU fallback) |
| Tencent/ncnn | **BSD-3** (+ zlib/BSD-2 third-party parts; glslang BSD/MIT/Apache) | https://github.com/Tencent/ncnn/blob/master/LICENSE.txt | the inference runtime |
| libwebp / stb | BSD-3 + patent grant / MIT-or-Unlicense | webmproject/libwebp, nothings/stb | codecs where the platform lacks one |

**ncnn facts:** latest release **20260526**; prebuilt `ncnn-20260526-android-vulkan.zip` (static `libncnn.a`, 33 MB) and
`-android-vulkan-shared.zip`; ABIs incl. arm64-v8a; built with NDK r29, `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` (16 KB
pages OK). `NCNN_SIMPLEVK=ON` loads `libvulkan.so` at runtime → the same binary falls back to CPU on a device without
Vulkan. 20260526 splits long Vulkan command streams (fewer driver timeouts on big images), disables cooperative matrix on
Adreno, adds a persistent PipelineCache (faster cold start) and mmap model loading. Android defaults: RTTI and exceptions
OFF, OpenMP ON. Useful API: `get_gpu_count()`, `GpuInfo::support_fp16_*`, `VulkanDevice::get_heap_budget()`.

Real-ESRGAN-ncnn-vulkan tiling: tile from heap budget (>1900 MB → 200 px, >550 → 100, >190 → 64, else 32), prepadding 10,
`use_fp16_storage/packed = true`, `use_fp16_arithmetic = false`.

Minimal CMake (from nihui/ncnn-android-squeezenet):
```cmake
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20260526-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)
add_library(kartun SHARED kartun_jni.cpp)
target_link_libraries(kartun ncnn android jnigraphics log)
```

**Verdict (MIT path):** own small C++/JNI tiling wrapper modelled on Real-ESRGAN-ncnn-vulkan (MIT) + realsr `process_cpu`
(MIT), linked to ncnn (BSD-3). Ship a NOTICE file + an in-app "Open-source licences" screen (Xintao Wang, nihui, ncnn and
its third-party list, glslang, libwebp, stb, every bundled model). MIT allows selling; it also allows others to resell
clones — the brand is protected by a trademark, not by the code licence.

## B. Android prior art (licences and facts from GitHub API / source files, 2026-10-14)

| App | Licence (reusable in MIT?) | Engine | Models | min / target SDK | Last activity |
|---|---|---|---|---|---|
| tumuyan/RealSR-NCNN-Android (~2.2k★) | MIT (yes) | CLI ELF copied from assets, `ProcessBuilder("sh")` | in APK (278 MB) + user import | 24 / **28** | 1.13.2, 2026-05-30 |
| Lucchetto/SuperImage (~1.6k★) | GPL-3.0 (no) | JNI, MNN Vulkan→OpenCL, fp16, tile 84 | 3 `.mnn` in APK | 24 / 33 | archived 2023-11-09 |
| T8RIN/ImageToolbox (~15k★) | Apache-2.0 (yes, keep NOTICE) | ONNX Runtime, CPU only | downloaded from HF + ONNX import | 24 / 37 | 4.2.1-rc01, 2026-10-09 |
| Jasonzhu1207/ZenConverter (~1.7k★) | AGPL-3.0 (no) | JNI, ncnn 20240410 Vulkan, fp16, tile 200 | downloaded, SHA-256 checked | 26 / 36 | v0.2.4, 2026-10-06 |
| aoihoshizaki/realcugan-android-sdk | MIT (yes) | JNI ncnn + foreground Service/Binder | in APK | 24 / 37 | 2.0.2, 2026-07-14 |
| HF-CYGG/LumaSR | noncommercial (no; design docs useful) | JNI ncnn 20260526 | in APK | 29 / 36 | 2026-07-19 |

**Lessons (field-paid by these projects):**
1. **JNI, not CLI exec.** The CLI design pinned tumuyan to targetSdk 28 (Android 10+ forbids exec from app storage) → no
   Play, Play Protect warnings, "Permission denied" for forks (#19, #29, #80).
2. **`VK_ERROR_DEVICE_LOST` (-4) is the #1 bug** (Adreno 618/725/740, Snapdragon 732G/7+ Gen 2/8 Gen 2 on Android 16):
   adaptive tiles from the heap budget, smaller on Adreno, automatic retry at half tile, full re-init after -4 (a retry
   without it failed with -3 in ZenConverter #36); ncnn ≥ 20260526 splits long command streams (PR #6541).
3. **Pin ncnn to a release tag** (20260526 fixes Adreno 840 cooperative-matrix crashes; master has an open Adreno 840
   regression, ncnn #7097 filed 2026-10-13); keep per-GPU kill switches (cooperative matrix, subgroup ops, fp16).
4. **fp16 is not guaranteed correct** (Mali-G715 driver 54.x wrong with fp16 only, ncnn #7054; Immortalis-G925 black
   output, tumuyan #94; Mali-G720 ncnn #6818) → FP32 safe path; enable fp16 only after an on-device self-test against a
   reference tile (LumaSR) or by ncnn's GPU bug flags (realcugan-sdk "Adreno safe mode").
5. **Edge cases:** image smaller than one tile (SuperImage #21), non-standard edge tiles (memcpy, tumuyan 1.13.0), black
   right/bottom borders, alpha → handle separately (models are RGB; SuperImage #42), EXIF orientation (#18).
6. **Memory:** never hold the whole 4× output as a Bitmap; stream tiles to the encoder; pre-check free memory; cap output pixels.
7. **Threading:** all ncnn/Vulkan calls on ONE dedicated thread; serialize create/release (Mali crashes); avoid OpenMP pool
   churn across threads (ncnn #6775, #6343).
8. **Background/cancel:** foreground service `mediaProcessing`; cancel checked between tiles (ImageToolbox could not cancel
   a single forward pass, #3076); publish outputs atomically.
9. **Models:** download-on-demand with SHA-256 keeps the APK small; custom-model import is the most requested feature but
   converted models often give black output → validate on import.
10. **ONNX Runtime on Android is effectively CPU-only** (NNAPI ~10× slower, XNNPACK crashed — ImageToolbox #3030) → ncnn
    Vulkan is the right engine.
11. **Collect ncnn GPU info lines in-app** (driver version, fp16 flags, bug flags) — every useful field report had them.

**Performance:** no published ncnn-Vulkan timings for Dimensity 8300-class GPUs → we measure on the Titan. Reference
points: SuperImage on Snapdragon 855, MNN Vulkan fp16, x4plus 1080p → 8K **11 min**, x4plus-anime **3 min 42 s**;
Helio G99 / Mali-G57 MC2, 256×256 ×4: ncnn CPU 19.3 s, ncnn GPU 24.9 s; Qualcomm NPU (not Vulkan) x4plus 128×128 tile
49.9 ms on 8 Gen 3, general-x4v3 4.5 ms.

## C. Model licences (VERIFIED 2026-10-14 — git blob hashes, OpenModelDB, Hugging Face cards)

**Upscayl trained no models of its own — its "own" models are renamed Real-ESRGAN files** (byte-identical git blobs +
rename commits):

| Upscayl name | What it really is | Licence | Commercial | Evidence |
|---|---|---|---|---|
| upscayl-standard-4x | realesrgan-x4plus (RRDB 23, 33.4 MB) | BSD-3 | ✅ | commit upscayl `6cfaf45` |
| upscayl-lite-4x | realesr-general-x4v3 (SRVGG compact, 2.4 MB) | BSD-3 | ✅ | commit upscayl `801755c`; maintainer in upscayl#982 |
| digital-art-4x | realesrgan-x4plus-anime (RRDB 6B, 8.9 MB) | BSD-3 | ✅ | byte-identical to xinntao zip |
| high-fidelity-4x | 4xHFA2k by Philip Hofmann (Phhofm) — an anime model | CC BY 4.0 | ✅ with attribution | Phhofm/models README |
| remacri-4x | FoolhardyVEVO | CC BY-NC-SA 4.0 | ❌ | openmodeldb 4x-Remacri |
| ultrasharp-4x, ultramix-balanced-4x | Kim2091 | CC BY-NC-SA 4.0 | ❌ | HF Kim2091/UltraSharp |

Other usable models: realesr-animevideov3 x2/x3/x4 (BSD-3, 1.25 MB; x2/x3 are the x4 net + bicubic downscale),
RealESRGAN_x2plus (BSD-3), 4xNomos8kSC (Phhofm, CC BY 4.0), Real-CUGAN (MIT, bilibili; ncnn port by nihui, ~2.6 MB),
waifu2x (MIT), 4x-Rybu / 4x-animerd (CC0), BSRGAN / RealSR-DF2K (Apache-2.0). Conflicting: 4x-ClearRealityV1 (HF card
Apache-2.0 since 2026-02-11, OpenModelDB says NC). Do not use: unknown-2.0.1.

**Take model files from their ORIGINAL sources** (xinntao release zip v0.2.5.0 — byte-identical but without a licence file,
add the BSD-3 notice; Phhofm/models `*_fp16.bin`; or convert from `.pth`), not from upscayl repos (AGPL, no model notice).

**Obligations:** BSD-3 — ship "Copyright (c) 2021, Xintao Wang" + text, no endorsement; CC BY 4.0 — credit, licence name
+ link, note "converted to NCNN fp16", never encrypt/obfuscate the file; NC models — never bundle or serve; only a
user-supplied "load your own model" path is clean. Model licences do not govern the upscaled output images
(openmodeldb.info/docs/licenses).

**Recommended bundle:** fast general — realesr-general-x4v3 (2.4 MB) · quality general — realesrgan-x4plus (33.4 MB) ·
art — realesrgan-x4plus-anime (8.9 MB) · anime — realesr-animevideov3 (1.2 MB); optional with credits — 4xHFA2k,
4xNomos8kSC (33.4 MB each), Real-CUGAN (MIT). Excluded: Remacri, UltraSharp, UltraMix, UniScale.

Residual risk (register, not a violation): training data with research-only terms (DIV2K, LSDIR, anime frames for HFA2k);
legally unsettled whether such terms reach the released weights; industry practice treats them as usable.

## D. Android 16 (API 36) platform facts

- **Input:** Photo Picker (`PickVisualMedia`, no permission) + SAF `OpenDocument` for non-gallery formats + `ACTION_SEND`
  share-in. Platform decoders: JPEG, PNG, WebP (+animated), GIF, BMP, ICO, WBMP, HEIF/HEIC, AVIF (31+), DNG (device),
  Ultra HDR. Not in platform: JPEG XL, TIFF, PSD, TGA, QOI, PNM, HDR, EXR, SVG → stb_image (TGA/PSD/PNM/HDR, MIT),
  libtiff, libjxl (BSD-3), qoi (MIT), tinyexr (BSD-3), AndroidSVG (Apache-2.0).
- **Output:** `Bitmap.CompressFormat` PNG, JPEG, WEBP_LOSSY, WEBP_LOSSLESS. HEIF via `androidx.heifwriter` (hardware
  HEVC, device-dependent); AVIF/JXL need libavif/libjxl. **WebP max 16383×16383 px** → a 192 MP result cannot be WebP.
- **Background work:** foreground service type `mediaProcessing` (`FOREGROUND_SERVICE_MEDIA_PROCESSING`, Android 15+),
  started only while the app is visible, **6 h per 24 h** cap, `onTimeout()` must stop the service; Android 16
  `Notification.ProgressStyle` for progress.
- **Saving/sharing:** MediaStore insert into `Pictures/KARTUN` with `IS_PENDING`, no storage permission; FileProvider +
  `ACTION_SEND` with read grant.
- **URL download needs `INTERNET`** → conflicts with a "no network permission" offline claim (store Data-safety form).
  Clipboard: read only on an explicit Paste tap (Android 12+ shows a toast).
- **Memory:** 192 MP ARGB = 768 MB; Canvas refuses bitmaps > ~100 MB → keep pixels in native buffers, tile, stream rows
  to disk (libspng / libjpeg-turbo), preview via downsample or `BitmapRegionDecoder`. `largeHeap` does not help.
- **Vulkan:** check `FEATURE_VULKAN_HARDWARE_VERSION` + `ncnn::get_gpu_count()`; fallback ladder: smaller tile → fp32 →
  CPU. The Titan reports Vulkan 1.3 + `vulkan.compute` (probe 2026-10-14, `HOUSE_RULES.md` §3).
- **Play (verified):** target API 36 required from 2026-08-31 (extension to 2026-11-01); 16 KB pages required (NDK r28+,
  AGP 8.5.1+); developer verification for installs (incl. sideload) starts 2026-09-30 in BR/ID/SG/TH, global in 2027.

- **Android 17 (API 37) — added 2026-10-14 from the platform subagent (sources: developer.android.com 17 pages):**
  stable since 2026-06-16 → plan `minSdk 36` / `targetSdk 37`. **Memory Limiter** (all apps; Pixel first, other OEMs over
  the coming year): anon RSS + swap soft limit by device RAM — 12 GB device: 8192 MiB visible / **4096 MiB not visible**
  (an FGS process counts as not visible); kill shows as `REASON_OTHER` "MemoryLimiter:AnonSwap". Implicit URI grants for
  `SEND`/`SEND_MULTIPLE` end in Android 18 → grant explicitly. `System.load()`ed files must be read-only (APK `.so` and
  model files unaffected). WorkManager long-running workers can exhaust the job quota on Android 16 → start the
  `mediaProcessing` FGS directly from the user's tap; hold a `PARTIAL_WAKE_LOCK` only while tiles run (Play vitals flags
  ≥ 2 h/24 h). `ProgressStyle` notification + Cancel action; Live Updates promotion is a poor fit.
- **Vulkan floor:** AOSP says devices launching with Android 16 must support Vulkan 1.4, but the 16 CDD only REQUIRES
  1.1 (1.3 strongly recommended) → treat 1.1 as the floor; check features at runtime (ncnn does).
- **Decoders:** `ImageDecoder` returns HARDWARE bitmaps by default (`ALLOCATOR_SOFTWARE` for pixel access); RAW formats
  decode the embedded JPEG preview; JPEG XL is not in the platform; `androidx.exifinterface` writes EXIF only to JPEG,
  PNG, WebP. Encoders: `Bitmap.CompressFormat` has no HEIF/AVIF/JXL through API 37; AVIF via `androidx.heifwriter`
  `AvifWriter` (1.1.0 stable) needs an AV1 encoder on the device; HEIC via `HeifWriter` (grid tiles, HEVC encoder).
- **Preview of huge outputs:** non-HARDWARE bitmaps above `max(ro.hwui.max_texture_allocation_size, 150 MB)` cannot be
  drawn → downsampled preview + region tiles (library `telephoto`, Apache-2.0, sub-sampling zoom for Compose).

## E. Per-app language

`generateLocaleConfig = true` (AGP 8.1+) + `res/resources.properties`; `AppCompatDelegate.setApplicationLocales()` or the
platform `LocaleManager` (KrinikCam uses the latter). RTL: `android:supportsRtl="true"`, Compose mirrors automatically,
`Icons.AutoMirrored`; the before/after comparison itself must NOT mirror. Top-10 by speakers (Ethnologue 2025): en, zh-Hans,
hi, es, ar, fr, bn, pt, ru, id; the owner's own list for KrinikCam: RU, EN, ES, PT, ZH, HI, BN, FR, DE, AR.

## Key risks (tiered)

- **(a) build defences:** AGPL contamination (copying upscayl code); non-commercial models bundled; GPU driver faults
  (fp16 black output, device lost) → fallback ladder + golden-image check; OOM on big outputs → native tiles streamed to
  disk; the engine not running on Android at all → Phase 0 spike on the Titan before any UI.
- **(b) contingency listed:** `mediaProcessing` 6 h cap and foreground-start rule; WebP size cap; INTERNET vs "offline"
  wording; thermal throttling on long runs; unclear model licences.
- **(c) remember:** developer verification for sideloading (2026–27); resold MIT clones (trademark).
