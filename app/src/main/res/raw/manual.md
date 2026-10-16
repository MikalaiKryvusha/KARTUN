# KARTUN user guide

## 1 What it does

**Upscaling** raises the resolution of a picture. The neural network turns every pixel of the source into several pixels of the result and restores fine detail that plain stretching does not have.

KARTUN upscales on the device: the picture never leaves the phone, and processing needs no network.

## 2 How to bring a photo

1. **Gallery.** Tap “Choose a photo” on the home screen and pick a shot.
2. **Another app.** Open a photo in Telegram, the gallery or a browser, tap “Share” and pick KARTUN.
3. **Link.** Copy a link to a picture and tap “Paste a photo link”.

## 3 Setting up

On the “Enhance” screen the photo is pinned on top. Swipe up — the photo folds into a row and shows the settings; pull down — the photo opens full screen.

- **Model.** Here are the two models you used last; the rest are in «All models» under them. The model loads as soon as you choose it.
- **Scale.** A slider from 1× to 16×; the result's size is shown under it.
- **Enhance.** The button shows an estimate of the time. It learns from your previous runs.

> Every setting has an (i) button that briefly explains what it does.

## 4 Scale

**Scale** is how many times the width and height grow. The number of pixels grows as the square of the scale: at 4× there are 16 times more.

| Scale | Passes | Photo 960 × 1280 |
| 1× | 1 | 960 × 1280 |
| 2× | 1 | 1920 × 2560 |
| **4× · default** | 1 | 3840 × 5120 |
| 8× | 2 | 7680 × 10 240 |
| 16× | 2 | 15 360 × 20 480 |

> The network always enlarges the picture exactly 4 times in one pass: every model is trained for 4×, so 4× gives the best result. Scales 1× and 2× come from scaling the result down, 8× and 16× from a second pass.

## 5 Compare and save

1. **Compare.** Touch the picture anywhere and move your finger: “before” on the left, “after” on the right. Two fingers zoom and move the picture.
2. **Save.** Pick a format — PNG without loss, JPEG or WebP with a quality setting. The file lands in Pictures/KARTUN as a new picture; the original stays as it is.
3. **Share.** Send the result to any app through the system menu.

> KARTUN can be left while it works: the progress is in the notification, with “Cancel” there too.

## 6 Models

A **model** is a trained neural network. Models differ in what pictures they learned from: one restores photos better, another drawings.

Two models are built into the app; they work at once and without the network:

| Model | What it is for | Licence |
| 4xNomosWebPhoto RealPLKSR (default) | photos from the web: removes compression and blur | CC BY 4.0 · commercial use with credit |
| 4x NMKD Siax | universal: clean or slightly compressed pictures | WTFPL · commercial use |

**All models.** Twelve more models — for photos, faces, anime and drawings — download with the «⤓ MB» button on the «All models» screen. Every card shows a «Before | After» example, the author, the licence and the size; while a model downloads, a percent ring turns on its card. A downloaded model stays in KARTUN's folder and works without the network; «Delete» frees the space. «Not for commercial use» means the model's author did not allow its commercial use. The models belong to their authors.
