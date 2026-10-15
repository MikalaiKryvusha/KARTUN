# Research 09 — модели Lossless Scaling и апскейл видео в KARTUN: быстрая разведка

> **Создан:** 2026-10-15 (agent; по слову владельца)
> **Родитель:** владелец, чат 2026-10-15 ≈ 16:25 +03:00: «а какие модели используются в LosslessScaling приложении? и можно ли нам их использовать для апскейла изображений? И можно ли в KARTUN добавить апскейлинг видео?» · «Это два вопроса на быструю разведку, которые могут в будущем превратиться в фичи KARTUN» · «не только Мали изучайте, но и снепдрегон · в разведке»
> **Статус:** 🔬 2026-10-15 16:31 +03:00 — разведка по источникам готова; решений нет, фичи не заведены
> **Вовне:** владельцу — выводы в чат; идеи в `ideas/` — только по его слову

Факты — с источником; строки с пометкой `[AI]` — вывод агента, не проверенный прогоном.

## 1. Lossless Scaling: что внутри и можно ли нам

| Способ в Lossless Scaling | Что это | Лицензия | Можно ли KARTUN |
|---|---|---|---|
| **LS1** | «проприетарный алгоритм на машинном обучении» (Corsair), «маленькая быстрая нейросеть общего назначения» для 1×–2× (страница разработчика, по выдержке поиска) | закрытая | **нет** — веса закрыты, лицензии нет |
| **LSFG** (генерация кадров) | «proprietary machine learning model developed from scratch» (страница Steam) | закрытая | **нет** |
| FSR 1 (AMD) | шейдерный фильтр, не нейросеть | MIT | да, с уведомлением |
| NIS (NVIDIA) | «six-tap scaling filter… adaptive sharpening» | MIT | да |
| Anime4K | фильтры + маленькие свёрточные сети для линий, «под 1080p аниме» | MIT | да |
| SGSR 1 (Snapdragon) | «one single GPU shader pass», 12 отсчётов, похоже на Lanczos + резкость | BSD-3 | да |
| xBR, integer | фильтры для пиксель-арта | MIT (часть файлов libretro — GPLv2, смотреть заголовок) | да, по файлу |
| BCAS | бикубика + AMD CAS | — | — |

- Порт LSFG для Linux (**lsfg-vk**) сделан обратной разработкой, берёт `Lossless.dll` из купленной копии пользователя и
  с 2025 года под CC BY-NC-ND 4.0 — запрещены коммерческое использование и производные. Официального Android-приложения
  нет; неофициальный порт LSFG в GameNative тоже требует купленную копию для ПК.
- AMD о FSR 1: вход «should be well anti-aliased» и «noise free» — фильтр не чинит шум и сжатие.
- `[AI]` **Для фото эти фильтры не соперники Real-ESRGAN**: они подчёркивают края, но не убирают шум, артефакты JPEG и не
  дорисовывают текстуру. Польза для KARTUN — мгновенный предпросмотр или режим «Мгновенно» за миллисекунды.

**Snapdragon и Mali:** SGSR настроен под плиточную архитектуру Adreno, про Mali в репозитории ничего. Неофициальный порт
LSFG на Android заявлен для Adreno 6xx+.

## 2. Апскейл видео на Android

**Как делают сейчас (факты):**
- Официальный путь Real-ESRGAN для видео: ffmpeg режет на кадры → `realesrgan-ncnn-vulkan -n realesr-animevideov3` → ffmpeg
  собирает и копирует звук. Модель AnimeVideo-v3 — «XS size», сделана именно для видео; на V100: 65,9 к/с при 640×480,
  22,6 к/с при 720p, 10,0 к/с при 1080p (масштаб в таблице не назван).
- Конкурс Mobile AI 2021 (видео 180×320 → 720×1280, ×4) на Adreno 650: лучший — ≈ 11,3 мс на кадр, но это крошечные
  сети под конкурс.
- Покадровые сети мерцают: BasicVSR++ показывает «significant noise, indicating flickering» у покадровых методов;
  стабильнее рекуррентные (BasicVSR++, AnimeSR, RealBasicVSR).
- Android-приложения на ncnn (RealSR-NCNN-Android) — только картинки и GIF, видео нет. Video2X (ПК) — AGPL-3.0, код брать
  нельзя; движки ncnn внутри — MIT.
- ffmpeg-kit **закрыт** в 2025 году, бинарники сняты; остались форки сообщества.
- Android гарантирует кодирование H.264 только в SD; HD — «SHOULD». Предел кодировщика проверяется вызовом
  `VideoCapabilities.isSizeSupported` / `areSizeAndRateSupported`.

