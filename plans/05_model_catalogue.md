# Plan 05 — The model catalogue: many models, downloaded on demand from KARTUN's own GitHub release

> **Создан:** 2026-10-14 (agent, autonomous window 21:56–22:26; the owner's orders in chat)
> **Родитель:** `plans/03_epic01_F2_screens.md` step 22 («В экране выбора всех моделей нужно картинку показывать превью по каждой модели» · «моделей нужно сильно больше добавить») and step 40 (ToU); `MASTER_PLAN.md` phase 4 («каталог моделей со скачиванием на устройство») and the decision-log row 2026-10-14 ≈ 22:04 (variant B); interview #002 (licence badges as Upscayl shows them)
> **Статус:** 🔧 2026-10-15 14:17 +03:00 — step 0 done (`researches/07_best_models_recon.md`); 12 models in the catalogue, 7 converted by `tools/convert-model.py`; the owner's APK pick NomosWebPhoto RealPLKSR + NMKD Siax (journal 14:12–14:14); none of the new ones run on the Titan yet
> **Вовне:** the owner — which non-commercial models without ready ncnn files to take (UltraMix, Remacri); the GitHub release `models` is created on the first upload

## Goal vector

Achieve: the user sees a catalogue of many models — each with a preview picture, its author, its licence badge
(«Разрешена для коммерческого использования» / «Не для коммерческого использования») and its size — downloads the ones
they want on demand, and uses them on «Улучшение» like the two built-in ones. Avoid: a file of unknown origin on the
device (every download is hash-checked), and a model shown as KARTUN's own (every card names its author).

## Acceptance criteria

1. **Downloads are verified.** A catalogue entry carries the SHA-256 of its `.param` and `.bin`; a file whose hash differs
   is deleted and the card says the download failed. Check: a unit test on the verifier with a tampered byte → refused.
2. **The card tells the truth.** Every card shows name, author, licence badge, size in MB and a preview; the badge text is
   the owner's wording (plan 03 step 35). Check: a screenshot of the catalogue on the Titan, read card by card.
3. **A downloaded model runs.** After a download the model appears on «Улучшение» and a run finishes on the GPU. Check:
   one run per downloaded model on the Titan, the result's size read from the result panel.
4. **Recency still rules «Улучшение».** The two last used models stand on top (plan 03 step 18); the rest are reached
   through «Все модели». Check: run model X from the catalogue → X is the top card.
5. **Built-in models stay offline.** The two models in the APK need no network.

## Sources and licences (from `researches/03` §C, plus the 22:00 run)

| Model | Author | Licence | Commercial | Source of the ncnn files | Note |
|---|---|---|---|---|---|
| Real-ESRGAN General v3 | Xintao Wang | BSD-3 | ✅ | built into the APK | own conversion |
| Real-ESRGAN x4plus | Xintao Wang | BSD-3 | ✅ | built into the APK | xinntao zip v0.2.5.0 |
| Real-ESRGAN x4plus anime | Xintao Wang | BSD-3 | ✅ | xinntao zip v0.2.5.0 `models/` | 8.9 MB |
| realesr-animevideov3 | Xintao Wang | BSD-3 | ✅ | xinntao zip v0.2.5.0 `models/` | 1.2 MB, already probed (237 ms at 256²) |
| 4xHFA2k | Philip Hofmann (Phhofm) | CC BY 4.0 | ✅ with credit | Phhofm/models `*_fp16.bin` | anime |
| 4xNomos8kSC | Philip Hofmann (Phhofm) | CC BY 4.0 | ✅ with credit | Phhofm/models | photos |
| 4x-UltraSharp | Kim2091 | CC BY-NC-SA 4.0 | ❌ | HF `Kim2091/UltraSharp` `NCNN/4x-UltraSharp-fp16.*` | proved 2026-10-14 22:00: blobs `data`/`output`, 5 431 ms at 256² on the GPU, 33.4 MB |
| 4x-UltraMix Balanced | Kim2091 | CC BY-NC-SA 4.0 | ❌ | only `.pth` in `Interpolations/` → our RRDB conversion | needs the owner's yes |
| 4x-Remacri | FoolhardyVEVO | CC BY-NC-SA 4.0 | ❌ | OpenModelDB → `.pth` → our conversion | needs the owner's yes |
| Real-CUGAN | bilibili (nihui port) | MIT | ✅ | realcugan-ncnn-vulkan | its own pre/post-processing — an engine change, later |

Per the owner's variant B every catalogue file is uploaded to the release `models` of `MikalaiKryvusha/KARTUN` with its
licence text beside it; the non-commercial ones stay there only while KARTUN is free (`researches/03` §C risk, MASTER_PLAN row).

## Steps

0. [x] (done 2026-10-15 14:17 — `researches/07`; the owner's cuts and picks in the MASTER_PLAN journal 13:57–14:14) (owner, chat 2026-10-14 ≈ 22:33 +03:00, after the close) «да, чем больше моделей, тем лучше. Смотрим на ЛУЧШИЕ, которые ОЧЕНЬ хвалят в интернете и на хаггинг фейс» — UltraMix and Remacri are in (own RRDB
   conversion from `.pth`, served from the release with the NC badge). FIRST a recon doc in `researches/` of the models the
   community rates highest — OpenModelDB ratings, Hugging Face downloads and likes, Reddit/forum praise — with licence,
   architecture (our engine runs ESRGAN/RRDB and SRVGG today; SwinIR/HAT/DAT/SPAN need a check) and a ready-ncnn flag;
   the catalogue grows from that list, best first.

1. [x] Prove a catalogue model on our engine: UltraSharp fp16 from the author's HF — blob names match, GPU run 5 431 ms at
   256² (`engine/third_party/testdata/out_4x-UltraSharp-fp16_gpu_t200_test256.png`), comparison sheet with General v3 and x4plus.
2. [~] `docs/model-catalogue.json` (in the repo, the single source): id, name, author, licence, commercial flag, files with
   SHA-256 and size, preview file, the release URL. A generator writes the app's copy; `--check` guards the pair.
   Started 2026-10-14 22:15: three entries (x4plus anime, AnimeVideo v3, UltraSharp) with hashes and sizes re-read from the
   files (3 models, 6 files, 0 mismatches); `published: false` until the release upload. Next: Phhofm's two models, previews,
   the generator for the app's copy.
   22:18: + 4xNomos8kSC and 4xHFA2k from Phhofm's own ncnn files (blobs `data`/`output`), GPU runs on the Titan 5 396 and
   5 285 ms at 256²; 5 models, 0 mismatches. Six-model comparison sheet (Bicubic, General v3, x4plus, UltraSharp, Nomos8kSC,
   HFA2k) - the first draft of the previews.
3. [~] Previews: one fixed crop of one fixed sample (the BSD-3 beach `input2.jpg`, and a drawing for anime models) run
   through every model on the Titan with `tools/titan-probe.mjs`; 320 px WebP per model, uploaded with the model.
   22:20: the beach crop for all 7 models (2 built-in + 5 catalogue; x4plus anime run 22:19 on the Titan GPU) in
   `docs/model-previews/<id>.webp`, 9–19 KB each, linked from the catalogue; a drawing sample for anime models still to do.
   Step 2's generator, 22:21: `tools/catalogue.mjs` writes `app/src/main/assets/catalogue.json` (shape, hash, NC-not-commercial
   checks); `--check` proved red on a hand-edited copy (DRIFT, exit 1), green after rewriting.
