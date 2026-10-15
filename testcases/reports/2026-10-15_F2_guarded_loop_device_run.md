# Test run report — the guarded-loop backlog (TC_F2) and the tail of `ea9d205`, walked on the Titan

**Created:** 2026-10-15 11:20 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** debug APK
`com.kriniks.kartun.debug` 0.1 (1): run 1 on the build installed 2026-10-14 23:25 (`ea9d205`), runs 2+ on `ea9d205` + the
`KARTUN_PANEL` debug logs removed (this commit)

## 1. Work

The case set `testcases/TC_F2_guarded_loop_device_backlog.md` (25 cases + K1–K3) and the open control of `ea9d205`
(STATUS «Где продолжить» 2): a gesture 55 dp from the divider but far above the knob must NOT move the line; the same
55 dp at the knob's height must (the invisible 72 dp grab zone, the owner 2026-10-14 ≈ 23:05 +03:00: «вокруг драга до
после нужно сделать невидимую более широкую область, за которую его можно хватать»).

## 2. Contour

HEADWOLF Titan 1 (Android 16, Mali-G615 MC6, 1600×2560, density 480 = 3 px per dp) over adb `100.99.111.48:35437`
(Tailscale). Test data: `Pictures/KARTUN_test/beach.png` (256×256, left from 2026-10-14) and `Pictures/KARTUN_test/photo1.jpg`
(960×1280, the phase 0 photo, pushed 2026-10-15 11:11) — both removed after the run (`HOUSE_RULES.md` R1). The UI is
driven by the on-device UI agent (`C:\Android\private\ui-agent`, `HOUSE_RULES.md` §6) through the system photo picker — the
user's path; swipes by `adb shell input swipe`; the divider's x is read from screencap row y = 900 (pink `#FF1A8C` run).

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-15 11:10 +03:00 | picker → `beach.png` → «Улучшить» (2×) → `python knob_control.py 100.99.111.48:35437 <dir>` (swipes `965,500→1365,500` and `965,1280→1300,1280`) | old build: line 800 → 800 (control) → 1302 (knob zone) |
| 2 | 2026-10-15 11:11 +03:00 | `.\gradlew.bat :app:testDebugUnitTest assembleDebug` → `adb install -r app-debug.apk` | 0 · 28 JVM tests, 0 failures; installed |
| 3 | 2026-10-15 11:18 +03:00 | `python s_c5.py` (home → picker → `photo1.jpg` → «Улучшить» at the default 2× → result) | «960 × 1280 · 1,2 Мп», «Улучшить · ≈ 8 секунд»; processing «Тайл 6 … 14 из 35», «GPU · Vulkan»; result «960 × 1280 → 1920 × 2560», «8 секунд · GPU · Vulkan» in 10.2 s |
| 4 | 2026-10-15 11:18 +03:00 | `python knob_control.py 100.99.111.48:35437 <dir>` on the new build | line 800 → 800 (control) → 1301 (knob zone); `logcat` FATAL/SIGSEGV count 0 |

## 4. Checks

Hygiene: unit 28/28 (`:app:testDebugUnitTest`)
Functional run: the main path in portrait and the result screen's divider gestures, walked on the Titan as the user (system
picker, taps, swipes); READ: the screen labels through the UI agent, screencaps (divider pixels), `logcat`.

| Case | Status | Observation |
|---|---|---|
| `ea9d205` control: 55 dp off the line, far above the knob | pass | the line stays at x = 800 px on both builds (`k1_control.webp`) |
| `ea9d205` positive: 55 dp off the line, at the knob's height | pass | the line follows to x = 1302 / 1301 px (finger stopped at 1300; `k2_knob.webp`) |
| C5 portrait regression (run 9 path) | pass | same texts as run 9 of `2026-10-14_F2_screens_slice1.md`: «960 × 1280 → 1920 × 2560», «8 секунд · GPU · Vulkan» |
| K1 portrait shows the portrait layouts | pass | home, «Улучшение», processing and result in their portrait layouts (`c5_*.webp`) |
| K2 a 2× run has no «2 прохода» | pass | processing counted «из 35» tiles, no pass line |

## 5. Found

- none so far

## 6. Traces

- screenshots (WebP, session scratchpad, not in git): `k0_start`, `k1_control`, `k2_knob`, `c5_0_home`, `c5_1_enhance`,
  `c5_2_processing`, `c5_3_result`
- harness scripts (throwaway, session scratchpad): `drv.py`, `knob_control.py`, `s_c5.py`

## 7. Verdict

partial — the `ea9d205` control and C5/K1/K2 pass; the rest of the case set is being walked in this session.
