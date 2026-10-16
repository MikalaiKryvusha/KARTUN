# Test run report — «Открыть с помощью → KARTUN» на Титане (plans/08 step 10)

**Created:** 2026-10-16 16:22 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** debug, working tree after
`1ca02cb` + the VIEW filter (APK 16:15:55 for runs 1–2, 16:19:44 with `application/octet-stream` for runs 3–4)

## 1. Work

A file manager opens a picture in KARTUN through «Открыть с помощью» (`ACTION_VIEW`), and the formats of the engine's second
decoder (TGA, PSD, TIFF) walk the screens to a result. Basis: `plans/08` step 10, STATUS «Где продолжить» 1б (the screen path of
step 3 was not walked), the owner's word 2026-10-16 ≈ 16:10 — the home screen stays as approved, no new button.

## 2. Contour

HEADWOLF Titan 1 (`100.99.111.48:5555`), Android 16; the system Files app (`com.google.android.documentsui`), folder
«Скачанные»; debug build `com.kriniks.kartun.debug`; model Real-ESRGAN General v3, 4×. Test files made by ImageMagick 7 from
`engine/third_party/testdata/test256.png` (256 × 256): `kartun_open_tga.tga`, `kartun_open_psd.psd`, `kartun_open_tiff.tiff`, and
the control `kartun_open_broken.tga` (22 bytes of text). The media store typed them: TGA `application/octet-stream`, PSD
`image/x-photoshop`, TIFF `image/tiff`. UI driven only by the UI agent by locators (`HOUSE_RULES.md` R7).

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-16 16:17 +03:00 | `ui.py "tapText kartun_open_tga.tga" "wait KARTUN 4000" labels` | the chooser lists ZArchiver, Snapseed…, no KARTUN |
| 2 | 2026-10-16 16:18 +03:00 | `ui.py "tapText kartun_open_psd.psd" … "tapText Debug KARTUN" … "tapText Улучшить" "wait Сохранить 30000"` | result 1024 × 1024 |
| 3 | 2026-10-16 16:19 +03:00 | `python openwith.py kartun_open_tiff.tiff` | exit 0, result 1024 × 1024 |
| 4 | 2026-10-16 16:21 +03:00 | `python openwith.py kartun_open_tga.tga` (APK with octet-stream) | exit 0, result 1024 × 1024 |
| 5 | 2026-10-16 16:21 +03:00 | `python openwith.py kartun_open_broken.tga` | exit 1 by design: no «Улучшение», the home screen shows the error |

| 6 | 2026-10-16 16:25 +03:00 | `python openwith.py kartun_open_heic.heic` | exit 0, result 5760 × 3840 |
| 7 | 2026-10-16 16:23 +03:00 | `adb shell am start -W -S -a android.intent.action.VIEW -t image/jpeg -d content://media/external/images/media/<id> --grant-read-uri-permission -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity` (ids 63298, 63297) | both reach «Улучшение» |
| 8 | 2026-10-16 16:28–16:30 +03:00 | `python openwith.py kartun_open_broken.tga`, `… kartun_open_cut.jpg`, the run-7 command without `--grant-read-uri-permission` (build 16:27:55 with the reasons) | lines read below |

The owner's word mid-run (≈ 16:22): «не удалось открыть это фото пишет… часто вижу эту ошибку» → the cause is now written to
the app's journal and the error names one of four reasons (≈ 16:26: «можем в ошибке писать причину понятную?»). Run 7 was
preceded by `logcat -c -b main` — against EXP-0023; the buffer started at 16:18 and held only the agent's own runs.

| 9 | 2026-10-16 16:30–16:52 +03:00 | `curl -sSfL https://raw.githubusercontent.com/python-pillow/Pillow/main/Tests/images/{truncated_image.png,truncated_jpeg.jpg}`; own cuts `head -c $((n/2))` of a PNG, a WebP and `crowd_1440x960.heic`; `python openwith.py <file> --no-run` each (the owner ≈ 16:30: «скачай такой файл из сети, наверняка есть примеры битых файлов») | lines read below; the agent's `wait` answered «found» for text not on screen, so the script reads `labels` |

`openwith.py` (session scratchpad): opens «Скачанные» by `am start -a VIEW -t vnd.android.document/root`, then
`wait/tapText <file>` → `wait Открыть с помощью` → `tapText Debug KARTUN` → `wait Улучшить` → `tapText Улучшить` → `wait Сохранить`.

