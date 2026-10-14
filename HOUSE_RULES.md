# Правила дома — KARTUN

> Местный закон проекта KARTUN (ярус 4 таксономии `AGENT_GUIDE.md`): правила владельца, внешние системы, стенды,
> рецепты и инструменты, с которыми агент работает в KARTUN. Метод работы агента — `AGENT_GUIDE.md`. Скелет —
> `.kaif/_house-rules-template.md`; бюджета размера нет, `/resume` читает файл на входе в сессию.

**Создан:** 2026-10-14 · **Владелец:** Mikalai Kryvusha · **Перенесено из руководства:** пока ничего

## 1. Правила владельца

### R1. Титан — стенд агента: ставь на него свои сборки сам

1. Устанавливай отладочные и тестовые сборки KARTUN на планшет Titan 1 (раздел 3) без отдельного согласия на каждую установку.
2. `[AI]` Чисти за собой тестовые результаты на планшете: место на нём заканчивается (раздел 3).
- **Исключение:** чужие приложения и данные владельца на планшете не трогай. Удалять и переустанавливать без
  отдельного слова владельца можно только пакеты KARTUN.

[OWNER] 2026-10-14 ≈ 13:33 +03:00 · чат сессии развёртывания KAIF: «титан в твоем распоряжении, можешь ставить на него сборки»

### R2. Ключи подписи stage и prod живут в `.env` вне git

1. Храни ключи и пароли подписи stage- и prod-сборок только в `.env` в корне проекта на машине владельца; файл, `.env.*`,
   `*.jks`, `*.keystore`, `keystore.properties` уже в `.gitignore` (строки добавлены 2026-10-14 13:34 +03:00, до появления файлов).
2. В репозиторий клади только `.env.example` — имена переменных без значений.
3. Не печатай значения секретов в чат, журналы, отчёты и коммиты; сборка читает их сама.
- **Исключение:** нет. Копию ключей владелец ведёт сам в своём секретном документе — агенту он не нужен.

[OWNER] 2026-10-14 ≈ 13:33 +03:00 · чат сессии развёртывания KAIF: «Ключи от стейдж и прод сборок будут храниться на этом компе в проекте в .env файле вне гита, и еще я в секретный документ запишу»

### R3. Название приложения на телефоне: stage — «Stage KARTUN», prod — «KARTUN»

1. Stage-сборка (тип сборки `stage`, пакет `com.kriniks.kartun.stage`) называется «Stage KARTUN».
2. Prod-сборка (тип сборки `release`, пакет `com.kriniks.kartun`) называется «KARTUN».
3. `[AI]` Отладочная сборка (тип `debug`, пакет `com.kriniks.kartun.debug`) называется «Debug KARTUN».
- **Исключение:** нет. Названия задаются в `app/build.gradle.kts` через `resValue("string", "app_name", …)`.

[OWNER] 2026-10-14 ≈ 15:15 +03:00 · чат, дословно: «стейдж сборки в названии должны иметь слово Stage KARTUN» · «прод просто KARTUN»

## 2. Внешние системы и доступ

