# Research 12 — what users say each catalogue model is good for
> **Created:** 2026-10-16 (agent, on the owner's word «и отзывы людей по этим моделям можешь почитать - наверняка есть описания, для чего они лучше пригодны»)
> **Parent:** researches/11_model_purposes_recon.md · **Status:** recon done 2026-10-16 · **Outbound:** —

**Method.** Only people who did **not** make the model count. Model authors are left out (Kim2091 on his own models, Phhofm/PhilipHofmann on his, the Real-ESRGAN team on theirs). The Upscayl maintainers (aaronliu0130, NayamAmarshe) are counted, but marked "app maintainer". Bot replies (dosubot, Jenna_AI, AutoModerator) are left out. Every quote is verbatim, short, and has its URL. A "voice" is one distinct username: a person who posts the same thing many times counts once, and the same person on two sites counts once. Generation-settings dumps that only *name* a model are not reviews and are skipped.

**Where the voices came from, and the gaps.**
- **Reddit:** reddit.com refuses this agent's crawler. Comments were read through the PullPush archive API (`api.pullpush.io`), which mirrors Reddit comments with their permalinks. Each Reddit URL below is the comment's original permalink. Subreddits: r/StableDiffusion, r/comfyui, r/upscaling, r/FluxAI, r/magicproxies, r/mpcproxies, r/GameUpscale, r/animepiracy, r/waifu2x and others.
- **GitHub:** Upscayl issues and discussions, chaiNNer issues, OpenModelDB issues, Phhofm/models discussions (read with `gh`).
- **Blogs:** Prompting Pixels on Medium (2025-06-02), hirorohi on note.com (2026-03-13), darkroast175696's Civitai article (2024-10-28), hollowstrawberry's comparison notes on Hugging Face. aiofm.info and neurocanvas.net show no tests of their own, so they are used only as a weak voice.
- **Civitai:** the upscaler pages here are re-uploads, and their review text is behind an authenticated API. Only one uploader blurb carried an opinion (Samael1976 on Superscale).
- **OpenModelDB:** the site has no comment section (checked on the Siax page).
- **Discord:** no reposted summaries of the upscale/ESRGAN Discord were found.

**Name map used to attribute app-level reviews.**
- Upscayl "High Fidelity" = 4xHFA2k (Upscayl README credits).
- Upscayl "Remacri" = 4x Remacri.
- Upscayl "Ultramix (Non-Commercial)" = `ultramix-balanced-4x`, i.e. UltraMix Balanced (`common/models-list.ts`).
- Upscayl "Ultrasharp" = UltraSharp **V1**.
- Upscayl "Digital Art" = RealESRGAN x4plus anime 6B (maintainers in discussions #1025 and #920).
- Upscayl "Standard" was formerly "Real-ESRGAN" (maintainer in #1249: "the slow REAL-ESRGAN model"), probably x4plus. Upscayl "Lite" was formerly "Fast Real-ESRGAN"; its 2.4 MB file matches the size of the realesr-general-x4v3 network. Both mappings are **inferred, not confirmed**, so Standard and Lite reviews are kept separate and marked.
- Automatic1111's names: "R-ESRGAN 4x+" = RealESRGAN_x4plus; "R-ESRGAN 4x+ Anime6B" = x4plus_anime_6B; "R-ESRGAN General 4xV3" / "General WDN 4xV3" = realesr-general-x4v3 and its weak-denoise twin. The `-dn` slider of our General v3 blends between those two.

---

## 1. Real-ESRGAN General v3 (realesr-general-x4v3)

- **Good for:**
  - **Speed.** "realesr-general-wdn-x4v3 is EXTREMELY fast with good quality." — Hambeggar, 2023-05-19, https://www.reddit.com/r/StableDiffusion/comments/12mdtqr/scaler_comparison_4x/jkqpl5w/
  - **Speed.** "[this one] is reasonably fast and provides decent results." — Kim2091 (trains other models; not this model's author), 2024-06-10, https://www.reddit.com/r/GameUpscale/comments/1dabs2y/pbrify_remix_my_set_of_free_cc0_upscaling_pbr/l7zyr0q/ (about the WDN twin)
  - **Paintings (WDN version).** "Very good, texture and definition without being overbearing" — Locke_Moghan, 2022-10-13, https://www.reddit.com/r/StableDiffusion/comments/y2mrc2/the_definitive_comparison_to_upscalers/
  - **Art for print (WDN version).** "'realesr-general-wdn-x4v3' for art" — Holicron78, 2022-11-30, https://www.reddit.com/r/midjourney/comments/z8lenw/how_can_i_get_a_print_quality_image_of_a1_36x24/iycb7d4/
  - **Small text.** "HUD text and font sheets get a second pass through **realesr-general-x4v3** with denoise disabled — that keeps glyph strokes sharp without the ink-bleed the anime model produces" — The32BitVault, 2026-08-06, https://www.reddit.com/r/EmulationOniOS/comments/1vhibww/
  - **Card art, against the anime model.** "I did some tests and comparisons with the anime model, but it produced more random artifacts." — axmccx (picked general-x4v3 for card proxies), 2026-08-31, https://www.reddit.com/r/Netrunner/comments/1w19fdm/proxy_nexus_update_20260828/p6w86j8/
  - **Old portrait photos.** "It tends to respect the original skin pores more than some commercial tools." — Capital-Bell4239, 2026-03-16, https://www.reddit.com/r/upscaling/comments/1rtbfpb/trying_to_upscale_this_image_of_marilyn_monroe/oao5fwi/
- **Bad for:**
  - **Photos (too soft).** "Okay, like BSRGAN, but a bit too much blur" (column: Photos) — Locke_Moghan, same URL as above.
  - **Photos (too soft).** "Both 4xV3 and WDN 4xV3 are softer than x4plus" — happypills00, 2023-01-09, https://www.reddit.com/r/StableDiffusion/comments/107k5yv/comparing_the_different_upscalers_for_realistic/j3mrc31/
  - **Upscayl "Lite" (probably this model):** "Every time I try to upscale any image, I get an extreme loss of sharpness" (listed: Standard, Lite, High Fidelity) — connierebel, Upscayl #1336, https://github.com/upscayl/upscayl/issues/1336
- **Compared to:**
  - "WDN 4xV3 produces more detail than 4xV3, looks less cartoony" — happypills00 (URL above).
  - "Much better here, but not as good as R-ESRGAN-Anime" (column: anime) — Locke_Moghan.
  - "Fast Real-ESRGAN (which is only slightly worse in terms of quality than the normal Real-ESRGAN)" — aaronliu0130 (app maintainer), https://github.com/orgs/upscayl/discussions/544
  - For live action: "Try RealESRGAN_x4plus or realesr-general-x4v3 instead [of animevideov3]" — Dagnarus15, 2026-07-21, https://www.reddit.com/r/upscaling/comments/1v2no9m/why_are_they_glazing_realesrgan_so_much/oywqdhx/
- **Voices:** 11 (2 of them only through Upscayl "Lite", a mapping that is inferred).
  - **Fast:** 3 agree (Hambeggar, Kim2091, the maintainer).
  - **Softer than x4plus on photos:** 2 agree (Locke_Moghan, happypills00); connierebel's Upscayl report agrees if Lite is this model.
  - **Clean on small text and card art, cleaner than the anime model:** 2 agree (The32BitVault, axmccx).

## 2. Real-ESRGAN x4plus (RealESRGAN_x4plus, A1111 "R-ESRGAN 4x+")

- **Good for:**
  - **Clean output, few artifacts.** "as a normal upscaler for postprocessing it's hard to beat plain old RealESRGAN_x4plus because it's been harshly trained for artifact reduction" — kjerk, 2024-03-07, https://www.reddit.com/r/StableDiffusion/comments/1b8vnd6/weed_out_upscalers/ktuaj0o/
  - **Clean output, few artifacts.** "RealESRGAN_x4plus (if I want less noise and artifacts)" — radianart, 2023-08-11, https://www.reddit.com/r/StableDiffusion/comments/15nlg1g/does_a1111_support_third_party_upscalers/jvp42e5/
  - **Photos and live action.** "For photos and real life videos the 3D Real-life model **RealESGRAN-x4plus** gives the best results at the cost of longer rendering times." — 4-Vektor, 2023-03-15, https://www.reddit.com/r/Deusex/comments/11rlghe/anyone_have_a_higher_res_version_of_this_infamous/jcc35be/
  - **Faces.** "4x-UltraSharp or RealESRGAN x4plus tend to preserve facial structure better than the default options." — Mason0816, 2026-06-06, https://www.reddit.com/r/StableDiffusion/comments/1txt8si/need_some_help_about_image_quality_forge/oq23k6z/
  - **Denoising poor images.** "does the job pretty well" — TheMcSebi, 2025-11-04, https://www.reddit.com/r/comfyui/comments/185m61h/workflow_to_denoise_low_quality_images/nn2oz8o/
  - **Game textures.** Listed among "The best results" for PS2 textures — Boarix, 2024-10-06, https://www.reddit.com/r/GameUpscale/comments/1fwx7pb/nolf_ps2_textures_upscale_any_tips/lqnukdj/
- **Bad for:**
  - **Fine detail (smeared).** "R-ESRGAN improved sharpness but blurred out a lot of details, producing a '3D Animation' type of effect." — NeuroXc, 2023-09-03, https://www.reddit.com/r/StableDiffusion/comments/168n555/experimenting_with_upscalers_results/
  - **Over-sharpening.** "ESRGAN tends to oversharpen to my liking" (said of RealESRGAN_x4plus) — ZenWheat, 2025-07-05, https://www.reddit.com/r/comfyui/comments/1lshu6r/hires_fix_scale_takes_forever_how_can_i_speed_it/n1it99d/
  - **Photos, Upscayl's built-in set (Real-ESRGAN then the default):** "it just oversharpens them, and blurs the insides, making the photo look like a painting instead of a photo" — markomitranic, https://github.com/orgs/upscayl/discussions/945
  - **Faces and limbs.** "Real-Ersgan and Ultrasharp have problems with this. The faces are not detailed, nor are the arms and legs." — andreybexus, https://github.com/orgs/upscayl/discussions/946
  - **Anime-style images.** "RealESRGAN_x4plus (will definitely try an anime upscaler next time, this works meh on this model)" — alexuprise, 2023-06-14, https://www.reddit.com/r/WaifuDiffusion/comments/148ht43/come_bathe_with_me/jo2lb1b/
- **Compared to:**
  - "R-ESRGAN-4x+ | Basically BSRGAN | Slightly more texture than BSRGAN | Basically BSRGAN" — Locke_Moghan, https://www.reddit.com/r/StableDiffusion/comments/y2mrc2/the_definitive_comparison_to_upscalers/
  - "I tried RealESRGAN_x2plus and RealESRGAN_x4plus but those are not better than the 4xLSDIR, 4x-UltraSharp, and 4x-UltraSharpV2." — krsnt8, 2025-12-11, https://www.reddit.com/r/StableDiffusion/comments/1pjzz76/what_is_the_best_image_upscaler_currently/ntj9l0q/
  - Weak voice (blog, no tests shown): "The generic variant is surpassed by 4x-UltraSharp for photoreal." — https://aiofm.info/en/guides/ai-image-upscaling
- **Voices:** 13.
  - **Clean and low-artifact:** 2 agree (kjerk, radianart).
  - **Photos and faces:** 2 agree (4-Vektor, Mason0816).
  - **The "processed" look — hard edges with smeared fine texture, "3D animation" or "painting":** 3 agree (NeuroXc, markomitranic, ZenWheat).

## 3. Real-ESRGAN x4plus anime (RealESRGAN_x4plus_anime_6B, A1111 "R-ESRGAN 4x+ Anime6B", Upscayl "Digital Art")

- **Good for:**
  - **Anime.** "The best for anime"; TL;DR row: "Anime | Anime6B" — Locke_Moghan, https://www.reddit.com/r/StableDiffusion/comments/y2mrc2/the_definitive_comparison_to_upscalers/
  - **Flat images with clean lines.** "R-ESRGAN 4x+ Anime6B is good for flat images with clean lines (eg anime)." — Mutaclone, 2024-03-28, https://www.reddit.com/r/StableDiffusion/comments/1bjbr6l/best_upscaler_for_anime_images/kwwxbtp/
  - **Line art and cel shading.** "does a far superior job with line art and cel shading compared to regular models" — Odd-Gear3376, 2026-05-15, https://www.reddit.com/r/StableDiffusion/comments/1tdlx41/whats_the_best_video_upscaler_available/olz5lu6/
  - **Anime art in general.** "realesrgan_x4plus anime 6b for anime art and remacri foolhardly for everything else" — ColdExample, 2025-02-09, https://www.reddit.com/r/StableDiffusion/comments/1ilndhl/best_anime_upscaler_for_4k_wallpaper/mbx87yf/
  - **JPEG cleanup on anime.** "just does an amazing job at upscaling and getting rid of stuff like jpg compression" — weeb-splat, 2023-07-04, https://www.reddit.com/r/evangelion/comments/14qeb5p/is_there_a_hq_version_of_the_misato_polaroid_ive/jqns7a8/
  - **Card scans.** "it will smooth a lot of the rosette patterns and does a good job with text" (old card scans) — PasDeDeux, 2024-02-01, https://www.reddit.com/r/mpcproxies/comments/1aghv9i/borderless_rvr_shocklands/koi0hk3/
  - **Stylised 2D textures.** "keeps hard edges and solid color regions clean instead of inventing photographic texture the way the general model does" — The32BitVault, 2026-08-06, https://www.reddit.com/r/EmulationOniOS/comments/1vhibww/
  - **Against everything else tried.** "all stuff I tried was worse than good old resrgan anime6b" — shapic, 2026-01-15, https://www.reddit.com/r/StableDiffusion/comments/1qd4i4g/best_upscaling_tool_for_anime_illustrious/nzp09th/
  - **Eyes.** "Anime6B got the best eyes." — hollowstrawberry, https://huggingface.co/hollowstrawberry/upscalers-backup
- **Bad for:**
  - **Photos and paintings.** "Straight-up anime dog" (photos), "Also anime" (paintings) — Locke_Moghan.
  - **Keeping the original drawing style.** "it will often *change the drawing style* of your image to the one that the model prefers ... Use with caution, even on anime." — NeuroXc, https://www.reddit.com/r/StableDiffusion/comments/168n555/experimenting_with_upscalers_results/
  - **Fine detail.** "Realesragan 4x plus anime sharper but loses detail." — Mr_Ahmet_02, 2025-11-09, https://www.reddit.com/r/comfyui/comments/1ost1df/loss_of_detail_when_upscaling_and_tiling_effect/nnzxjor/
  - **Tiny dots in comics.** "digital art blur small dots and this didnt [animevideov3]" — JoeBidenSleep, https://github.com/orgs/upscayl/discussions/920
  - **Small text.** "the ink-bleed the anime model produces" — The32BitVault (URL above).
  - **Icons.** "Ultrasharp V2 and RealESRGAN_x4plus_anime_6B, not very good." — OrdinaryAdditional91, 2025-07-25, https://www.reddit.com/r/StableDiffusion/comments/1m8xa4e/icon_upscale/
  - **Speed in Upscayl.** "don't use the digital art default model that comes with Upscayl, takes lot of time, Use a custom animevideov3 model" — bewiz123, 2023-12-02, https://www.reddit.com/r/mangapiracy/comments/lkiwn5/where_can_i_read_or_dl_high_quality_raw_scans/kbomopi/
- **Compared to:**
  - "it does produce better results than Waifu2X though imo" — CanonOverseer, 2022-10-24, https://www.reddit.com/r/datealive/comments/yc4rif/do_you_guys_have_a_higher_res_version_of_this/itmsp1h/
  - The animevideo row: "2nd best for anime, but Anime6B is better" — Locke_Moghan.
- **Voices:** 15.
  - **Best for anime, flat colour and clean lines:** 7 agree (Locke_Moghan, Mutaclone, Odd-Gear3376, ColdExample, shapic, The32BitVault, weeb-splat).
  - **Turns photos and paintings into anime / redraws style:** 2 agree (Locke_Moghan, NeuroXc).
  - **Loses fine detail (small dots, thin text):** 3 agree (Mr_Ahmet_02, JoeBidenSleep, The32BitVault).

## 4. Real-ESRGAN AnimeVideo v3 (realesr-animevideov3)

- **Good for:**
  - **2D artwork.** "The realesr-animevideov3 model is my favorite for 2D artwork." — Nenotriple, 2024-01-13, https://www.reddit.com/r/StableDiffusion/comments/195oa6z/resrgan_4x_upscaling/kho8zr0/
  - **Anime video.** "for the life of me I cannot find a single model that out performs the standard Animevideov3" — PeRmWaZe, 2026-03-29, https://www.reddit.com/r/animepiracy/comments/17368hn/announcing_aniscale_2_a_faithful_and_versatile/od26gix/
  - **Old cartoons and anime.** "I upscaled some of my old cartoons/anime from 480/720p to 1080p with RealESRGAN vulcann & animevideov3 and they looked amazing!" — dem_c, 2023-11-15, https://www.reddit.com/r/PleX/comments/17vhjxq/what_do_you_think_of_1080p_movies_being_upscaled/k9dcq33/
  - **Anime and art.** "The animevideov3 output is best for anime and art." — BananaRamaBam, 2024-05-01, https://www.reddit.com/r/GakiNoTsukai/comments/1ch17kv/jimmy_onishi_student_photo_colorization_attempt/l23r9py/
  - **Comics.** "work best for me" — JoeBidenSleep, https://github.com/orgs/upscayl/discussions/920
  - **Manga.** "better at keeping lines sharp" — DisasterPrudent1030, 2026-04-18, https://www.reddit.com/r/StableDiffusion/comments/1soayfc/best_model_to_clearn_up_manga/ogxzg8w/
  - **Print rosettes on card scans.** "If you want to keep the rosette pattern try the 2022 model realesr-animevideov3-x4." — Cslagem11 (2025-09-28) and Miam0228 (2025-08-03), identical wording, https://www.reddit.com/r/magicproxies/comments/1nt14qb/epson_et2803_printing_muted/ngqauto/
  - **Faithful look.** Grouped as "fast/faithful (RealESRNet, animevideov3)" — NXGZ (texture-tool author), 2026-08-31, https://www.reddit.com/r/emulation/comments/1w3qgtu/duckstation_texture_upscaler_ai_hd_textures_for/p721c5d/
  - **Speed.** "suele ir bastante más rápido que el x4plus" [usually runs quite a lot faster than x4plus] — NeuralNomad87, 2026-10-03, https://www.reddit.com/r/AIToolBench/comments/1wu2xgi/software_de_ia_local_sin_internet/pdkx3r8/
- **Bad for:**
  - **Live-action faces.** "can produce unstable or overly simplified facial details on live-action footage" — Dagnarus15, 2026-07-21, https://www.reddit.com/r/upscaling/comments/1v2no9m/why_are_they_glazing_realesrgan_so_much/oywqdhx/
  - **Photos.** "Tends to 'unphoto' a subject" — Locke_Moghan, https://www.reddit.com/r/StableDiffusion/comments/y2mrc2/the_definitive_comparison_to_upscalers/
  - **Photos.** A photo came out wrong because the tool "uses the model 'realesr-animevideov3' by default which explains why it turned out like it did" — Kedomas, 2024-05-01, https://www.reddit.com/r/GakiNoTsukai/comments/1ch17kv/jimmy_onishi_student_photo_colorization_attempt/l23p23s/
  - **Texture.** The ideal would be "a slightly softer Animevideov3 with better texture retention" — PeRmWaZe (URL above), i.e. a bit hard and loses texture.
- **Compared to:**
  - "2nd best for anime, but Anime6B is better" — Locke_Moghan.
  - Against Anime6B on comics: animevideov3 kept small dots that "digital art" blurred — JoeBidenSleep. This contradicts Locke_Moghan.
- **Voices:** 14.
  - **Good for anime, cartoons, comics and manga (2D):** 7 agree (Nenotriple, PeRmWaZe, dem_c, BananaRamaBam, JoeBidenSleep, DisasterPrudent1030, NXGZ).
  - **Bad on photos and live-action faces:** 3 agree (Dagnarus15, Locke_Moghan, Kedomas).
  - **Fast:** 2 agree (NeuralNomad87, bewiz123).

## 5. 4xNomos8kSC (Phhofm)

- **Good for:**
  - **Low-res upscaling.** "I like this" (answering "what is the best ESRGAN model for upscaling lower …") — Obvious_Set5239, 2025-11-28, https://www.reddit.com/r/StableDiffusion/comments/1p8ivvv/what_is_the_best_esgran_model_for_upscaling_lower/nr5f5dh/
  - **Beats UltraSharp V2 and Siax.** "also try 4xNomos8kSC, ultrasharp v2 and the Siax 200k one were my go-to for a while but Nomos is best imo" — 7satsu, 2025-11-29, https://www.reddit.com/r/StableDiffusion/comments/1p9e6hu/it_is_possible_to_use_zimage_turbo_with_8gb_of/nrcc84z/
  - **Realistic skin (possibly this model).** "Try the Siax upscaler or Nomos, I find them both the best for realistic images specially skin and other human textures" — Gh0stbacks, 2025-09-28, https://www.reddit.com/r/StableDiffusion/comments/1nsiect/from_muddled_to_4k_sharp_my_comfyui_restoration/ngo8m4c/ (which Nomos model is not named)
- **Bad for:** no reviews found.
- **Contradicts the author:** a FaceFusion model round-up files `nomos8k_sc_x4` under "GANs for Anime/Illustrations … Tuned for 2D stuff" — Educational_Sea6013, 2025-09-18, https://www.reddit.com/r/video_enhance/comments/1nk6gtn/video_enhancement_model_review/ . The author's text says photos; this table shows no test images.
- **Voices:** 2 clear positives (Obvious_Set5239, 7satsu), plus 1 unspecified "Nomos". Very thin.

## 6. 4xHFA2k (Phhofm; Upscayl "High Fidelity")

- **Good for:**
  - **Anime (one person, many posts).** "Personally, I use this one myself for upscales of anime images"; "[HFA2k] is an upscaler specifically for anime screencaps"; "it is pretty fast"; "relatively fast for 2x upscales from 1024x1024" — Dezordan, 2025-06…2026-04, e.g. https://www.reddit.com/r/StableDiffusion/comments/1ofcvgv/what_is_the_best_anime_upscaler/nl8bhef/ and https://www.reddit.com/r/comfyui/comments/1ost1df/loss_of_detail_when_upscaling_and_tiling_effect/no091ri/
  - **Photos of people (surprising).** One Upscayl user on photos of people: "Mainly the HFA2k custom model; tends to produce the best results (for me, anyway…)" (2026-07-02; the thread is in an adult subreddit, so its link is left out of this public file — editor's note 2026-10-16).
- **Bad for:**
  - **Sharp photos.** "Extreme loss of sharpness in upscaled image … It happens both with real photos and AI generated images" (with Upscayl Standard, Lite, High Fidelity); "The only ones that give a good result are … ReMacri and Ultramix" — connierebel, https://github.com/upscayl/upscayl/issues/1336
- **Compared to:** the maintainer suggests it for keeping colours on anime: "Have you tried the digital art or high fidelity model?" — aaronliu0130, https://github.com/orgs/upscayl/discussions/1152 (a suggestion, not a test).
- **Voices:** 3, plus 1 untested maintainer suggestion.
  - **Anime and fast:** 1 (Dezordan).
  - **Real photos:** 1 positive (the Upscayl user above) and 1 negative (connierebel, on sharpness).
  - Thin evidence.

## 7. 4x NMKD Superscale (SP_178000_G)

*Not counted here:* reviews of the separate 8x_NMKD-Superscale_150000_G ("very sharp, detailed, but generate noise", "crunchy aliasing").

- **Good for:**
  - **Overall pick.** "The best upscaler I've come across is 4x_NMKD-Superscale-SP_178000_G. The second place I would give to 4x_NMKD-Siax_200k. I've tried about 30 of them" — x89553421, 2026-07-06, https://www.reddit.com/r/drawthingsapp/comments/1up42r3/best_upscaler_all_give_bad_results/ovx8ffa/
  - **Animal fur.** "At the moment I use 4x_NMKD-Siax_200k and 4x_NMKD-Superscale-SP_178000_G which do pretty well, even better than Gigapixel AI" — Early-Ad-1140, 2025-01-02, https://www.reddit.com/r/StableDiffusion/comments/1hrrkmu/which_upscaler_that_runs_in_a1111swarmui_is_best/
  - **Skin detail.** "NMKD_Superscale_4x upscaler model which gives very good results for skin detail etc." — ratttertintattertins, 2025-08-02, https://www.reddit.com/r/comfyui/comments/1mfunfi/workflow_to_upscale_and_improve_facebody_details/n6jtr5c/
  - **Keeping detail.** "I'd suggest 4x Superscale as your upscaler which preserves details the best" — budwik, 2025-08-20, https://www.reddit.com/r/comfyui/comments/1mv24wy/want_to_upscale_my_existing_image_to_make_it/n9na12r/
  - **2.5D and anime.** "For 2.5D and anime styles I've had good luck with the 4x_NMKD-Superscale model, picks up linework and texture real clean without making it look like an oil painting." — UprightAbsorbing, 2026-08-24, https://www.reddit.com/r/comfyui/comments/1vx2099/what_kind_of_detailing_techniques_or_process_do/p5lcr2j/
  - **Low-quality photos only.** "NMKD superscale should only be used for low-quality images." — nsfwVariant, 2026-04-01, https://www.reddit.com/r/StableDiffusion/comments/1s8uqyo/anima_preview_2_simple_gen_inpaint_workflows_tips/odn8e9d/
  - **Realistic images (Civitai uploader).** "This upscale works better with realistic images. For Anime style, I suggest you to use 4X Ultrasharp" — Samael1976, https://civitai.com/models/141491
- **Bad for:**
  - **Too sharp.** "ultrasharp and nmkd superscale makes the image too sharp which i dont like" — Major_Specific_23, 2026-02-12, https://www.reddit.com/r/StableDiffusion/comments/1r1qyj2/zimageturboprogressivelockedupscale_works_with_z/o4zc4xt/
  - **High-contrast edges.** "Also check the NMKD superscale x4 if you use it, it gave me artifacting on high contrast fringes. Siax performed better." — Turbantibus, 2025-12-14, https://www.reddit.com/r/comfyui/comments/1phldrb/upscale_process_for_photorealism/nu0r6y5/
  - **Weak memory.** "Requires more VRAM." — AbdallahAlswa80 (Civitai article), https://civarchive.com/articles/13452/best-upscaler
- **Compared to:**
  - "siax is pretty good for first passes, followed by superscale" — SDuser12345, 2025-01-06, https://www.reddit.com/r/FluxAI/comments/1hr3drh/how_to_enhance_upscale_a_low_quality_photo_with/m5pqdit/
- **Voices:** 11.
  - **Realistic photos, skin, fur and detail retention:** 5 agree (x89553421, Early-Ad-1140, ratttertintattertins, budwik, Samael1976).
  - **Too sharp or edge artifacts:** 2 agree (Major_Specific_23, Turbantibus).
  - **Anime:** 1 says good (UprightAbsorbing), 1 says use another model (Samael1976).

## 8. 4x NMKD Siax ("CX", 4x_NMKD-Siax_200k)

The most-reviewed model in the set after UltraSharp V1.

- **Good for:**
  - **Faithful, general purpose.** "it's an 'unopinionated' general purpose upscaler that preserves most fine detail without hallucinating too much" — Enivex, https://github.com/upscayl/upscayl/issues/1249
  - **People.** "I find 4x_NMKD-Siax_200k to be the best upscaler especially with people." — Open_Channel_8626, 2024-05-02, https://www.reddit.com/r/StableDiffusion/comments/1ci9vrg/does_anyone_else_think_siax_is_the_best/
  - **Humans.** "4xUltrasharp is good for almost everything but on humans I go with 4x_NMKD-Siax_200k, it's softer and produces less artifacts." — remarkedcpu, 2025-03-11, https://www.reddit.com/r/StableDiffusion/comments/1j8i0bv/what_is_the_best_upscaler_currently/mh6wppy/
  - **Skin.** "I find them both the best for realistic images specially skin and other human textures … add details without changing the facial features" — Gh0stbacks, https://www.reddit.com/r/StableDiffusion/comments/1nsiect/from_muddled_to_4k_sharp_my_comfyui_restoration/ngo8m4c/
  - **Face LoRA training.** "The 4x_NMKD-SIAX_200k model seems to work the best based on my testing." — eastisdecraiglist, 2024-11-04, https://www.reddit.com/r/StableDiffusion/comments/1gjahhk/why_do_sd15_and_sdxl_suck_in_face_lora_training/lvbt4a3/
  - **Animal fur.** "4x_NMKD-Siax_200k … will beat Gigapixel in many, if not most, cases" — Early-Ad-1140, 2024-11-24, https://www.reddit.com/r/StableDiffusion/comments/1gyxo8t/best_thing_for_4k_photos/lys655w/
  - **Fine structures.** "Siax is able to extract much more detail from fine structures" (vs NVIDIA VFX) — TBG______, 2026-03-15, https://www.reddit.com/r/comfyui/comments/1rua09c/the_brandnew_nvidia_vfx_upscaler_fast_vs_fine/
  - **Detail without sharpness.** "4x nmkd siax 200k although not very sharp, it preserves details." — Mr_Ahmet_02, https://www.reddit.com/r/comfyui/comments/1ost1df/loss_of_detail_when_upscaling_and_tiling_effect/nnzxjor/
  - **Grain as a base for realism.** "it introduces artefacts to the image that resemble film grain or noise. This makes the results from that model a good base for realistic images." — lothariusdark, 2025-04-14, https://www.reddit.com/r/StableDiffusion/comments/1jysb7h/what_is_the_best_upscaling_model_currently/mn2sf79/
  - **Photography for SDXL.** "4xNMKD-Siax_200k is my choice (photography)" — SpaceNinjaDino, 2025-03-11, https://www.reddit.com/r/comfyui/comments/1j8i0yv/what_is_the_best_upscaler_currently/mh7e4xr/
  - **Overall pick.** "4xNMKD-Siax_200k .. definitely the best." — BoldCock, https://www.reddit.com/r/comfyui/comments/1j8i0yv/what_is_the_best_upscaler_currently/mhc6zf6/
  - **Almost anything.** "I use Siax on almost anything." — Candiru666, https://www.reddit.com/r/comfyui/comments/1j8i0yv/what_is_the_best_upscaler_currently/mhb9uo4/
  - **More detail than UltraSharp.** "4x_NMKD-Siax_200k, which gave me much better and more details" (vs UltraSharp V1) — Secret_Passage6488, 2023-04-26, https://www.reddit.com/r/StableDiffusion/comments/12zruqc/upscalers_comparison/
  - **Selfies (with a catch).** "I find the 4x_NMKD-Siax_200k best but it has too many white dot artifacts in darker images." — Xelvoren, https://github.com/orgs/upscayl/discussions/1056
- **Bad for:**
  - **Over-sharpening.** "the result from Siax feels 'over-sharpened' to me" — Keldris70, 2025-01-02, https://www.reddit.com/r/StableDiffusion/comments/1hrrkmu/which_upscaler_that_runs_in_a1111swarmui_is_best/m52bery/
  - **Unwanted texture.** "it was too aggressive and added unwanted texture to the end result" — AconexOfficial, 2025-05-30, https://www.reddit.com/r/StableDiffusion/comments/1kz9q84/comparing_a_few_different_upscalers_in_2025/mv45vzx/
  - **Noise.** "this model tends to add too much noise in the upscaled output, making it appear unnaturally sharp" — Ok-Vacation5730, 2024-05-12, https://www.reddit.com/r/StableDiffusion/comments/1cqcg49/creative_upscaling_all_the_way_to_16k_and_beyond/
  - **Noise.** "4x_NMKD Siax 200k also adds noise" — roxoholic, 2026-01-25, https://www.reddit.com/r/comfyui_elite/comments/1qmwbmi/problem_with_upscaling_marble_texture_getting/o1pfq4j/
  - **Dark images, white dots.** "it has a lot of white dot artifacts" — BeamBlizzard, 2024-12-01, https://www.reddit.com/r/StableDiffusion/comments/1h428ti/custom_upscalerenhancer_model_recommendation/
  - **Flat monotone areas.** "I love this particular upscaler but it`s screwing images on flat monotone areas." — Bubbly-Syllabub-8313, 2023-07-22, https://www.reddit.com/r/StableDiffusion/comments/156gz9x/weird_artifacts_with_4x_nmkdsiax_200k/
  - **People.** "I think is good until you put a human in the image, then is one of the worst upscalers." — Popular_Building_805, 2025-09-02, https://www.reddit.com/r/comfyui/comments/1n57bku/help_me_upscale/nc2uaen/
  - **Faces in video frames.** "the results were bad, messing up the faces etc." (video frames) — bonesoftheancients, 2025-09-03, https://www.reddit.com/r/comfyui/comments/1n7sl5r/upscaling_node_for_wan22_first_frame_last_frame/
  - **Detail ceiling.** "I think it's limited to 2K. 4K resolution doesn't show extra detail above the 2K zoomed to 4K." — SpaceNinjaDino (URL above).
  - **Speed.** "Upscaling with Siax / Superscale / BSRGAN etc. is way way slower" (vs RTX upscale) — Simple-Variation5456, https://www.reddit.com/r/comfyui/comments/1rua09c/the_brandnew_nvidia_vfx_upscaler_fast_vs_fine/oak2iod/
- **Compared to:**
  - "ClearReality appears more 'natural,' while the result from Siax feels 'over-sharpened'" — Keldris70.
  - In return, Early-Ad-1140 found ClearReality "doesn't even come close" on fur (see §10).
  - "Remacri for smoother, less grain" — AIposting, 2024-10-21, https://www.reddit.com/r/StableDiffusion/comments/1g8h0rb/what_is_the_best_upscaler_for_flux/lt0eq8q/
  - "2xNomosUni_esrgan for realistic stuff, works better than siax or ultramix imo." — radianart, https://www.reddit.com/r/StableDiffusion/comments/1j8i0bv/what_is_the_best_upscaler_currently/mh5x51z/
  - "4x_NMKD-Siax (if I want more sharp and texture), RealESRGAN_x4plus (if I want less noise and artifacts)" — radianart, 2023-08-11 (URL in §2).
- **Voices:** about 25.
  - **Realistic photos, people, skin, fur and fine detail:** 10+ agree (Enivex, Open_Channel_8626, remarkedcpu, Gh0stbacks, eastisdecraiglist, Early-Ad-1140, TBG______, SpaceNinjaDino, Secret_Passage6488, Mr_Ahmet_02).
  - **Adds grain/noise/texture, can look over-sharp:** 6 agree (Keldris70, AconexOfficial, Ok-Vacation5730, roxoholic, lothariusdark, radianart). lothariusdark counts the grain as a plus.
  - **White dots or artifacts on dark or flat areas:** 3 agree (Xelvoren, BeamBlizzard, Bubbly-Syllabub-8313).
  - **Humans:** 5 say good, 2 say bad (Popular_Building_805, bonesoftheancients).

## 9. 4xNomosWebPhoto RealPLKSR (Phhofm)

- **Good for:**
  - **Speed.** Listed (no comment) among answers to "fastest lightest open source upscaler" — lothariusdark, 2025-07-14, https://www.reddit.com/r/StableDiffusion/comments/1lzrp5y/fastest_lightest_open_source_upscaler/n34mu7e/
- **Bad for:**
  - **Pre-enlarged input.** A user found that when small images were first resized 2x and then upscaled, "the output was far from satisfactory", while the un-resized input was "as expected" — avan06, 2024-12-17, https://github.com/Phhofm/models/discussions/8 . The author replied "model is not trained for that use case", so do not feed it images that were already enlarged.
- **Not counted:** reviews of its siblings with other architectures. One user found Remacri "gave better result" than 4x-NomosWebPhoto-**esrgan** on video frames (No-Sleep-4069, https://www.reddit.com/r/aivids/comments/1lwxsev/wan21_fusionx_gguf_generates_10_seconds_video_on/n2hu0cq/). Another recommends the **atd** version as "slow but good quality" (Renwar_G, https://www.reddit.com/r/comfyui/comments/1dyjf15/trying_to_recreate_results_from_tensorart/lc96d1h/).
- **Voices:** 2, and neither judges the quality. **No real quality reviews found.**

## 10. 4x ClearReality V1 (Kim2091)

- **Good for:**
  - **Fur, hair, wildlife.** "I often use it when I generate Wildlife Images. Some good results with fur or hair." — Keldris70, 2025-01-02, https://www.reddit.com/r/StableDiffusion/comments/1hrrkmu/which_upscaler_that_runs_in_a1111swarmui_is_best/m50hcn2/
  - **Natural look vs Siax.** "I find that the image from ClearReality appears more 'natural,' while the result from Siax feels 'over-sharpened'" — Keldris70 (m52bery).
  - **Small and fast.** "small 4x ClearReality (1.6MB) model are a great starting point" — notaneimu, 2026-03-04, https://www.reddit.com/r/StableDiffusion/comments/1rknd6w/upscale_images_inbrowser_with_onnx_model_no/
  - **Fast video passes.** "For fast on each generation I use tensorrt and clearreality at 1.5x" — hidden2u, 2026-03-07, https://www.reddit.com/r/StableDiffusion/comments/1rnebg9/video_upscaling_reference/o96tmw3/
  - **Fast tier blend.** "For fast, I do Updraft Midweight and ClearReality at a 50% blend." — nitroviper, 2026-04-14, https://www.reddit.com/r/upscaling/comments/1sko20k/local_inbrowser_upscaling/og4hvf5/
  - **Against V2 Lite.** "4x-ClearRealityV1_fp16 TensorRT version give better results than the 4x-UltraSharpV2_Lite [16/32] versions." — JumpingQuickBrownFox, 2025-05-26, https://www.reddit.com/r/StableDiffusion/comments/1ku1pc1/ultrasharpv2_is_released_the_successor_to_one_of/muc0jke/
  - **Eyes and skin (inside an SD tiled upscale).** "Eyes that look like Black dots have now huge Details, skin gets textures, mindblowing.. for some images i used the clearreality upscaler with ultimate upcaler sd" — MsHSB, 2024-07-15, https://www.reddit.com/r/StableDiffusion/comments/1e3wk43/sharpen_photos_using_automatic1111_and_sd15/ldb08j8/
  - **General.** "ClearReality is solid" — Capital-Bell4239, https://www.reddit.com/r/StableDiffusion/comments/1rknd6w/upscale_images_inbrowser_with_onnx_model_no/o9g5r8m/
- **Bad for:**
  - **Fur detail and overall blur.** "it doesn't even come close to the 4x_NMKD I currently use. The overall image appears slightly blurred and fur details get lost completely." — Early-Ad-1140, 2025-01-02, https://www.reddit.com/r/StableDiffusion/comments/1hrrkmu/which_upscaler_that_runs_in_a1111swarmui_is_best/m51hg3o/
  - **Blur in an SD tiled upscale.** "the picture seems somewhat upscaled but very blury" (512→2048 with Ultimate SD Upscale + 4x ClearReality) — ninton364, 2024-02-20, https://www.reddit.com/r/StableDiffusion/comments/1avsudi/proper_upscale_method/
  - **Skin before a diffusion pass.** "remacri, ultrasharp and clearreality are all sharpening esrgan-family models. they inject high-frequency detail *before* the diffusion pass ever runs" — Pale_Coyote7451, 2026-07-24, https://www.reddit.com/r/StableDiffusion/comments/1v5oayu/krea_2_usdu_skin_cracks_weird_textures/ozke04e/
- **Compared to:** the author's own reply, which does not count, says "ClearReality is a significantly faster model, but UltraSharpV2 Lite should be much higher quality". JumpingQuickBrownFox's result above contradicts it.
- **Voices:** 10.
  - **Fast and small:** 4 agree (notaneimu, hidden2u, nitroviper, JumpingQuickBrownFox).
  - **Soft/blurred, loses fine fur detail:** 2 agree (Early-Ad-1140, ninton364).
  - **"Natural":** 1 voice (Keldris70). This matches the author's "softer, more natural look".
  - **Sharpening model:** 1 voice (Pale_Coyote7451). This contradicts the softness reports.

## 11. 4x Remacri (Foolhardy)

- **Good for:**
  - **Skin and texture.** "You might also want to try '4x foolhardy remacri' instead of '4x ultrasharp' as it also preserves the texture more from my testing." — roxoholic, 2026-04-13, https://www.reddit.com/r/comfyui/comments/1sjdbfw/why_does_body_skin_become_smoothplasticless/ofxvrua/
  - **Skin details.** "Could you try Remacri 4X I feel like it preserves skin details more." — NoNipsPlease, 2025-04-30, https://www.reddit.com/r/comfyui/comments/1kbifcr/wan21_smoother_moves_and_sharper_views_using_full/mpvttlb/
  - **Skin and hair, not eyes.** "the best one I found is '4x foolhardy_remacri' for color and image sharpness regarding skin and hair, though it falls short on the irises." — Ikythecat, 2026-06-26, https://www.reddit.com/r/StableDiffusion/comments/1tdlx41/whats_the_best_video_upscaler_available/otwec6i/
  - **Fine wrinkles.** "I tested simple upscales and Remacri the best, it preserves fine detailed wrinkles" — Mundane-Apricot6981, 2025-04-30, https://www.reddit.com/r/StableDiffusion/comments/1kb7iuq/what_is_the_gold_standard_in_ai_image_upscaling/mpum7sk/
  - **Fast option for skin.** "if I had to choose amongst the fast ones '4x_foolhardy_Remacri' or '4x_NMKD-Siax_200k' are pretty good" — Rumaben79, 2025-11-11, https://www.reddit.com/r/StableDiffusion/comments/1ouocir/best_way_to_enhance_skin_details_with_wan22/nodco0h/
  - **Manga and comic pages.** "the best images for pages are produced with ReMaCRI" — Adventurous-Nerve858, 2025-01-21, https://www.reddit.com/r/StableDiffusion/comments/1i6wrmp/best_anime_and_manga_upscalers/
  - **Cartoon game textures.** "use the remacri model its alot easier on cartoony textures" — Acceleratorlvl7, 2025-10-05, https://www.reddit.com/r/Insaniquarium/comments/1azb1l4/remaking_and_expanding_insaniquarium_deluxe/nhuswoq/
  - **Low-bitrate cartoon Blu-ray.** "the 4k downscale has sharper lines and cleaner backgrounds … the Remacri upscaler cleaned it up" — knuckles_didintdied, 2026-05-16, https://www.reddit.com/r/rickandmorty/comments/1teqvg1/the_iconic_whirly_dirly_scene_upscaled_to_4k/om5z3lr/
  - **Card art.** "My preferred is 'Remacri' for most cards that are already fairly high resolution." — dndkk2020, 2025-08-13, https://www.reddit.com/r/magicproxies/comments/1mof75q/anyone_thats_having_issues_with_color/n8g150b/
  - **Video.** "Personally I like foolhardy remacri tensorrt better than seedvr2 for video" — joseph_jojo_shabadoo, 2025-11-28, https://www.reddit.com/r/comfyui/comments/1p8dds6/what_is_the_best_video_upscaler_that/nr6bhj9/
  - **Video.** "Upscale model gave better result: Remacri" — No-Sleep-4069 (§9 URL).
  - **AI-generated images (2022 test).** "Remacri has best results across the board, tested on 8 AI generated Images" — laesarifa, 2022-11-10, https://www.reddit.com/r/StableDiffusion/comments/yr9xbk/extensive_research_of_upscalers_remacri_has_best/
  - **Smoother than Siax.** "NMKD Siax and Foolhardy Remacri [are] my favorites (Remacri for smoother, less grain)" — AIposting (§8 URL).
  - **Hiding grain.** "foolhardy remacri is really the only way I have found to disguise the grain since it blurs the images slightly" — Rumaben79, 2025-06-19, https://www.reddit.com/r/StableDiffusion/comments/1lfn67b/noisy_output_with_stepdistillcfgdistill_lora/
  - **Upscayl, sharpness kept.** "The only ones that give a good result are the non-commercial models ReMacri and Ultramix." — connierebel, https://github.com/upscayl/upscayl/issues/1336
- **Bad for:**
  - **Landscapes.** "Remacri completely flattens and over-sharpens the image, making it look overly processed." — Prompting Pixels, 2025-06-02, https://medium.com/code-canvas/testing-different-upscalers-paid-vs-free-options-1260ab82d403 (same author on Reddit: "I prefer the outputs of UltraShaperV2 over Remacri", https://www.reddit.com/r/StableDiffusion/comments/1kz9q84/comparing_a_few_different_upscalers_in_2025/)
  - **Artifacts / over-sharpening.** "it usually creates artifacts or excessive sharpness when using remacri foolhardy" — k7rk, 2025-07-31, https://www.reddit.com/r/StableDiffusion/comments/1mdlmgs/looks_amazing_to_me_but_took_8100_seconds_like_2/n63437q/
  - **Eyes and hair.** "I have always gotten severe artifacts and distortion with Remacri and Ultrasharp (especially in eyes and hair)" — Omniumtenebre, 2026-01-01, https://www.reddit.com/r/StableDiffusion/comments/1osjn0n/what_is_your_favorite_upscaler/nx0alz5/
  - **Low-res video frames.** "it made it look weird - like sharp and artifacted at the same time" (Remacri, ESRGAN, FaceUpDAT on 480p video) — martinerous, 2025-08-08, https://www.reddit.com/r/StableDiffusion/comments/1mk7xaq/and_if_we_use_in_wan22_the_models_i2v_in_high/n7l1isk/
  - **Line art.** "it has other presets too like Remacri but I don't find them great either - … little lineart details can get kind of blurry or smeary" — Nazgarmar, 2025-07-17, https://www.reddit.com/r/StableDiffusion/comments/1m25jqh/whats_the_current_best_way_to_upscale_anime_style/
  - **Flux images.** "upscaled flux images look significantly worse & artifacty" — PixarCEO, 2024-08-17, https://www.reddit.com/r/StableDiffusion/comments/1euqrd4/whats_the_best_upscaler_for_flux/
  - **Before a diffusion pass.** Remacri named among "sharpening esrgan-family models" — Pale_Coyote7451 (§10 URL).
- **Compared to:**
  - "Remacri is somewhere in the middle for all of them to me" (anime and illustration tests) — hollowstrawberry, https://huggingface.co/hollowstrawberry/upscalers-backup
  - Power scale "Ultrasharp > Ultramix Balanced > Remacri > Fast Real-ESRGAN … the higher the power, the more likely it is for a model to add unfounded details" — aaronliu0130 (app maintainer), https://github.com/orgs/upscayl/discussions/544
  - "Ultrasharp tends to produce more flat textures, so it's more suitable for anime style images, and something like Remarchi should work a little better with realistic style" — 8RETRO8, 2026-05-23, https://www.reddit.com/r/StableDiffusion/comments/1tlgkj3/flux2klein_tile_upscaler_node_basically_usdu_with/onibl23/
- **Voices:** 24.
  - **Keeps skin, hair and fine texture better than UltraSharp:** 6 agree (roxoholic, NoNipsPlease, Ikythecat, Mundane-Apricot6981, Rumaben79, 8RETRO8).
  - **Cartoon, comic and manga pages, and card art:** 4 agree (Adventurous-Nerve858, Acceleratorlvl7, knuckles_didintdied, dndkk2020).
  - **Over-sharp / artifacts / "processed":** 5 agree (Prompting Pixels, k7rk, Omniumtenebre, martinerous, PixarCEO).
  - **Contradiction on the look:** 2 say it is smoother or slightly blurring (AIposting, Rumaben79); 5 say it over-sharpens.

## 12. 4x UltraMix Balanced (Kim2091; Upscayl "Ultramix (Non-Commercial)")

- **Good for:**
  - **Keeping detail.** "Try these two models Ultrasharp and Ultramix Balanced these leave more details." (advice to someone who loses noise and texture) — DJboutit, https://github.com/upscayl/upscayl/issues/849
  - **Sharpness kept in Upscayl.** "The only ones that give a good result are the non-commercial models ReMacri and Ultramix." — connierebel, https://github.com/upscayl/upscayl/issues/1336
  - **Blurry 300 dpi card scans.** "try a free program called upscale (using the ai model 'ultramix non-commercial') … This should resolve the the clarity." — PlaneAcrobatic4918, 2026-09-10, https://www.reddit.com/r/magicproxies/comments/1wbk0uv/advice_to_increase_print_quality/p8wxb47/
  - **Card prints.** "I run the sheets through Upscayl on Windows using the Ultramix AI model at 3x or 4x. Even a 300dpi 9-card PNG straight from Proxxied comes out sharper after that than its native 1200dpi export." — Status-Mycologist-26, 2026-07-06, https://www.reddit.com/r/magicproxies/comments/1uo711y/any_other_et2860_users_new_to_proxying/ovu0f8t/
  - **Card proxies (workflow).** "Upscayl app > Batch Upscale>ultramix non commercial>image scale 4x" — RikuExe00, 2026-08-10, https://www.reddit.com/r/mpcproxies/comments/1vk4wsb/question_about_upscaling_and_proxxied/p2rlx1d/
- **Bad for:** no direct complaints found.
- **Compared to:**
  - "Ultrasharp > Ultramix Balanced > Remacri > Fast Real-ESRGAN" (power), and higher power means more invented detail — aaronliu0130 (app maintainer), https://github.com/orgs/upscayl/discussions/544
  - "2xNomosUni_esrgan for realistic stuff, works better than siax or ultramix imo" — radianart (which UltraMix variant is not named), https://www.reddit.com/r/StableDiffusion/comments/1j8i0bv/what_is_the_best_upscaler_currently/mh5x51z/
- **Voices:** 7.
  - **Keeps or adds sharpness and detail; used for blurry card scans:** 5 agree (DJboutit, connierebel, PlaneAcrobatic4918, Status-Mycologist-26, RikuExe00).
  - This **contradicts our card's wording only in emphasis**: users treat it as a *sharp* option. The maintainer places it between UltraSharp and Remacri, i.e. "balanced" = a bit gentler than UltraSharp V1.

## 13. 4x UltraSharp V2 Lite (Kim2091, RealPLKSR)

- **Good for:**
  - **Speed, with good quality.** "I did also throw in a UltrasharpV2-Lite model that works *pretty damn good* and will complete significantly faster." — ghost990, 2026-09-01, https://www.reddit.com/r/magicproxies/comments/1vxltp4/v_020_proxy_scaler_update_deck_list_upscale_pdf/p79bh5c/
  - **Speed (timed).** "To upscale 27 images, it tool 1 minute and 2 seconds using the Ultrasharp V2 Lite model. With the non-lite model it took around 9 minutes" — tomatus89, 2026-09-04, https://www.reddit.com/r/magicproxies/comments/1vxltp4/v_020_proxy_scaler_update_deck_list_upscale_pdf/p7qa1ds/
  - **Realistic photos (app maker's choice).** An on-device app uses "4x_UltraSharpV2_Lite for realistic photos" — mikemend, 2025-10-13, https://www.reddit.com/r/StableDiffusion/comments/1o5aaxj/local_dream_210_with_upscalers_for_npu_models/
  - **Middle tier.** "For good, I do the same, but replace ClearReality with UltraSharp V2 Lite." — nitroviper (§10 URL).
- **Bad for:**
  - **Quality vs full V2.** "not the same quality as regular UltrasharpV2" — ghost990, 2026-10-01, https://www.reddit.com/r/magicproxies/comments/1wuazbn/proxy_scaler_v040_upscaling_custom_images_vulkan/pd5j25w/
  - **Quality vs full V2.** "there is also a 'full' version … It will do a better upscale but it will take longer." — sci032, 2025-11-22, https://www.reddit.com/r/comfyui/comments/1p3191p/looking_for_a_favour_please/nq4812x/
  - **Card texture.** "The UltrasharpV2 and Lite variant will 'sharpen' them and produce some odd artifacts." — ghost990, 2026-09-02, https://www.reddit.com/r/magicproxies/comments/1vxltp4/v_020_proxy_scaler_update_deck_list_upscale_pdf/p7bgvpo/
  - **Against ClearReality.** "4x-ClearRealityV1_fp16 TensorRT version give better results than the 4x-UltraSharpV2_Lite" — JumpingQuickBrownFox (§10 URL).
- **Voices:** 6.
  - **Much faster than full V2 (about 9x in one timing):** 3 agree (ghost990, tomatus89, sci032).
  - **Below full V2 in quality:** 2 agree (ghost990, sci032).
  - **Better or worse than ClearReality:** 1 voice each way (JumpingQuickBrownFox vs the author, who does not count).

## 14. 4x UltraSharp V2 (Kim2091, DAT2)

- **Good for:**
  - **Best free local model; skin and eyes.** "I found UltraSharp v2 to be the best local model"; "For free I prefer the UltraSharp V2 model, which does a great job of preserving the details in the skin and eyes while also being performant." — Prompting Pixels, 2025-06-02, https://medium.com/code-canvas/testing-different-upscalers-paid-vs-free-options-1260ab82d403
  - **Illustration.** "UltraSharp V2 again performs well in keeping true to the original while keeping lines sharp and colors vibrant" — Prompting Pixels (same URL).
  - **Against V1, materials.** "I saw considerable improvements from using UltraSharpV2 over V1 … Some of the material surfaces are better as well." — Dangthing, 2025-05-24, https://www.reddit.com/r/StableDiffusion/comments/1ku1pc1/ultrasharpv2_is_released_the_successor_to_one_of/mtzl5na/
  - **Architecture, food, textures.** "There is more detail when zooming in, window structures make more sense, reflections are better, floor textures look quite good" — raven_xcx, 2025-05-24, https://www.reddit.com/r/StableDiffusion/comments/1ku1pc1/ultrasharpv2_is_released_the_successor_to_one_of/mu0hvgm/
  - **Overall pick.** "IMO these two models are by far the best for upscaling images." (with SwinIR-L) — Beavisguy, 2026-04-01, https://www.reddit.com/r/upscaling/comments/1q9925m/when_using_upscayl_what_are_the_best_models_to/odoh0hy/
  - **Printed card art.** "UltrasharpV2 as an option which in my opinion more often will give you a image that's so similar to the real card on paper it's nuts" — ghost990, 2026-09-30, https://www.reddit.com/r/magicproxies/comments/1wuazbn/proxy_scaler_v040_upscaling_custom_images_vulkan/pd28uzc/
  - **Game textures with a paper look.** "This model helps to keep VC series signature paper look and not to make it yet another generic anime." — ExSolus, 2026-08-15, https://www.reddit.com/r/valkyria/comments/1vp1dyz/valkyria_chronicles_2_ulus10515_hd_texture_pack/
  - **Anime hires fix.** "it's free and good for every situation" — NearbyCriticism, 2025-07-17, https://www.reddit.com/r/StableDiffusion/comments/1m25jqh/whats_the_current_best_way_to_upscale_anime_style/n3mk8b3/
  - **JPEG artifacts.** "upscale it with something like UltraSharpV2 - this will remove the JPEG artifacts" — ComethTheS, 2025-08-17, https://www.reddit.com/r/comfyui/comments/1mn3rhm/mimic_text_art_style/n985rba/
  - **Against Remacri.** "4x remarci or better 4x ultrasharp v2" — Ok-Establishment4845, 2025-07-08, https://www.reddit.com/r/comfyui/comments/1lubc6z/nsfw_image_upscaler_workflows/n1zzoow/
  - **General.** "The new Ultrasharp v2 is quite impressive as well." — Last_Ad_3151, 2025-08-21, https://www.reddit.com/r/comfyui/comments/1mvqeyw/what_upscaler_is_the_best_now/n9ts0fi/
  - **General (web-app builder, interested party).** "That model is beautiful IMO, works a lot better than huge models." — HatEducational9965, 2026-06-07, https://www.reddit.com/r/SideProject/comments/1tzaec2/free_4x_image_upscaling_in_your_browser/
- **Bad for:**
  - **Speed.** "The most major drawback is that the upscale process is almost 3x slower on the UltraSharp V2 model stages." — Dangthing (URL above).
  - **Speed.** "I re-recognized the slowness of 4x-UltraSharpV2" — hirorohi, 2026-03-13, https://note.com/hirorohi03/n/na505681a1130
  - **Speed (timed).** About 9 minutes vs 1 minute for Lite on 27 images — tomatus89 (§13).
  - **GPU load.** "UltraSharp V2 is a beast. Small model in MB, but crazy GPU hungry at inference times." — nitroviper, 2026-04-14, https://www.reddit.com/r/upscaling/comments/1sko20k/local_inbrowser_upscaling/og2ae4a/
  - **Weak hardware.** "they'll bring my M2 MacBook to a halt" — ghost990, 2026-08-25, https://www.reddit.com/r/magicproxies/comments/1vxltp4/v_020_proxy_scaler_update_deck_list_upscale_pdf/p5qv1rz/
  - **Photoreal skin.** "V2 resulted in noise-like grains appearing everywhere, making it look dirty." — hirorohi, same URL as above.
  - **Anime grain.** "V2 has a stronger grainy feel than V1, which can sometimes feel like noise." — hirorohi, same URL.
  - **Against V1.** "4x-ultrasharp (in my opinion v1 did better than v2)" — yezreddit, 2026-01-02, https://www.reddit.com/r/StableDiffusion/comments/1q0dsnj/zimageturbo_vs_qwen_image_2512/nx83cif/
  - **Faces.** "UltraSharp V2, which, I see is for faces, is not working either" — cherishjoo, 2026-01-20, https://www.reddit.com/r/upscaling/comments/1qh5ia3/face_restorationhow_do_you_think_it_works/o0mwqtc/
  - **Flat textures.** "Ultrasharp tends to produce more flat textures, so it's more suitable for anime style images" (said after testing V2) — 8RETRO8 (§11 URL).
  - **Icons.** "Ultrasharp V2 and RealESRGAN_x4plus_anime_6B, not very good." — OrdinaryAdditional91 (§3 URL).
- **Compared to:**
  - V1 vs V2: Dangthing and raven_xcx say V2 is better; yezreddit and hirorohi (on grain) say V1 is better or cleaner.
  - Speed claims conflict too: one voice says "fast (with generations typically less than 10 seconds per image on an RTX 3060)" (Prompting Pixels) and the web-app builder calls it "small and fast". Five voices report it as slow or GPU-heavy.
- **V1 only, for context (not this model):**
  - "4x-UltraSharp V1 also had a very strong processed look, making the skin look like an illustration." — hirorohi, https://note.com/hirorohi03/n/na505681a1130
  - "Upscaling it with ultrasharp is out of question" (realistic soft skin) — KylseS, https://www.reddit.com/r/FluxAI/comments/1moc45y/finally_got_rid_of_that_sharp_look_soft_skin/n8gunh5/
  - "I used Ultrasharp for a while, but on i spection of lots of my upscales I found out it leaves a lot of artefacts." — Candiru666, https://www.reddit.com/r/comfyui/comments/1j8i0yv/what_is_the_best_upscaler_currently/mhb9uo4/
  - "I've eventually settled on 4x-UltraSharp as my favorite … it does at least preserve the colors" — darkroast175696, https://civitai.com/articles/8456/upscaling-guide
- **Voices:** 18 on V2 itself.
  - **Detail, materials and architecture; better than V1:** 4 agree (Prompting Pixels, Dangthing, raven_xcx, Beavisguy).
  - **Faithful on printed art, game textures and illustration:** 3 agree (ghost990, ExSolus, Prompting Pixels).
  - **Slow / GPU-hungry:** 5 agree (Dangthing, hirorohi, tomatus89, nitroviper, ghost990).
  - **Grain/noise on photoreal skin:** 1 (hirorohi). This contradicts Prompting Pixels' "preserving the details in the skin".

---

## Summary table

| model | users: best for | users: avoid for | voices |
|---|---|---|---|
| Real-ESRGAN General v3 | speed; paintings and art (WDN end); clean small text and card art | photos when you want crisp detail (softer than x4plus) | 11 (fast 3; softer 2; text and cards 2) |
| Real-ESRGAN x4plus | clean, low-artifact general/photo upscale; faces keep their structure | fine texture (smeared, "3D animation"/painted look); anime-style images | 13 (clean 2; photo/faces 2; processed look 3) |
| Real-ESRGAN x4plus anime (6B) | anime, flat colour, clean line art, cel shading; JPEG cleanup on anime | photos and paintings (turns them anime); tiny dots and thin text; keeping the original drawing style | 15 (anime 7; restyles 2; loses fine detail 3) |
| Real-ESRGAN AnimeVideo v3 | anime/cartoon video and frames, comics, manga lines; fast | photos and live-action faces ("unphoto", simplified faces) | 14 (2D 7; not photos 3; fast 2) |
| 4xNomos8kSC | realistic upscale, preferred over Siax/UltraSharp V2 by one user | — no complaints found | 2 (+1 unspecified "Nomos") |
| 4xHFA2k | anime screencaps and anime images; fast | sharp photos (soft, loses sharpness in Upscayl) | 3 (anime 1; photo +1 / −1) + 1 maintainer suggestion |
| 4x NMKD Superscale | realistic photos, skin, animal fur, detail retention; low-quality photos | can be too sharp; artifacts on high-contrast edges; heavy on VRAM | 11 (realistic/detail 5; too sharp 2) |
| 4x NMKD Siax | realistic photos, people, skin, fur, fine structures; "unopinionated" | adds grain/noise and can look over-sharp; white dots on dark/flat areas; slow | ~25 (realistic 10+; grain/over-sharp 6; dots 3; humans 5 good / 2 bad) |
| 4xNomosWebPhoto RealPLKSR | — (only "fast/light" listing) | input that was already enlarged before upscaling | 2 (no quality reviews) |
| 4x ClearReality V1 | fast, tiny; natural look; fur/hair for one user; quick video passes | fine fur detail (soft/blurred) for another user | 10 (fast 4; soft 2; natural 1) |
| 4x Remacri | skin/hair/fine texture (vs UltraSharp V1); comic/manga pages, cartoons, card art; video frames | landscapes and eyes/hair (over-sharp, artifacts, "processed"); fine line art can smear | 24 (texture 6; 2D/cards 4; over-sharp 5; smoother 2) |
| 4x UltraMix Balanced | keeping sharpness and detail; blurry card scans for print | — no complaints found | 7 (sharp/detail 5) |
| 4x UltraSharp V2 Lite | about 9x faster than V2 with decent quality; realistic photos in one app | anything where full V2 quality matters; one user prefers ClearReality | 6 (fast 3; below V2 2) |
| 4x UltraSharp V2 | detail, materials, architecture; printed art/game textures; illustration lines and colours; JPEG cleanup | speed and weak GPUs (slow, GPU-hungry); grain on photoreal skin (one test); some prefer V1 | 18 (detail 4; faithful art 3; slow 5; grain 1) |

## Where users contradict the authors (researches/11)

- **General v3.** The authors say "not too strong deblur and denoise capacity", and users agree it is soft on photos. Users add something the authors never say: it is good on small text and card art, cleaner than the anime model.
- **x4plus.** The authors promise "enhance details while removing annoying artifacts". 3 users describe a smeared-texture "3D animation" or painted look. The artifact-free side is confirmed by 2.
- **AnimeVideo v3.** The authors list it for anime **video**. Users use it as much for still 2D art, comics and manga, and one user rates it above the 6B still-image model on comics. That contradicts both the authors' framing and another user.
- **Nomos8kSC.** The author says photos. A FaceFusion round-up files it as anime/2D. No test backs either.
- **HFA2k.** The author says anime. One user uses it on photos of people and likes it; another finds it soft on photos.
- **NMKD Superscale.** The author says photos "with noise and compression artifacts". Users mostly use it on clean AI renders, and one says "should only be used for low-quality images", which matches the author.
- **Siax.** The author says "clean and slightly compressed". Users agree it suits clean renders, and add that it injects grain, which some like and some don't.
- **ClearReality.** The author says "softer, more natural look". 2 users confirm it is softer, and it costs fur detail. One calls it a "sharpening" model.
- **Remacri.** The author says "more details and less smoothing … skin … don't become mushy". 6 users confirm it on skin. 5 report over-sharpening, and 2 report the opposite (slight blur).
- **UltraMix Balanced.** Users treat it as a sharp, detail-keeping option. The maintainer ranks it just under UltraSharp V1, which is consistent with the author's "more balanced" than UltraSharp.
- **UltraSharp V2.** The author says "easily the best model I've ever trained". Most users agree on quality, but one test finds dirty grain on photoreal skin, and 5 users stress that it is slow. The author never mentions speed.

## Models with (almost) no reviews

- **4xNomosWebPhoto RealPLKSR:** no quality review; only a "fast/light" listing and one known failure case.
- **4xNomos8kSC:** 2 short positives.
- **4xHFA2k:** 3 voices, and one person supplies most of the anime praise.
