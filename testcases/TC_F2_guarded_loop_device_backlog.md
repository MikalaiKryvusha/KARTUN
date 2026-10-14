# Test cases — what the guarded loop of 2026-10-14 19:12–19:53 built without the device

**Created:** 2026-10-14 · **Under test:** landscape layouts, the processing service, 8×/16×, the GPU fallback ladder, save and
share, the debug-only probe · **Version/build:** debug APK from commit `fcbb788` or later
**Test basis:** the owner in chat 2026-10-14 — «лендскейп нужно поддерживать тоже» (≈ 19:12), «работай автономно, без
тестирования на клиенте, 40 минут» / «потом потестируем» (≈ 19:12); `plans/03_epic01_F2_screens.md` steps 10–13;
`plans/04_epic01_F1_reliable_engine.md` steps 1–2; `plans/01_EPIC_mvp.md` §2.8 (save), §2.9 (share), §4.4 (service), §4.5 (memory).

## 1. Goal vector

Maintain: everything built in the guarded loop works on the Titan by the user's path — before any of it is called done.
Every item below is `[NOT-TESTED]` in the code until its case passes here.

## 2. Requirements under test — the basis, made testable

| # | Requirement (EARS sentence) | Fit criterion (Scale · Meter · Target) |
|---|---|---|
| R1 | WHILE the Titan is in landscape, each of the four screens shall show all its controls without overlap | screenshot per screen · eye · 4 of 4 readable |
| R2 | WHILE a run goes and the app is left, the system shall keep the run and show a notification with the percent | notification shade screenshot · eye · the percent grows; the run reaches the result |
| R3 | WHEN «Отменить» in the notification is tapped, the run shall stop between tiles | uiautomator dump · «Улучшение» back within one tile |
| R4 | WHEN 8× is chosen for the 960×1280 photo, the result shall be 7680×10240 | result screen text · `7 680 × 10 240` |
| R5 | IF 16× is chosen for the 960×1280 photo, THEN «Улучшить» shall be off and the notice shall name 8× | screenshot · button grey, notice «…до 8×» |
| R6 | IF the GPU pass fails, THEN the run shall retry with 100 px tiles, then on the CPU, then show «Нейросеть не смогла обработать фото.» | `adb logcat -s KARTUN` lines · the three steps in order |
| R7 | WHEN «Сохранить» is confirmed, a NEW file shall appear in Pictures/KARTUN and the original shall stay | `adb shell ls /sdcard/Pictures/KARTUN` + original's mtime unchanged |
| R8 | WHEN «Поделиться» is tapped, the system share sheet shall open with the PNG | screenshot of the sheet; sending to Telegram «Saved messages» delivers a 4× image |
| R9 | WHERE the build is release, an adb launch with the `in` extra shall NOT run the probe | logcat without `KARTUN_PROBE` for a release/stage-off build |

## 3. Coverage matrix — dimensions and holes

| Dimension | Values covered | Explicitly NOT covered (risk named) |
|---|---|---|
| orientation | portrait (regression of the refactor) · landscape | rotation DURING a run — risk medium, add if time allows |
| scale | 1× · 2× · 4× · 8× · 16× refused | 16× of a photo ≤ 0.78 MP that fits — needs a small test photo |
| format | PNG · JPEG q92 · WebP q92 | WebP over 16 383 px (needs 8× of a ≥ 2048 px side photo) |
| failure | forced GPU failure | out-of-memory kill — research 05 is the cure, not a test here |
| app state | foreground · left with Home | process killed by the system mid-run |

## 4. Cases

Machinery: `adb shell input` taps by coordinates read from `uiautomator dump`, `adb exec-out screencap`, `adb logcat`;
test photo copied to `Pictures/KARTUN_test/` and removed after (R1 of HOUSE_RULES).

