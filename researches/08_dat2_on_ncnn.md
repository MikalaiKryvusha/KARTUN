# Research 08 — DAT2 на ncnn: как запустить трансформер UltraSharp V2 в движке KARTUN на Vulkan

> **Создан:** 2026-10-15 (agent; по слову владельца)
> **Родитель:** `researches/07_best_models_recon.md` §5.2; владелец, чат 2026-10-15 ≈ 14:19 +03:00: «Довести DAT2 на ncnn» · «погугли» · «напиши ресерч док по этой работе»
> **Статус:** 🔬 2026-10-15 14:22 +03:00 — разведка и диагноз готовы; попытки 1–2 перевода провалились; попытка 3 (переписать граф под ncnn) — в работе
> **Вовне:** владельцу — вывод и цифры в чат; если план A не выйдет — развилка «другой движок (MNN) или без DAT2» (§7)

## 1. Цель и критерии приёмки

**Цель (Achieve):** модели на архитектуре DAT2 работают в движке KARTUN на видеочипе Титана, как остальные модели
каталога. Первая — UltraSharp V2 (владелец 14:13: «ультрашарп обе принимаем, можно докачать»), следом коммерчески чистые
Nomos2 hq dat2 и BHI dat2 real (Phhofm, CC BY 4.0).

| № | Критерий | Проверка |
|---|---|---|
| 1 | Перевод даёт `.param` без слоёв, которых нет в ncnn | `tools/convert-model.py` печатает «UNSUPPORTED: none» (иначе exit 1) |
| 2 | ncnn считает то же, что PyTorch | ≥ 45 дБ PSNR на куске фото, `tools/convert-model.py` (сейчас у моделей каталога 60–73 дБ) |
| 3 | Движок принимает модель с постоянным размером входа | краевые куски дополняются до размера, выход обрезается; картинка без швов на Титане |
| 4 | Модель идёт на видеочипе Титана | прогон `tools/titan-probe.mjs`, время на кусок записано в отчёт прогона |

## 2. Что такое DAT2 (из кода сети, spandrel 0.4.2, `architectures/DAT/__arch/DAT.py`)

UltraSharp V2: 11,2 млн параметров (оригинал 139,8 МБ, на телефоне ≈ 22 МБ в fp16 по счёту параметров), 18 блоков
пространственного внимания + 18 блоков внимания по каналам, ширина 180, 3 головы, окна 8×32 со сдвигом 4×16, обучена на
кусках 64×64 (`patches_resolution = 64`), увеличение — свёртка + PixelShuffle.

Каждый блок пространственного внимания:
1. дополняет вход нулями до кратного 32 (`F.pad`, размер считается в Python);
2. в половине блоков сдвигает карту `torch.roll` **сразу по двум осям** и строит маску окон `calculate_mask(H, W)` — **в
   Python, под конкретный размер входа**;
3. режет карту на окна через `view(B, C, H/8, 8, W/32, 32).permute(0, 2, 4, 3, 5, 1)` — **6-мерный тензор**, и собирает
   обратно тоже через 6 измерений (`windows2img`);
4. внимание в окне: `q @ kᵀ` + относительная позиционная поправка (MLP над постоянной таблицей) + маска → softmax → `@ v`.

## 3. Что уже пробовали (KARTUN, 2026-10-15)

| Попытка | Путь | Итог |
|---|---|---|
| 1 | spandrel → TorchScript → pnnx, два размера входа (64² и 96×80), `tools/convert-model.py` | pnnx молча умирает в `pass_level0`; в трассировке — TracerWarning на `assert L == H*W`, `int(img_splits_hw.shape[0] / …)`, `if self.patches_resolution != _H` |
| 2 | ONNX автора (`4x-UltraSharpV2_fp32_op17.onnx`, sha256 `6c0201e3…`) → `pnnx.exe` | segfault (exit 139) в `fold_constants_dynamic_shape` после 58 с свёртки констант и 27 с вывода форм |

Для сравнения — PyTorch на ПК, кусок 200×200: полная V2 12,9 с, V2 Lite (RealPLKSR) 3,0 с.

## 4. Что известно в мире (поиск 2026-10-15)

- **Готового DAT, SwinIR или HAT в ncnn нет ни у кого.** Сборки моделей для ncnn (TNTwise Universal-NCNN-Upscaler — ESRGAN,
  Compact, SPAN, MSDAN через chaiNNer; tumuyan RealSR-NCNN-Android; upscayl-ncnn) — только свёрточные сети.
- **ncnn #4299** (открыта с 2022-10-22): «SwinIR models … cannot be converted to NCNN»; в 2024 nihui советует новый pnnx —
  отчёта об успехе нет.
- **ncnn #3542** (открыта с 2022-02-03, перевод Swin через pnnx): «reshape to 6-rank tensor is not supported yet!»,
  «reshape tensor with batch index N is not supported yet!», «slice with step 2 is not supported». В той же ветке:
  Swin переводится в **MNN** («推理也正确» — вывод верный) и в **TNN** («swin transformer в TNN медленный, узкое место —
  MATMUL», вдвое медленнее ResNet-100).
- **ncnn #3783** (nihui, 2022): сдвинутое оконное внимание Swin нельзя свернуть в один слой `nn.MultiheadAttention` —
  граф остаётся россыпью matmul/softmax/reshape.
