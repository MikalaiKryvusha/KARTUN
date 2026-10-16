# Test run report — release smoke of KARTUN 1.0 candidate on the Redmi Note 12 Pro+ (Android 14)

**Created:** 2026-10-16 19:56 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** release `com.kriniks.kartun`
`0.1 (1)`, commit `da96325` (engine on the fork MikalaiKryvusha/ncnn `20260526-kartun.1`, minSdk 34), APK 19:26

## 1. Work

`plans/08_release_1_0.md` step 4 (the release on another device, the owner ≈ 17:48: «смоук тестирования на других девайсах»;
≈ 19:1x: «тестовый девайс тебе для релизного смоук тестирования: 192.168.1.76:41705»). Basis: the main path of `GOAL.md`
§3 (pick → Enhance → progress → before/after → save PNG/JPEG/WebP → share), «Вставить ссылку», the model catalogue,
«О приложении» naming the fork (owner ≈ 19:1x), and Android 14 itself (owner ≈ 19:2x «снижаем сдк до андроид 14»).

## 2. Contour

Xiaomi Redmi Note 12 Pro+ 5G (`22101316UG`), Android 14 (API 34), Dimensity 1080, Mali-G68 MC4, 7.4 GB RAM, home Wi-Fi
`192.168.1.76:41705`, system language English. UI agent (`C:\Android\private\ui-agent`) by locators after the owner enabled
«USB debugging (Security settings)» with a SIM inserted for a minute; before that MIUI refused every injected tap
(`INJECT_EVENTS`). Test photo `photo1.jpg` 960 × 1280 pushed as `Pictures/KARTUN_test/kartun_smoke_noexif.jpg` (EXIF
stripped so the picker sorts it first; picked by its description «Photo taken on Oct 16, 2026 7:45 PM»).

## 3. Runs

| # | Moment | Command / action | Outcome |
|---|---|---|---|
| 1 | 2026-10-16 19:24 +03:00 | `adb install -r app-release.apk` (minSdk 34 build, before the service fix) | ✅ after the owner enabled «Install via USB» |
| 2 | 2026-10-16 19:27 +03:00 | `node tools/kartun-cli.mjs upscale engine/third_party/testdata/test256.png` (fixed build) | ✅ 256 → 1024, 21 s, GPU; picture correct by eye |
| 3 | 2026-10-16 19:45–19:51 +03:00 | UI: «Choose a photo» → picker → «Done» → «Enhance» → «Allow» (node from `tree`) | ✅ 960 × 1280 → 3840 × 5120, **4 min 22 s**, GPU · Vulkan |
| 4 | 2026-10-16 19:51–19:52 +03:00 | «Save» → PNG, JPEG, WebP (dialog «Save» by its node) | ✅ three files in `Pictures/KARTUN`, 3840 × 5120 |
| 5 | 2026-10-16 19:52 +03:00 | «Share» → system sheet → «Cancel» | ✅ sheet with `…_4x.png`, nothing sent |
| 6 | 2026-10-16 19:53 +03:00 | «Paste a photo link» → `type <raw.githubusercontent…/4x-NMKD-Siax-fp16.webp>` → «Download» | ✅ opened on «Enhance», 1024 × 512 → 4096 × 2048 |
| 7 | 2026-10-16 19:54 +03:00 | «All models» → «Download» on 4x UltraSharp V2 Lite (node `812 1411`) | ✅ card shows «Delete»; `kartun-cli info`: installed |
| 8 | 2026-10-16 19:54 +03:00 | Settings → About; `cmd locale set-app-locales … --locales ru` | ✅ engine line and the fork row in EN and RU |

## 4. Checks

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleRelease :app:lintRelease` green (19:26), lint 0 errors;
`python tools/make-translations.py --check` green; `node tools/build-ncnn.mjs` — fork tag cloned, commit checked, fixed
shader found in the build (stock 20260526: 0 hits of `min(gy4`, the tag: 4); the APK's `libkartun_engine.so` carries it.
Functional run: the release on the Redmi as a user, English and Russian — every step read by the accessibility tree,
key screens by screenshots, saved files by MediaStore queries.

| Case | Status | Observation |
|---|---|---|
| Install on Android 14 (minSdk 34) | pass | `dumpsys package`: minSdk=34 targetSdk=37 |
| Background processing service on API 34 | pass | before the fix lint flagged `FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING` (API 35); with `dataSync` below 35 the agent door and the UI run both started it, the notification «KARTUN» appeared |
| Engine on the fork, GPU | pass | «GPU · Vulkan», Mali-G68 MC4; results correct by eye (beach, beehives) |
| Pick → Enhance → result | pass | sizes right, before/after handle in the centre |
| Save PNG / JPEG / WebP | pass | 40.5 MB / 7.0 MB (promised ≈ 7.0) / 6.4 MB (promised ≈ 6.5), all 3840 × 5120 |
| Share | pass | system sheet with the PNG |
| Paste a link | pass | download from GitHub, opens on «Enhance» |
| Catalogue download | pass | UltraSharp V2 Lite, 15 MB |
| About names the fork | pass | EN «custom modified and improved fork», RU «кастомный модифицированный и улучшенный форк» |
| Time estimate on a new device | **fail (soft)** | promised «≈ 1 minute 20 seconds», took 4 min 22 s; after the run it learned only to «≈ 2 minutes 51 seconds» → `ideas/04` |
| Arabic, landscape, cancel, huge photo | skipped | not walked on this device |

## 5. Found

- The time estimate on a new, slower device is 3.3× too optimistic and stays ~1.5× optimistic after one run (`ideas/04`, the
  owner's idea of a GPU test at first launch, ≈ 19:52).
- Result screen at phone width: the size line wraps as «960 × 1280 → / 3840 × 5120» (cosmetic).
- The photo picker on this Android 14 is multi-select: it waits for «Done» instead of returning on the first tap.
- The file name from the gallery is a number («1000009282.jpg») — known (STATUS, noticed item).

## 6. Traces

Screenshots (session scratchpad `redmi_smoke/`): `01_enhance`, `02_result`, `03_about_en`, `04_about_ru`; MediaStore query
output above; test files removed from the device after the run (no `%kartun%` rows left).

## 7. Verdict

partial — the main path, saving in three formats, sharing, the link, the catalogue and About pass on Android 14; the time
estimate on a new device is wrong (soft fail, idea 04); Arabic, landscape, cancel and huge photos were not walked here.
