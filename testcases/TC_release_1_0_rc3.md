# Test cases — RC3 check on a device (one run per device)

> **Created:** 2026-10-16 · **Parent:** `testcases/TC_release_1_0_rc2_fixes.md` and the four RC2 reports · the owner ≈ 21:5x «сколько багов осталось починить и протестировать для релиза 1.0?»
> **Status:** in use 2026-10-16 (4 devices)
> **Outbound:** —

Build under test: `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc3.apk` — release `com.kriniks.kartun` `0.1 (271)`, commit
`43cf18c`. Install with `adb install -r`, never rebuild. Same rules as before: the UI agent by locators (`tree` node coordinates
for a button whose word repeats in a title), WebP screenshots you READ yourself at full resolution where text edges matter,
touch nothing of the owner's, delete your test files, reset locale and rotation.

| ID | What | Check | Expected |
|---|---|---|---|
| G0 | version | About | «0.1 (271)» |
| G1 | first-launch bench | `pm clear`, launch, time `Start proc` → `{"bench":true` | dialog over a darker screen, closes itself; note seconds and the `bench_probe` line; spinner squares do not cover letters |
| G2 | collapsed photo row on «Enhance» | pick the EXIF-free test photo; read the collapsed row in en, de, ar | the file name `IMG_…jpg` whole (narrower font, may wrap to 2–3 lines); the size change fully visible — one line, or three lines «960 × 1280» / pink ↓ / «3840 × 5120» with the row taller; in Arabic the numbers read «960 × 1280» and «3840 × 5120» (not reversed) |
| G3 | expanded photo card | drag the handle down to expand the photo | the «960 × 1280 · 1,2 Мп» chip and «Другое фото» do not overlap |
| G4 | favourites | «Enhance» cards and «All models» | NomosWebPhoto, Siax, UltraSharp V2, V2 Lite show a gold chip «★ ВЫБОР KARTUN» (en: «KARTUN'S CHOICE») under the name; the star sits level with the capitals; every model has its (i), and tapping one (i) opens a text |
| G5 | catalogue line | «Enhance» → under «All models» | «Каталог: 14 · Скачано: N» (en «Catalogue: 14 · Downloaded: N») |
| G6 | licence badge | «All models» | «Не для коммерческого использования» violet text on a dark violet chip |
| G7 | cancel feedback | start «Enhance», at ≥ 10 % press «Cancel» | at once the title reads «Отменяем…» / «Cancelling…» and «Cancel» greys out; then back to «Enhance»; a second «Enhance» works |
| G8 | result screen | finish one run; read the result panel in en and ar | no pink divider line through the bottom panel; in Arabic a visible gap between the size and the time; on «Enhance» in Arabic the scale slider's fill and its ×1…×16 marks agree (both left to right) |
| G9 | share label | result → «Share» | while it prepares, «Save» keeps the label «Save» / «Сохранить»; the sheet opens; back out, nothing sent |
| G10 | PNG size | Save → PNG: read «File ≈ N MB», save, query MediaStore `_size` | real / promised within 0.97–1.03 |
| G11 | Arabic About | `--locales ar` → About | the version reads «0.1 (271)» whole |
| G12 | crash journal | `logcat -d -b crash` for com.kriniks.kartun | empty |
