# Run report — input formats on the Titan (plans/08 step 3)

**Work:** which input formats KARTUN opens; the basis is the owner's order in `GOAL.md` («Входных прям очень много
нужно») and `MASTER_PLAN.md` phase 4 (system formats + TGA, PSD, PNM, HDR, TIFF from open libraries). Change under test:
the engine's second decoder `ImageCodec` (stb_image v2.30, qoi.h, libtiff 4.7.2) behind `UpscaleEngine.decodeAny`.

**Contour:** HEADWOLF Titan 1 (`100.99.111.48:5555`), Android 16, debug build of the working tree; the agent door
(`AgentService`, the same `decodeAny` the screens call); model `realesr-general-x4v3`, scale 2×, PNG out. Test pictures
made from one photo (`engine/third_party/preview-src/cat.jpg`, 400 px wide) by ImageMagick 7.

**Runs:**
1. 2026-10-16 14:53 +03:00 — before the change: `node tools/kartun-cli.mjs upscale fmt_<ext>.<ext> --model realesr-general-x4v3 --scale 2 --format png --out out/<ext>.png` per format.
2. 2026-10-16 15:09 +03:00 — stb_image + qoi: `KARTUN_SERIAL=100.99.111.48:5555 node tools/format-matrix.mjs engine/third_party/preview-src/cat.jpg <scratch>/fm1` (exit 1: TIFF).
3. 2026-10-16 15:12 +03:00 — + libtiff: the same command into `<scratch>/fm2` (exit 0).

**Checks:**
- Hygiene: `.\gradlew.bat assembleDebug` green; `tools/format-matrix.mjs` reddened on TIFF before libtiff (run 2) — the tool can fail.
- Functional run: each format sent through the agent door on the Titan, the result PNG pulled and compared with the PNG
  run's result by PSNR (run 3):

| Format | Run 1 (ImageDecoder only) | Run 3 (with the second decoder) | PSNR to the PNG result |
|---|---|---|---|
| PNG | opens | opens | — |
| JPEG | opens | opens | 42.6 dB (lossy source) |
| WebP | opens | opens | 36.3 dB (lossy source) |
| GIF | opens | opens | 42.0 dB (256 colours) |
| BMP | opens | opens | identical |
| AVIF | opens | opens | 33.9 dB (lossy source) |
| ICO | opens | opens | identical |
| TGA | `decode_failed` | opens | identical |
| PSD | `decode_failed` | opens | identical |
| PPM | `decode_failed` | opens | identical |
| PGM | `decode_failed` | opens | 45.1 dB to the grey PNG |
| QOI | `decode_failed` | opens | identical |
| HDR | `decode_failed` | opens | 38.5 dB (8 → HDR → 8 bit) |
| TIFF | `decode_failed` | opens | identical |
| JPEG XL | `decode_failed` | `decode_failed` | — (not in the plan) |
| OpenEXR | `decode_failed` | `decode_failed` | — (not in the plan) |

Not covered: HEIC (ImageMagick here cannot write it — a real HEIC photo is still to be tried), DNG, a 16-bit or CMYK TIFF,
a layered PSD with no composite; the screen path (a TGA opened from a file manager by «Поделиться» / «Открыть с помощью»).

**Found:** none in the decoder. Gap noted for the owner: the system Photo Picker lists only what Android counts as
photos, so TGA/PSD/… reach KARTUN through «Поделиться» or a file manager — the owner's question in chat 2026-10-16.

**Traces:** results in the session scratchpad `fm1/out/`, `fm2/out/` (outside git); the matrix output above is the
tool's stdout.

**Verdict:** pass for the agent path — every format of the plan opens and the lossless ones give the same picture as PNG;
partial for the feature as a whole until the screen path is walked.