| Система | Что агент там делает | Точка входа | Где лежат учётные данные |
|---|---|---|---|
| GitHub `MikalaiKryvusha/KARTUN` | коммиты, пуши, релизы (релиз — только по слову владельца) <!-- attribution-ok: KAIF canon AGENT_GUIDE.md → Git workflow: releases wait for the owner's quoted words --> | `gh` CLI, `git push` | `gh auth` этой машины |
| GitHub `MikalaiKryvusha/KAIF` | полевые отчёты и тикеты KAIF (постоянное разрешение владельца KAIF, issues #15 и #78) | `node .kaif/kaif-core.mjs report <файл>` | `gh auth` этой машины |
| Сеть Tailscale владельца | `adb` до тестовых устройств | служба `Tailscale` (`C:\Program Files\Tailscale\tailscale.exe status`) | вход держит служба; агенту ничего не нужно |

## 3. Стенды, окружения и устройства

| Стенд / устройство | Для чего | Как агент до него добирается | Известные ловушки |
|---|---|---|---|
| Планшет HEADWOLF Titan 1 (`Titan_1`, `kriniks-titan1`): Android 16 (API 36, патч 2026-05-05), MediaTek MT8792 (платформа `mt6897`, класс Dimensity 8300), только `arm64-v8a`, GPU Mali-G615 MC6, Vulkan 1.3 + `vulkan.compute`, ОЗУ 11,8 ГБ, Java-куча 256 МБ (`largeHeap` 512 МБ), экран 1600×2560 | главный стенд MVP: установка APK, прогоны апскейла на Vulkan и CPU, logcat, скриншоты | `D:\Android\Sdk\platform-tools\adb.exe connect 100.99.111.48:<порт>` — порт с главного экрана «Беспроводной отладки» (2026-10-14 был `44421`; меняется после перезагрузки — спроси у владельца); сопряжение уже сделано 2026-09-26 в KAST | ⚠ свободно 27 ГБ из 225 (88 %, проба 2026-10-14 14:30; до удаления записей экрана было 18 ГБ) — тестовые 192-Мп результаты чистить за собой; только `adb.exe` из `D:\Android\Sdk` стоит в исключениях NordVPN, `C:\adb\adb.exe` до `100.x` может не дойти; ошибка `10061` (отказ в соединении) значит, что порт сменился: спроси новый порт у владельца |

## 4. Досье окружения — факты машины

> **Досье окружения.** Снято: `2026-10-14` (частично) · Пересборка: `/refresh-context` → шаг досье · **Факты старше
> четырёх недель — ГИПОТЕЗЫ: перепроверь пробой перед опорой на них.**

| Факт | Значение | Проба |
|---|---|---|
| ОС | Windows 11 Pro 10.0.26200 | `ver` |
| Оболочки | PowerShell 5.1 (основная), Git Bash | — |
| Node.js | v24.15.0 | `node -v` |
| Java в PATH | OpenJDK 21.0.11 (Microsoft), `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot` | `java -version` |
| `JAVA_HOME` | ⚠ `C:\Users\krinik\.bubblewrap\jdk\jdk-17.0.11+9` — каталога нет (в проекте KAST `JAVA_HOME` указывает на этот путь и тоже не работает); для Gradle задавай JDK 21 явно | `echo $JAVA_HOME` |
| Android SDK | `D:\Android\Sdk` (переменная `ANDROID_HOME` НЕ задана): platforms `android-36`, `android-37.0`; build-tools `35.0.0`; NDK `27.0.12077973` и `29.0.14206865` (r29 — им собран ncnn 20260526); CMake `3.31.6` (поставлены агентом 2026-10-14 13:51) | `ls /d/Android/Sdk/{ndk,cmake,build-tools,platforms}` |
| Установка пакетов SDK | `sdkmanager` объявлен устаревшим (замена — `android sdk`), но работает; имена пакетов — через `/`: `sdkmanager.bat --sdk_root=D:\Android\Sdk cmake/3.31.6 ndk/29.0.14206865` (через `;` их режет `cmd`) | `D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat --list` |
| Gradle | обёртка проекта, Gradle 9.8.1; AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00 (версии сверены 2026-10-14) | `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml` |
| Android Studio | не установлен — сборка только из командной строки | `ls "C:/Program Files/Android"` |
| adb | `D:\Android\Sdk\platform-tools\adb.exe` 1.0.41 (рабочий для Tailscale); есть ещё `C:\adb\adb.exe` — первый в PATH | `which -a adb` |
| Остальное (кодировки, локали, `tar`/`curl`/`find`) | `— not probed yet —` | |

## 5. Маршруты, рецепты и соглашения — указатель своей работы

| Поверхность | Где живёт работа | Что покрывает |
|---|---|---|
| `adb` к Титану через Tailscale при NordVPN | `D:\work\ai_sandbox\KAST\HOUSE_RULES.md` (строки 147–156 и 224, только чтение) | рецепт сопряжения, раздельное туннелирование NordVPN (в исключениях ТОЛЬКО `adb.exe` из `D:\Android\Sdk`), `adb kill-server` после правки исключений |
| Сборка Android из командной строки на этой машине | `D:\work\ai_sandbox\KAST\HOUSE_RULES.md` строка 233 (образец) | PowerShell: `$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'; $env:ANDROID_HOME='D:\Android\Sdk'; $env:GRADLE_USER_HOME='D:\Android\gradle-home'; .\gradlew.bat assembleDebug --console=plain` |
| Стиль Криника (цвета, компоненты, сборка) | `researches/01_krinikcam_style_and_stack.md` | палитра `#FF1A8C` / `#0D0D0D`, компоненты Compose, версии инструментов KrinikCam |
| UI/UX и движок Upscayl 2.15.0 | `researches/02_upscayl_desktop_reverse_engineering.md` | экраны, настройки, аргументы `upscayl-bin`, модели, ловушки |
| Пуш и аутентификация | `gh auth setup-git` | non-fast-forward → `git pull --rebase` → повтор |

## 6. Инструменты проекта

| Команда | Что делает | От чего страхует |
|---|---|---|
| PowerShell: сборка из раздела 5 (`.gradlew.bat assembleDebug`) | собирает APK с нативным движком | — |
| `node tools/fetch-ncnn.mjs` | скачивает ncnn `20260526` android-vulkan в `engine/third_party/` и сверяет SHA-256 | подмена или порча пакета ncnn |
| `node tools/titan-probe.mjs --in <файл> --model <модель> --gpu true --tile 200 [--ref <png>]` | запускает пробу движка на Титане через adb, печатает JSON и PSNR | заявление «движок работает» без прогона на устройстве |
| `.venv-convert/Scripts/python.exe -I tools/convert-model.py <веса.pth> <папка>` | конвертирует веса SRVGGNetCompact (BSD-3) в ncnn с именами входа и выхода движка | — |
| `node tools/shoot-mockups.mjs` | снимает экраны `design/kartun-mockups.html` в `design/flow-map/screens/` (вне git) | лист KUIPS из устаревших кадров |
| `node D:/work/ai_sandbox/ndim/tools/build-flow-map.mjs` (из корня KARTUN) | собирает лист KUIPS `design/flow-map.html` сборщиком NDim на месте (код AGPL не копируется, лист вне git) | — |
| `python tools/make-icon.py` (системный Python с Pillow; `-I` не годится — Pillow стоит в пользовательских пакетах) | собирает адаптивную иконку Android (`app/src/main/res/mipmap-*`), `design/icon/store_512.png` и превью масок `design/icon/preview_masks.png` из картинок владельца в `design/icon/` | ручная правка производных файлов иконки; сердце, срезанное маской лаунчера |
| `node tools/stamp-guard.mjs --staged` (git-хук `.git/hooks/pre-commit` этого клона) | отказывает коммиту с отметкой времени позже текущего момента | время, написанное на глаз (EXP-0004, EXP-0005) |

## 7. Знания о продукте

Пока нет.
