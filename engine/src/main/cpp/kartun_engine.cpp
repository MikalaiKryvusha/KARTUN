// kartun_engine.cpp - KARTUN native upscaler: an NCNN Real-ESRGAN net run tile by tile, exposed to Kotlin via JNI.
//
// The tiling scheme (tiles with a 10 px prepadding, output cropped back to tile * scale) follows the approach of
// Real-ESRGAN-ncnn-vulkan (MIT, (c) 2021 Xintao Wang; (c) 2019 nihui) - written anew here, no code copied from
// Upscayl (AGPL-3.0). NCNN itself is BSD-3 (Tencent). [NOT-TESTED]
//
// Contract (phase 0): input and output are Android Bitmaps in ARGB_8888; output must be exactly scale x input.
// All calls must come from ONE thread (research 03 section B, lesson 7). Return codes: 0 ok, negative = error.

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <string>
#include <algorithm>

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

bool g_gpuInstance = false;

// One loaded model: the NCNN net plus how it was configured.
struct Upscaler {
    ncnn::Net net;
    bool gpu = false;
};

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

// Upscales `input` into `output` (output = input * scale) tile by tile.
JNIEXPORT jint JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeUpscale(JNIEnv* env, jclass, jlong handle, jobject input,
                                                          jobject output, jint scale, jint tile) {
    auto* u = reinterpret_cast<Upscaler*>(handle);
    if (!u || scale < 1 || tile < 16) return kErrBadArgs;

    AndroidBitmapInfo inInfo{}, outInfo{};
    if (AndroidBitmap_getInfo(env, input, &inInfo) < 0 || AndroidBitmap_getInfo(env, output, &outInfo) < 0)
        return kErrBitmap;
    if (inInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || outInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        outInfo.width != inInfo.width * scale || outInfo.height != inInfo.height * scale)
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
    // Whole input as a normalized RGB Mat (CPU side); NCNN uploads each tile to the GPU itself.
    ncnn::Mat full = ncnn::Mat::from_pixels(static_cast<const unsigned char*>(inPixels),
                                            ncnn::Mat::PIXEL_RGBA2RGB, w, h, static_cast<int>(inInfo.stride));
    const float norm[3] = {kNorm, kNorm, kNorm};
    full.substract_mean_normalize(nullptr, norm);

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

            ncnn::Mat in;
            ncnn::copy_cut_border(full, in, y0, h - y1, x0, w - x1);

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

            unsigned char* dst = outBase + static_cast<size_t>(ty * scale) * outInfo.stride + static_cast<size_t>(tx * scale) * 4;
            crop.to_pixels(dst, ncnn::Mat::PIXEL_RGB2RGBA, static_cast<int>(outInfo.stride));
        }
    }

    AndroidBitmap_unlockPixels(env, output);
    AndroidBitmap_unlockPixels(env, input);
    return rc;
}

}  // extern "C"
