<p align="center">
  <img src="design/icon/heart_vector.svg" alt="KARTUN: a glass heart, pixels on the left, smooth gloss on the right" width="200">
</p>

<h1 align="center">KARTUN</h1>

<p align="center"><b>Krinik Android Restoration Tool for Upscaling with Neural Networks</b><br>
Open source · Offline · AI on the device</p>

<p align="center">
  <a href="#english"><img src="https://img.shields.io/badge/English-2C7BE5?style=for-the-badge" alt="English"></a>
  &nbsp;
  <a href="#russian"><img src="https://img.shields.io/badge/Русский-C0392B?style=for-the-badge" alt="Русский"></a>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"></a>
  <a href="https://github.com/Tencent/ncnn"><img src="https://img.shields.io/badge/engine-ncnn%20%2B%20Vulkan-FF1A8C.svg" alt="Engine: ncnn + Vulkan"></a>
  <a href="https://github.com/xinntao/Real-ESRGAN"><img src="https://img.shields.io/badge/models-Real--ESRGAN-6A5ACD.svg" alt="Models: Real-ESRGAN"></a>
  <a href="#english"><img src="https://img.shields.io/badge/platform-Android%2016%2B-3DDC84.svg" alt="Platform: Android 16+"></a>
  <a href="#english"><img src="https://img.shields.io/badge/languages-10-0EA5E9.svg" alt="10 languages"></a>
  <a href="STATUS.md"><img src="https://img.shields.io/badge/status-early%20development-orange.svg" alt="Status: early development"></a>
  <a href="https://github.com/MikalaiKryvusha/KAIF"><img src="https://img.shields.io/badge/framework-KAIF-7F52FF.svg" alt="Framework: KAIF"></a>
</p>

---

<a name="english"></a>

## English

