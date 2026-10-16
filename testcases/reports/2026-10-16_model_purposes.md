# Run report — 2026-10-16 · model descriptions say what the model is for

## Work
The owner's word 2026-10-16 ≈ 12:52–12:53 +03:00: «Чистые и слегка сжадтые снимки - можно подумать ,что такие получатся на
выходе. Нужно писать для каких целей модель. Для чистых, но слегка сжатых снимков» · «и так в описаниях по всем поделям» ·
«проверить, чтобы было понятно». The 12 purpose strings (`model_fast_desc`, `model_max_desc`, `model_purpose_*`) rewritten
in EN and RU by hand and in 8 languages through `tools/make-translations.py` (scripted, every replace asserted once per
language block); then a clean-instance clarity pass (a subagent with the strings only), its language fixes taken.

## Contour
HEADWOLF Titan 1, `app-debug.apk` built ≈ 13:00 from the working tree, installed 13:00; the beehive test photo
(`Pictures/KARTUN_test`, 1920 × 2560) through the system photo picker.

## Runs
1. ≈ 12:58 — `python -I <scratchpad>/purpose_fix.py`; `python tools/make-translations.py`; `git diff --stat` (script 88 lines,
   12 strings in each of 8 languages).
2. ≈ 12:59 — the clarity pass (subagent) on the 12 RU and 12 EN lines.
3. ≈ 13:00 — `python tools/make-translations.py --check`; `.\gradlew.bat :app:testDebugUnitTest assembleDebug`; install.
4. 13:01 — «Улучшение» and «Все модели» on the Titan: `labels` of the UI agent, screenshot `models_desc.webp`.

## Checks
- Hygiene: `make-translations.py --check` green («locales_config.xml matches the 10 shipped languages»); build exit 0, 54 JVM tests.
- Functional run: the owner's path on the Titan, read from labels and a screenshot.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | Siax card on «Улучшение» | pass | «Универсальная · для чистых, но слегка сжатых снимков · ≈ 43 минуты 2 секунды» |
| 2 | General v3 card | pass | «Быстрая · для фото с шумом и размытием · ≈ 2 минуты 48 секунд» |
| 3 | «Все модели» | pass (partial view) | «Быстрая · для лиц, волос и природы · 0,9 МБ», «Для фото, где нужны и резкость, и детали · 34 МБ»; the rest scrolled past the agent's view |
| 4 | the clarity pass | done | taken: RU «Для сжатых и размытых фото из интернета», «…для аниме и кадров из видео»; EN «Fast · for noisy, blurry photos», «For compressed, blurry photos from the web», «Lightest · …». Not taken (no source for the models' real input): the pass's guesses for the «нужны / важны / нужна» lines — raised to the owner in STATUS |

## Found
The clarity pass reads five lines as possible promises about the result: «Детальная · для фото, где важны мелкие детали»,
«Для фото, где нужны естественные детали», «Для фото, где нужны и резкость, и детали», «Для фото, арта и аниме, которым нужна
резкость», «Очень медленная · для снимков, где нужно больше всего деталей». They name the PURPOSE (the goal), which is the
owner's «для каких целей»; the pass's test — «if it works after "the result will be…", rewrite it; it should work after
"feed it…"» — would need each model's real input from its author's page (OpenModelDB / Hugging Face), not yet read. Owner's call.

## Traces
`<scratchpad>/models_desc.webp`, `purpose_fix.py`; the diffs of the commit.

## Verdict
pass — the descriptions now say what each model is for, on the device in RU; five goal-type lines wait for the owner's word.
