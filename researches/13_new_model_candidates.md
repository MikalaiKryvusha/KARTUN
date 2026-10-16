# Research 13 — new model candidates now that DAT2 runs on the phone

> **Created:** 2026-10-16 (agent, on the owner's word «и есть ещё кандидаты крутые моделей? Мы DAt2 ведь теперь умеем»)
> **Parent:** researches/07_best_models_recon.md, researches/08_dat2_on_ncnn.md · **Status:** recon done 2026-10-16 · **Outbound:** the shortlist → the owner

## 1. How this was read (2026-10-16)

| Source | What was read | How |
|---|---|---|
| OpenModelDB data repo | fresh shallow clone, HEAD `782aac0` (2026-08-14), 671 models — the same snapshot as research 07, nothing newer exists | every 4× entry by architecture and date, `data/models/<id>.json` |
| Phhofm (Philip Hofmann, "helaman") | `gh api repos/Phhofm/models/releases` (all releases, assets, sizes) + release notes; README "Released (sorted by new)" | release bodies quoted verbatim |
| Hugging Face API | `models?author=Phips`, `author=Kim2091`, `search=dat2`; model cards of `Phips/NERVE`, `Phips/HEART` (both new, 2026-09) | `likes`, file sizes from the `tree` API |
| the-database/MangaJaNai | GitHub releases 2.0.0 (2025-07-08) and 3.0.0 (2025-11-26) + zip listings read by HTTP range (central directory only) | file names and sizes of the weights inside the zips |
| Civitai API | `models?types=Upscaler&sort=Most Downloaded&limit=100` and model details | `downloadCount`, `thumbsUpCount` |
| Kim2091 | `gh api repos/Kim2091/Kim2091-Models/releases` | no new 4× model after UltraSharp V2 (2025-05) — only 1×/2× (TFDAT, TSPAN, AnimeUp) |
| Reddit | **not reachable**: WebSearch refuses the domain ("not accessible to our user agent"), `reddit.com/search.json` returns non-JSON to curl | no Reddit evidence below — unknown, not "absent" |

OpenModelDB shows no likes or download counters (checked on `openmodeldb.info/models/4x-Nomos2-hq-dat2`: only file sizes).
Hugging Face shows `downloads: 0` for all these SR repos (not tracked) — likes are the only HF signal.

## 2. What changed since research 07

1. **DAT2 runs** (researches/08 §10–§15): `tools/ncnn_dat.py` patches spandrel's `DAT` class generically; UltraSharp V2 = 66.6 dB vs
   PyTorch on the Mali, ~11 s per 128 px tile, ~29 min for a 1.2 MP photo (catalogue `gpuMsPerMp` 1 416 016).
2. **Every 4× DAT2 photo model has UltraSharp V2's exact tensor layout** — `[inference from sizes]`: the `.safetensors` of
   UltraSharp V2, 4xBHI_dat2_* (all four), 4xNomos2_hq_dat2, 4xRealWebPhoto_v4_dat2 are all **139 792 588 bytes**;
   IllustrationJaNai V2/V3 DAT2 are 139 793 020 (bf16 header differs). Same layout → same converter, same tile time, no new
   engine work. Their cost on the phone is UltraSharp V2's cost: **~29 min per 1.2 MP**.
3. **DAT (full)** models (FaceUp*, Nomos8kDAT, LSDIRDAT, NomosUniDAT_*) are the same spandrel class with a 2× wider MLP
   (safetensors 154 152 604 bytes). Expected to pass `tools/ncnn_dat.py` unchanged — **unverified**; slower than DAT2, not measured.
4. **New in 2026: Phhofm's BODY suite** (HF, created 2026-09-03 / 09-10, last modified 2026-10-02): **NERVE** (1.8 M-param
   pure-conv, ncnn files shipped, Apache-2.0) and **HEART** (16.7 M-param window-attention transformer, Apache-2.0). Both trained
   only on a CC0 dataset — "the released weights are clean to use commercially" (both cards).
