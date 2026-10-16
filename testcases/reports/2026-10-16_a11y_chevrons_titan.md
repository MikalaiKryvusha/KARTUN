# Test run report — TalkBack names, thin chevrons and the Arabic mirror on the Titan

**Created:** 2026-10-16 17:59 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** release `com.kriniks.kartun`,
working tree after `9c9284f` (APKs 17:42, 17:46, 17:55)

## 1. Work

The owner's words: ≈ 17:43 «TalkBack должен работать корректно. Все им должно быть описано»; ≈ 17:46 «шевроны на кнопочках >
нужно больше сделать»; ≈ 17:48 «чтобы они были локаничными, тонкими, аля кнопка Назад <»; ≈ 17:55 «арабский перевод остаётся,
с зеркалирвоанием интерфейса делаем … и тестируем». Changes: RoundIconButton names itself from its icon (Back / Settings);
InfoButton reads «Подробнее: <что>» instead of «i»; the GPU switch is named; the result's before/after pictures, the compare
handle, the summary tab and the photo previews are named; one Chevron (the back arrow turned forward, 20 dp) replaces every «›»
glyph; ic_back is auto-mirrored. New check: `tools/a11y-scan.py` (uiautomator dump → every clickable / focusable / scrollable
node must have a name of its own or a named descendant).

## 2. Contour

HEADWOLF Titan 1 (`100.99.111.48:5555`), Android 16; release build; KARTUN's language switched by
`adb shell cmd locale set-app-locales com.kriniks.kartun --locales ar|ru`. UI agent by locators; it is stopped for each scan
(one UiAutomation client) and needs a quit + 2 s pause before it sees the screen again.

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-16 17:43 +03:00 | `python -I tools/a11y-scan.py --name home` (release 17:42) | exit 0, 0 unnamed |
| 2 | 2026-10-16 17:44 +03:00 | the same on the OLD debug build's home | exit 1: `android.view.View [clickable,focusable] [1402,99][1546,243]` - the gear (the check can fail) |
| 3 | 2026-10-16 17:45 +03:00 | settings scan (release 17:42) | exit 1: 1 unnamed - the GPU switch; the (i) buttons were named «i» |
| 4 | 2026-10-16 17:56–17:57 +03:00 | `cmd locale set-app-locales … --locales ar`; home and settings screenshots + scans (release 17:55) | 0 unnamed on both |
| 5 | 2026-10-16 17:58 +03:00 | `--locales ru`; settings screenshot + `python -I tools/a11y-scan.py --name "ru settings"` | exit 0, 0 unnamed |

## 4. Checks

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleRelease` green (17:42, 17:46, 17:55); `python tools/make-translations.py --check` green.
Functional run: the release on the Titan as a user in Russian and Arabic - home and settings READ by screenshots and by the
accessibility dump.

| Case | Status | Observation |
|---|---|---|
| Home - every control named | pass | 0 unnamed (the gear is «Настройки» / «الإعدادات») |
| Settings - every control named | pass | 0 unnamed after the fix (run 3 found the GPU switch) |
| Thin chevrons | pass | ru settings: the guide row, «Язык», «О приложении», «Условия использования» end in the thin stroke arrow, the same as «Назад» |
| Arabic mirror - home | pass | gear left, logo right, «صورة ← مشاركة ← KARTUN» right to left |
| Arabic mirror - settings | pass | back arrow top right pointing right; chevrons on the left pointing left; (i) and switch on the mirrored side |
| Setup, processing, result, models, about, guide - scans | skipped | not walked in this run - next |

## 5. Found

- Unnamed for TalkBack: the gear and every back button (RoundIconButton), the GPU switch; the (i) buttons said «i» - fixed.
- The «›» glyph at 16 sp invisible on the tablet (the owner) - replaced by the stroke chevron.

## 6. Traces

Screenshots (session scratchpad): `ar_home.webp`, `ar_settings.webp`, `ru_settings.webp`; the scanner's output above.

## 7. Verdict

partial — home and settings pass in Russian and Arabic; the remaining screens are not scanned yet.
