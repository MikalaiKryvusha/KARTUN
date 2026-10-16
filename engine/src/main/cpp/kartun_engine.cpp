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
#include <cerrno>
#include <cmath>
#include <fcntl.h>
#include <unistd.h>

#include "net.h"
#include "gpu.h"

#define LOG_TAG "KARTUN_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

constexpr int kPrepadding = 10;             // context pixels around each tile, dropped from the output
constexpr float kNorm = 1.f / 255.f;        // Real-ESRGAN expects RGB in [0, 1]
constexpr float kDenorm = 255.f;
constexpr float kRoundMean = -0.5f / 255.f;  // shifts the output by half a step before the 8-bit cast: rounding

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

// Debug switch for driver investigations: a file `<param>.opts` next to the model ("lightmode=0 local_memory=0
// subgroup=0 coopmat=0", any subset) turns the named ncnn options off before the net loads, so one device run tells which
// shader path breaks (researches/08 section 11). Models shipped in the APK never have such a file. [TESTED: 2026-10-15 ·
// Titan: the logcat line showed each switch applied; lightmode=0 / local_memory=0 changed the DAT2 SDPA output, subgroup=0 crashed ncnn]
void applyDebugOpts(const char* paramPath, ncnn::Option& opt) {
    const std::string path = std::string(paramPath) + ".opts";
    FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return;
    char buf[256] = {0};
    const size_t n = std::fread(buf, 1, sizeof buf - 1, f);
    std::fclose(f);
    const std::string s(buf, n);
    if (s.find("lightmode=0") != std::string::npos) opt.lightmode = false;
    if (s.find("local_memory=0") != std::string::npos) opt.use_shader_local_memory = false;
    if (s.find("subgroup=0") != std::string::npos) opt.use_subgroup_ops = false;
    if (s.find("coopmat=0") != std::string::npos) opt.use_cooperative_matrix = false;
    LOGI("debug opts from %s: lightmode=%d local_memory=%d subgroup=%d coopmat=%d", path.c_str(), opt.lightmode,
         opt.use_shader_local_memory, opt.use_subgroup_ops, opt.use_cooperative_matrix);
}

void ensureGpuInstance() {
    if (!g_gpuInstance) { ncnn::create_gpu_instance(); g_gpuInstance = true; }
}

}  // namespace

