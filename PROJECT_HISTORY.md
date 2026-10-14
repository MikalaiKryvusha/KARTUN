# KARTUN — Project History (the chronicle)

> The APPEND-ONLY chronicle of how this project lived and grew: closed sessions, shipped phases,
> releases, big decisions in the order they happened. This is where `STATUS.md` sheds its past —
> STATUS stays a short live summary of NOW; everything finished moves HERE (the "bonsai trim" step
> of `/end-chat-soft`).
>
> **Not required reading.** This file is NOT part of `/resume`'s canon set and not in the
> before-every-task minimum — open it only when you actually need the archaeology: how a decision
> came to be, what an old phase contained, when something shipped.
>
> **Chronicle rules (ADR discipline):**
> - **Append-only, newest on top.** A recorded entry is never edited to say something else —
>   history that can be rewritten is not history. Corrections come as NEW entries that reference
>   and supersede the old one.
> - An entry moves here VERBATIM from `STATUS.md` when its work closes — move, don't rewrite;
>   the entry already carries its dates, counters and file pointers.
> - Entries mention versions and dates freely — a chronicle legitimately speaks of old versions,
>   and the update machinery's stale-claims scan knows to leave this file alone.
> - When the file grows unwieldy, split by era: keep the newest era here, move older ones to
>   `PROJECT_HISTORY_<era>.md` files, and leave a one-line index at the top of this file
>   (the pattern large changelogs use).
>
> Living document — never DONE-tagged.

---

## Entries (newest first)

### 2026-10-14 18:12–20:25 +03:00 — вторая сессия: фаза 0 закрыта, экраны MVP, защищённый цикл без устройства ✅

Перенесено из `STATUS.md` дословно при мягком закрытии 2026-10-14 ≈ 20:26 +03:00 (бонсай-обрезка).

Последняя сессия: 2026-10-14 15:20–15:45 +03:00, закрыта мягко по слову владельца «закрывай чат». Долг церемоний
прошлого срочного закрытия оплачен (запись — в `PROJECT_HISTORY.md`).

Неисправленные ложные утверждения: нет. Слова агента в чате «с `icon_v3` оба условия сразу не выполнить» (оценка края
сердца на глаз) опровергнуты замером и исправлены в чате; в файлы они не попадали.

**Защищённый цикл 2026-10-14 19:12–19:53 без устройства** (слово владельца «работай автономно, без тестирования на
клиенте, 40 минут», «потом потестируем»): альбомная ориентация всех экранов; фоновая служба `mediaProcessing` с
уведомлением; 8× и 16× двумя проходами и лимит 200 Мп; лестница отката при сбое GPU; «Сохранить» и «Поделиться»;
проба движка только в отладочных сборках; черновые `[AI]` переводы на 8 языков и `localeConfig`; «Вставить ссылку»
(сеть только по ней); экраны «Настройки» (GPU, язык), «Руководство» (Markdown, 6 разделов), «О приложении»; настройки
запоминаются между запусками; уведомление «Фото готово»; экран обработки знает два прохода 8×/16×; 21 JVM-тест
(мутационно проверены); разведка `researches/05` (большие результаты); план фазы 1 `plans/04`. Всё собрано, на Титане НЕ
проверено — набор для прогона: `testcases/TC_F2_guarded_loop_device_backlog.md` (25 случаев + 3 контрольных).
Android Lint (`gradlew :app:lintDebug`) работает (падение первого запуска 19:40 не повторилось); после правок остались 2
ошибки `NotificationPermission` — вопрос владельцу `interviews/interview_004_notifications_permission.md`.

**Сессия 2026-10-14 18:12–… (идёт):** фаза 0 закрыта (своя быстрая модель на Титане), владелец ответил на интервью
№003 («Нормы нет… прогресс бары-кольца в процентах»); фаза 2 — первый срез экранов на Титане касаниями: главный экран,
«Улучшение», обработка с кольцом процентов, «до/после»; правки владельца по ходу (превью без прыжков, (i) диалогами,
предзагрузка модели, центровка, время словами, раскрытие фото на весь экран); своё векторное сердце-иконка. Коммиты
`2155bcc` … `18cb0a3`.

