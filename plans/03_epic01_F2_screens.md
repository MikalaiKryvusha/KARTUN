# Plan 03 — Epic 01, phase 2: the main path on screens — pick a photo → set up → upscale with progress → compare before/after

> **Создан:** 2026-10-14 (agent)
> **Родитель:** `plans/01_EPIC_mvp.md` §3 row «2 — основной сценарий», gate: criteria 2.2, 2.3, 2.5, 2.7 walked on the Titan by touch, no adb commands; `MASTER_PLAN.md` phase 2
> **Статус:** 🔧 started 2026-10-14 18:27 +03:00 — slice 1 (steps 1–6) in work
> **Вовне:** each screen's Titan screenshot next to its mockup → to the owner (taste class: his eye judges the look)

## Goal vector

Achieve: on the Titan a user opens KARTUN, picks a photo with the system photo picker, sees it with its resolution,
picks a model and a scale, taps «Улучшить», sees progress, and compares before/after with a slider grabbed anywhere on
the picture — all by touch, in Krinik's style, as on the accepted mockups (`design/kartun-mockups.html`, variant A v2;
`[OWNER]` interview #002: «в остальном ОК, принято, можем делать!»).

## Acceptance criteria (Situation · Action · Result · Check)

1. Home → picker. Situation: KARTUN is open on its home screen. Action: the user taps «Выберите фото». Result: the system
   photo picker opens; no permission dialog. Check: `aapt2 dump permissions` of the APK lists no `READ_MEDIA_IMAGES` /
   `READ_EXTERNAL_STORAGE`; a Titan screenshot shows the picker.
2. Setup screen. Situation: the user picked the 960×1280 photo. Action: the setup screen opens. Result: the preview with
   «960 × 1280 · 1,2 Мп», two model cards (fast selected), the scale slider at 2×, the line «960 × 1280 → 1920 × 2560».
   Check: Titan screenshot + uiautomator dump contains those strings.
3. Upscale. Situation: setup screen, fast model, 4×. Action: tap «Улучшить». Result: a progress screen, then the result
   screen with «3840 × 5120» and the time. Check: uiautomator dump of the result screen shows `3840 × 5120`; logcat has no
   `FATAL`.
4. Compare. Situation: result screen. Action: the user drags from any point of the picture. Result: the before/after line
   follows the finger. Check: two screenshots after `input swipe` at two x positions show the line at those x (±20 px).
5. Look. The four screens stand next to their mockups on one sheet for the owner; his verdict is recorded (taste class).
6. Strings: every visible string in `res/values/strings.xml` (en) and `res/values-ru/strings.xml`; no literal in Kotlin.
   Check: `grep -rn 'Text("' app/src/main/kotlin` prints nothing.

## Steps

Slice 1 (this session):
1. [ ] Theme: `ui/theme` — colours of the mockups (`--bg #0D0D0D`, `--card #1C1C1E`, `--pink #FF1A8C` …), shapes
   (20–32 dp), type: Unbounded (headings, logo) + Manrope (text) as OFL font files in `res/font` (`[OWNER]` interview #002, Q2 = A).
2. [ ] Models in the APK: `tools/stage-models.mjs` copies `realesr-general-x4v3` (own conversion) and `realesrgan-x4plus`
   into `app/src/main/assets/models/` (gitignored, like `engine/third_party/`); first launch copies them to `filesDir`
   (the engine loads from a path).
3. [ ] Home screen A: logo, «Выберите фото» card → Photo Picker (`PickVisualMedia`), «Вставить ссылку» (disabled until
   phase 4), the share hint, «Обработка на устройстве, без сети».
4. [ ] Setup screen: pinned collapsing preview, model cards (fast · maximum), scale slider 1×/2×/4×/8×/16× with the result
   size, (i) buttons with a short text sheet, «Улучшить» button. Scales 1× and 2× = one 4× pass + downscale; 8×/16× stay
   disabled with «скоро» until phase 1 brings the megapixel limit and the second pass.
5. [ ] Upscale in a ViewModel on the single engine thread; progress screen (indeterminate until phase 1 adds tile progress).
6. [ ] Result screen: before/after compare grabbed anywhere, pinch zoom + pan, size and time line; «Сохранить» /
   «Поделиться» shown disabled until phase 3.
7. [ ] Titan run by touch per criteria 1–4 → run report; sheet «mockup | build» for the owner.

Slice 2 (next): foreground service `mediaProcessing` with a notification; the probe activity moves to a debug-only entry.

## Risks

- Holding a 4× bitmap of a 12 Mp photo in Java memory (192 Mp, 768 MB) → slice 1 limits the input to what the heap
  takes (`largeHeap`) and says so; phase 1 brings tiles-to-disk.
- Fonts with Cyrillic: Unbounded and Manrope both carry Cyrillic (Google Fonts) — verify on a screenshot.
- Compose BOM 2026.09 API drift vs my recall → read the compiler's errors, do not guess.
