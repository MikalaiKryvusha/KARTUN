// kartun_engine.cpp - KARTUN native upscaler: an NCNN Real-ESRGAN net run tile by tile, exposed to Kotlin via JNI.
//
// The tiling scheme (tiles with a 10 px prepadding, output cropped back to tile * scale) follows the approach of
// Real-ESRGAN-ncnn-vulkan (MIT, (c) 2021 Xintao Wang; (c) 2019 nihui) - written anew here, no code copied from
// Upscayl (AGPL-3.0). NCNN itself is BSD-3 (Tencent). [NOT-TESTED]
//
// Contract (phase 0): input and output are Android Bitmaps in ARGB_8888; output must be exactly scale x input.
// All calls must come from ONE thread (research 03 section B, lesson 7). Return codes: 0 ok, negative = error.
// Phase 2: after every tile the engine calls `listener.onTile(done, total)` (a com.kriniks.kartun.engine.TileListener,
// may be null); a `false` answer stops the run between tiles with kErrCancelled - the progress ring and the cancel
// button of the processing screen (plans/03 step 5). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <string>
#include <vector>
#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "net.h"
#include "gpu.h"

#define LOG_TAG "KARTUN_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

constexpr int kPrepadding = 10;             // context pixels around each tile, dropped from the output
constexpr float kNorm = 1.f / 255.f;        // Real-ESRGAN expects RGB in [0, 1]
constexpr float kDenorm = 255.f;

constexpr int kErrBadArgs = -1;
constexpr int kErrBitmap = -2;
constexpr int kErrExtract = -3;
constexpr int kErrCancelled = -4;

bool g_gpuInstance = false;

// One loaded model: the NCNN net plus how it was configured. `fixed` > 0: the net was traced at one input size (DAT2 -
// tools/convert-model.py --fixed writes `0=<w> 1=<h>` into its Input layer); such a net gets tiles of exactly that size.
struct Upscaler {
    ncnn::Net net;
    bool gpu = false;
    int fixedW = 0;
    int fixedH = 0;
};

// Reads `0=<w> 1=<h>` from the Input line of a .param text file; leaves 0 when the net takes any size. [NOT-TESTED]
void readFixedInput(const char* paramPath, int& w, int& h) {
    FILE* f = std::fopen(paramPath, "rb");
    if (!f) return;
    char line[512];
    while (std::fgets(line, sizeof line, f)) {
        if (std::strncmp(line, "Input", 5) != 0) continue;
        for (char* t = std::strtok(line, " \t\r\n"); t; t = std::strtok(nullptr, " \t\r\n")) {
            if (std::strncmp(t, "0=", 2) == 0) w = std::atoi(t + 2);
            if (std::strncmp(t, "1=", 2) == 0) h = std::atoi(t + 2);
        }
        break;
    }
    std::fclose(f);
}

void ensureGpuInstance() {
    if (!g_gpuInstance) { ncnn::create_gpu_instance(); g_gpuInstance = true; }
}

}  // namespace

extern "C" {

// Human-readable GPU summary for diagnostics: count, name, fp16 support, heap budget.
JNIEXPORT jstring JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeGpuInfo(JNIEnv* env, jclass) {
    ensureGpuInstance();
    const int count = ncnn::get_gpu_count();
    std::string s = "gpu_count=" + std::to_string(count);
    if (count > 0) {
        const int i = ncnn::get_default_gpu_index();
        const ncnn::GpuInfo& info = ncnn::get_gpu_info(i);
        s += " name=" + std::string(info.device_name());
        s += " fp16_storage=" + std::to_string(info.support_fp16_storage());
        s += " fp16_arith=" + std::to_string(info.support_fp16_arithmetic());
        ncnn::VulkanDevice* vkdev = ncnn::get_gpu_device(i);
        if (vkdev) s += " heap_budget_mb=" + std::to_string(vkdev->get_heap_budget());
    }
    return env->NewStringUTF(s.c_str());
}

// Loads a model. Returns an opaque handle (0 on failure).
JNIEXPORT jlong JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeCreate(JNIEnv* env, jclass, jstring jparam, jstring jbin,
                                                         jboolean useGpu, jint threads) {
    const char* param = env->GetStringUTFChars(jparam, nullptr);
    const char* bin = env->GetStringUTFChars(jbin, nullptr);
    auto* u = new Upscaler();
    if (useGpu) ensureGpuInstance();
    u->gpu = useGpu && ncnn::get_gpu_count() > 0;
    u->net.opt.use_vulkan_compute = u->gpu;
    u->net.opt.use_fp16_storage = true;      // storage in fp16 is safe on the tested drivers
    u->net.opt.use_fp16_packed = true;
    u->net.opt.use_fp16_arithmetic = false;  // fp16 arithmetic gives black output on some Mali/Adreno (research 03 B4)
    u->net.opt.num_threads = threads > 0 ? threads : 4;
    readFixedInput(param, u->fixedW, u->fixedH);
    if (u->fixedW > 0) {
        // A fixed-size net is a DAT transformer: in fp16 storage its first Gemm overflows to inf (ncnn 20260526, seen on
        // the PC's Vulkan GPU 2026-10-15; fp32 storage gives 66.6 dB against PyTorch) - keep its blobs in fp32.
        u->net.opt.use_fp16_storage = false;
        u->net.opt.use_fp16_packed = false;
        LOGI("fixed input %dx%d, fp32 storage", u->fixedW, u->fixedH);
    }
    const int rp = u->net.load_param(param);
    const int rm = u->net.load_model(bin);
    env->ReleaseStringUTFChars(jparam, param);
    env->ReleaseStringUTFChars(jbin, bin);
    if (rp != 0 || rm != 0) {
        LOGE("model load failed: param=%d model=%d", rp, rm);
        delete u;
        return 0;
    }
    LOGI("model loaded, gpu=%d", u->gpu ? 1 : 0);
    return reinterpret_cast<jlong>(u);
}

JNIEXPORT void JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    delete reinterpret_cast<Upscaler*>(handle);
}