5. **New 4× anime/illustration:** IllustrationJaNai V2 (2025-07) and V3 (2025-11) by the-database, with a DAT2 variant.
6. **Still no new 4× DAT2 photo model after Dec 2024.** Phhofm's 4× photo line ends with `4xBHI_dat2_otf_nn` (2024-12-27);
   his 2025 releases are 1×/2× pretrains; OpenModelDB's 2025–2026 entries are 1×/2× (game, cartoon, video).

## 3. Candidates — DAT2, runs on our engine now (same layout as UltraSharp V2)

| Model | Author | Arch | For (author's words) | Licence | Weights | Size | Evidence |
|---|---|---|---|---|---|---|---|
| **4xBHI_dat2_real** (2024-12-14) | Philip Hofmann | DAT2 | "4x upscaling images. Handles realistic noise, some realistic blur, and webp and jpg (re)compression." · "4x dat2 upscaling model for web and realistic images" | CC-BY-4.0 (release note + HF card) — commercial OK with attribution | https://github.com/Phhofm/models/releases/download/4xBHI_dat2_real/4xBHI_dat2_real.safetensors | 139 792 588 B (.pth 140 196 334) | HF 10 likes; **the author's own Space `Phips/Upscaler` pre-loads it as the default** (`value="4xBHI_dat2_real"`, app.py l.171); third-party Core ML ports for iPhone (`333i/4xBHI_dat2_real-coreml-ios`, `local-vision-models/4xbhi-dat2-real-coreml`, 2026-09/10); offered on Replicate `zf-kbot/image-upscale`; used in a video upscale comparison on X: "中：ピクセルアップスケール(4xBHI_dat2_real.safetensors) 右：SeedVR2…" (https://x.com/MireilleDartois/status/1956209554049655138) |
| **4xNomos2_hq_dat2** (2024-08-29) | Philip Hofmann | DAT2 | "trained and for usage on non-degraded input to give good quality output" | CC-BY-4.0 (OpenModelDB, HF card; the note itself misprints "CC-BY-0.4") | https://github.com/Phhofm/models/releases/download/4xNomos2_hq_dat2/4xNomos2_hq_dat2.pth · HF `Phips/4xNomos2_hq_dat2/4xNomos2_hq_dat2.safetensors` | .pth 140 196 334 · .safetensors 139 792 588 | HF 4 likes; the base (pretrain) of the whole BHI dat2 series; author scored 21 checkpoints with 68 metrics (sheet linked in the note); Core ML port `local-vision-models/4xnomos2-hq-dat2-coreml` |
| **4xRealWebPhoto_v4_dat2** (2024-04-04) | Philip Hofmann | DAT2 ("Architecture Option: DAT-2") | "4x Upscaling Model for Photos from the Web" · Purpose: "Compression Removal, Deblur, Denoise, JPEG, WEBP, Restoration" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xRealWebPhoto_v4_dat2/4xRealWebPhoto_v4_dat2.safetensors | 139 792 588 B | **HF 22 likes — the most-liked Phips DAT2** (07 counted the same); Core ML port `local-vision-models/4xrealwebphoto-v4-dat2-coreml` |
| 4xBHI_dat2_otf (2024-12-14) | Philip Hofmann | DAT2 | "4x upscaling images, handles noise and jpg compression" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xBHI_dat2_otf/4xBHI_dat2_otf.safetensors | 139 792 588 B | HF 0 likes |
| 4xBHI_dat2_otf_nn (2024-12-27) | Philip Hofmann | DAT2 | "4x upscaling images, handles jpg compression and resizes" (trained "without noise") | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xBHI_dat2_otf_nn/4xBHI_dat2_otf_nn.safetensors | 139 792 588 B | not stated |
| 4xBHI_dat2_multiblurjpg (2024-12-14) | Philip Hofmann | DAT2 | "4x upscaling images, handles jpg compression" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xBHI_dat2_multiblurjpg/4xBHI_dat2_multiblurjpg.safetensors | 139 792 588 B | HF 2 likes |
| 4xNomosUniDAT2_multijpg_ldl (2024-01-28) | Philip Hofmann | DAT2 | "4x universal DoF preserving upscaler, pair trained with jpg degradation (down to 40) and multiscale" | CC-BY-4.0 | Google Drive only (OpenModelDB link `drive.google.com/uc?…id=1MUowsfsY_BgNcE0QP6W2TEnRlFFAoLjX`) | .pth 140 299 118 | not stated |
| 4x IllustrationJaNai **V3detail** / **V3denoise** DAT2 (2025-11-26) | the-database | DAT2 | V3detail: "does not clean any halftone noise, and instead retains as much detail and texture as possible"; V3denoise: "cleans halftone noise"; table: "4x DAT2 — Best (Denoise), Nearly Best (Detail) — 15 seconds" (764×1200 on RTX 4090) | **CC-BY-NC-4.0** (repo `LICENSE`: "Attribution-NonCommercial 4.0 International"; OpenModelDB lists V1 as CC-BY-NC-SA-4.0) — **non-commercial** | https://github.com/the-database/MangaJaNai/releases/download/3.0.0/IllustrationJaNai_V3detail.zip → `4x_IllustrationJaNai_V3detail_DAT2_28k_bf16.safetensors`; `…/IllustrationJaNai_V3denoise.zip` → `4x_IllustrationJaNai_V3denoise_DAT2_27k_bf16.safetensors` | 139 793 020 B each (zips 318 641 534 / 166 680 846) | MangaJaNai repo 266 GitHub stars; per-model sample set https://slow.pics/c/k9Qknnpg |
| 4x IllustrationJaNai V2standard DAT2 (2025-07-08) | the-database | DAT2 | "4x_IllustrationJaNai_V2standard_DAT2_27k (highest quality)" | CC-BY-NC-4.0 (as above) | https://github.com/the-database/MangaJaNai/releases/download/2.0.0/4x_IllustrationJaNai_V2standard_ModelsOnly.zip | 139 793 020 B | superseded by V3 |

## 4. Candidates — DAT (full), same converter expected, unverified

| Model | Author | Arch | For (author's words) | Licence | Weights | Size | Evidence |
|---|---|---|---|---|---|---|---|
| **4xFaceUpDAT** / 4xFaceUpLDAT / 4xFaceUpSharpDAT / 4xFaceUpSharpLDAT (2023-09-02) | Philip Hofmann | DAT (full) | "4x photo upscaler for faces"; "4xFaceUpDAT (for good quality input)", "4xFaceUpLDAT (for lower quality input, can additionally denoise)", "I recommend trying out 4xFaceUpDAT" | CC-BY-4.0 | https://huggingface.co/Phips/4xFaceUpDAT/resolve/main/4xFaceUpDAT.safetensors (LDAT/Sharp: same pattern) | 154 152 604 B | HF 4 / 2 / 3 likes; the only 4× face-trained transformer found |
| 4xNomos8kDAT (2023-08-13) | Philip Hofmann | DAT (full) | "A 4x photo upscaler with otf jpg compression, blur and resize" | CC-BY-4.0 | https://huggingface.co/Phips/4xNomos8kDAT/resolve/main/4xNomos8kDAT.safetensors | 154 152 604 B | **HF 26 likes — most-liked Phips repo** |
| 4xNomosUniDAT_bokeh_jpg (2023-09-14) | Philip Hofmann | DAT (full) | "Multipurpose · Handles bokeh effect · Handles jpg compression"; "will not: Denoise, Deblur" (author text, quoted by the Civitai copy) | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomosUniDAT_bokeh_jpg/4xNomosUniDAT_bokeh_jpg.safetensors | 154 152 604 B | Civitai re-upload (Creepybit) 3 854 downloads, 59 👍 |

## 5. Candidates — CNN, runs now or nearly

| Model | Author | Arch | For (author's words) | Licence | Weights | Size | Evidence |
|---|---|---|---|---|---|---|---|
| **4x NERVE OTF gan** (+ 4x NERVE release for clean input) (2026-09) | Philip Hofmann | **NERVE** — new name, but plain ops: the shipped ncnn `.param` is 50 Convolution, 26 BinaryOp, 1 Interp (bicubic), 1 PixelShuffle | "restore a blurry / noisy / JPEG-y image 4x → `models/4x_NERVE_OTF_gan.safetensors`"; "Small and fast. ~1.8M params — SPAN-class size but conv-only, suited to mobile, edge, web and game engines" | **Apache-2.0** (card + LICENSE); "trained **only** on … a **CC0** dataset — the released weights are clean to use commercially" | ncnn: https://huggingface.co/Phips/NERVE/resolve/main/ncnn/4x_NERVE_OTF_gan_fp16.ncnn.bin + `.param` · PyTorch: `…/models/4x_NERVE_OTF_gan.safetensors` | ncnn .bin 3 604 040 B · .safetensors 7 212 448 B | HF 5 likes (new); the author ships an Android folder for RealSR-NCNN-Android but writes "**Status: unverified.** … have **not yet been tested inside the app on a real phone**"; no on-device speed published |
| 4xNomos2_otf_esrgan (2024-06-22) | Philip Hofmann | ESRGAN 23 blocks | "4x ESRGAN model for photography, trained using the Real-ESRGAN otf degradation pipeline." | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomos2_otf_esrgan/4xNomos2_otf_esrgan_fp16.bin + `_fp16.param` (author's own ncnn) · `.pth` | ncnn .bin 33 424 520 B · .pth 33 686 030 | HF 2 likes |
| 4xNomosWebPhoto_esrgan (2024-06-16) | Philip Hofmann | ESRGAN 23 blocks | "4x ESRGAN model for photography, trained with realistic noise, lens blur, jpg and webp re-compression." — "ESRGAN version of 4xNomosWebPhoto_RealPLKSR" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomosWebPhoto_esrgan/4xNomosWebPhoto_esrgan_fp16.bin + `_fp16.param` | 33 424 520 B | HF 3 likes; overlaps our default (same dataset, RealPLKSR) |
| 4xPurePhoto-RealPLSKR (2024-09-21) | asterixcool (starinspace) | RealPLKSR | "Skilled in working with cats, hair, parties, and creating clear images. Also proficient in resizing photos and enlarging large, sharp images." | CC-BY-SA-4.0 (commercial OK, share-alike) | https://github.com/starinspace/StarinspaceUpscale/releases/download/Models/4xPurePhoto-RealPLSKR.pth | 29 663 426 B | recommended for photography by a third-party HF Space (WebSearch summary, unverified) |
| 4x IllustrationJaNai V1 ESRGAN (2024-02-23) | the-database | ESRGAN 23 blocks | "Model for color images including manga covers and color illustrations, digital art … The ESRGAN version is high quality with balanced performance." | CC-BY-NC-SA-4.0 (OpenModelDB) — non-commercial | Google Drive (OpenModelDB) / `IllustrationJaNai_V1_ModelsOnly.zip` in MangaJaNai release 1.0.0 | .pth 33 682 500 | superseded by V2/V3 (which have no ESRGAN variant) |

## 6. Candidates that need new conversion work («needs new conversion work»)

| Model | Author | Arch | For (author's words) | Licence | Weights · size | Evidence |
|---|---|---|---|---|---|---|
| 4x HEART OTF gan / OTF v2 (2026-09) | Philip Hofmann | **HEART** (window attention, i-LN, window 32) — needs new conversion work | "you want the **best quality** and have the compute (desktop, server, high-end device)"; "HEART is the desktop / server quality tier"; claims "matches HAT_M on PSNR (25.02 vs 25.05 dB…)" | Apache-2.0; CC0 training data, "clean to use commercially" | https://huggingface.co/Phips/HEART/resolve/main/models/heart_4x_otf_gan.safetensors · 66 800 316 B (ONNX opset 17 shipped) | HF 8 likes; not in spandrel upstream ("needs HEART registered") |
| 4xNomos8k_atd_jpg (2024-03-22) | Philip Hofmann | **ATD** — needs new conversion work | "4x photo upscaler, handles jpg compression" · "This model will preserve noise." | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomos8k_atd_jpg/4xNomos8k_atd_jpg.safetensors · 81 689 540 B | **the strongest 2026 community signal for a Phhofm photo model**: Civitai re-upload "Z Image 4X UPSCALER by Helaman" (2026-02-09, credits this release) — 6 478 downloads, 174 👍; HF 6 likes |
| 4xRealWebPhoto_v3_atd · 4xNomosWebPhoto_atd · 4xNomos2_hq_atd (2024) | Philip Hofmann | ATD — needs new conversion work | v3: "recommended to try out ;)"; Nomos2_hq_atd: "A 4x atd upscale model for good quality/non-degraded input" | CC-BY-4.0 | Phhofm releases, 81.7–81.9 MB each | HF 5 likes (v3) |
| 4xNomos8kHAT-L_otf (2023-11-01) | Philip Hofmann | **HAT-L** — needs new conversion work | "4x photo upscaler trained with otf" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomos8kHAT-L_otf/4xNomos8kHAT-L_otf.safetensors · 165 299 372 B | Civitai 10 985 downloads, 172 👍 (07 counted the same); HF 5 likes |
| 4xBHI_small_hat-l (+ `_sharp`) (2025-02-04) | Philip Hofmann | HAT-L — needs new conversion work | "4x upscaling good quality (handles no degradations) photography"; "This model is rather soft" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xBHI_small_hat-l/4xBHI_small_hat-l.safetensors · 165 299 372 B | not stated |
| 4x IllustrationJaNai V3detail HAT L (2025-11) | the-database | HAT-L — needs new conversion work | "4x HAT L — Best (Detail)" | CC-BY-NC-4.0 | inside `IllustrationJaNai_V3detail.zip` · 165 300 012 B | — |
| 4xNomos2_hq_drct-l · 4xRealWebPhoto_v4_drct-l (2024) | Philip Hofmann | **DRCT-L** — needs new conversion work | "A 4x drct-l upscale model for good quality/non-degraded input" | CC-BY-4.0 | Phhofm releases · 242.7 MB | HF 1 / 2 likes |
| 4xNomos2_hq_mosr (2024-08-25) | Philip Hofmann | **MoSR** — needs new conversion work | "A 4x mosr upscale model for good quality/non-degraded input" | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xNomos2_hq_mosr/4xNomos2_hq_mosr.pth · 17 213 494 B | HF 2 likes |
| 4xBHI_realplksr_dysample_real (+ multi/otf/otf_nn) (2024-12-14) | Philip Hofmann | **RealPLKSR + DySample** — needs GridSample on our path (07 §3: not tried) | "Trained with my real degraded LR set, handles webp and jpg compression, realistic noise, some lens blur etc." — but the same note: "I recommend using the dat2 models over these, since they give better results quality wise." | CC-BY-4.0 | https://github.com/Phhofm/models/releases/download/4xbhi_realplksr/4xBHI_realplksr_dysample_real.safetensors · 29 641 748 B | HF 5 likes |
| 4xArtFaces_realplksr_dysample (2024-08-08) | Philip Hofmann | RealPLKSR + DySample | "4x realplksr dysample upscale model for art / paintings of faces" | CC-BY-4.0 | Phhofm release `4xArtFaces_realplksr_dysample` · 29 715 988 B | not stated |
| 4xHFA2k_ludvae_realplksr_dysample (2024-07-13) | Philip Hofmann | RealPLKSR + DySample | "4x anime single-image super-resolution with degradation handling" | CC-BY-4.0 | Phhofm release · 29 715 988 B | not stated |
| IllustrationJaNai V2/V3 FDAT XL / FDAT M (2025) | the-database | **FDAT** (sharekhan's "FastDAT") — needs new conversion work; not in chaiNNer/spandrel upstream | "4x FDAT XL — Better — 4.8 seconds", "4x FDAT M — Good — 1.8 seconds" (vs DAT2 15 s, RTX 4090) | CC-BY-NC-4.0 | inside the 2.0.0 / 3.0.0 zips · 92.6 MB / 16.2 MB | — |

Not candidates: RGT, SRFormer, SwinIR, OmniSR, GRL 4× models (all 2023 – early 2024, superseded by the same author's DAT2/ATD
versions); the NTIRE 2026 mobile-track team weights (07 §4) — the challenge repo (last push 2026-06-08) points to a Google Drive
folder, **licence for the weights not stated**.

## 7. Gaps

- **Old / scanned photos:** no dedicated 4× model for scans or prints was found in OpenModelDB or the authors' releases.
  The nearest fits: 4xBHI_dat2_real (noise, "some realistic blur", recompression) and, for faces, 4xFaceUpLDAT ("can additionally
  denoise"). Face *restoration* nets (GFPGAN, CodeFormer) are a different class — out of scope here.
- **Faces:** the newest 4× face-trained models are the 2023 FaceUp DAT family; nothing newer.
- **Praise** is thin for everything after 2024: Reddit was unreachable; HF likes are single or low double digits; Civitai
  counts favour 2022–2023 ESRGAN classics. The only big 2026 number is the ATD re-upload (6 478 downloads).

## 8. Ranked shortlist

| rank | model | arch | for | licence | why |
|---|---|---|---|---|---|
| 1 | 4xBHI_dat2_real | DAT2 | compressed web / messenger photos, noisy phone photos | CC-BY-4.0 (commercial OK) | same layout as UltraSharp V2 → converts with the existing tool; the author's own Space default; Core ML ports show on-device demand; commercially clean, unlike UltraSharp V2 |
| 2 | 4xNomos2_hq_dat2 | DAT2 | clean camera / phone photos | CC-BY-4.0 | the clean-input DAT2 the BHI series is built on; commercially clean "sharpest" alternative to UltraSharp V2 (NC) |
| 3 | 4x NERVE OTF gan (+ release) | NERVE (conv; ncnn shipped) | blurry / noisy / JPEG photos, fast | Apache-2.0, CC0 data | the only 2026 4× release; 3.6 MB ncnn with plain ops → likely seconds instead of DAT2's ~29 min per photo; quality unproven, no praise yet — needs an A/B against our NomosWebPhoto default |
| 4 | 4xRealWebPhoto_v4_dat2 | DAT2 | photos downloaded from the web (JPEG/WEBP, noise, lens blur) | CC-BY-4.0 | most-liked Phips DAT2 on HF (22); overlaps #1 — keep one of the two after a side-by-side |
| 5 | 4x IllustrationJaNai V3detail / V3denoise DAT2 | DAT2 | anime, illustration, manga covers | CC-BY-NC-4.0 (non-commercial) | newest 4× anime DAT2 (2025-11); author rates DAT2 "Best (Denoise), Nearly Best (Detail)"; same layout → converts now |
| 6 | 4xFaceUpDAT (or FaceUpLDAT for poor input) | DAT (full) | faces / portraits | CC-BY-4.0 | the only 4× face-trained transformer; one unverified step: full DAT through `tools/ncnn_dat.py` |
| 7 | 4xNomos2_otf_esrgan | ESRGAN 23 blocks | general photos, Real-ESRGAN-style degradations | CC-BY-4.0 | zero work — the author ships ncnn fp16 files; phone speed of our RRDB models |
| 8 | 4x HEART OTF gan | HEART — needs new conversion work | degraded real photos, top quality | Apache-2.0, CC0 data | 2026 transformer, author claims HAT_M quality; commercially clean; attention path may reuse what DAT2 taught us, but it is new engine work |

Costs to keep in mind: every DAT2 / DAT row costs ~29 min per 1.2 MP on the Titan (UltraSharp V2's measured rate); NERVE and
the ESRGAN rows are the phone-speed options.