| # | Case (steps → expected) | Technique | Status + evidence |
|---|---|---|---|
| C1 | rotate the Titan to landscape → home: pick card left, link button + share hint right | state | [NOT-TESTED] |
| C2 | landscape, pick the photo → «Улучшение»: photo left full height, cards + slider right, «Улучшить» under them | state | [NOT-TESTED] |
| C3 | landscape, «Улучшить» → processing: photo with tiles left, ring panel right | state | [NOT-TESTED] |
| C4 | landscape result → divider follows the finger, panel ≤ 600 dp at the bottom | state | [NOT-TESTED] |
| C5 | portrait, the full path of run 9 (`testcases/reports/2026-10-14_F2_screens_slice1.md`) → unchanged after the refactor | regression | [NOT-TESTED] |
| C6 | x4plus «Улучшить» → Home key → pull the shade → notification «Нейросеть улучшает фото», percent grows → back to app → result | state | [NOT-TESTED] |
| C7 | x4plus run → shade → «Отменить» → app shows «Улучшение», no error notice | state | [NOT-TESTED] |
| C8 | 8× on the 960×1280 photo → ring runs once 0→100 % over both passes → result `960 × 1280 → 7 680 × 10 240`; button showed «2 прохода · дольше · большой файл» | boundary | [NOT-TESTED] |
| C9 | 16× on the same photo → notice «Результат больше 200 Мп — для этого фото подойдёт масштаб до 8×», button off | boundary | [NOT-TESTED] |
| C10 | force a GPU failure (debug: a model whose .param is valid but too large a tile, or `adb shell setprop debug.vulkan.layers` to a broken layer) → logcat `KARTUN` shows retry 100 → CPU; result or the honest error | error guess | [NOT-TESTED] — the forcing method itself is to be found |
| C11 | result → «Сохранить» → PNG → `ls /sdcard/Pictures/KARTUN` shows `…_kartun_realesr-general-x4v3_2x.png`; the gallery shows it | partition | [NOT-TESTED] |
| C12 | the same with JPEG q92 and WebP q92 → two more files, sizes smaller than PNG | partition | [NOT-TESTED] |
| C13 | result → «Поделиться» → system sheet → Telegram «Избранное» → the image arrives 4× sized | use case | [NOT-TESTED] |
| C15 | Settings → System → Languages: Arabic → KARTUN mirrors (RTL), arrows point left, the text falls back to the system font; then Hindi and Chinese → readable, nothing cut | state | [NOT-TESTED] |
| C16 | Settings → Apps → Debug KARTUN → Language → the list shows the 10 languages; picking «Español» switches KARTUN without changing the system language | state | [NOT-TESTED] |
| C17 | copy `https://upload.wikimedia.org/wikipedia/commons/3/3f/JPEG_example_flower.jpg` in Chrome → KARTUN home → «Вставить ссылку на фото» → «Скачиваем фото…» → «Улучшение» with the flower; then with plain text in the clipboard → «В буфере обмена нет ссылки…»; then airplane mode → upscale still works | use case | [NOT-TESTED] |
| C14 | debug build: `node tools/titan-probe.mjs --in test256.png --model realesr-general-x4v3 --gpu true --tile 200` → JSON line as before | regression | [NOT-TESTED] |

## 5. Control cases — MUST

| # | Control → expected: the behaviour is absent | Status + evidence |
|---|---|---|
| K1 | portrait → the landscape layouts do NOT appear (C5 is the same screen as before the loop) | [NOT-TESTED] |
| K2 | a 2× run → no «2 прохода» notice and the ring counts the photo's own 35 tiles | [NOT-TESTED] |
| K3 | release build (`assembleRelease`, unsigned is enough for adb install on the Titan?) → probe intent does nothing | [NOT-TESTED] — signing key in `.env` (R2) may be needed |

## 6. Verdict

Not run yet. When run, the result goes to `testcases/reports/<date>_F2_guarded_loop_device_run.md`, and each case flips
its marker in the code it names.
