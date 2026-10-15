# Run report — 2026-10-16 · the model catalogue on the Titan («Все модели», download, choose, upscale, delete)

## Work
The catalogue feature of commit `fc6b8d5` against `plans/05_model_catalogue.md` acceptance criteria 1–4 (download
verified · the card tells the truth · a downloaded model runs · recency rules «Улучшение»); the owner's words of plans/05
step 5 («Удалить», size in MB, app folder, percent ring).

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `192.168.1.3:5555`; build `app-debug.apk` of `fc6b8d5` (00:38) installed by
`adb install -r`; the GitHub release `models` of MikalaiKryvusha/KARTUN (29 assets, published 00:3x by
`node tools/publish-models.mjs`); the owner's accumulated app state on the Titan (old General v3 / x4plus copies in
`files/models`, stored scale 8×, stored model order).

## Runs
1. 00:39–00:42 — launch `am start -S -f 0x10008000 -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity`; UI agent
   `C:\Android\private\ui-agent\ui.py --serial 192.168.1.3:5555`: `tapText Выберите фото` → «Подборки» → «На этом устройстве»
   → `KARTUN_test` → `tap 1334 697` (photo1.jpg) → `tapText Готово` → `wait Улучшить`.
2. 00:43–00:45 — `tapText Все модели`, `seek:ClearReality` (scratchpad `run_catalogue.py`), `tap 1393 2315` («⤓ 0,9 МБ»),
   `run-as … sha256sum files/models/4x-ClearRealityV1-fp16.*`.
3. 00:45–00:46 — `tap 354 2282` (the ClearReality card) → «Улучшение» → `tapText Улучшить` → `wait Сохранить`.
4. 00:47–00:48 — «Все модели» → `tap 1377 1018` («Удалить» of ClearReality) → `run-as … ls files/models` → `back`, `labels`.

## Checks
- Hygiene: 37/37 JVM tests (`:app:testDebugUnitTest`), DownloaderTest reddened by a mutant without the hash check;
  `catalogue.mjs --check`, `make-translations.py --check`, `check-app-names.mjs` green.
- Functional run: the path above walked on the Titan by the UI agent, screens read from `labels` and screenshots.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | «Улучшение» shows the two last used models on the device + «Все модели» | pass | General v3 and x4plus (the owner's last used, files left by the old build) + «Все модели · Выбрать ›» |
| 2 | «Все модели»: chips, cards with name · purpose · size, before/after picture, badge, author, state | pass (with defect 1, 2) | chips Все / Фото / Аниме / Для коммерческого использования; badges green BSD-3 / WTFPL, blue CC BY 4.0, amber NC; «Встроена в приложение» on NomosWebPhoto and Siax; «Удалить» on downloaded ones; pink «⤓ 34 МБ» |
| 3 | Download from the release | pass | ClearReality 854 788 + 6 131 bytes in `files/models`, no `.part`; SHA-256 on the device `b3c89c06…` / `7a1d0956…` = the catalogue |
| 4 | The percent ring with «N из M МБ» during a download | not observed | the 0.9 MB file came faster than a screenshot; to see on a 34 MB model |
| 5 | A downloaded model runs | pass | ClearReality chosen from its card → «Улучшение»; run 960×1280 → 7680×10 240 (stored 8×) in 25 s, «GPU · Vulkan», result panel names «4x ClearReality V1» |
| 6 | Recency | pass | after the run «Улучшение» shows ClearReality on top, General v3 next |
| 7 | «Удалить» | pass | both files gone from `files/models`; the card shows the download pill again; the chosen model fell back to 4xNomosWebPhoto RealPLKSR (default) |
| 8 | Estimate | pass | ClearReality 8× ≈ 35 s against 25 s measured (5 passes-worth × 1.23 MP × 5 722 ms); NomosWebPhoto 8× ≈ 6:38 = 5 × 1.23 × 64 850 ms |

Not run: the hash-mismatch path on the device (JVM-tested only); a download cut mid-way and resumed; offline behaviour of
the two built-in models in a fresh install; the release build.

## Found
1. The card line put «≈» before the exact size («≈ 2,4 МБ») — the estimate's glue was reused; fixed after the run by a
   separate `model_desc_with_size` (not yet seen on the device).
2. The previews were the old square model crops shown under «До» as well — a «before» that was not before. Fixed after
   the run: `tools/make-previews.py` writes real bicubic | model pairs (not yet seen on the device; the subjects are being
   chosen from the owner's themes).
3. Stand: two adb versions (C:\adb 32 and the SDK's 41) kill each other's server; ui.py's `shot` calls bare `adb` — run it
   with the SDK's platform-tools first in PATH.

## Traces
Screenshots in the session scratchpad (`t3`, `m1`, `m5`, `r1`, `d1`), not kept; `labels` lines quoted above.

## Verdict
partial — criteria 1, 3, 4 of plans/05 pass on the device; criterion 2 passes with defects 1–2 fixed but not re-seen; the
download ring and the resume path are not observed yet.
