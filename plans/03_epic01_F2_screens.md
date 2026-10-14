# Plan 03 — Epic 01, phase 2: the main path on screens — pick a photo → set up → upscale with progress → compare before/after

> **Создан:** 2026-10-14 (agent)
> **Родитель:** `plans/01_EPIC_mvp.md` §3 row «2 — основной сценарий», gate: criteria 2.2, 2.3, 2.5, 2.7 walked on the Titan by touch, no adb commands; `MASTER_PLAN.md` phase 2
> **Статус:** 🔧 started 2026-10-14 18:27 +03:00 — slice 1 steps 1–6 done 2026-10-14 18:47 +03:00, step 7 run (`testcases/reports/2026-10-14_F2_screens_slice1.md`, criteria 1–4, 6 pass; pinch and >12.5 MP not run); the mockup sheet for the owner and the icon (step 8) open
> **Вовне:** each screen's Titan screenshot next to its mockup → to the owner (taste class: his eye judges the look)

## Goal vector

Achieve: on the Titan a user opens KARTUN, picks a photo with the system photo picker, sees it with its resolution,
picks a model and a scale, taps «Улучшить», sees progress, and compares before/after with a slider grabbed anywhere on
the picture — all by touch, in Krinik's style, as on the accepted mockups (`design/kartun-mockups.html`, variant A v2;
`[OWNER]` interview #002: «в остальном ОК, принято, можем делать!»).

## Acceptance criteria (Situation · Action · Result · Check)

1. Home → picker. Situation: KARTUN is open on its home screen. Action: the user taps «Выберите фото». Result: the system
   photo picker opens; no permission dialog. Check: `aapt2 dump permissions` of the APK lists no `READ_MEDIA_IMAGES` /
   `READ_EXTERNAL_STORAGE`; a Titan screenshot shows the picker.
2. Setup screen. Situation: the user picked the 960×1280 photo. Action: the setup screen opens. Result: the preview with
   «960 × 1280 · 1,2 Мп», two model cards (fast selected), the scale slider at 2×, the line «960 × 1280 → 1920 × 2560».
   Check: Titan screenshot + uiautomator dump contains those strings.
3. Upscale. Situation: setup screen, fast model, 4×. Action: tap «Улучшить». Result: a progress screen, then the result
   screen with «3840 × 5120» and the time. Check: uiautomator dump of the result screen shows `3840 × 5120`; logcat has no
   `FATAL`.
4. Compare. Situation: result screen. Action: the user drags from any point of the picture. Result: the before/after line
   follows the finger. Check: two screenshots after `input swipe` at two x positions show the line at those x (±20 px).
5. Look. The four screens stand next to their mockups on one sheet for the owner; his verdict is recorded (taste class).
6. Strings: every visible string in `res/values/strings.xml` (en) and `res/values-ru/strings.xml`; no literal in Kotlin.
   Check: `grep -rn 'Text("' app/src/main/kotlin` prints nothing.

## Steps

