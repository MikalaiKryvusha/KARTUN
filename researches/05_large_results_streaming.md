# Research 05 — Writing and showing upscale results larger than memory: row-streamed encoders and region decoding

> **Created:** 2026-10-14 (agent, guarded loop)
> **Parent:** `plans/04_epic01_F1_reliable_engine.md` step 3 (tiles to disk); `plans/01_EPIC_mvp.md` §4.5 (memory), risk 5.1-4
> **Status:** recon done 2026-10-14 ≈ 19:24 +03:00; no code yet — the recommendation below waits for its plan step
> **Outbound:** —

## The question

Today the whole 4× result is one `Bitmap` (plans/03 slice 1, `MAX_OUTPUT_MP = 200` → up to 800 MB of native memory).
How do proven tools write a result that never exists whole in memory, and how do viewers show one?

## Findings (sources at the end)

1. **Encoding is row-sequential by design in both reference libraries.**
   - libpng: `png_write_info` → `png_write_row` once per row → `png_write_end`; the caller keeps one scanline
     ([codemia: libpng write path](https://codemia.io/knowledge-hub/path/fast_encode_bitmap_buffer_to_png_using_libpng)).
     Android ships no public libpng in the NDK — it must be bundled (zlib can be the system one —
     [51cto build note](https://blog.51cto.com/u_16087831/7682455)).
   - libjpeg-turbo: `jpeg_start_compress` → `jpeg_write_scanlines` while `next_scanline < image_height`; Skia's own JPEG
     encoder feeds it one row at a time ([SkJPEGImageEncoder](https://skia.googlesource.com/skia.git/+/4871f22/src/images/SkJPEGImageEncoder.cpp)).
     Scanlines must come top to bottom; with default parameters no temp files are needed, while optimized Huffman tables
     / progressive mode may spill to temp files ([libjpeg-turbo example.txt](https://cobalt.googlesource.com/cobalt.git/+/308ed6b84664d59944cd65f4b4359c28a2443be6/src/third_party/libjpeg-turbo/example.txt)).
   - PNGJ — a pure-Java PNG reader/writer that works row by row on Android ([leonbloy/pngj](https://www.github.com/leonbloy/pngj)).
2. **The platform encoder needs the whole image.** `AndroidBitmap_compress` (NDK, API 30+) takes one pixel pointer and
   streams only its OUTPUT through a callback ([NDK bitmap reference](https://developer.android.com/ndk/reference/group/bitmap));
   `Bitmap.compress` likewise. They cannot write a result that is not in memory.
3. **Consequence for our tiles:** the engine produces tiles of 200 px × 4 = 800 px; a row encoder needs full-width rows,
   so the writer keeps a STRIP — one tile row of output: width × 800 px × 4 B (e.g. 15 360 × 800 × 4 ≈ 49 MB for the
   16× of a 1.2 MP photo) instead of the whole 1.26 GB.
4. **Showing a huge result:** proven viewers never decode it whole — subsampling-scale-image-view loads a low-resolution
   base and overlays full-resolution tiles of the visible area through `BitmapRegionDecoder` (PNG and JPEG of any size)
   ([SSIV](https://sourceforge.net/projects/subsampling-scale-iview.mirror/), [custom decoders](https://github.com/davemorrissey/subsampling-scale-image-view/wiki/04.-Custom-decoders)).
5. **Tile seams** are a known cost of tiling ([Real-ESRGAN ncnn guide](https://mintlify.com/xinntao/Real-ESRGAN/guides/ncnn-executable));
   our 10 px prepadding already handles them and stays unchanged.

## Recommendation (for the plan step, not decided here)

- Engine: a native "strip writer" — after each tile ROW the engine hands the strip to an encoder instead of a full output
  bitmap: libpng (bundled, BSD-like libpng licence) for PNG, libjpeg-turbo (BSD-style/IJG) for JPEG; WebP keeps its
  16 383 px limit (plans/01 §4.5) and stays bitmap-based for results that fit.
- The written file goes to app cache, then MediaStore (phase 3) — the original is never touched.
- The result screen shows the file through `BitmapRegionDecoder` tiles (or SSIV, Apache-2.0) instead of one bitmap.
- Second-order effect: the 200 MP limit then only bounds TIME, and the 16× of normal photos becomes possible.
- `FORK:` libpng + libjpeg-turbo in C++ vs PNGJ in Java — price of error: a JNI round-trip per strip vs a second native
  library to build with NDK r29 — consulted: this recon; to be decided at the plan step with a measurement.

## Sources

[codemia: libpng write path](https://codemia.io/knowledge-hub/path/fast_encode_bitmap_buffer_to_png_using_libpng) ·
[51cto: libpng on Android](https://blog.51cto.com/u_16087831/7682455) ·
[Skia SkPNGImageEncoder](https://skia.googlesource.com/skia/+/chrome/m54/src/images/SkPNGImageEncoder.cpp) ·
[Skia SkJPEGImageEncoder](https://skia.googlesource.com/skia.git/+/4871f22/src/images/SkJPEGImageEncoder.cpp) ·
[libjpeg-turbo example.txt](https://cobalt.googlesource.com/cobalt.git/+/308ed6b84664d59944cd65f4b4359c28a2443be6/src/third_party/libjpeg-turbo/example.txt) ·
[libjpeg-turbo 2.0.6 release](https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/2.0.6) ·
[PNGJ](https://www.github.com/leonbloy/pngj) ·
[NDK bitmap reference](https://developer.android.com/ndk/reference/group/bitmap) ·
[subsampling-scale-image-view](https://sourceforge.net/projects/subsampling-scale-iview.mirror/) ·
[SSIV custom decoders](https://github.com/davemorrissey/subsampling-scale-image-view/wiki/04.-Custom-decoders) ·
[Real-ESRGAN ncnn executable guide](https://mintlify.com/xinntao/Real-ESRGAN/guides/ncnn-executable)