4. [ ] The release `models`: upload files + licence texts with `gh release upload`; a tool `tools/publish-models.mjs`
   checks every hash against the catalogue before upload.
5. [ ] The app: a downloader (resume-safe, hash-verified, into app storage), the «Все модели» screen with cards
   (preview, author, badge, size, «Скачать» / «Удалить»), downloaded models on «Улучшение» by recency.
   The owner's words for this step (chat 2026-10-15 ≈ 11:36–11:40 +03:00), each an acceptance line:
   - «модели, которые скачаны - нужно давать возможность их удалять через экран "Все модели"» · «нужно давтаь
     возможность удалять файл модели» → «Удалить» on a downloaded card deletes the .param/.bin from disk; Check: the
     file is gone from `files/models/` and the card shows «Скачать» again.
   - «Там нужно писатьих размер в мегабайтах» → the card shows the size in MB («33 МБ»).
   - «хранить их в директории приложения» → `Context.filesDir/models/` (private, removed with the app).
   - «при скачивании модели в ее плашке нужно крутить кольцо-прогресса с процентами, сколько мегабайт модели скачено»
     → while downloading, the card shows a small percent ring (round ends, like the processing ring) and «12 из 33 МБ».
6. [ ] The ToU line on each card: «Модель принадлежит автору: <name>» (plan 03 step 40).
7. [ ] Titan run of criteria 1–4 → run report.

## Risks

- The release serves non-commercial models: allowed under CC BY-NC-SA while KARTUN is free; if KARTUN is ever sold, the
  NC files must leave the release first (named in the MASTER_PLAN decision log).
- RRDB models are ~33 MB each and ~15× slower than General v3 on the Titan: the card's time estimate must say so.
- `.pth` conversion of RRDB (UltraMix, Remacri) needs the old-ESRGAN key mapping — not done yet; UltraSharp avoided it.
