<p align="center">
  <img src="design/icon/heart_vector.svg" alt="KARTUN: a glass heart, pixels on the left, smooth gloss on the right" width="220">
</p>

# KARTUN — Krinik Android Restoration Tool for Upscaling with Neural Networks

<p align="center">
  <a href="#english"><img src="https://img.shields.io/badge/English-2C7BE5?style=for-the-badge" alt="English"></a>
  &nbsp;
  <a href="#russian"><img src="https://img.shields.io/badge/Русский-C0392B?style=for-the-badge" alt="Русский"></a>
</p>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Engine](https://img.shields.io/badge/engine-ncnn%20%2B%20Vulkan-FF1A8C.svg)](https://github.com/Tencent/ncnn)
[![Models](https://img.shields.io/badge/models-Real--ESRGAN-6A5ACD.svg)](https://github.com/xinntao/Real-ESRGAN)
[![Platform](https://img.shields.io/badge/platform-Android%2016%2B-3DDC84.svg)](#english)
[![Status](https://img.shields.io/badge/status-early%20development-orange.svg)](STATUS.md)
[![Framework](https://img.shields.io/badge/framework-KAIF-7F52FF.svg)](https://github.com/MikalaiKryvusha/KAIF)

---

<a name="english"></a>

## English

[Читать по-русски →](#russian)

**KARTUN** enlarges photos with a neural network right on an Android phone: pick a photo, tap one button, compare
before and after with a slider you grab anywhere, save it or share it. Open source · offline · AI on the device.

Sections: 1 What it does · 2 Status · 3 Build · 4 Credits and licences.

### 1. What it does

1. Upscales a photo 1×, 2×, 4×, 8× or 16× with Real-ESRGAN models through ncnn on the GPU (Vulkan), with the CPU as
   a fallback. The picture never leaves the phone.
2. Takes a photo from the gallery, from another app's «Share», or from a link in the clipboard.
3. Shows the progress as a percent ring over the photo's real tiles, with a time estimate before the run.
4. Saves into `Pictures/KARTUN` as PNG, JPEG or WebP (with quality), or shares through the system menu.
5. Speaks ten languages; the Krinik style: near-black, one pink accent.

### 2. Status

Early development. Measured on a HEADWOLF Titan 1 (Mali-G615): a 1 MP photo in 5.8 s with the fast model. Known
limits: photos up to 12.5 MP and results up to 200 MP; translations beyond English and Russian are drafts.

### 3. Build

```powershell
node tools/fetch-ncnn.mjs          # ncnn 20260526 android-vulkan, hash-checked
node tools/stage-models.mjs        # the shipped models into the APK, hash-checked
.\gradlew.bat assembleDebug        # JDK 21, Android SDK with NDK r29 and CMake 3.31
```

### 4. Credits and licences

KARTUN is MIT. It ships ncnn (BSD-3, Tencent), the Real-ESRGAN models (BSD-3, Xintao Wang), the Unbounded and Manrope
fonts (OFL 1.1) — details in [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md) and [docs/model-licenses.md](docs/model-licenses.md).

---

<a name="russian"></a>

## Русский

[Read in English →](#english)

**KARTUN** увеличивает фотографии нейросетью прямо на Android-телефоне: выберите фото, нажмите одну кнопку, сравните
«до» и «после» ползунком, который хватается в любом месте, сохраните или поделитесь. Открытый код · офлайн · нейросеть
на устройстве.

Разделы: 1 Что умеет · 2 Состояние · 3 Сборка · 4 Авторы и лицензии.

### 1. Что умеет

1. Увеличивает фото в 1×, 2×, 4×, 8× или 16× моделями Real-ESRGAN через ncnn на видеочипе (Vulkan), процессор — запасной
   путь. Снимок не покидает телефон.
2. Берёт фото из галереи, из «Поделиться» другого приложения или по ссылке из буфера обмена.
3. Показывает прогресс кольцом процентов поверх настоящих тайлов фото и оценку времени до запуска.
4. Сохраняет в `Pictures/KARTUN` в PNG, JPEG или WebP (с качеством) или отправляет через системное меню.
5. Говорит на десяти языках; стиль Криника: почти чёрный фон и один розовый акцент.

### 2. Состояние

Ранняя разработка. Замер на HEADWOLF Titan 1 (Mali-G615): фото 1 Мп за 5,8 с быстрой моделью. Ограничения: фото до
12,5 Мп и результат до 200 Мп; переводы кроме английского и русского — черновые.

### 3. Сборка

```powershell
node tools/fetch-ncnn.mjs          # ncnn 20260526 android-vulkan со сверкой хеша
node tools/stage-models.mjs        # модели в APK со сверкой хеша
.\gradlew.bat assembleDebug        # JDK 21, Android SDK с NDK r29 и CMake 3.31
```

### 4. Авторы и лицензии

KARTUN — под MIT. Внутри — ncnn (BSD-3, Tencent), модели Real-ESRGAN (BSD-3, Xintao Wang), шрифты Unbounded и Manrope
(OFL 1.1); подробности — в [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md) и [docs/model-licenses.md](docs/model-licenses.md).