// Debug probe: runs the net on a flat w x h x 3 input (0.5 everywhere) up to `blob` and returns the time in ms (negative:
// error). Extracting earlier and later blobs shows where the time goes on a device. [NOT-TESTED]
JNIEXPORT jlong JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeTimeToBlob(JNIEnv* env, jclass, jlong handle, jint w, jint h, jstring jblob) {
    auto* u = reinterpret_cast<Upscaler*>(handle);
    if (!u || w < 1 || h < 1) return kErrBadArgs;
    const char* blob = env->GetStringUTFChars(jblob, nullptr);
    ncnn::Mat in(w, h, 3);
    in.fill(0.5f);
    const auto t0 = std::chrono::steady_clock::now();
    ncnn::Extractor ex = u->net.create_extractor();
    ex.input("data", in);
    ncnn::Mat out;
    const int rc = ex.extract(blob, out);
    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - t0).count();
    env->ReleaseStringUTFChars(jblob, blob);
    return rc != 0 ? kErrExtract : static_cast<jlong>(ms);
}

// Averages each f x f block of an RGBA tile into one pixel of `dst` (row pitch `dstStride`): the network's 4x tile shrunk
// to the output scale right away, so a 1x/2x run never holds the full 4x image (the owner, chat 2026-10-15 ≈ 11:35 +03:00:
// «нужно сделать на входе поддержку файлов больше 12 мб. Думаю, до 100 мб можно давтаь поддержку»). [NOT-TESTED]
static void boxDown(const unsigned char* src, int sw, int sh, int f, unsigned char* dst, size_t dstStride) {
    const int dw = sw / f, dh = sh / f, n = f * f;
    for (int y = 0; y < dh; ++y) {
        unsigned char* d = dst + static_cast<size_t>(y) * dstStride;
        for (int x = 0; x < dw; ++x) {
            int r = 0, g = 0, b = 0;
            for (int yy = 0; yy < f; ++yy) {
                const unsigned char* s = src + (static_cast<size_t>(y * f + yy) * sw + x * f) * 4;
                for (int xx = 0; xx < f; ++xx, s += 4) { r += s[0]; g += s[1]; b += s[2]; }
            }
            d[x * 4 + 0] = static_cast<unsigned char>((r + n / 2) / n);
            d[x * 4 + 1] = static_cast<unsigned char>((g + n / 2) / n);
            d[x * 4 + 2] = static_cast<unsigned char>((b + n / 2) / n);
            d[x * 4 + 3] = 255;
        }
    }
}

