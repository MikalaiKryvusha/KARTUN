# Interview #005 — Станция для ИИ-агентов: как она открывается и кого пускает

> Topic: шесть решений об API для ИИ-агентов (ADB и локальная сеть), которые меняют то, что видит пользователь и кто может пользоваться его планшетом; фаза A (ADB) ждёт только Q3
> Source of the idea: слово владельца 2026-10-15 ≈ 23:55 +03:00 «API для ИИ-агентов по ADB и HTTPS (ideas/02); Сейчас делаем!» и ≈ 23:58 «чтобы IP девайса и порт приложения можно было дать, и этого хватало для работы по локальной сети»; `plans/06_EPIC_agent_api.md` §6; `researches/10_agent_api_recon.md` §2
> Created: 2026-10-16 00:59 +03:00
> Status: ❓ ждёт ответа владельца

## Context / what I already found

План API готов: `plans/06_EPIC_agent_api.md` (эпик, фазы A и B) и `plans/07_epic06_A_adb_api.md` (фаза A по шагам).
Агент на компьютере кладёт фото командой `adb push` в папку KARTUN, KARTUN считает его в фоне и кладёт результат рядом,
агент забирает результат `adb pull`. В фазе B то же самое идёт по Wi-Fi: агенту дают адрес планшета и порт.
Разведка — как это делают Ollama, ComfyUI, AUTOMATIC1111 и LocalSend — в `researches/10` §2.

## QUESTIONS

### Q1. Шифровать ли снимки в домашней сети (HTTP или HTTPS)?

**Answer target:** `plans/06_EPIC_agent_api.md` §6.1, фаза B

**Origin:** ваши слова «чтобы IP девайса и порт приложения можно было дать, и этого хватало» — рекомендация A сделана по ним.

<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "https|ключ доступа|tailscale|mcp|станци" interviews/ GOAL.md MASTER_PLAN.md ideas/02_agent_api_adb_and_lan_workstation.md → 14 hits · read: interview_001 Q1 (network only by the user's button; HTTPS server named, not decided), ideas/02 · prior: unrelated — no owner word on encryption or a key -->

- **A) (recommended) Без шифрования (HTTP): адреса и порта достаточно**
  - Situation. Станция включена на Титане, адрес на экране: `192.168.1.3:8765`.
  - Action. Агент на компьютере отправляет фото командой `curl -F image=@photo.jpg http://192.168.1.3:8765/v1/upscale?wait=1 -o out.png`.
  - Result. Через несколько секунд рядом лежит `out.png`, увеличенный в 4 раза; никаких настроек на компьютере. Цена: снимок идёт по Wi-Fi открыто — в домашней сети его может перехватить тот, кто знает пароль Wi-Fi, в открытой сети кафе — любой рядом.
  - Check. На Титане команда выше даёт PNG 4× за одну строку; `curl -v` показывает `HTTP/1.1 200`.
- **B) С шифрованием (HTTPS, свой сертификат)**
  - Situation. То же, адрес `https://192.168.1.3:8765`.
  - Action. Агент отправляет фото той же командой, но с `-k` или с отпечатком сертификата, который показан на экране планшета.
  - Result. Снимок по Wi-Fi зашифрован; на компьютере нужен лишний флаг или переписанный отпечаток.
  - Check. Без `-k` команда отказывает с ошибкой сертификата; с `-k` — PNG 4×.
- **C) your own answer** —

### Q2. Нужен ли ключ доступа?

**Answer target:** `plans/06_EPIC_agent_api.md` §6.2, фаза B

**Origin:** та же фраза «IP девайса и порт… и этого хватало»; цена названа ниже.


<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "ключ доступа|токен|bearer|требовать ключ" interviews/interview_00[1-4]*.md GOAL.md MASTER_PLAN.md plans/ (plans 06-07 of this epic excluded) → 0 hits · read: none · prior: none — no owner word on an access key -->

- **A) (recommended) Без ключа, а в настройках переключатель «Требовать ключ»**
  - Situation. Станция включена, переключатель «Требовать ключ» выключен.
  - Action. Любой компьютер в той же Wi-Fi отправляет задание на адрес и порт.
  - Result. Станция его выполняет; чужие результаты он не видит, но может занять видеочип и тратить батарею. Включив «Требовать ключ», вы закрываете станцию для всех, кто не знает ключа с экрана.
  - Check. С выключенным ключом `curl` без заголовков получает PNG; с включённым — `401`.
- **B) Ключ всегда**
  - Situation. Станция включена.
  - Action. Агент отправляет задание без ключа, потом с заголовком `Authorization: Bearer <ключ с экрана>`.
  - Result. Без ключа — отказ; с ключом — PNG. Агенту каждый раз нужно переписать ключ с экрана планшета.
  - Check. `curl` без заголовка получает `401`, с заголовком — PNG.
- **C) your own answer** —

### Q3. Пускать ли агентов по кабелю (ADB) в обычной сборке «KARTUN»?

**Answer target:** `plans/07_epic06_A_adb_api.md` шаг 2; фаза A ждёт этот ответ

**Origin:** вопрос агента: обычная сборка ставится людям, и дверь для агентов в ней видна им через отладку.