Slice 1 (this session):
1. [x] Theme: `ui/theme` — colours of the mockups (`--bg #0D0D0D`, `--card #1C1C1E`, `--pink #FF1A8C` …), shapes
   (20–32 dp), type: Unbounded (headings, logo) + Manrope (text) as OFL font files in `res/font` (`[OWNER]` interview #002, Q2 = A).
2. [x] Models in the APK: `tools/stage-models.mjs` copies `realesr-general-x4v3` (own conversion) and `realesrgan-x4plus`
   into `app/src/main/assets/models/` (gitignored, like `engine/third_party/`); first launch copies them to `filesDir`
   (the engine loads from a path).
3. [x] Home screen A: logo, «Выберите фото» card → Photo Picker (`PickVisualMedia`), «Вставить ссылку» (disabled until
   phase 4), the share hint, «Обработка на устройстве, без сети».
4. [x] Setup screen: pinned collapsing preview, model cards (fast · maximum), scale slider 1×/2×/4×/8×/16× with the result
   size, (i) buttons with a short text sheet, «Улучшить» button. Scales 1× and 2× = one 4× pass + downscale; 8×/16× stay
   disabled with «скоро» until phase 1 brings the megapixel limit and the second pass.
5. [x] Upscale in a ViewModel on the single engine thread; progress screen with a PERCENT ring fed by tile progress from the
   engine, and a time estimate before the run (`[OWNER]` interview #003, Q1 = D: «Рисуем примерную эстимацию для юзера, и
   прогресс бары-кольца в процентах»). The estimate starts from the Titan measurement and learns from the device's own runs.
6. [x] Result screen: before/after compare grabbed anywhere, pinch zoom + pan, size and time line; «Сохранить» /
   «Поделиться» shown disabled until phase 3.
7. [~] Titan run by touch per criteria 1–4 → run report; sheet «mockup | build» for the owner.

8. [x] Own vector icon (2026-10-14 19:00 +03:00: `tools/make-vector-icon.py`, applied, seen on the Titan's splash and app list — sheet `design/icon/compare_vector.png`; the look is the owner's verdict; ≈ 19:07 the owner: «пиксели слева нужно рандомно ярче темнее хаос» → random tones, run 18, sheet `design/icon/compare_vector_chaos.png`; ≈ 19:10 «умный рандом» → the light of the mirrored smooth half + jitter, run 19, sheet `design/icon/compare_vector_smart.png`) — the owner's order in chat 2026-10-14 ≈ 18:33 +03:00, verbatim: «Нарисуй вообще свою векторную
   иконку. Сердце, как нам ГПТ пытался рисовать. И правая и левая части гладкие. Стеклянные. Но левая - покрыта
   шахматными квадратиками стекла - а левая идеально гладкая, блеслящая». Reading (said in chat, awaiting correction):
   the right half is the smooth shiny one (his earlier «слева пиксели, справа от черты-лазера - гладкое»). A
   VectorDrawable heart; a sheet «own vector | icon_v3» on the home screen and the splash goes to the owner first.

Owner's remarks on the build, chat 2026-10-14 ≈ 18:48–18:50 (verbatim), all applied and seen on the Titan (run report runs 11–12):
- [x] «прыгает картинка на экране Улучшение» → collapsing header (NestedScrollConnection), the preview turns into the small row at 80 dp.
- [x] «Инфо открывать диалогами, а не ботом шитами» → `InfoDialog` in `ui/Common.kt`.
- [x] «модель качать на экране улучшение, а не на прогрессе» → `UpscaleViewModel.preload()` on entering the screen and on a card tap.
- [x] «Фото - Поделиться - Кртун - выровнять по центру» → the row centred in the share hint card.
- [x] «минут секунд в кнопке улучшения писать целым слвом» (≈ 18:58) → `durationText` with `plurals` («7 секунд», «1 минута 49 секунд»), run 15.

9. [x] Pull-down to full screen (2026-10-14 19:05 +03:00, `ui/FullPhoto.kt`, runs 16–17) — the owner in chat 2026-10-14 ≈ 18:57 +03:00, verbatim: «На экране Улучшения свайпом внижз
   было бы здорово раскрывать вью картинки на весь экран». A downward pull past the full preview opens the photo full
   screen on black, pinch zoom inside; a swipe up or Back returns to the settings.

10. [~] Landscape — the owner in chat 2026-10-14 ≈ 19:12 +03:00, verbatim: «лендскейп нужно поддерживать тоже». Built
    2026-10-14 19:15 (guarded loop, no device by the owner's word «работай автономно, без тестирования на клиенте»):
    home — pick card left, the rest right; «Улучшение» — photo left at full height, settings right with the button under
    them; processing — photo left, ring panel right; result — the panel capped at 600 dp. Open: walk all four on the
    Titan rotated («потом потестируем»).

Owner's remarks 2026-10-14 ≈ 20:14–20:20 (chat, verbatim), the backlog of the next chat:
17. [~] «и под превью фото нужно добавить драг-хендл, чтобы было понятно, что там можно хватать, и двигать превь фото» ·
    «а превью фото можно тапом и удержанием двигать и перемещать прямо во вьюшке преввью» · «даблтам увеличивает» →
    `DragHandle` + `PhotoCard` gestures built ≈ 20:16 (double tap ×2.5, long press + drag pans, single tap full screen);
    NOT walked on the Titan yet.
18. [x] «в выборе моделей на экране Улучшение нужно показывать две последние использованные модели. Вверху последнюю
    использованную, ниже предпоследнюю».
    Done 2026-10-14 ≈ 21:41 +03:00: a run moves its model to the front of the stored order (`model_order`), the cards follow
    it; `RecencyTest` 4/4, reddened by a mutant that appended instead of prepending. On the Titan: General v3 on top before,
    after an x4plus run x4plus on top. With only two models today the whole list is the two last used.
19. [ ] «описание модели на экране Улучшение сделать подробнее. Слово - Максимум - странное, и ни о чем не говорит».
20. [x] «в кольце прогресса цифры процентов пляшут, вероятно из-за разной ширины цифр. Нужен там или моноширинный шрифт, или
    как-то закрепить, чтобы не плясали цифры» (tabular figures: `fontFeatureSettings = "tnum"`).
    Done 2026-10-14 ≈ 21:37 +03:00: both fonts carry `tnum`; measured on the Titan by the percent text's width during an x4plus 16×
    run - before: two-digit values 268..320 px (control, old build); after: 21 values from 10 % to 72 %, all 295 px.
21. [ ] «в настройках нужно будет сделать секцию о приложении и авторе, как в KrinikCam, ссослаться на вжохновителей -
    репозиторий Upscayl, и репозиторий движка, и моделей (если они отдельными репами)» — read KrinikCam's about first.
22. [ ] «В экране выбора всех моделей нужно картинку показывать превью по каждой модели» · «моделей нужно сильно больше
    добавить» — the model catalogue (phase 4 pulled forward; mockup 'models', licence badges as accepted in interview #002).
23. [ ] (owner, chat 2026-10-14 ≈ 20:49 +03:00, looking at the «Улучшение» screenshot) «убрать строчку По умолч» — the
    «2× · по умолч.» tick label under the scale slider becomes plain «2×».
24. [ ] (owner, chat 2026-10-14 ≈ 20:50 +03:00) «в настройки пускать только с главного экрана приложения» — the gear leaves
    the «Улучшение» top bar (the only other screen that had it); the home screen keeps it.
25. [ ] (owner, chat 2026-10-14 ≈ 20:54 +03:00) «непонятно зачем две кнопки поделиться на результате тебе» — the round share
    icon leaves the result's top bar; «Поделиться» stays in the panel next to «Сохранить».
26. [ ] (owner, chat 2026-10-14 ≈ 20:54 +03:00) «и почему у кнопки назад нет видимой подложки, хоть чуть-чуть серой, или какой
    в дизайн схеме нашего приложения» — the translucent round button (result, full-screen photo) takes the theme's grey card
    `surfaceContainerHigh` at 85 % instead of the near-black background at 55 %.
27. [ ] (owner, chat 2026-10-14 ≈ 20:58 +03:00) «до после можно не ярко, а полупрозрачно» · «после сейчас слишком кричащее» — both
    result tags translucent grey (`Pill(translucent)` = `surfaceContainerHigh` at 70 %); the pink «После» pill is gone.
28. [ ] (owner, chat 2026-10-14 ≈ 20:59 +03:00) «и в сводке внизу нужно писать, какая модель улучшила» — the result panel opens
    with the model's title.
29. [ ] BUG (owner, chat 2026-10-14 ≈ 20:57 +03:00) «когда картинку ты свернул - драг хендл пропал - баг» — the handle was drawn in
    the expanded branch only; now it stands under the photo in both states and the compact row drags too.
30. [ ] (owner, chat 2026-10-14 ≈ 21:00 +03:00) «м там у тебя перемешает слайдер до после - а должен хватать фото на навигацию» —
    on the result the divider moves only when grabbed on the line or its knob (±36 dp); a finger elsewhere pans the photo;
    a double tap zooms ×2.5 at the point and back (as the preview, step 17).
31. [ ] (owner, chat 2026-10-14 ≈ 21:01 +03:00) «виджет сводки чуть ниже сместить» — the result panel's bottom gap 28 → 8 dp.
32. [ ] (owner, chat 2026-10-14 ≈ 21:05 +03:00) «и полоса до после зачем-то у тебя от края экрана до края экрана, перекрывает вверху
    статус бар ОС. Нужно её начинать мягкий фейдом и ззаканчивать мягим фейдом в прозрачность на концах» — the divider spans
    the picture's fitted rect inside the system bars, with a 12 % transparent fade at each end. Revised ≈ 21:11: «ну ты уже
    слишком сократил полосу до после. Пусть от верха девайся буде до низа, но немножко от низа отсутпает, и немножко от
    верха, чтобы статус бар не перекрывала и достаточно» — the divider runs the screen's height inside the system bars with
    an 8 dp gap from each, the fades kept.
33. [ ] (owner, chat 2026-10-14 ≈ 21:08 +03:00) «давай виждет сводки на экране результата сделаем скрывающися в сторону за пределы
    экрана, и чтобы на краб оставалась стрелочка, в которую он скрылся. И отуда его можно ообратно раскрыть» — a sideways swipe
    hides the panel past the right edge (released beyond 25 % of the width or flung); a grey arrow tab stays on the edge at
    the panel's height; a tap or a swipe left on it brings the panel back. The tab is excluded from the system back gesture
    (`systemGestureExclusion`: on the Titan 21:11 a swipe left on it left the screen). ≈ 21:13: «в что виджет сводки можно
    смахнуть - нукто не догадается, ему нужен или драг хендл или кнопочка-стрелочка» — a round «›» button in the panel's top
    right corner hides it.
34. [~] (owner, chat 2026-10-14 ≈ 20:58 +03:00) «м в настройках в пункте о приложении и авторе нужна и ссылка на наш GH, и ссылка
    на николая нотификейшен алертс, где можно ему донат отправить» — done with step 21's half: «О приложении» gains «Проект»
    (our GitHub), «Вдохновители и движок» (Upscayl, ncnn, Real-ESRGAN) and «Автор» (KrinikCam's set: GitHub, Telegram,
    YouTube, Instagram @kotkrinik); the GPU row reads the name only. Walked on the Titan 21:29: a link opens the GitHub app.
    Open: the DonationAlerts address - in no project on disk and not in KrinikCam; asked in chat 21:25, the row comes with
    the owner's URL.

Slice 2:
11. [~] Foreground service `mediaProcessing` (`UpscaleService.kt`, built 2026-10-14 ≈ 19:17, guarded loop): the ViewModel
    keeps owning the engine job; the service keeps the process alive and mirrors `ProcessingState.progress` in a silent
    notification (percent, «Отменить», tap opens the app), stops itself when the run ends, cancels on the 6 h/day limit
    (`onTimeout`). Permissions `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PROCESSING` (install-time, no dialog).
    Open: the device run — start, leave the app, watch the notification, «Отменить» from it («потом потестируем»).
    `[AI]` POST_NOTIFICATIONS is NOT requested: without it Android 13+ still lists the run in the task manager, a
    permission dialog is a UX decision for the owner.
13. [~] (phase 3 work pulled forward, `[AI]`: phase 1 steps 3–4 need a native library decision and the device) «Сохранить» and «Поделиться» on the result screen — `ResultStore.kt` (MediaStore `Pictures/KARTUN`, pending until written; FileProvider copy for the share sheet), a centred save dialog (format PNG/JPEG/WebP, quality 92 by default from the mockup, WebP off over 16 383 px), `FileNameTest` 4/4 (it caught a double underscore in «Фото мамы (1)» first). Built 2026-10-14 ≈ 19:25; the device run waits for the owner.
16. [~] «О приложении» (plans/01 §2.14, phase 4 pulled forward): version, device, Android, the engine's GPU line, ncnn version, the licences of everything shipped; from the settings' top-right icon. Built 2026-10-14 ≈ 19:39; device case C22.
15. [~] The user guide (mockups 'manual' / 'manual-models'): `res/raw*` Markdown per language (RU sections 1, 2, 4, 6 verbatim from the mockup, EN translated), a small renderer for headings, lists, tables, notes and bold; opened from the settings' pink hero row. Pictures and sections 3 and 5 come with the guide's next revision. Built 2026-10-14 ≈ 19:36; device case C20.
14. [~] Settings (mockup 'settings'), the working rows only: «Ускорение GPU» (kept between launches; off = CPU nets) and «Язык» (the system per-app language list); the gear on the home screen. Built 2026-10-14 ≈ 19:34; device case C18.
12. [x] The adb engine probe runs only in debuggable builds (`BuildConfig.DEBUG` in `MainActivity`, 2026-10-14 ≈ 19:19; release `BuildConfig.DEBUG = false` read from the generated file). Device check of the probe in the debug build — with the next Titan session.

## Risks

- Holding a 4× bitmap of a 12 Mp photo in Java memory (192 Mp, 768 MB) → slice 1 limits the input to what the heap
  takes (`largeHeap`) and says so; phase 1 brings tiles-to-disk.
- Fonts with Cyrillic: Unbounded and Manrope both carry Cyrillic (Google Fonts) — verify on a screenshot.
- Compose BOM 2026.09 API drift vs my recall → read the compiler's errors, do not guess.
