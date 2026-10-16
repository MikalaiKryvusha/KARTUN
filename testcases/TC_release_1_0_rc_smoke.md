# Test cases — release 1.0 RC smoke on a device (one run per device)

> **Created:** 2026-10-16 · **Parent:** `plans/08_release_1_0.md` step 4; the owner ≈ 20:3x «займись смоук тестирование RC на этих девайсах» · «раздай тест кейсы субагентам»
> **Status:** in use 2026-10-16 (4 devices)
> **Outbound:** —

Build under test: `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc-2028.apk` (release `com.kriniks.kartun`, engine on the fork
`MikalaiKryvusha/ncnn 20260526-kartun.1`, first-launch bench with REF 1130 / 966 ms). Do NOT rebuild; install this file.

## Rules of the run (HOUSE_RULES.md R1, R7)

- Drive the UI ONLY by locators with the UI agent `C:\Android\private\ui-agent\ui.py` (`wait`, `tapText`, `labels`, `tree`,
  `back`, `type`). When `tapText` would hit a title with the same word (system «Allow» dialog, «Save» dialog), take the
  button's node from `tree` and `tap x y` THAT node. Never tap by guessed coordinates. Screenshots are evidence, not navigation.
- `wait <text>` may answer «found» for a text not on screen — confirm every step with `labels`.
- Touch nothing of the owner's on the device: only the package `com.kriniks.kartun`, the UI agent package
  `evegf.app.macrobenchmark`, and the test files you push (`Pictures/KARTUN_test/`). Delete your test files at the end.
- Network: allowed only for case C7 (our GitHub raw file) and C8 (our model release).

## Cases

| ID | Case | Steps | Expected |
|---|---|---|---|
| C1 | Install | `adb install -r <apk>` | `Success`; `dumpsys package com.kriniks.kartun` minSdk=34 |
| C2 | First-launch bench | `pm clear com.kriniks.kartun`; `logcat -c`; launch; screenshot at +2 s | dimmed home screen + dialog «Measuring the device's GPU and CPU performance…» (or the device language) + live spinner + «Cancel»; the dialog closes by itself; logcat line `{"bench":true,…}`; note the seconds from `Start proc` to that line |
| C3 | Bench «Cancel» | `pm clear`; launch; within 1 s press the dialog's cancel button | dialog closes at once; no crash (`logcat -b crash` clean); relaunch → no dialog again |
| C4 | Main path + estimate | push the test photo (below); «Choose a photo» → pick it by its description in the picker («Photo taken on …» with the push time) → «Done» if the picker asks → Enhance screen: read size line and the estimate on the button → Enhance → notification permission: «Allow» (node from `tree`) → wait for the result | Enhance shows 960 × 1280 → 3840 × 5120; result screen shows the same size and the real time; record estimate vs real (ratio) |
| C5 | Save PNG / JPEG / WebP | on the result: Save → pick format → the dialog's Save (node) — three times | MediaStore query: three files in `Pictures/KARTUN/`, 3840 × 5120 each; sizes near the dialog's «File ≈ … MB» |
| C6 | Share | Share → system sheet appears with the PNG → back out without sending | sheet shows the file name; nothing sent |
| C7 | Paste a link | home → «Paste a photo link» → `type https://raw.githubusercontent.com/MikalaiKryvusha/KARTUN/main/docs/model-previews/4x-NMKD-Siax-fp16.webp` → Download | Enhance screen with 1024 × 512 → 4096 × 2048 |
| C8 | Catalogue download | Enhance → «All models» → «Download» on «4x UltraSharp V2 Lite» (15 MB; node from `tree`) | the card shows «Delete»; `kartun-cli info` lists it installed |
| C9 | About | Settings → About | «Engine: MikalaiKryvusha/ncnn 20260526-kartun.1»; row «MikalaiKryvusha/ncnn — custom modified and improved fork»; GPU name; Android version |
| C10 | Languages | `adb shell cmd locale set-app-locales com.kriniks.kartun --locales ru` then `ar`; screenshot home and About each | Russian text; Arabic mirrored (back arrow on the right, chevrons pointing left), nothing cut |
| C11 | Landscape | `settings put system accelerometer_rotation 0; settings put system user_rotation 1`; screenshot home and Enhance; restore `user_rotation 0`, `accelerometer_rotation` to its old value | layouts fit, nothing cut |
| C12 | Cancel processing | Enhance a photo; when the ring shows ≥ 10 %, press «Cancel» | returns to Enhance; no crash; engine still works on a second Enhance |
| C13 | Crash journal | `adb logcat -d -b crash` filtered by `com.kriniks.kartun` | empty |

Test photo: `D:\work\ai_sandbox\KARTUN\engine\third_party\testdata\photo1.jpg` stripped of EXIF
(`magick photo1.jpg -strip kartun_smoke.jpg`) and pushed to `/sdcard/Pictures/KARTUN_test/kartun_smoke.jpg`, then
`am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/KARTUN_test/kartun_smoke.jpg`
— with no EXIF its «Photo taken on» is the push time, which identifies it in the picker.