[Читать по-русски →](#russian)

**KARTUN** makes a small or blurry photo big and sharp with a neural network, right on an Android phone or tablet. Pick
a photo, tap one button, drag the line across the picture to compare before and after, then save the result or send it
on. The picture never leaves the device: the network runs on the phone's graphics chip through Vulkan.

<p align="center">
  <img src="docs/screenshots/en/01_home.webp" width="19%" alt="Home screen">
  <img src="docs/screenshots/en/02_enhance.webp" width="19%" alt="Enhance screen with the photo">
  <img src="docs/screenshots/en/04_working.webp" width="19%" alt="The network at work">
  <img src="docs/screenshots/en/05_result.webp" width="19%" alt="Before and after">
  <img src="docs/screenshots/en/06_settings.webp" width="19%" alt="Settings">
</p>

Sections: [1 What it does](#en-1) · [2 Screens](#en-2) · [3 Models and speed](#en-3) · [4 Privacy](#en-4) · [5 Formats and limits](#en-5) · [6 Build](#en-6) · [7 Project map](#en-7) · [8 Status and plans](#en-8) · [9 Credits and licence](#en-9).

<a name="en-1"></a>
### 1. What it does

1. Enlarges a photo 1×, 2×, 4×, 8× or 16× with a Real-ESRGAN neural network. The network restores detail that plain
   stretching does not have: edges, texture, small text.
2. Takes the photo three ways: from the gallery, from any app's «Share» menu (Telegram, the gallery, a browser), or from
   a link — taken from the clipboard or typed in.
3. Estimates the time before the run and shows the progress as a percent ring over the photo's real tiles. The estimate
   learns from the runs on your device.
4. Keeps working when you leave the app: the progress and «Cancel» stay in the notification, «Your photo is ready»
   comes when it is done.
5. Compares before and after over the whole screen: drag the pink line, move the photo with a finger, double-tap to
   zoom in, pinch to zoom further. The panel with the buttons slides away to the edge.
6. Saves into `Pictures/KARTUN` as PNG, JPEG or WebP with a quality slider, always as a new file, or sends the result
   through the system «Share» menu.
7. Speaks ten languages: English, Russian, Spanish, Portuguese, French, German, Chinese, Hindi, Bengali, Arabic. The
   language follows the system or is chosen for KARTUN alone.
8. Works in portrait and landscape; every setting has an (i) button with a short explanation, and the user guide is
   built in.

<a name="en-2"></a>
### 2. Screens

| Screen | What you do there |
|---|---|
| Home | pick a photo, paste a link, or read how to send a photo from another app |
| Enhance | see the photo and its size, pick the model and the scale, start; swipe up to fold the photo into a row, pull the handle down to open it full screen |
| Processing | watch the ring and the tiles fill in, see the time spent and the time left, cancel |
| Result | compare before and after, save, share |
| Settings | GPU acceleration, notifications, language, the user guide, «About» with the device, the author and the licences, the terms of use |

<p align="center">
  <img src="docs/screenshots/en/03_setup.webp" width="24%" alt="Model and scale">
  <img src="docs/screenshots/en/07_manual.webp" width="24%" alt="User guide">
  <img src="docs/screenshots/en/08_about.webp" width="24%" alt="About">
</p>

<a name="en-3"></a>
### 3. Models and speed

Two models ship inside the app; both are Real-ESRGAN under BSD-3 and free for commercial use.

| Model | What it is for | Photo 1024 × 1024 → 4096 × 4096 | Photo 960 × 1280 → 3840 × 5120 |
|---|---|---|---|
| Real-ESRGAN General v3 (default) | photos, fast | 5.8 s | 6.6 s |
| Real-ESRGAN x4plus | photos, the richest texture | 89 s | 105 s |

Measured on a HEADWOLF Titan 1 tablet: MediaTek MT8792, Mali-G615 MC6 graphics, Vulkan 1.3, Android 16. Without the
graphics chip the fast model takes 85 s for the same 1024 × 1024 photo. The fast model is KARTUN's own conversion of the
original weights; its output differs from the original PyTorch network by 52.3 dB PSNR, which the eye does not see.

<a name="en-4"></a>
### 4. Privacy

1. The neural network runs on the device. No photo is uploaded anywhere.
2. The network permission is used for one thing only: downloading a picture from a link you paste.
3. No account, no ads, no analytics, no telemetry.
4. The original is never changed: every result is a new file.

<a name="en-5"></a>
### 5. Formats and limits

| | |
|---|---|
| Input | any picture format Android itself opens: JPEG, PNG, WebP, HEIC, AVIF, GIF, BMP and others |
| Output | PNG (lossless), JPEG and WebP with a quality slider; WebP up to 16 383 px per side |
| Largest input photo | 100 MP |
| Largest result | 500 MP |
| 8× and 16× | two passes of the network: four to sixteen times longer than 2×–4× |
| Translations | English and Russian are written by hand; the other eight are AI translations checked by the author |

<a name="en-6"></a>
### 6. Build

There are no ready APKs yet; build the app from source on Windows. You need JDK 21, the Android SDK with platform 36,
NDK `29.0.14206865` and CMake `3.31.6`, Node.js and Python 3.

```powershell
git clone https://github.com/MikalaiKryvusha/KARTUN.git
cd KARTUN
node tools/fetch-ncnn.mjs                     # ncnn 20260526 for Android with Vulkan, hash-checked
```

Put the two models in place (sources and hashes: [docs/model-licenses.md](docs/model-licenses.md)):

1. Download `realesrgan-ncnn-vulkan-20220424-windows.zip` from the
   [Real-ESRGAN release v0.2.5.0](https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0) and unpack it into
   `engine/third_party/realesrgan-win/`.
2. Download `realesr-general-x4v3.pth` from the same release into `engine/third_party/` and convert it (Python with
   `torch`, `pnnx` and `numpy`):
   `python -I tools/convert-model.py engine/third_party/realesr-general-x4v3.pth engine/third_party/converted`

```powershell
node tools/stage-models.mjs                   # copies the models into the APK and checks every hash
.\gradlew.bat assembleDebug                   # app/build/outputs/apk/debug/app-debug.apk
```

<a name="en-7"></a>
### 7. Project map

| Path | What lives there |
|---|---|
| `app/` | the Android app: Kotlin, Jetpack Compose, the screens, the background service, saving and sharing |
| `engine/` | the native engine: C++ over ncnn behind one JNI boundary, tiles, Vulkan with a CPU fallback |
| `tools/` | build and test helpers: ncnn download, model conversion and staging, translations, the icon, device runs |
| `docs/` | licences of everything shipped, model provenance, the README screenshots |
| `design/` | the icon sources and the screen mockups |

<a name="en-8"></a>
### 8. Status and plans

Early development: the whole main path works on a real device, no public release yet. Next: a model catalogue with
downloadable models and preview pictures, writing very large results in strips, a release on GitHub. The roadmap is
[MASTER_PLAN.md](MASTER_PLAN.md), the current state is [STATUS.md](STATUS.md).

<a name="en-9"></a>
### 9. Credits and licence

1. [Upscayl](https://github.com/upscayl/upscayl) — the desktop app whose simplicity KARTUN follows. No Upscayl code is
   used: KARTUN's engine is written from scratch.
2. [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN) by Xintao Wang — the models (BSD-3).
3. [ncnn](https://github.com/Tencent/ncnn) by Tencent — the neural network engine (BSD-3).
4. [Unbounded](https://fonts.google.com/specimen/Unbounded) and [Manrope](https://fonts.google.com/specimen/Manrope) —
   the fonts (SIL Open Font License 1.1).
5. KARTUN is licensed under MIT ([LICENSE](LICENSE)); everything it ships is listed in
   [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md). The project is run with [KAIF](https://github.com/MikalaiKryvusha/KAIF),
   a framework for work with an AI agent.

Author: Mikalai Kryvusha (Krinik).

---

<a name="russian"></a>

## Русский

[Read in English →](#english)

**KARTUN** делает маленькое или размытое фото большим и чётким с помощью нейросети прямо на Android-телефоне или
планшете. Выберите фото, нажмите одну кнопку, проведите линию по картинке, чтобы сравнить «до» и «после», затем
сохраните результат или отправьте дальше. Снимок не покидает устройство: нейросеть работает на видеочипе телефона через
Vulkan.

<p align="center">
  <img src="docs/screenshots/ru/01_home.webp" width="19%" alt="Главный экран">
  <img src="docs/screenshots/ru/02_enhance.webp" width="19%" alt="Экран «Улучшение» с фото">
  <img src="docs/screenshots/ru/04_working.webp" width="19%" alt="Нейросеть работает">
  <img src="docs/screenshots/ru/05_result.webp" width="19%" alt="До и после">
  <img src="docs/screenshots/ru/06_settings.webp" width="19%" alt="Настройки">
</p>

Разделы: [1 Что умеет](#ru-1) · [2 Экраны](#ru-2) · [3 Модели и скорость](#ru-3) · [4 Приватность](#ru-4) · [5 Форматы и ограничения](#ru-5) · [6 Сборка](#ru-6) · [7 Карта проекта](#ru-7) · [8 Состояние и планы](#ru-8) · [9 Благодарности и лицензия](#ru-9).

<a name="ru-1"></a>
### 1. Что умеет

1. Увеличивает фото в 1×, 2×, 4×, 8× или 16× нейросетью Real-ESRGAN. Нейросеть восстанавливает то, чего нет при
   обычном растяжении: края, фактуру, мелкий текст.
2. Берёт фото тремя путями: из галереи, из меню «Поделиться» любого приложения (Telegram, галерея, браузер) или по
   ссылке — из буфера обмена или набранной вручную.
3. Оценивает время до запуска и показывает ход работы кольцом процентов поверх настоящих тайлов фото. Оценка учится на
   прогонах вашего устройства.
4. Продолжает работу, когда вы выходите из приложения: прогресс и «Отменить» остаются в уведомлении, по готовности
   приходит «Фото готово».
5. Сравнивает «до» и «после» на весь экран: тяните розовую линию, двигайте фото пальцем, двойное касание увеличивает,
   двумя пальцами — ещё крупнее. Панель с кнопками уезжает за край экрана.
6. Сохраняет в `Pictures/KARTUN` в PNG, JPEG или WebP с ползунком качества, всегда новым файлом, или отправляет
   результат через системное меню «Поделиться».
7. Говорит на десяти языках: английском, русском, испанском, португальском, французском, немецком, китайском, хинди,
   бенгальском, арабском. Язык берётся из системы или выбирается только для KARTUN.
8. Работает в портретной и альбомной ориентации; у каждой настройки есть кнопка (i) с коротким пояснением, руководство
   пользователя встроено.

<a name="ru-2"></a>
### 2. Экраны

| Экран | Что здесь делают |
|---|---|
| Главный | выбирают фото, вставляют ссылку или читают, как отправить фото из другого приложения |
| «Улучшение» | видят фото и его размер, выбирают модель и масштаб, запускают; свайп вверх сворачивает фото в строку, хендл вниз раскрывает его на весь экран |
| Обработка | следят за кольцом и тайлами, видят, сколько прошло и сколько осталось, могут отменить |
| Результат | сравнивают «до» и «после», сохраняют, делятся |
| Настройки | ускорение GPU, уведомления, язык, руководство пользователя, «О приложении» с устройством, автором и лицензиями, условия использования |

<p align="center">
  <img src="docs/screenshots/ru/03_setup.webp" width="24%" alt="Модель и масштаб">
  <img src="docs/screenshots/ru/07_manual.webp" width="24%" alt="Руководство пользователя">
  <img src="docs/screenshots/ru/08_about.webp" width="24%" alt="О приложении">
</p>

<a name="ru-3"></a>
### 3. Модели и скорость

В приложение встроены две модели; обе — Real-ESRGAN под BSD-3, обе можно использовать в коммерции.

| Модель | Для чего | Фото 1024 × 1024 → 4096 × 4096 | Фото 960 × 1280 → 3840 × 5120 |
|---|---|---|---|
| Real-ESRGAN General v3 (по умолчанию) | фото, быстро | 5,8 с | 6,6 с |
| Real-ESRGAN x4plus | фото, самая живая фактура | 89 с | 105 с |

Замер на планшете HEADWOLF Titan 1: MediaTek MT8792, графика Mali-G615 MC6, Vulkan 1.3, Android 16. Без видеочипа
быстрая модель обрабатывает то же фото 1024 × 1024 за 85 с. Быстрая модель — собственная конвертация KARTUN из исходных
весов; её результат отличается от оригинальной нейросети PyTorch на 52,3 дБ PSNR, глазом разницы не видно.

<a name="ru-4"></a>
### 4. Приватность

1. Нейросеть работает на устройстве. Фото никуда не загружается.
2. Разрешение на сеть нужно для одного: скачать картинку по ссылке, которую вы вставили.
3. Нет аккаунта, рекламы, аналитики и телеметрии.
4. Оригинал не меняется: каждый результат — новый файл.

<a name="ru-5"></a>
### 5. Форматы и ограничения

| | |
|---|---|
| Вход | любой формат картинки, который открывает сам Android: JPEG, PNG, WebP, HEIC, AVIF, GIF, BMP и другие |
| Выход | PNG без потерь, JPEG и WebP с ползунком качества; WebP — до 16 383 пикселей по стороне |
| Самое большое исходное фото | 100 Мп |
| Самый большой результат | 500 Мп |
| 8× и 16× | два прохода нейросети: в 4–16 раз дольше, чем 2×–4× |
| Переводы | английский и русский написаны руками; остальные восемь — переводы ИИ, проверенные автором |

<a name="ru-6"></a>
### 6. Сборка

Готовых APK пока нет; соберите приложение из исходников под Windows. Нужны JDK 21, Android SDK с платформой 36,
NDK `29.0.14206865` и CMake `3.31.6`, Node.js и Python 3.

```powershell
git clone https://github.com/MikalaiKryvusha/KARTUN.git
cd KARTUN
node tools/fetch-ncnn.mjs                     # ncnn 20260526 для Android с Vulkan, со сверкой хеша
```

Положите на место две модели (источники и хеши — в [docs/model-licenses.md](docs/model-licenses.md)):

1. Скачайте `realesrgan-ncnn-vulkan-20220424-windows.zip` из
   [релиза Real-ESRGAN v0.2.5.0](https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0) и распакуйте в
   `engine/third_party/realesrgan-win/`.
2. Скачайте `realesr-general-x4v3.pth` из того же релиза в `engine/third_party/` и сконвертируйте (Python с `torch`,
   `pnnx` и `numpy`):
   `python -I tools/convert-model.py engine/third_party/realesr-general-x4v3.pth engine/third_party/converted`

```powershell
node tools/stage-models.mjs                   # кладёт модели в APK и сверяет каждый хеш
.\gradlew.bat assembleDebug                   # app/build/outputs/apk/debug/app-debug.apk
```

<a name="ru-7"></a>
### 7. Карта проекта

| Путь | Что там |
|---|---|
| `app/` | Android-приложение: Kotlin, Jetpack Compose, экраны, фоновая служба, сохранение и «Поделиться» |
| `engine/` | нативный движок: C++ поверх ncnn за одной границей JNI, тайлы, Vulkan с запасным путём на процессоре |
| `tools/` | помощники сборки и проверки: загрузка ncnn, конвертация и раскладка моделей, переводы, иконка, прогоны на устройстве |
| `docs/` | лицензии всего, что едет в приложении, происхождение моделей, скриншоты README |
| `design/` | исходники иконки и макеты экранов |

<a name="ru-8"></a>
### 8. Состояние и планы

Ранняя разработка: весь основной путь работает на настоящем устройстве, публичного релиза пока нет. Дальше — каталог
моделей со скачиванием и картинками-превью, запись очень больших результатов полосами, релиз на GitHub. Дорожная
карта — [MASTER_PLAN.md](MASTER_PLAN.md), текущее состояние — [STATUS.md](STATUS.md).

<a name="ru-9"></a>
### 9. Благодарности и лицензия

1. [Upscayl](https://github.com/upscayl/upscayl) — настольное приложение, простоте которого следует KARTUN. Код Upscayl
   не используется: движок KARTUN написан с нуля.
2. [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN) — модели, автор Xintao Wang (BSD-3).
3. [ncnn](https://github.com/Tencent/ncnn) — движок нейросетей от Tencent (BSD-3).
4. [Unbounded](https://fonts.google.com/specimen/Unbounded) и [Manrope](https://fonts.google.com/specimen/Manrope) —
   шрифты (SIL Open Font License 1.1).
5. KARTUN распространяется по лицензии MIT ([LICENSE](LICENSE)); всё, что едет в приложении, перечислено в
   [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md). Проект ведётся по [KAIF](https://github.com/MikalaiKryvusha/KAIF) —
   фреймворку работы с ИИ-агентом.

Автор: Николай Кривуша (Krinik).