## 4. Checks

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` green (16:15); `aapt2 dump xmltree` of the 16:19 APK — 2
`application/octet-stream` entries (the build carries the change).
Functional run: the Files app → a file → «Открыть с помощью» → «Debug KARTUN» → «Улучшение» → «Улучшить» → the result screen, on
the Titan as a user; READ: the chooser's labels, the screen's labels (file name, `256 × 256 → 1024 × 1024`, `1 секунда · GPU ·
Vulkan`), the focused window and the process id.

| Case | Status | Observation |
|---|---|---|
| PSD via «Открыть с помощью» | pass | KARTUN in the chooser; «Улучшение» shows `kartun_open_psd.psd · 256 × 256 → 1024 × 1024`; result in 1 s on GPU |
| TIFF via «Открыть с помощью» | pass | the same, `kartun_open_tiff.tiff` |
| TGA via «Открыть с помощью», image types only | fail → fixed | KARTUN absent: the media store types `.tga` as `application/octet-stream` |
| TGA with `application/octet-stream` in the filter | pass | KARTUN in the chooser, result 1024 × 1024 |
| Control: text file named `.tga` | pass | home screen «Не удалось открыть это фото.», process 13775 alive, KARTUN keeps the focus |
| TGA via «Поделиться» from the Files app | skipped | the UI agent has no long press to select a file for sharing; the SEND filter carries the same octet-stream line |
| HEIC via «Открыть с помощью» (run 6) | pass | a real HEIC (nokiatech/heif `autumn_1440x960.heic`) → `1440 × 960 → 5760 × 3840`, 10 s on GPU |
| 50 MP and 84 MP JPEG by VIEW with a grant (run 7) | pass | `chimp50mp.jpg 8640 × 5760` and `zurich84mp.jpg 12 732 × 6619` reach «Улучшение» |
| Reason «не нашёл фото» (run 8) | pass | text file named `.tga` → «В этом файле KARTUN не нашёл фото: формат не поддерживается или файл повреждён.» (screenshot) |
| Reason «нет доступа» (run 8) | pass | VIEW of a media URI without a grant → «Нет доступа к файлу. Если фото хранится в облаке, сначала скачайте его на телефон.» |
| Reason «скачан не до конца» (run 9) | pass | Pillow's `Tests/images/truncated_image.png` and a WebP / PNG cut in half → «Файл фото обрывается — похоже, он скачан не до конца.» (journal: `DecodeException: Input was incomplete.`) |
| A cut JPEG (own 40 %, Pillow's `truncated_jpeg.jpg`) | pass, by Android | opens: Android's JPEG decoder fills the missing part itself (960 × 1280 → result in 8 s; 100 × 100) - no error to show |
| A HEIC cut in half (run 9, then the build of 16:45) | fail → fixed | first «Не удалось открыть это фото.» (`IOException: getPixels failed with error invalid input`); now «…формат не поддерживается или файл повреждён.» |
| Reason «не хватило памяти» | skipped | not reproduced on purpose; JVM test `OpenErrorTest.memoryIsNamed` |
| The cause in the app's own journal | pass | `files/crashes/engine-stderr.log`: `open failed: <uri> type=… DecodeException: …` / `SecurityException: … has no access …` |

## 5. Found

- TGA invisible to «Открыть с помощью» and «Поделиться» from a file manager (Android has no MIME type for it) — fixed in the same
  step: `application/octet-stream` in both filters of `app/src/main/AndroidManifest.xml`; no bug document (found and fixed inside
  the step's own run, severity S3).

## 6. Traces

UI-agent answers quoted above (labels); the test files stay in `/sdcard/Download/kartun_open_*` on the Titan until the release run
of `plans/08` step 4, then deleted (R1). Scripts: the session scratchpad `openwith/`.

## 7. Verdict

pass — PSD, TIFF, TGA and HEIC open from the Files app into KARTUN and reach the result; a non-picture and a file without
access end in an error that names the reason; damaged files from the internet and own cuts show «обрывается» or «повреждён».
Not walked: the share path from a file manager, the «не хватило памяти» line on the device (skipped, reasons above). The owner's frequent «Не удалось открыть» is not reproduced
yet — its next occurrence leaves the cause in `files/crashes/engine-stderr.log`.
