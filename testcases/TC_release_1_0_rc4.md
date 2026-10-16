# Test cases — RC4: the final round, every language, both orientations (one run per device)

> **Created:** 2026-10-16 · **Parent:** `TC_release_1_0_rc3.md` and its four reports · the owner ≈ 22:2x «проверить на разных локалях на разных девайсах в разных ориентациях»
> **Status:** in use 2026-10-16 (4 devices)
> **Outbound:** —

Build under test: `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc4.apk` — release `com.kriniks.kartun` `0.1 (275)`, commit
`3c8ca15`. Install with `adb install -r`, never rebuild. Rules as before: the UI agent by locators (`tree` node coordinates for
a button whose word repeats in a title; `adb shell input swipe` only for a drag or a scroll that has no locator — say so in the
report), WebP screenshots you READ yourself (full-resolution crops where text edges matter), touch nothing of the owner's,
delete your test files, reset locale (`--locales ""`) and rotation at the end.

Languages: en, ru, es, pt, fr, de, zh, hi, bn, ar (`adb shell cmd locale set-app-locales com.kriniks.kartun --locales <l>`,
then `am force-stop` + `am start -n com.kriniks.kartun/.MainActivity`). Landscape: `settings put system accelerometer_rotation 0`
+ `settings put system user_rotation 1`; portrait: `user_rotation 0`; restore the old values at the end.

| ID | What | Check | Expected |
|---|---|---|---|
| H0 | version | About | «0.1 (275)» |
| H1 | first-launch bench | `pm clear`, launch, time `Start proc` → `{"bench":true`; screenshots at +2 s and +3 s | darker screen, dialog closes itself; the title's letters are never covered by the flying squares; note seconds and the two `bench_probe` lines' numbers |
| H2 | battery saver (Titan only — the owner allowed the agent to switch it there) | `adb shell cmd power set-mode 1`, `pm clear`, launch | a dialog «Включён режим экономии батареи» with «Выключить экономию», «Замерить так», «Отмена»; then `cmd power set-mode 0` — the dialog goes and the check starts by itself; restore the saver to how it was |
| H3 | language × screen matrix, portrait | for each of the 10 languages: home, «Enhance» with the test photo (collapsed card), «All models» (top and one scroll), Settings, About (top and the Device group with «Benchmark») | right language; nothing cut, overlapping or running out of a card/button; no English left except names, «GPU», «CPU», «KARTUN», numbers; Arabic mirrored with numbers whole (LTR) |
| H4 | landscape | en, ru, ar: home, «Enhance», «All models», About; plus processing and result once (en) | everything fits; the «Photo → Share → KARTUN» chips readable; no clipping |
| H5 | collapsed photo card | en, de, ru | thumbnail; the file name whole on its own line(s), wrapping only after «_», never inside «.jpg»; under it «960 × 1280 → 3840 × 5120» (or the three lines with a pink ↓) with the small «Другое фото» on its right |
| H6 | expanded photo card | drag the handle down | the chip «960 × 1280 · 1,2 Мп» never breaks inside a value; no overlap with «Другое фото» |
| H7 | About «Benchmark» | Device group | ONE row «Benchmark» / «Бенчмарк» with «GPU x MP/s · CPU y MP/s» |
| H8 | processing | one 4× NomosWebPhoto run (en, then ar for the panel only) | title whole, Cancel on the GPU chip line, tiles whole; real / estimate within 0.67–1.5 (note it); Siax card's estimate noted |
| H9 | result | after the run | the screen dims to about half during «Rendering…» (Save → JPEG) and clears after it — screenshot during and after; no divider through the panel |
| H10 | models list values | «All models» in ru | sizes like «15 МБ» never split between number and unit |
| H11 | crash journal | `logcat -d -b crash` | empty |
