# Research 11 — what each catalogue model is for, in its authors' words
> **Created:** 2026-10-16 (agent, on the owner's word «погугли. Можно писать и что на выходе получается, и для каких фото на входе предназначена. Можно кратко в карточке писать и подробнее - в (i) инфо кнопке по модели»)
> **Parent:** the owner's question in STATUS (model descriptions) · **Status:** recon done 2026-10-16 · **Outbound:** —

**Method.** Only the author's own words (or the official model page) count. Every quote below is verbatim. OpenModelDB text was read from the site's source JSON (`github.com/OpenModelDB/open-model-database/data/models/<name>.json`), which is what the model page shows. Where a source is silent the line says **not stated**. OpenModelDB *tags* are site metadata, not the author's prose, so they are marked "(tag)" when used.

**Sources used (short names):**
- RE-README — https://github.com/xinntao/Real-ESRGAN
- RE-ZOO — https://github.com/xinntao/Real-ESRGAN/blob/master/docs/model_zoo.md
- RE-v0.3.0 — https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.3.0
- RE-v0.2.5.0 — https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0
- RE-ANIME — https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_model.md
- RE-AVIDEO — https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_video_model.md
- RE-ACMP — https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_comparisons.md
- RE-TRAIN — https://github.com/xinntao/Real-ESRGAN/blob/master/docs/Training.md
- RE-PAPER — https://arxiv.org/abs/2107.10833 (Real-ESRGAN paper, Wang et al. 2021)
- OMDB — https://openmodeldb.info/models/<name>
- PH — https://github.com/Phhofm/models/releases/tag/<name>
- HF — https://huggingface.co/Kim2091/<repo>
- WIKI — https://web.archive.org/web/20221229095812/https://upscale.wiki/wiki/Model_Database (archived old community model database, where Kim2091 and NMKD first published their descriptions; the live site returns 403)

---

## 1. Real-ESRGAN General v3 (realesr-general-x4v3)

- **Input:**
  - "a tiny small model for general scenes." — RE-README
  - "They are very tiny models for general scenes, and they may more robust." — RE-v0.3.0
  - Training dataset and degradations for this specific model: **not stated** (RE-TRAIN describes the x4plus training only).
- **Output:**
  - "It also supports the **-dn** option to balance the noise (avoiding over-smooth results). **-dn** is short for denoising strength." — RE-README
  - "Denoise strength. 0 for weak denoise (keep noise), 1 for strong denoise ability. Only used for the realesr-general-x4v3 model." — RE-v0.3.0
  - "X4 (can also be used for X1, X2, X3)" — RE-ZOO
- **Weak spots:**
  - "A tiny small model (consume much fewer GPU memory and time); not too strong deblur and denoise capacity" — RE-ZOO
  - "But as they are tiny models, their performance may be limited." — RE-v0.3.0

## 2. Real-ESRGAN x4plus (RealESRGAN_x4plus)

- **Input:**
  - "X4 model for general images" — RE-ZOO
  - "We use DF2K (DIV2K and Flickr2K) + OST datasets for our training." — RE-TRAIN
  - "The low-quality images are generated with the degradation process described in Real-ESRGAN during training." — RE-TRAIN
  - The paper's degradation process (the paper describes the Real-ESRGAN method; it does not name the `x4plus` file — the repo ships x4plus as its default general model): "Classical degradation model [11, 29], which consists of blur, downsampling, noise and JPEG compression"; "we adopt a second-order degradation process"; "we propose a high-order degradation process and employ sinc filters to model common ringing and overshoot artifacts." The target case is photos like this: "the original image might be taken with a cellphone many years ago, which inevitably contains degradations such as camera blur, sensor noise, low resolution and JPEG compression. The image was then edited with sharpening and resize operations ... After that, it was uploaded to some social media applications, which introduces a further compression and unpredictable noises." — RE-PAPER
- **Output:**
  - "Real-ESRGAN trained with synthetic data is able to enhance details while removing annoying artifacts for most real-world images." — RE-PAPER
  - Training losses: "a combination of L1 loss, perceptual loss and GAN loss" (vs. RealESRNet: "X4 model with MSE loss (over-smooth effects)") — RE-TRAIN, RE-ZOO
- **Weak spots:**
  - "1) some restored images (especially building and indoor scenes) have twisted lines due to aliasing issues. 2) GAN training introduces unpleasant artifacts on some samples. 3) It could not remove out-of-distribution complicated degradations in the real world. Even worse, it may amplify these artifacts." — RE-PAPER (§4.4 Limitations)
  - "the improved high-order degradation process is not perfect and could not cover the whole degradation space in the real world." — RE-PAPER

## 3. Real-ESRGAN x4plus anime (RealESRGAN_x4plus_anime_6B)

- **Input:**
  - "Optimized for anime images; 6 RRDB blocks (smaller network)" — RE-ZOO (section "For Anime Images / Illustrations")
  - "RealESRGAN_x4plus_anime_6B for anime images (动漫插图模型)" [= "anime illustration model"] — RE-README
  - Training dataset / degradations: **not stated**.
- **Output:**
  - "which is optimized for **anime** images with much smaller model size." — RE-ANIME
  - The output look itself: **not stated** (RE-ANIME only shows comparisons with waifu2x).
- **Weak spots:**
  - **not stated** for the model. (RE-ANIME's only caveat is about viewing the demo: "otherwise, you may encounter aliasing issue.")

## 4. Real-ESRGAN AnimeVideo v3 (realesr-animevideov3)

- **Input:**
  - "We add small models that are optimized for anime videos :-)" — RE-AVIDEO
  - "realesr-animevideov3 (animation video)" — RE-README
  - Training dataset / degradations: **not stated**.
- **Output:**
  - "Anime video model with XS size"; "This model can also be used for X1, X2, X3." — RE-ZOO, RE-AVIDEO
  - "which can achieve better results with a faster inference speed. The improvements are: better naturalness; Fewer artifacts; more faithful to the original colors; better texture restoration; better background restoration" — RE-v0.2.5.0 (same list in RE-ACMP)
  - "Our RealESRGAN-AnimeVideo-v3 can achieve better results with faster inference speed." (compared with waifu2x and Real-CUGAN) — RE-ACMP
- **Weak spots:** **not stated**.

## 5. 4xNomos8kSC

- **Input:**
  - "Purpose: A 4x photo ESRGAN upscaling model, handles jpg compression and slight blur" — PH 4xNomos8kSC
  - "Dataset: Nomos8k_sfw"; "Number of train images: 6118"; "OTF Training: Yes"; "Pretrained_Model_G: RealESRGAN_x4plus.pth" — PH 4xNomos8kSC
  - "4x photo upscaler with otf jpg compression and blur, trained on musl's Nomos8k_sfw dataset for realisic sr" — OMDB 4x-Nomos8kSC
- **Output:**
  - "for realisic sr" [sic — realistic super-resolution] — PH / OMDB. Nothing more specific about the look: **not stated**.
- **Weak spots:** **not stated**.

## 6. 4xHFA2k

- **Input:**
  - "Purpose: An anime images 4x upscaling model based on musl's HFA2k dataset" — PH 4xHFA2k
  - "4x anime image upscaler with a bit of otf jpg compression and blur. Trained on musl's hfa2k dataset release for Anime SISR, which has been extracted from modern anime films, where the selection criteria was high SNR, no DOF and high frequency information." — PH 4xHFA2k, OMDB 4x-HFA2k
  - "(PS Example input images are small, each around 500x280 px)" — OMDB 4x-HFA2k
- **Output:** **not stated** (beyond "4x anime image upscaler").
- **Weak spots:** **not stated**.

## 7. 4x NMKD Superscale (SP_178000_G)

- **Input:**
  - "Purpose: Clean Real-World Images" / "Upscaling of realistic images/photos with noise and compression artifacts" — OMDB 4x-NMKD-Superscale; same text in WIKI ("Clean Real-World Images | ... | Upscaling of realistic images/photos with noise and compression artifacts")
  - Tags (site metadata): denoise, photo, restoration (tag).
  - Dataset: **not stated**.
- **Output:** **not stated** (the purpose line "Clean Real-World Images" reads as the job — cleaning real-world photos — not as a requirement that the input be clean, since the next line says the input has "noise and compression artifacts").
- **Weak spots:** **not stated**.

## 8. 4x NMKD Siax ("CX", Siax 200k)

- **Input:**
  - "Universal upscaler for clean and slightly compressed images (JPEG quality 75 or better)" — OMDB 4x-NMKD-Siax-CX; same in WIKI
  - Dataset: "DIV2K Train+Valid" — OMDB 4x-NMKD-Siax-CX
- **Output:** **not stated**.
- **Weak spots:** implied by the input line only — images worse than "JPEG quality 75" are outside its stated range. Explicit weaknesses: **not stated**.

## 9. 4xNomosWebPhoto RealPLKSR

- **Input:**
  - "short: 4x RealPLKSR model for photography, trained with realistic noise, lens blur, jpg and webp re-compression." — PH 4xNomosWebPhoto_RealPLKSR, OMDB 4x-NomosWebPhoto-RealPLKSR
  - "I then made 12 different low resolution degraded folders, using kim's datasetdestroyer for scaling and compression, my ludvae200 model for realistic noise, and umzi's wtp_dataset_destroyer with its floating point lens blur implementation for better control." — same
  - "Purpose: Restoration"; "Subject: Photography"; "Dataset: Nomos-v2"; "Dataset Size: 6000" — OMDB
  - "PS in general degradation strengths have been reduced/adjusted in comparison to my previous RealWebPhoto models" — same
- **Output:** **not stated** (losses listed include "perceptual, gan, dists, ldl, ff, color and lumaloss", but no description of the look).
- **Weak spots:** **not stated** (only: "This model took multiple retrainings and reworks of the dataset, until I am now satisfied enough with the quality to release this version.").

## 10. 4x ClearReality V1

- **Input:**
  - "Purpose: Realistic images of humans, foliage, trees, buildings, etc." — HF ClearRealityV1
  - "This one is intended for realistic imagery, and works especially well on faces, hair, and nature shots. It should only be used on somewhat clear shots, without a lot of grain." — HF ClearRealityV1, OMDB 4x-ClearRealityV1
  - "Dataset: My own UltraSharpV2 dataset, my 8k dataset (v2), Nomos8k, and FaceUp"; "OTF Training No (made with datasetDestroyer)" — HF ClearRealityV1
- **Output:**
  - "I aimed for a softer, more natural look for this model with as few artifacts as possible." — HF, OMDB
  - Architecture "SPAN" — HF. Speed: **not stated** by the author.
- **Weak spots:**
  - "Note: In shots with DOF (depth of field) or bokeh, unfortunately there will be artifacts." — HF, OMDB
  - Input caveat above: "without a lot of grain".
  - (About the separate *Soft* variant, not the Normal one: "It produces more natural output on games or rendered content, but suffers a bit more with realistic stuff.")

## 11. 4x Remacri

- **Input:** **not stated** (tag: general-upscaler; the WIKI lists it under "General Upscaler").
- **Output:**
  - "A creation of BSRGAN with more details and less smoothing, made by interpolating IRL models such as Siax, Superscale, Superscale Artisoft, Pixel Perfect, etc. This was, things like skin and other details don't become mushy and blurry." — OMDB 4x-Remacri; same in WIKI
  - "Pretrained: none - interpolated" — OMDB
- **Weak spots:** **not stated**.

## 12. 4x UltraMix Balanced

The file exists only as `Interpolations/4x-UltraMix_Balanced.pth` inside https://huggingface.co/Kim2091/UltraSharp ; it has no model card and no OpenModelDB page (no `4x-UltraMix*` entry in the OpenModelDB data). The only author words are about the whole UltraMix collection:

- **Input:** **not stated** for Balanced. Collection: "This is a mixture of models based around UltraSharp and my other available models. These are usually interpolations that have separate but very helpful uses. As an example, UltraMix_Restore is a combination of UltraSharp and UniScale_Restore, and is great for video game textures." — WIKI (UltraMix Collection row)
- **Output:** "If you want a more balanced output, check out the UltraMix Collection down below. It's a bunch of interpolated models based around UltraSharp and my other models" — OMDB 4x-UltraSharp, WIKI. Which models Balanced mixes: **not stated**.
- **Weak spots:** **not stated**.
- Context (parent model 4x-UltraSharp, NOT Balanced itself): "It generates lots and lots of detail and leaves a nice texture on images. It works on most images, whether compressed or not. It does work best on JPEG compression though, as that's mostly what it was trained on." — HF UltraSharp, OMDB 4x-UltraSharp

## 13. 4x UltraSharp V2 Lite

No separate page: the Lite file ships in the same repo as V2 (https://huggingface.co/Kim2091/UltraSharpV2) and the card has one Lite-specific line.

- **Input:** **not stated separately**; the shared card says "Subject: AI Generated, Anime, Cartoon, CGI, Faces, Screenshots, Textures, Photography, Text" and "Purpose: Compression Removal, Dehalo, Denoise, General Upscaler, JPEG, Restoration" — HF UltraSharpV2
- **Output:** **not stated** separately. The only Lite line: "The Lite model is based on the RealPLKSR architecture, rather than DAT2." — HF UltraSharpV2, OMDB 4x-UltraSharpV2
- **Weak spots:** **not stated** (speed and quality vs full V2 not stated either).

## 14. 4x UltraSharp V2 (DAT2)

- **Input:**
  - "Subject: AI Generated, Anime, Cartoon, CGI, Faces, Screenshots, Textures, Photography, Text" — HF UltraSharpV2
  - "It can handle realistic images, anime, cartoons, and quite a bit more without much hassle. It also works amazingly on illustrations and artwork." — HF UltraSharpV2, OMDB 4x-UltraSharpV2
  - "Purpose: Compression Removal, Dehalo, Denoise, General Upscaler, JPEG, Restoration"; "Dataset: Private dataset" — HF UltraSharpV2
  - OMDB tags additionally list anti-aliasing, deblur, game-textures, text (tag).
- **Output:**
  - "This is easily the best model I've ever trained." — HF, OMDB. A description of the look (sharp / natural / detail): **not stated**.
  - "Architecture: DAT2" — HF. Speed: **not stated** by the author.
- **Weak spots:** **not stated**.

---

## Summary table (only what the quotes support)

| model | input in 6–8 words | output in 4–6 words |
|---|---|---|
| Real-ESRGAN General v3 | general scenes; any degradation, tiny model | adjustable denoise; weak deblur/denoise |
| Real-ESRGAN x4plus | real-world photos: blur, noise, JPEG, resizing | more detail, fewer artifacts |
| Real-ESRGAN x4plus anime | anime images and illustrations | not stated (smaller network) |
| Real-ESRGAN AnimeVideo v3 | anime video frames | natural, faithful colours, XS, fast |
| 4xNomos8kSC | photos with JPEG compression, slight blur | realistic super-resolution |
| 4xHFA2k | anime (film frames), light JPEG and blur | not stated |
| 4x NMKD Superscale | real photos with noise and compression artifacts | cleaned real-world images |
| 4x NMKD Siax | clean or slightly compressed (JPEG ≥ q75) images | universal upscale (look not stated) |
| 4xNomosWebPhoto RealPLKSR | photos with noise, lens blur, JPEG/WebP recompression | restoration (look not stated) |
| 4x ClearReality V1 | fairly clean realistic shots: faces, hair, nature | softer, natural, few artifacts |
| 4x Remacri | not stated (general upscaler) | more detail, less smoothing |
| 4x UltraMix Balanced | not stated | more balanced than UltraSharp |
| 4x UltraSharp V2 Lite | photo, anime, art, CGI, text (shared card) | not stated (RealPLKSR version) |
| 4x UltraSharp V2 | photo, anime, cartoon, art, CGI, text | author's best model (look not stated) |

## Where our current cards disagree with the sources

- **General v3** — card «fast, removes noise and blur»; the author says the opposite about strength: "not too strong deblur and denoise capacity". "Fast" is supported ("much fewer GPU memory and time"). Its stated feature is the adjustable denoise strength (`-dn`).
- **NMKD Superscale** — card «clean camera shots»; the author's input is photos "with noise and compression artifacts". "Clean" is what it does, not what it needs.
- **UltraMix Balanced** — card «sharpness and detail»; the only author statement is that UltraMix gives "a more balanced output" than UltraSharp, i.e. less aggressive, not sharper.
- **UltraSharp V2 / V2 Lite** — «sharpness» and «very slow» are not in the author's words; if «very slow» stays, it must come from our own measurement, not from the author.
- **ClearReality** — «fast» is not stated by the author (architecture is SPAN only); "softer, more natural look" and the DOF/bokeh and grain caveats are stated and missing from our card.
- **Remacri** — «natural detail»: "more details and less smoothing" is supported; "natural" is not stated.