**Snapdragon и Mali (факты):** бенчмарк ncnn (Vulkan, мс): vgg16 — Adreno 740: 39,3, Immortalis-G720: 31,9; resnet50 —
44,3 и 16,1. У Snapdragon есть NPU (Hexagon): QuickSRNetLarge на 8 Gen 3 — 1,29 мс (float) и 0,37 мс (int8) через QNN; у
Mali такого пути в ncnn нет. В ncnn вшиты обходы ошибок драйверов Adreno (`bug_storage_buffer_no_l1`,
`bug_corrupted_online_pipeline_cache`, `bug_buffer_image_load_zero`, отключение fp16 на старых драйверах). Готового
«видео-апскейла при просмотре» у Snapdragon не найдено — «video super resolution» в списках 8 Gen 3 / 8 Elite относится к
камере.

**`[AI]` Как это собрать в KARTUN:** MediaExtractor → MediaCodec (декодер) → кадр в ncnn/Vulkan → MediaCodec (кодировщик
HEVC) → MediaMuxer; звук копируется из MediaExtractor в MediaMuxer как есть — ffmpeg не нужен. Оболочка-кандидат —
Media3 Transformer.

**`[AI]` Ожидания на Титане (не замерено):** 480p ×4 моделью AnimeVideo-v3 — порядка 1–5 кадров в секунду, нашими
фото-моделями — меньше кадра в секунду; минута видео 30 к/с (1800 кадров) — 6–30 минут до нагрева и сброса частот. 480p ×4
= 2560×1920; 720p ×4 = 5120×2880 — большинство кодировщиков телефонов не примут, выше 480p реально ×2. 4K HEVC 20–40 Мбит/с
≈ 150–300 МБ на минуту.

## 3. Итог для владельца `[AI]`

| Вопрос | Можно? | Цена и риск | Первый шаг |
|---|---|---|---|
| Модели Lossless Scaling | LS1 и LSFG — **нет** (закрыты); открытые фильтры FSR 1, NIS, SGSR 1, Anime4K, xBR — да | низкая: один шейдер, миллисекунды; качество для фото много ниже наших моделей | режим «Мгновенно» / предпросмотр на FSR 1 или SGSR 1 и слепое сравнение с нашими моделями |
| Апскейл видео | да, офлайн, короткие ролики, ×2 или 480p ×4 | высокая: новая подсистема кодеков, минуты на ролик, нагрев, пределы кодировщика, мерцание | прототип MediaCodec → AnimeVideo-v3 в ncnn → MediaCodec со звуком насквозь; замер кадров в секунду и нагрева на Титане, позже на Adreno 7xx |

## Источники

- Lossless Scaling в Steam https://store.steampowered.com/app/993090/ · Corsair о LS https://www.corsair.com/br/en/explorer/gamer/gaming-pcs/lossless-scaling-everything-you-need-to-know/ · LS1 https://losslessscaling.com/introducing-a-new-scaling-type-ls1/ (403, по выдержке поиска)
- FSR 1 https://github.com/GPUOpen-Effects/FidelityFX-FSR · руководство AMD https://gpuopen.com/manuals/fidelityfx_sdk/techniques/super-resolution-spatial/ · NIS https://github.com/NVIDIAGameWorks/NVIDIAImageScaling · Anime4K https://github.com/bloc97/Anime4K · SGSR https://github.com/SnapdragonGameStudios/snapdragon-gsr · xBR (лицензии файлов) https://metadata.ftp-master.debian.org/changelogs/main/d/dosbox-x/stable_copyright
- lsfg-vk https://www.gamingonlinux.com/2025/07/lsfg-vk-aims-to-bring-lossless-scalings-frame-generation-to-linux/ · https://lsfg-vk.dev/docs/contributing · порт на Android https://www.androidauthority.com/android-frame-generation-tool-pc-port-3665332/
- Real-ESRGAN для видео https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_video_model.md · скорости https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_comparisons.md
- Mobile AI 2021 https://arxiv.org/abs/2105.08826 · BasicVSR++ https://arxiv.org/pdf/2104.13371 · RealSR-NCNN-Android https://github.com/tumuyan/RealSR-NCNN-Android
- ffmpeg-kit https://cdn.jsdelivr.net/gh/arthenica/ffmpeg-kit@main/README.md · Media3 Transformer https://android-developers.googleblog.com/2023/05/media-transcoding-and-editing-transform-and-roll-out.html · Dimensity 8300 https://www.mediatek.com/products/smartphones/mediatek-dimensity-8300
- бенчмарк ncnn https://github.com/Tencent/ncnn/blob/master/benchmark/README.md · QuickSRNetLarge https://huggingface.co/qualcomm/QuickSRNetLarge
