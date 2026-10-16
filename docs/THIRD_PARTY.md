# Third-party components in KARTUN

KARTUN's own code is under the MIT licence. It ships these components of other authors; each licence text lies next to
this file in [licenses/](licenses/).

| Component | What it does in KARTUN | Licence | Text |
|---|---|---|---|
| [ncnn](https://github.com/Tencent/ncnn) `20260526` (Tencent) | runs the neural network on the GPU (Vulkan) or the CPU | BSD 3-Clause | in the ncnn package, fetched by `tools/fetch-ncnn.mjs` |
| Real-ESRGAN models (Xintao Wang) | the upscaling networks | BSD 3-Clause | [Real-ESRGAN_BSD-3-Clause.txt](licenses/Real-ESRGAN_BSD-3-Clause.txt) — sources and hashes: [model-licenses.md](model-licenses.md) |
| [libtiff](https://libtiff.gitlab.io/libtiff/) `4.7.2` (Sam Leffler, Silicon Graphics) | opens TIFF photos (the engine's second decoder) | libtiff licence (BSD-like) | [libtiff_LICENSE.md](licenses/libtiff_LICENSE.md) — fetched by `tools/fetch-libtiff.mjs` (SHA-256 pinned) |
| [stb_image](https://github.com/nothings/stb) `v2.30`, commit `2c980bb` (Sean Barrett) | opens TGA, PSD, PPM/PGM, HDR (the engine's second decoder) | public domain (alternative: MIT) | [stb_image_LICENSE.txt](licenses/stb_image_LICENSE.txt) — `engine/src/main/cpp/third_party/stb_image.h` |
| [QOI](https://github.com/phoboslab/qoi), commit `ffb2d2c` (Dominic Szablewski) | opens QOI pictures | MIT | in `engine/src/main/cpp/third_party/qoi.h` |
| [Unbounded](https://github.com/googlefonts/unbounded) (The Unbounded Project Authors) | headings and the logo | SIL Open Font License 1.1 | [Unbounded_OFL.txt](licenses/Unbounded_OFL.txt) |
| [Manrope](https://github.com/googlefonts/manrope) (The Manrope Project Authors) | body text | SIL Open Font License 1.1 | [Manrope_OFL.txt](licenses/Manrope_OFL.txt) |
| AndroidX / Jetpack Compose (Google) | the user interface | Apache 2.0 | via Maven, versions in `gradle/libs.versions.toml` |