<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "adb.*(релиз|release|обычн)|только в debug|debug kartun" interviews/interview_00[1-4]*.md GOAL.md MASTER_PLAN.md plans/ (plans 06-07 of this epic excluded) → 1 hits · read: plans/03_epic01_F2_screens.md step 12 · prior: unrelated — step 12 is the agent's own rule for the engine PROBE (debug builds only), not for the agent API; no owner word -->

- **A) (recommended) В обеих сборках, без переключателя**
  - Situation. У пользователя обычный «KARTUN», он включил отладку по USB и подключил компьютер.
  - Action. Агент на компьютере вызывает `node tools/kartun-cli.mjs upscale photo.jpg`.
  - Result. KARTUN увеличивает фото в фоне, результат появляется на компьютере. Без включённой отладки дверь закрыта: включить её может только сам владелец телефона.
  - Check. На Титане с релизной сборкой команда выше даёт PNG 4×.
- **B) Только в «Debug KARTUN»**
  - Situation. То же, но у пользователя обычный «KARTUN».
  - Action. Агент вызывает ту же команду.
  - Result. Отказ «станция есть только в Debug KARTUN»; агентам нужна отладочная сборка.
  - Check. С релизной сборкой CLI печатает ошибку, с отладочной — PNG.
- **C) your own answer** —

### Q4. Помнит ли станция, что была включена?

**Answer target:** `plans/06_EPIC_agent_api.md` §6.4, фаза B

**Origin:** вопрос агента: от ответа зависит, слушает ли планшет сеть без вашего ведома.


<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "станци.*(включ|помни|перезагр)|переключател.*станц" interviews/interview_00[1-4]*.md GOAL.md MASTER_PLAN.md plans/ (plans 06-07 of this epic excluded) → 0 hits · read: none · prior: none -->

- **A) (recommended) Нет: после закрытия KARTUN и перезагрузки станция выключена, включение — рукой**
  - Situation. Вчера вы включили «Станцию для ИИ-агентов», сегодня планшет перезагрузился.
  - Action. Агент шлёт задание на адрес и порт.
  - Result. Ответа нет, пока вы снова не включите станцию в настройках KARTUN.
  - Check. После `adb reboot` порт закрыт (`curl` — отказ соединения), после включения — `200`.
- **B) Да: станция поднимается при каждом запуске KARTUN**
  - Situation. То же.
  - Action. Вы открываете KARTUN, агент шлёт задание.
  - Result. Станция отвечает сразу; в шторке висит уведомление «Станция включена».
  - Check. После перезагрузки и запуска KARTUN `curl` получает `200` без касаний настроек.
- **C) your own answer** —

### Q5. Только домашний Wi-Fi или ещё и Tailscale?

**Answer target:** `plans/06_EPIC_agent_api.md` §6.5, фаза B

**Origin:** вопрос агента: ваш Титан доступен и через Tailscale (`100.99.111.48`).


<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "tailscale.*(станц|сервер|api)|частные адрес|домашн.*сет" interviews/interview_00[1-4]*.md GOAL.md MASTER_PLAN.md plans/ (plans 06-07 of this epic excluded) → 0 hits · read: none · prior: none — Tailscale appears only as the agent's own route to the Titan (HOUSE_RULES), not as a product decision -->

- **A) (recommended) Только домашняя сеть (адреса 192.168.x.x, 10.x.x.x, 172.16–31.x.x)**
  - Situation. Вы вне дома, компьютер в сети Tailscale.
  - Action. Агент шлёт задание на `100.99.111.48:8765`.
  - Result. Отказ: станция отвечает только в домашней сети.
  - Check. Запрос с адреса `100.x` получает `403`, с `192.168.x` — `200`.
- **B) Плюс сеть Tailscale**
  - Situation. То же.
  - Action. Агент шлёт задание на `100.99.111.48:8765`.
  - Result. Станция работает и из дома, и вне его, если оба устройства в вашей сети Tailscale.
  - Check. Запрос с адреса `100.x` получает `200`.
- **C) your own answer** —

### Q6. Делать ли сразу MCP-сервер для Claude Code?

**Answer target:** `plans/06_EPIC_agent_api.md` §6.6

**Origin:** вопрос агента: MCP даёт Claude Code инструмент «увеличить фото» без чтения документа.


<!-- archaeology: LC_ALL=C.UTF-8 grep -rniE "mcp" interviews/interview_00[1-4]*.md GOAL.md MASTER_PLAN.md plans/ (plans 06-07 of this epic excluded) → 0 hits · read: none · prior: none -->

- **A) (recommended) Позже, отдельной фазой C: в фазе A агенту хватает CLI и документа `docs/agent-api.md`**
  - Situation. Claude Code в терминале получает задачу «увеличь эти фото на планшете».
  - Action. Он читает `docs/agent-api.md` и зовёт `node tools/kartun-cli.mjs upscale …`.
  - Result. Фото увеличены; MCP появится после фаз A и B.
  - Check. Новая сессия агента с одним документом выполняет `info` и `upscale` на Титане.
- **B) Сразу после фазы A**
  - Situation. То же.
  - Action. Claude Code видит в списке инструмент `kartun_upscale` и вызывает его.
  - Result. Без чтения документа; фаза B (сеть) сдвигается на время MCP.
  - Check. `claude mcp list` показывает `kartun`, вызов инструмента возвращает путь к PNG.
- **C) your own answer** —