extern "C" {

// Native stderr goes nowhere in an Android app, and that is where libomp prints why it aborts ("OMP: Error #13: Assertion
// failure at kmp_affinity…", bugs/01). Points stderr at a file in the app's crash folder: opened in append mode and left
// unbuffered, so every line is on the disk before the abort that follows it. Returns 0, or errno. [NOT-TESTED]
JNIEXPORT jint JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeRedirectStderr(JNIEnv* env, jclass, jstring jpath) {
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    const int fd = open(path, O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, 0660);
    const int err = fd < 0 ? errno : 0;
    env->ReleaseStringUTFChars(jpath, path);
    if (fd < 0) return err;
    dup2(fd, STDERR_FILENO);
    close(fd);
    setvbuf(stderr, nullptr, _IONBF, 0);
    // The witness: this line in the file proves that what native code prints to stderr lands there.
    fprintf(stderr, "native stderr -> this file, pid %d\n", getpid());
    return 0;
}

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
    applyDebugOpts(param, u->net.opt);
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

// Logs a blob's shape and numbers - the sum, the mean |x|, the NaN/inf count and the first values - so a layer on the
// device can be compared with the same layer on the PC (tools/dat-blob-stats.py prints the same line). Walks each
// channel's w*h*d values (cstep may pad a channel). [TESTED: 2026-10-15 · Titan: DAT2 blobs vs the PC, three bisection
// passes found sdpa_4671 - researches/08 section 10]
static void logBlobStats(const char* blob, const ncnn::Mat& m) {
    double sum = 0, sabs = 0;
    long bad = 0, n = 0;
    const int per = m.w * m.h * m.d;
    for (int q = 0; q < m.c; ++q) {
        const float* p = m.channel(q);
        for (int i = 0; i < per; ++i) {
            const float v = p[i];
            if (v != v || v > 3e38f || v < -3e38f) { ++bad; continue; }
            sum += v; sabs += v < 0 ? -v : v; ++n;
        }
    }
    const float* p0 = m.channel(0);
    LOGI("{\"blobstat\":\"%s\",\"dims\":%d,\"w\":%d,\"h\":%d,\"d\":%d,\"c\":%d,\"elempack\":%d,\"sum\":%.6g,\"mabs\":%.6g,\"bad\":%ld,"
         "\"v\":[%.5g,%.5g,%.5g,%.5g]}", blob, m.dims, m.w, m.h, m.d, m.c, m.elempack, sum, n ? sabs / n : 0.0, bad,
         per > 0 ? p0[0] : 0.f, per > 1 ? p0[1] : 0.f, per > 2 ? p0[2] : 0.f, per > 3 ? p0[3] : 0.f);
}

// Debug probe: runs the net up to `blob` and returns the time in ms (negative: error). The input is `bitmap` (RGBA, RGB
// in [0, 1] as the engine feeds it) when it is exactly w x h, else a flat picture of 0.5. Extracting earlier and later
// blobs shows where the time goes on a device; the blob's numbers go to logcat (logBlobStats) for the layer-by-layer
// comparison with the PC; with `dumpPath` the blob's raw float32 values (c, h, w, no channel padding) are written there
// for a float-vs-float check against PyTorch. [TESTED: 2026-10-15 · Titan, test128 through DAT2: 66.58 dB vs PyTorch]
JNIEXPORT jlong JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeTimeToBlob(JNIEnv* env, jclass, jlong handle, jint w, jint h, jstring jblob,
                                                             jobject bitmap, jstring jdump) {
    auto* u = reinterpret_cast<Upscaler*>(handle);
    if (!u || w < 1 || h < 1) return kErrBadArgs;
    ncnn::Mat in(w, h, 3);
    in.fill(0.5f);
    AndroidBitmapInfo info{};
    void* px = nullptr;
    if (bitmap && AndroidBitmap_getInfo(env, bitmap, &info) >= 0 && info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 &&
        static_cast<int>(info.width) == w && static_cast<int>(info.height) == h && AndroidBitmap_lockPixels(env, bitmap, &px) >= 0) {
        in = ncnn::Mat::from_pixels(static_cast<const unsigned char*>(px), ncnn::Mat::PIXEL_RGBA2RGB, w, h, static_cast<int>(info.stride));
        AndroidBitmap_unlockPixels(env, bitmap);
        const float norm[3] = {kNorm, kNorm, kNorm};
        in.substract_mean_normalize(nullptr, norm);
    }
    const char* blob = env->GetStringUTFChars(jblob, nullptr);
    const auto t0 = std::chrono::steady_clock::now();
    ncnn::Extractor ex = u->net.create_extractor();
    ex.input("data", in);
    ncnn::Mat out;
    const int rc = ex.extract(blob, out);
    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - t0).count();
    if (rc == 0) logBlobStats(blob, out);
    if (rc == 0 && jdump) {
        const char* dump = env->GetStringUTFChars(jdump, nullptr);
        if (FILE* f = std::fopen(dump, "wb")) {
            const size_t per = static_cast<size_t>(out.w) * out.h * out.d;
            for (int q = 0; q < out.c; ++q) std::fwrite(out.channel(q), sizeof(float), per, f);
            std::fclose(f);
        }
        env->ReleaseStringUTFChars(jdump, dump);
    }
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

// The fractional scale at the output limit (plans/08 step 9; the owner 2026-10-16 ≈ 16:10: «да, правильно» to «4× для
// этого фото не поместится, можем 3,2×»). `src` is the network's whole padded tile in RGBA (sw x sh), its pixel (0, 0)
// standing at (ox, oy) of the network-scale image (imgW x imgH). Output pixels [dx0, dx1) x [dy0, dy1) of `dst` are each
// the area average of their own span [X * fx, (X + 1) * fx) of that image, partial pixels weighted by their overlap. A
// tile writes the output pixels whose CENTRE falls inside it; a span that crosses into the next tile reads the tile's own
// context padding (kPrepadding x scale >= fx), so neighbouring tiles agree on every pixel and no seam appears.
// [TESTED: 2026-10-16 · Titan, 8640 x 5760 -> 28 354 x 18 903, seams border/near 1.025 / 1.035 - testcases/reports/2026-10-16_fractional_scale_titan.md]
static void areaResample(const unsigned char* src, int sw, int sh, double ox, double oy, int imgW, int imgH, double fx, double fy,
                         unsigned char* dst, size_t dstStride, int dx0, int dx1, int dy0, int dy1) {
    // Horizontal spans once per tile: first source column and the weights of the columns it covers.
    // `first` is the span's first column in the tile's own coordinates (may be off the tile; reads are clamped).
    struct Span { int first; std::vector<float> w; };
    auto spans = [](int d0, int d1, double f, double origin, int limit) {
        std::vector<Span> out(static_cast<size_t>(std::max(d1 - d0, 0)));
        for (int d = d0; d < d1; ++d) {
            const double a = d * f, b = std::min((d + 1) * f, static_cast<double>(limit));
            const int i0 = static_cast<int>(std::floor(a)), i1 = std::max(static_cast<int>(std::ceil(b)), i0 + 1);
            Span& s = out[static_cast<size_t>(d - d0)];
            s.first = i0 - static_cast<int>(origin);
            for (int i = i0; i < i1; ++i)
                s.w.push_back(static_cast<float>(std::max(std::min(b, i + 1.0) - std::max(a, static_cast<double>(i)), 1e-6)));
        }
        return out;
    };
    const auto xs = spans(dx0, dx1, fx, ox, imgW);
    const auto ys = spans(dy0, dy1, fy, oy, imgH);
    for (int y = dy0; y < dy1; ++y) {
        const Span& sy = ys[static_cast<size_t>(y - dy0)];
        unsigned char* d = dst + static_cast<size_t>(y) * dstStride;
        for (int x = dx0; x < dx1; ++x) {
            const Span& sx = xs[static_cast<size_t>(x - dx0)];
            float r = 0, g = 0, b = 0, tot = 0;
            for (size_t j = 0; j < sy.w.size(); ++j) {
                const int row = std::clamp(sy.first + static_cast<int>(j), 0, sh - 1);
                const unsigned char* p = src + (static_cast<size_t>(row) * sw) * 4;
                for (size_t i = 0; i < sx.w.size(); ++i) {
                    const int col = std::clamp(sx.first + static_cast<int>(i), 0, sw - 1);
                    const float wgt = sy.w[j] * sx.w[i];
                    const unsigned char* q = p + static_cast<size_t>(col) * 4;
                    r += q[0] * wgt; g += q[1] * wgt; b += q[2] * wgt; tot += wgt;
                }
            }
            d[x * 4 + 0] = static_cast<unsigned char>(std::lround(r / tot));
            d[x * 4 + 1] = static_cast<unsigned char>(std::lround(g / tot));
            d[x * 4 + 2] = static_cast<unsigned char>(std::lround(b / tot));
            d[x * 4 + 3] = 255;
        }
    }
}

// The first output pixel whose centre lies at or after network-scale coordinate `edge` (f = network pixels per output pixel).
static int firstOwned(double edge, double f) { return static_cast<int>(std::ceil(edge / f - 0.5)); }

// Upscales `input` into `output` tile by tile, reporting each finished tile to `listener`. The network enlarges by
// `scale`; the output is `outScale` x input (outScale divides scale: each tile is box-averaged down by scale/outScale),
// or, with outScale 0, exactly the output bitmap's size - any factor up to `scale` (areaResample).
JNIEXPORT jint JNICALL
Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeUpscale(JNIEnv* env, jclass, jlong handle, jobject input,
                                                          jobject output, jint scale, jint outScale, jint tile, jobject listener) {
    auto* u = reinterpret_cast<Upscaler*>(handle);
    // A fixed-size net decides the tile itself: its input minus the context on both sides.
    if (u && u->fixedW > 0) tile = std::min(u->fixedW, u->fixedH) - 2 * kPrepadding;
    const bool fractional = outScale == 0;
    if (!u || scale < 1 || outScale < 0 || tile < 16 || (!fractional && scale % outScale != 0)) return kErrBadArgs;
    const int shrink = fractional ? 0 : scale / outScale;

    AndroidBitmapInfo inInfo{}, outInfo{};
    if (AndroidBitmap_getInfo(env, input, &inInfo) < 0 || AndroidBitmap_getInfo(env, output, &outInfo) < 0)
        return kErrBitmap;
    if (inInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || outInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return kErrBadArgs;
    if (fractional ? (outInfo.width < 1 || outInfo.height < 1 || outInfo.width > inInfo.width * scale ||
                      outInfo.height > inInfo.height * scale)
                   : (outInfo.width != inInfo.width * outScale || outInfo.height != inInfo.height * outScale))
        return kErrBadArgs;
    // Fractional: network-scale pixels per output pixel on each axis (>= 1).
    const double fx = fractional ? static_cast<double>(inInfo.width) * scale / outInfo.width : 0;
    const double fy = fractional ? static_cast<double>(inInfo.height) * scale / outInfo.height : 0;

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

            // (x - mean) * norm with mean = -0.5/255: x * 255 + 0.5, so to_pixels' cast ROUNDS instead of cutting the
            // fraction off (cut, every output was half a step darker: mean -0.46 against PyTorch rounded, 2026-10-15).
            // [TESTED: 2026-10-15 · Titan: DAT2 test108 mean diff 0.037 (was -0.46); x4plus 39.34 dB vs its reference]
            const float rmean[3] = {kRoundMean, kRoundMean, kRoundMean};
            const float denorm[3] = {kDenorm, kDenorm, kDenorm};
            out.substract_mean_normalize(rmean, denorm);

            if (fractional) {
                // The whole padded tile as pixels; its output cells are the pixels whose centres fall inside the tile.
                tilePx.resize(static_cast<size_t>(out.w) * out.h * 4);
                out.to_pixels(tilePx.data(), ncnn::Mat::PIXEL_RGB2RGBA, out.w * 4);
                const int dx0 = firstOwned(static_cast<double>(tx) * scale, fx);
                const int dx1 = tx + tw >= w ? static_cast<int>(outInfo.width) : firstOwned(static_cast<double>(tx + tw) * scale, fx);
                const int dy0 = firstOwned(static_cast<double>(ty) * scale, fy);
                const int dy1 = ty + th >= h ? static_cast<int>(outInfo.height) : firstOwned(static_cast<double>(ty + th) * scale, fy);
                areaResample(tilePx.data(), out.w, out.h, static_cast<double>(x0) * scale, static_cast<double>(y0) * scale,
                             w * scale, h * scale, fx, fy, static_cast<unsigned char*>(outPixels), outInfo.stride,
                             std::max(dx0, 0), std::min(dx1, static_cast<int>(outInfo.width)),
                             std::max(dy0, 0), std::min(dy1, static_cast<int>(outInfo.height)));
                ++done;
                LOGI("tile %d/%d %dx%d", done, total, in.w, in.h);
                if (onTile && !env->CallBooleanMethod(listener, onTile, done, total)) rc = kErrCancelled;
                if (env->ExceptionCheck()) { env->ExceptionClear(); rc = kErrCancelled; }
                continue;
            }

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