- **Исходник pnnx** `tools/pnnx/src/pass_ncnn/torch_roll.cpp`: `torch.roll` переводится в `Slice` + `Concat` только если
  `dims` и `shifts` — по ОДНОЙ оси.
- **ncnn** хранит тензор не более чем в 4 измерениях (w, h, d, c); слой `MultiHeadAttention` с ядром на Vulkan есть
  (`src/layer/vulkan/multiheadattention_vulkan.cpp`), `Permute`, `Reshape`, `Softmax`, `MatMul`, `LayerNorm`, `GELU` — есть.
- **Другие движки на Vulkan:** ExecuTorch Vulkan — «under active development», неподдержанное уходит на процессор; Arm
  гонит Swin2SR через ExecuTorch + VGF только на ПК. MNN (Vulkan-бэкенд) Swin переводит, но это второй движок в приложении.

## 5. Диагноз — три препятствия, все в форме графа, не в вычислениях

1. **6-мерные reshape/permute** при нарезке на окна — в ncnn максимум 4 измерения (как в #3542).
2. **Размер-зависимая логика в Python** — дополнение до кратного 32, маска окон, `assert`, `int(shape …)`: при двух
   размерах трассировки граф «раздваивается», и pnnx падает (попытки 1 и 2).
3. **`torch.roll` по двум осям** — pnnx умеет только по одной.

Сами вычисления (свёртки, LayerNorm, GELU, matmul, softmax, sigmoid) ncnn и Vulkan умеют.

## 6. План A — переписать граф DAT2 под ncnn, не меняя математику

Лучшая практика для трансформеров в ncnn — не ждать поддержки, а переписать модель так, чтобы граф состоял из того, что
ncnn умеет (так делают для Swin в MNN/TNN и для 4D-ограничений в ncnn). Шаги — каждый проверяется **в PyTorch побитно
против исходной сети ДО pnnx**:

1. **Постоянный размер входа** (кусок, например 128×128 или 160×160 — кратно 32): маска окон, дополнение и все `int(shape)`
   становятся константами; трассировка — одним размером.
2. **Нарезка на окна в ≤ 4 измерениях** при B = 1: `(C, H, W) → view(C·H/8, 8, W/32, 32) → permute(0, 2, 1, 3) →
   view(C, n_окон, 256) → permute(1, 2, 0)`; сборка обратно — зеркально.
3. **`roll` по двум осям → два `roll` по одной.**
4. pnnx с одним `inputshape`; `tools/convert-model.py` проверяет слои и сверяет с PyTorch (≥ 45 дБ).
5. **Движок:** у модели в каталоге — поле «постоянный размер входа»; движок дополняет краевые куски до этого размера
   (отражением) и обрезает выход — сейчас края режутся меньшими кусками.
6. **Титан:** время на кусок на видеочипе, fp16. Риск: внимание в fp16 на Mali может переполняться — тогда эти слои в fp32.

## 7. План B и выход

- **План B — MNN с Vulkan** только для трансформеров: Swin в MNN переводится (ncnn #3542), но это второй движок, второй
  JNI и вдвое больше нативного кода — решение владельца.
- **Выход без DAT2:** в каталоге уже есть V2 Lite (RealPLKSR) — владелец принял её 14:13.
- Правило трёх попыток (`BUG_FIXING_FRAMEWORK.md`): попытка 3 = план A; если и он не даст критерии 1–2 — стоп и
  развилка владельцу с цифрами.

## 8. Риски

- (a) **Скорость на Mali:** на ПК DAT2 в 4,3 раза медленнее RealPLKSR; TNN сообщал matmul как узкое место Swin. Замер на
  Титане до обещаний.
- (a) **fp16 во внимании** — переполнение softmax/matmul на видеочипе; проверка на Титане сверкой с PyTorch.
- (b) **Швы** между кусками при постоянном размере — проверка глазом на большом фото.
- (c) **Память:** окно 8×32 = 256 токенов, матрица внимания 256×256 на голову — мелочь на фоне свёрток.

## Источники

- ncnn #3542 https://github.com/Tencent/ncnn/issues/3542 · #3783 https://github.com/Tencent/ncnn/issues/3783 ·
  #4299 https://github.com/Tencent/ncnn/issues/4299
- pnnx `torch_roll.cpp` https://github.com/Tencent/ncnn/blob/master/tools/pnnx/src/pass_ncnn/torch_roll.cpp
- pnnx README https://github.com/pnnx/pnnx · ncnn Vulkan FAQ https://github.com/Tencent/ncnn/wiki/FAQ-ncnn-vulkan
- Universal-NCNN-Upscaler https://github.com/tntwise/universal-ncnn-upscaler · RealSR-NCNN-Android https://github.com/tumuyan/RealSR-NCNN-Android
- SwinIR (6-мерная нарезка окон) https://github.com/jingyunliang/swinir · ExecuTorch Vulkan https://pytorch.org/executorch/main/_sources/backends-vulkan.md.txt
- Arm, Swin2SR через ExecuTorch https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/swin2sr-vgf/1-prepare-environment/
- UltraSharp V2 https://huggingface.co/Kim2091/UltraSharpV2 · код DAT — spandrel 0.4.2 `architectures/DAT/__arch/DAT.py`
