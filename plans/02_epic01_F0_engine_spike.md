# Plan 02 — Epic 01, phase 0: prove the Real-ESRGAN engine on the Titan (Vulkan and CPU) before any UI

> **Создан:** 2026-10-14 (agent)
> **Родитель:** `plans/01_EPIC_mvp.md` §3 row «0 — проверка движка», gate: criteria 2.4 and 2.6 met by a console probe on the Titan
> **Статус:** 📋 planned, not started
> **Вовне:** the measured time per 1 MP → to the owner at phase close (proposes the number for criterion 2.6)

## Goal vector

Achieve: a 256×256 test image is upscaled 4× by `realesrgan-x4plus` (and by `realesr-general-x4v3`) on the Titan through
our own JNI engine, once on Vulkan and once on CPU; the output is 1024×1024, matches the PC reference within a fixed
tolerance, differs from a bicubic resize by more than that tolerance, and the wall time of every run is recorded.

## Acceptance criteria (Scale · Meter · Target)

1. Build: `assembleDebug` exits 0 with the native engine linked (Meter: build log; Target: exit 0, `libkartun.so` for arm64-v8a in the APK).
2. Vulkan run: Scale — output pixels; Meter — instrumented probe on the Titan (`adb shell am instrument …` or a debug
   activity driven by `adb`); Target — 1024×1024 PNG pulled back, PSNR vs PC reference ≥ 40 dB, PSNR vs bicubic < 40 dB
   (tolerance numbers are proposed here and confirmed by the first PC run; any change is justified in the commit).
3. CPU run: the same with `use_vulkan_compute=false`.
4. Timing: ms per run for 256×256 and for a 1 MP image (1024×1024) on Vulkan and CPU, both models — written to a run
   report `testcases/reports/<date>_F0_engine_probe.md`.
5. Honest failure: with the model file deleted, the probe returns an error code and message; no crash (logcat clean of `SIGSEGV`).

## Steps

1. [ ] Toolchain: `sdkmanager "cmake;3.31.6"` (or the latest available 3.x) into `D:\Android\Sdk`; confirm NDK r27 is
   enough for ncnn 20260526 prebuilt (built with r29 → if link errors, install NDK r29 via sdkmanager). Record versions in
   `HOUSE_RULES.md` §4.
2. [ ] Gradle project skeleton: wrapper (current stable Gradle/AGP for API 36), `:app` (minimal Compose activity),
   `:engine` (Android library, CMake, `abiFilters arm64-v8a`, 16 KB pages), version catalog. Ignore-first: `.gitignore`
   already covers `build/`, `.gradle/`, `.cxx/`, `local.properties`.
3. [ ] Fetch `ncnn-20260526-android-vulkan.zip` from the Tencent/ncnn GitHub release into `engine/third_party/` (gitignored;
   a script `tools/fetch-ncnn.mjs` downloads and checks SHA-256 so the build is reproducible). Record the hash.
4. [ ] Models: download `realesrgan-ncnn-vulkan-…` release zip v0.2.5.0 from xinntao, take `realesrgan-x4plus.{param,bin}`;
   convert or obtain `realesr-general-x4v3` ncnn files from the original `.pth` (BSD-3) — record provenance and SHA-256 in
   `docs/model-licenses.md`; add the BSD-3 notice.
5. [ ] Engine C++ (`engine/src/main/cpp/`): `kartun_engine.cpp` — create `ncnn::Net`, options (Vulkan on/off,
   fp16 storage on, fp16 arithmetic off), load param/bin from a file path, process one image with tiling (tile from
   `get_heap_budget()` like Real-ESRGAN-ncnn-vulkan, prepadding 10), return RGBA; JNI `kartun_jni.cpp` with
   `nativeInit`, `nativeUpscale(inPath, outPath, model, useGpu, tile)`, `nativeRelease`; all calls on one thread.
   Written from the MIT references (Real-ESRGAN-ncnn-vulkan, realsr `process_cpu`) — notices kept in file headers.
6. [ ] Probe harness: a debug-only activity/receiver that runs the engine on a file under the app's files dir, writes
   the PNG and a JSON line (ms, backend, tile, gpu name) to logcat; driven with `adb shell am start … --es in … --ez gpu true`.
7. [ ] PC reference: run the same model with the desktop `realesrgan-ncnn-vulkan` (MIT, v0.2.0 release) on the same
   input → `testdata/ref_*.png`; a node script computes PSNR (sharp/pngjs or ImageMagick `magick compare -metric PSNR`).
8. [ ] Run on the Titan: Vulkan and CPU, both models, 256×256 and 1 MP; pull outputs; compare; write the run report.
9. [ ] Control case: delete the model file → expect the error path (criterion 5).
10. [ ] Close: update `STATUS.md`, `MASTER_PLAN.md` phase 0 status, report the timing to the owner, write the phase 1 plan.

## Risks

- ncnn prebuilt needs NDK r29 / a newer libc++ → install r29 (step 1).
- `vkCreateComputePipelines` or device-lost on Mali-G615 → tile 64/32, fp32, CPU (research 03 §B lessons 2, 4).
- `realesr-general-x4v3` has no official ncnn file → conversion with pnnx (BSD-3) may cost time; x4plus alone satisfies the gate.
- Relay latency of adb over Tailscale (150–450 ms) slows pushes of 33 MB models → push once, keep on the device.
