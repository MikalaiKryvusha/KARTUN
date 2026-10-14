# KARTUN — Internal architecture map

> **The INTERNAL map: the project's logical architecture** — the abstractions and how they interact. Where
> `PROJECT_STRUCTURE_EXTERNAL_MAP.md` says *where things live*, this says *how the system thinks*.
> **Living reference — never DONE-tagged.** Status 2026-10-14: the TARGET model of the MVP; no code exists yet —
> every row is `[NOT-TESTED]` until the Phase 0 spike on the Titan proves the engine.

---

## The core abstractions

| Abstraction | Essence | Responsibility |
|-------------|---------|----------------|
| `SourceImage` | the user's picked image (content URI + decoded size, format, orientation) | read-only; the original is never modified |
| `UpscaleRequest` | model + scale + output format/quality | validated BEFORE the run: output megapixels, format limits (WebP ≤ 16383 px) |
| `ModelCatalog` | bundled ncnn models with licence metadata | lists models, their scale, licence, attribution |
| `Engine` (C++ behind JNI) | ncnn `Net` + tiler | load model; pick Vulkan or CPU; process tiles; report progress; cancel between tiles; free GPU/CPU |
| `UpscaleJob` | one run in a foreground service (`mediaProcessing`) | owns the engine call, progress, cancel, error mapping, atomic publish |
| `ResultImage` | the output file in app cache, streamed tile by tile | compared to the source; then saved or shared |
| `ResultStore` | MediaStore (`Pictures/KARTUN`) + FileProvider share | save without overwriting the original; share via the system sheet |
| `DeviceProfile` | Vulkan presence/version, GPU name, fp16 safety, heap budget | chooses tile size, precision and the fallback ladder; shown in diagnostics |

## How they interact

```
Photo Picker / SAF / share-in ─▶ SourceImage ─▶ UpscaleRequest ─▶ UpscaleJob ─▶ Engine(JNI) ─tiles─▶ ResultImage
                                                       ▲                 │               ▲
                                              ModelCatalog        progress/cancel   DeviceProfile
                                                                         ▼
                                              Compare screen (before/after slider) ─▶ ResultStore (save · share)
```

## Invariants & rules of the model

1. The image never leaves the device in the processing path; the neural upscale happens only inside `Engine`.
2. No fake upscale: if the engine cannot run, the user gets an honest error — never an interpolated result.
3. The original is never overwritten; output names are new files.
4. All ncnn/Vulkan calls run on ONE dedicated thread; create/release are serialized (research 03 §B lesson 7).
5. The full 4× output is never held as one Java `Bitmap`; tiles stream to the encoder (research 03 §D).
6. Engine fallback ladder on GPU failure: smaller tile → fp32 → CPU → honest error (research 03 §B lessons 2, 4).

## Key decisions embedded in the architecture

- JNI over a C++ wrapper modelled on MIT code (Real-ESRGAN-ncnn-vulkan, realsr `process_cpu`) + ncnn BSD-3 — keeps KARTUN
  MIT and Play-compatible (CLI exec forces targetSdk 28) — research 03 §A, §B.
- ncnn pinned to release `20260526` (prebuilt android-vulkan) — research 03 §A, §B lesson 3.
- Decisions are logged in `MASTER_PLAN.md` → Decision log.
