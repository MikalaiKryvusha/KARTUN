# KARTUN user guide

## 1 What it does

**Upscaling** raises the resolution of a picture. The neural network turns every pixel of the source into several pixels of the result and restores fine detail that plain stretching does not have.

KARTUN upscales on the device: the picture never leaves the phone, and processing needs no network.

## 2 How to bring a photo

1. **Gallery.** Tap “Choose a photo” on the home screen and pick a shot.
2. **Another app.** Open a photo in Telegram, the gallery or a browser, tap “Share” and pick KARTUN.
3. **Link.** Copy a link to a picture and tap “Paste a photo link”.

## 4 Scale

**Scale** is how many times the width and height grow. The number of pixels grows as the square of the scale: at 4× there are 16 times more.

| Scale | Passes | Photo 960 × 1280 |
| 1× | 1 | 960 × 1280 |
| **2× · default** | 1 | 1920 × 2560 |
| 4× | 1 | 3840 × 5120 |
| 8× | 2 | 7680 × 10 240 |
| 16× | 2 | 15 360 × 20 480 |

> The network always enlarges the picture exactly 4 times in one pass. Scales 1× and 2× come from scaling the result down, 8× and 16× from a second pass.

## 6 Models

A **model** is a trained neural network. Models differ in what pictures they learned from: one restores photos better, another drawings.

| Model | Purpose | Licence |
| General v3 | photos, fast | BSD-3 · commercial use |
| x4plus | photos, maximum | BSD-3 · commercial use |