// Upscales `input` into `output` tile by tile, reporting each finished tile to `listener`. The network enlarges by
// `scale`; the output is `outScale` x input (outScale divides scale: each tile is box-averaged down by scale/outScale).
JNIEXPORT jint JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeUpscale(JNIEnv* env, jclass, jlong handle, jobject input,
                                                          jobject output, jint scale, jint outScale, jint tile, jobject listener) {
    auto* u = reinterpret_cast<Upscaler*>(handle);
    // A fixed-size net decides the tile itself: its input minus the context on both sides.
    if (u && u->fixedW > 0) tile = std::min(u->fixedW, u->fixedH) - 2 * kPrepadding;
    if (!u || scale < 1 || outScale < 1 || scale % outScale != 0 || tile < 16) return kErrBadArgs;
    const int shrink = scale / outScale;

    AndroidBitmapInfo inInfo{}, outInfo{};
    if (AndroidBitmap_getInfo(env, input, &inInfo) < 0 || AndroidBitmap_getInfo(env, output, &outInfo) < 0)
        return kErrBitmap;
    if (inInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || outInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        outInfo.width != inInfo.width * outScale || outInfo.height != inInfo.height * outScale)
        return kErrBadArgs;

    void* inPixels = nullptr;
    void* outPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, input, &inPixels) < 0) return kErrBitmap;
    if (AndroidBitmap_lockPixels(env, output, &outPixels) < 0) {
        AndroidBitmap_unlockPixels(env, input);
        return kErrBitmap;
    }

    const int w = static_cast<int>(inInfo.width);
    const int h = static_cast<int>(inInfo.height);
    // Each padded tile is read straight from the bitmap: a whole-photo float Mat would be 12 bytes a pixel (1.2 GB for
    // 100 MP). NCNN uploads each tile to the GPU itself.
    const auto* inBase = static_cast<const unsigned char*>(inPixels);
    const float norm[3] = {kNorm, kNorm, kNorm};
    std::vector<unsigned char> tilePx;  // one 4x tile in RGBA before the box-down (only when shrink > 1)

    // Progress: the tile count is known up front, so the caller can show an exact percentage.
    const int total = ((w + tile - 1) / tile) * ((h + tile - 1) / tile);
    jmethodID onTile = nullptr;
    if (listener) onTile = env->GetMethodID(env->GetObjectClass(listener), "onTile", "(II)Z");
    int done = 0;

    int rc = 0;
    auto* outBase = static_cast<unsigned char*>(outPixels);
    for (int ty = 0; ty < h && rc == 0; ty += tile) {
        for (int tx = 0; tx < w && rc == 0; tx += tile) {
            const int tw = std::min(tile, w - tx);
            const int th = std::min(tile, h - ty);
            // Padded input region, clamped to the image - the upstream engine does the same at the image edge
            // (a replicate-padding variant was measured on the Titan 2026-10-14 and matched the reference worse).
            const int x0 = std::max(tx - kPrepadding, 0);
            const int y0 = std::max(ty - kPrepadding, 0);
            const int x1 = std::min(tx + tw + kPrepadding, w);
            const int y1 = std::min(ty + th + kPrepadding, h);

            ncnn::Mat in = ncnn::Mat::from_pixels(inBase + static_cast<size_t>(y0) * inInfo.stride + static_cast<size_t>(x0) * 4,
                                                  ncnn::Mat::PIXEL_RGBA2RGB, x1 - x0, y1 - y0, static_cast<int>(inInfo.stride));
            in.substract_mean_normalize(nullptr, norm);
            // A fixed-size net: edge tiles (and small images) are padded on the right and bottom by repeating the last
            // row/column; the crop below takes the real region from the top-left, so the padding never reaches the output.
            if (u->fixedW > 0 && (in.w < u->fixedW || in.h < u->fixedH)) {
                ncnn::Mat padded;
                ncnn::copy_make_border(in, padded, 0, u->fixedH - in.h, 0, u->fixedW - in.w, ncnn::BORDER_REPLICATE, 0.f);
                in = padded;
            }

            ncnn::Extractor ex = u->net.create_extractor();
            ex.input("data", in);
            ncnn::Mat out;
            if (ex.extract("output", out) != 0) { rc = kErrExtract; break; }

            const float denorm[3] = {kDenorm, kDenorm, kDenorm};
            out.substract_mean_normalize(nullptr, denorm);

            // Crop the padding away: the tile starts at (tx - x0) * scale inside `out`.
            const int ox = (tx - x0) * scale;
            const int oy = (ty - y0) * scale;
            const int ow = tw * scale;
            const int oh = th * scale;
            ncnn::Mat crop;
            ncnn::copy_cut_border(out, crop, oy, out.h - oy - oh, ox, out.w - ox - ow);

            unsigned char* dst = outBase + static_cast<size_t>(ty * outScale) * outInfo.stride + static_cast<size_t>(tx * outScale) * 4;
            if (shrink == 1) {
                crop.to_pixels(dst, ncnn::Mat::PIXEL_RGB2RGBA, static_cast<int>(outInfo.stride));
            } else {
                tilePx.resize(static_cast<size_t>(ow) * oh * 4);
                crop.to_pixels(tilePx.data(), ncnn::Mat::PIXEL_RGB2RGBA, ow * 4);
                boxDown(tilePx.data(), ow, oh, shrink, dst, outInfo.stride);
            }

            ++done;
            LOGI("tile %d/%d %dx%d", done, total, in.w, in.h);  // logcat time stamps give the per-tile time
            if (onTile && !env->CallBooleanMethod(listener, onTile, done, total)) rc = kErrCancelled;
            if (env->ExceptionCheck()) { env->ExceptionClear(); rc = kErrCancelled; }
        }
    }

    AndroidBitmap_unlockPixels(env, output);
    AndroidBitmap_unlockPixels(env, input);
    return rc;
}

}  // extern "C"
