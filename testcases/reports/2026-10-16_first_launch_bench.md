# Test run report — the first-launch GPU/CPU speed check (ideas/04) on the Redmi Note 12 Pro+

**Created:** 2026-10-16 20:17 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** release `com.kriniks.kartun`
`0.1 (1)`, working tree after `a241fa5` (DeviceBench, BenchDialog), APKs 20:06 (crash), 20:09, 20:11, 20:14

## 1. Work

`ideas/04` — the owner's words (chat 2026-10-16 ≈ 20:0x): on the first launch, while the estimates are empty, a dimmed home
screen with the dialog «Измеряем производительность GPU и CPU устройства…», the live spinner, a quick test of both chips
whose result starts the estimator; the dialog closes itself; «Отмена» closes it without the better estimate; ≈ 20:1x
«сильнее тонировку». Acceptance (ideas/04): the first estimate on a new device within 1.5× of the real time.

## 2. Contour

Xiaomi Redmi Note 12 Pro+ 5G, Android 14, Mali-G68 MC4, `192.168.1.76:41705`; first launch made by `pm clear
com.kriniks.kartun` + `am start`. Calibration data of the same device the same day: GPU photo 960 × 1280 (NomosWebPhoto 4×)
262 s = 213 216 ms/MP (`2026-10-16_release_1_0_redmi.md`); CPU 256 × 256 70 006 ms = 1 068 206 ms/MP (`kartun-cli --cpu`).

## 3. Runs

| # | Moment | Command / action | Outcome |
|---|---|---|---|
| 1 | 2026-10-16 20:06 +03:00 | first launch, APK 20:06 | ❌ crash at start: `benchRunning` declared below `init` (NPE in the ViewModel) — fixed by moving it up |
| 2 | 2026-10-16 20:09 +03:00 | first launch, 96² GPU / 48² CPU, 3 runs | dialog shown; bench 1525 / 2163 ms; ≈ 15 s from launch |
| 3 | 2026-10-16 20:10–20:11 +03:00 | three first launches, 64² GPU / 32² CPU, 2 runs | GPU 734 / 791 / 756 ms, CPU 1121 / 1108 / 1124 ms; process start 20:11:02.5 → result 20:11:10.6 (8 s) |
| 4 | 2026-10-16 20:12 +03:00 | first launch, calibrated (REF 231 / 1018 ms), dim 0.85 | `gpu_factor 3.36`, `cpu_factor 1.09`; screenshot `07_bench_dim` |
| 5 | 2026-10-16 20:13–20:16 +03:00 | `kartun-cli upscale p512.png --model 4x-NMKD-Siax-fp16 --gpu` | estimate 123 396 ms, real 193 358 ms |

## 4. Checks

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleRelease :app:lintRelease` green, lint 0 errors;
`python tools/make-translations.py --check` green.
Functional run: the release on the Redmi as a first launch — the dialog, its dimming and the spinner READ by screenshots;
the bench numbers by logcat; the estimate against a real run by the agent door.

| Case | Status | Observation |
|---|---|---|
| Dialog on first launch over a dimmed home screen | pass | screenshots `05_bench`, `07_bench_dim` (dim 0.85 after the owner's word) |
| Dialog closes itself | pass | home screen without it after ≈ 8 s |
| Repeatability of the bench | pass | GPU spread 8 %, CPU 1 % over three launches |
| Estimate of the calibration model (NomosWebPhoto, GPU) | pass by construction | the factor is calibrated on it — not evidence |
| Estimate of another model (Siax, GPU, 0.35 MP) | **fail (near)** | 123 s against 193 s = 0.64 (target 0.67–1.5); without the bench 37 s = 0.19 |
| «Отмена» | skipped | not pressed in this run |
| Another device (the Titan) | blocked | the Titan is not reachable (`adb connect` 10060) |

## 5. Found

- A launch crash in the first build (declaration order) — caught on the device before any commit.
- One factor for all models misses models that slow down more on a weaker GPU (Siax on Mali-G68: 0.64 of the real time).
  The learning after the first real run corrects it as before; a per-model bench would cost more seconds at launch.

## 6. Traces

Screenshots (session scratchpad `redmi_smoke/`): `05_bench`, `07_bench_dim`; logcat lines `{"bench":true,…}` quoted above;
the crash stack (logcat `-b crash`, 20:06:56) — NPE `setValue` in the ViewModel constructor.

## 7. Verdict

partial — the dialog, the dimming, the self-closing and the factors work on a first launch; the first estimate is 3× closer
to the truth than before but misses the 1.5× target for Siax (0.64); «Отмена» and a second device are not checked.