**Сделано в сессии 2026-10-14 15:20–15:45:** ответы владельца на интервью №001 и №002 внесены в план MVP и мастер-план,
интервью закрыты; адаптивная иконка из `icon_v3` (скрипт `tools/make-icon.py`, вариант B); заставка запуска на тёмном поле
(тема `Theme.KARTUN`, `app/src/main/res/values/themes.xml`); `GOAL.md` объявлен архивом владельца (ответ «а»), рабочий слой
цели — выжимка `researches/04_goal_digest.md`. Коммиты `d127cea` … `eed6870` и коммит закрытия.

**Иконка (растр, история):** картинки владельца — в `design/icon/` (`heart_neon.png` — бывшее «Неоновое сердце_ пиксели и глянец.png»,
`icon_v1.png`, `icon_v2.png`, `icon_v3.png`). Адаптивная иконка Android собрана скриптом `tools/make-icon.py` из
`icon_v3` — перерисовки владельца под круг ОС (15:30, «чтобы в круге ОС кропало сердце так, чтобы оно по центру
оказывалось»). Размер — вариант B (владелец в чате ≈ 15:37: «B», после «сликом близко края сердечка к краям круга»): сердце 27 dp
от центра при видимом круге 36 dp, ободок подложки растворён в тёмном стекле; переход к стеклу, который владелец
увидел («виден переход от темного к твоему стеклу»), убран. Стоит на Титане (снимок 15:39 — `design/icon/titan_home_v3.png`);
рядом показан вариант из `heart_neon` того же размера (`design/icon/compare_B.png`); вердикт по виду — за владельцем.
`[AI]` `icon_v2` целиком → `design/icon/store_512.png` для магазина и README (ждёт возражения владельца).
Заставка запуска (Android 12+ рисует на ней только передний слой иконки): фон заставки, подложка значка и окно — в цвет
стекла `#0F000C` по жалобе владельца «на сплеш экране ужас, плохо выглядит, нет фона под сердцем»; снято на Титане до и
после — `design/icon/splash_titan.png`. Сердце на заставке мельче, чем без подложки значка (так делает Android); вариант
крупнее — убрать `windowSplashScreenIconBackgroundColor` — предложен владельцу, ответа нет.
Пересборка иконки: `python tools/make-icon.py` (без `-I`); новая картинка владельца — `--src design/icon/<файл>.png`.
В корне лежит отвергнутая владельцем генерация «Неоновое сердце_ пиксели и глянец.png» («по твоему промпту оно херню
сгенерировало») — вне git, судьбу файла решает владелец.

**Ответы владельца 2026-10-14 15:28** (страница очереди): сеть — одна сборка с `INTERNET`, сеть только по кнопке
«Вставить ссылку» и для моделей; по умолчанию — быстрая модель, x4plus — карточка «Максимальное качество»; шрифт —
Unbounded + Manrope. Внесены в `plans/01_EPIC_mvp.md` и журнал `MASTER_PLAN.md`.

### 2026-10-14 15:11 +03:00 — первая сессия (развёртывание KAIF, фаза 0, макеты) закрыта срочно ✅

Перенесено из `STATUS.md` дословно при мягком закрытии 2026-10-14 ≈ 15:43 +03:00; долг церемоний оплачен этим закрытием.

> ⚠️ Чат закрыт срочно 2026-10-14 15:11 +03:00 по слову владельца «и на этом закроем быстро чат, с этого места в новом чате
> продолжаем»: пропущены церемонии (проход судьи по последним правкам, стрижка STATUS, README, линтеры витрины) — долг
> оплачивает первый `/end-chat-soft`.
>
> Неисправленные ложные утверждения: нет (найденные судьёй исправлены там, где стояли, коммиты `6566e12`, `3e6598e`,
> `f919b4e`).
