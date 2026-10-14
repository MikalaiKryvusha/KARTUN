# Research 02 — Upscayl 2.15.0 desktop: UI/UX, engine calls, models (reverse engineering)

> **Создан:** 2026-10-14 (agent: Claude Opus 5.5, read-only study of the owner's install `C:\Program Files\Upscayl`; `app.asar` unpacked with a hand-written header parser into the session scratchpad; nothing run, nothing modified)
> **Родитель:** `GOAL.md` — «Простатой оригинального десктопного Upscayl для Виндовз. Есть локально на этой машине для реверс инжиниринга UI/UX»
> **Статус:** 🔬 living reference — recon done 2026-10-14; feeds the parity inventory of the MVP epic
> **Вовне:** —

## 1. Version and licenses — the hard constraint

- Upscayl **2.15.0**, `"license": "AGPL-3.0"` (`app/package.json`), Electron 27.3.11. No AGPL text shipped in the install.
- **Consequence for KARTUN (MIT goal): re-implement behaviour, never copy Upscayl code, strings, translations or assets**
  (incl. the model preview images) unless KARTUN itself becomes AGPL.
- `resources/bin/upscayl-bin.exe` (6.4 MB): embeds the ncnn BSD-3 notice, libwebp decoder, glslang; imports `vulkan-1.dll`.
  Its upstream/licence is not stated in the install — see research 03.
- Models carry NO licence files. UI strings label **Remacri, Ultramix, Ultrasharp "(Non-Commercial)"**; Remacri
  description: "No commercial use."

## 2. Models shipped (`resources/models`), all native 4×, blobs `data` → `output`

| File id | .bin size | Architecture | UI name / description |
|---|---|---|---|
| upscayl-standard-4x | 33.4 MB | RRDBNet 23 blocks | Upscayl Standard — "Suitable for most images." (DEFAULT) |
| upscayl-lite-4x | 2.4 MB | SRVGGNetCompact (34 conv + PReLU + PixelShuffle) | Upscayl Lite — "High-speed upscaling with minimal quality loss." |
| high-fidelity-4x | 33.4 MB | RRDBNet 23 | High Fidelity — realistic details, smooth textures |
| remacri-4x | 33.4 MB | RRDBNet 23 | Remacri (Non-Commercial) |
| ultramix-balanced-4x | 33.4 MB | RRDBNet 23 | Ultramix (Non-Commercial) |
| ultrasharp-4x | 33.4 MB | RRDBNet 23 | Ultrasharp (Non-Commercial) |
| digital-art-4x | 8.9 MB | RRDBNet 6 blocks (anime_6B class) | Digital Art — "For digital art and illustrations." |

Total 178.5 MB. `realesrgan-x4plus` is not shipped (only the binary's `-n` default). Custom models: a folder that must be
named `models`; any `.param`/`.bin` pair; scale inferred from the name (`x2`/`2x` → 2, `x3`/`3x` → 3, else 4).

## 3. Engine invocation (`electron/utils/get-arguments.js`, `spawn-upscayl.js`)

`upscayl-bin` flags: `-i` input · `-o` output · `-s` output scale (only when ≠ model scale; above native 4× it is an AI 4×
pass + plain resize) · `-m` models dir · `-n` model id · `-g` GPU id · `-f` png|jpg|webp · `-w` custom width · `-c`
compression 0–100 · `-t` tile size (0 = auto) · `-x` TTA. Never used: `-z`, `-r`, `-j`, `-v`.
Output name: `<name>_upscayl_<scale>x_<model>.<ext>` (or `_<W>px_`). Existing output + overwrite off → returns the
existing file. Progress: stderr chunks like `45.23%`, ` Upscayled Successfully!`; errors by substring (`Error`, `failed`,
`Invalid GPU`, `tile size`, `read`/`write`). Stop = `kill()` the child.

## 4. UI/UX (single window, 1300×940, bg `#171717`)

- **Left sidebar (350 px), tabs Upscayl / Settings.** Upscayl tab = a 4-step flow: (0) Batch toggle · (1) Select Image /
  Folder · (2) Select AI Model — a dialog of model CARDS, each with a half-and-half before/after preview image; "Double
  Upscayl" checkbox; scale slider 1–16 (default 4, warning ≥ 6) · (3) output folder (defaults to the image's folder) ·
  (4) "Upscayl from W×H to W'×H'" + big start button.
- **Right pane:** empty state "Select or drag and drop a PNG, JPG, JPEG or WEBP image", Ctrl+V paste; preview after load;
  full-pane blurred progress overlay with spinning logo, percent text, "Doing the Upscayl magic...", STOP button.
- **Result comparison — the feature the owner wants:** `react-compare-slider` — handle at **50 %**, the WHOLE surface is
  draggable (not only the handle), labels "Original" bottom-left / "Upscayled" bottom-right; plus a Lens view (hover, fixed
  4× magnifier). Hover-zoom setting exists but its CSS class was never compiled (dead feature).
- **Settings (persisted):** theme (30 daisyUI themes, default dark slate "upscayl") · language (11, default `en`, system
  locale never detected) · save as PNG/JPG/WEBP (default PNG) · scale · custom output width · compression 0–100 · remember
  output folder · overwrite · notifications · auto-update · telemetry · GPU id · tile size (0 = auto) · custom models
  folder · TTA (8× slower) · reset · system info. **No EXIF/metadata option, no video.**
- Input formats: png, jpg, jpeg, jfif, webp only. Output: png, jpg, webp.
- i18n: 11 locales (ar, en, ru, ja, zh, es, fr, de, vi, id, pt), 168 keys each, native-name picker.

## 5. Port verdict for KARTUN

**Keep (behaviour, re-implemented):** pick → model → scale → run flow; model cards with before/after previews; result
before/after slider at 50 %, draggable anywhere; percent progress + Stop; format + quality; output naming pattern; skip/
overwrite policy; custom models (later).
**Drop/replace:** GPU id, multi-GPU, auto-update, news fetch, cloud waitlist, telemetry (KARTUN has none by GOAL);
hover lens → pinch-zoom + pan synced between both images; drag-drop/Ctrl+V → Photo Picker, share intent, clipboard URL;
Double Upscayl (16×), scales > 4×, TTA → not in MVP (memory/heat traps on a phone).
**Pitfalls to design out:** no output-size guard (12 MP × 4² = 192 MP ≈ 768 MB RGBA) → cap or stream tiles to disk;
progress parsed from stderr chunk lengths → report tile-by-tile progress from the in-process engine; RRDB-23 models are
heavy on mobile → upscayl-lite / SRVGGNet-class as the realistic fast default.
**Bugs seen in Upscayl code (do not repeat):** wrong batch counter; Double Upscayl naming/pass counter; path-length
check continues after error; tile-size field clears the wrong key; language picker may write the theme; telemetry
initialised regardless of the opt-out switch (runtime unverified).
