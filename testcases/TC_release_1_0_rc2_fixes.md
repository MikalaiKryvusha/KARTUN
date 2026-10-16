# Test cases — RC2 fix verification on a device (one run per device)

> **Created:** 2026-10-16 · **Parent:** `testcases/TC_release_1_0_rc_smoke.md` (RC smoke findings) · the owner ≈ 21:1x «на все девайсы переустанови, проверяй твои фиксы на всех девайсах»
> **Status:** in use 2026-10-16 (4 devices)
> **Outbound:** —

Build under test: `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc2.apk` — release `com.kriniks.kartun` `0.1 (268)`, commit
`64b66a4`. Install it over the old RC (`adb install -r`), never rebuild. Same rules of the run as the RC smoke: the UI agent
by locators (`tree` node coordinates for buttons whose word repeats in a title), WebP screenshots you READ yourself, touch
nothing of the owner's, delete your test files, reset locale and rotation at the end.

| ID | What was fixed | Check | Expected |
|---|---|---|---|
| F0 | version | About | «0.1 (268)» |
| F1 | first-launch bench | `pm clear`; launch; time `Start proc` → `{"bench":true` | dialog over a darker screen, closes itself; note seconds, `gpu_factor`, `esrgan_correction` and the `bench_probe` line |
| F2 | name of a picked photo | pick the EXIF-free test photo in the picker (by «Photo taken on <push time>») | Enhance shows `IMG_<yyyyMMdd_HHmmss>.jpg`, not a number; a saved result is named `IMG_…_kartun_…` |
| F3 | size line | Enhance (scale row and the collapsed photo row) and the result panel, in en and de | «960 × 1280 → 3840 × 5120» on one line, or three centred lines «960 × 1280» / pink ↓ / «3840 × 5120» — never a break inside a number |
| F4 | estimate | Enhance with NomosWebPhoto 4×: read the button's estimate, run, read the real time | ratio real / estimate in 0.67–1.5; also note the Siax card's estimate (no need to run it) |
| F5 | processing panel | screenshots during the run, en + one long language (de or ru) | title whole (up to 3 lines, no «…»), «Cancel» on the GPU chip's line under the title, tiles 2 × 2 on a phone (4 in a row on the Titan portrait) with whole values («5 min 40 s», «≈ 1 min 20 s») |
| F6 | Enhance button | Enhance screen in en, de, ru | label on one line, words whole, icon not pressed to the edge |
| F7 | share | result → Share | the system sheet shows the file name and/or a thumbnail; back out, nothing sent |
| F8 | Arabic | `--locales ar`: Enhance, result, About | sizes read «3840 × 5120» (not «5120 × 3840»), file name «IMG_….jpg» whole, About «16 (API 36)» whole; on the result the tag over the ORIGINAL (left of the divider) is «قبل» (before) |
| F9 | models list | Enhance → All models → scroll down a little | a visible gap between the filter chips and the first card |
| F10 | home | landscape home (`user_rotation 1`) and French home (`--locales fr`) | landscape: «Photo → Share → KARTUN» chips readable; French: «Choisissez une photo» centred |
| F11 | German settings | `--locales de` → Settings | «GPU-Beschleunigung» whole or hyphenated at a syllable («Beschleu-nigung»), never «Beschleuni / gung» without a hyphen |
| F12 | crash journal | `logcat -d -b crash` for com.kriniks.kartun | empty |
