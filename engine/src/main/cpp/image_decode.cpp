// image_decode.cpp - the second door for photos Android's ImageDecoder cannot open (plans/08 step 3; the owner's order in
// GOAL.md: «Входных прям очень много нужно»). Measured 2026-10-16 on the Titan: ImageDecoder refuses TGA, PSD, PPM/PGM, HDR,
// QOI ("unimplemented"). This file decodes them with stb_image v2.30 (MIT or public domain, nothing copied from Upscayl) and
// qoi.h (MIT) into an ARGB_8888 Bitmap the caller made, so no full-size copy lands on the Java heap.
// Formats: TGA, PSD (the composited image), PNM (PPM/PGM), HDR (tone-mapped by stb to 8 bit), PIC, QOI, TIFF (libtiff) - and the JPEG / PNG /
// BMP / GIF that stb also reads, as a fallback when the system codec failed on a broken file.
// [TESTED: 2026-10-16 · Titan, agent door: TGA, PSD, PPM, QOI, TIFF identical to PNG - testcases/reports/2026-10-16_input_formats_titan.md]

#include <android/bitmap.h>
#include <jni.h>
#include <unistd.h>

#include <cstdint>
#include <cstring>
#include <vector>

#define STB_IMAGE_IMPLEMENTATION
#define STBI_NO_STDIO
#define STBI_FAILURE_USERMSG
#include "third_party/stb_image.h"

#define QOI_IMPLEMENTATION
#define QOI_NO_STDIO
#include "third_party/qoi.h"

#include <tiffio.h>

namespace {

// libtiff reads from our in-memory copy of the file through these callbacks (TIFFClientOpen).
struct MemFile {
    const unsigned char* data;
    toff_t size;
    toff_t pos;
};
tsize_t memRead(thandle_t h, tdata_t buf, tsize_t n) {
    auto* f = static_cast<MemFile*>(h);
    toff_t left = f->pos < f->size ? f->size - f->pos : 0;
    if (static_cast<toff_t>(n) > left) n = static_cast<tsize_t>(left);
    std::memcpy(buf, f->data + f->pos, static_cast<size_t>(n));
    f->pos += n;
    return n;
}
tsize_t memWrite(thandle_t, tdata_t, tsize_t) { return 0; }
toff_t memSeek(thandle_t h, toff_t off, int whence) {
    auto* f = static_cast<MemFile*>(h);
    toff_t base = whence == SEEK_CUR ? f->pos : whence == SEEK_END ? f->size : 0;
    f->pos = base + off;
    return f->pos;
}
int memClose(thandle_t) { return 0; }
toff_t memSize(thandle_t h) { return static_cast<MemFile*>(h)->size; }
int memMap(thandle_t, tdata_t*, toff_t*) { return 0; }
void memUnmap(thandle_t, tdata_t, toff_t) {}
void quiet(const char*, const char*, va_list) {}

bool isTiff(const std::vector<unsigned char>& d) {
    return d.size() >= 8 && ((d[0] == 'I' && d[1] == 'I' && (d[2] == 42 || d[2] == 43) && d[3] == 0) ||
                             (d[0] == 'M' && d[1] == 'M' && d[2] == 0 && (d[3] == 42 || d[3] == 43)));
}

TIFF* openTiff(MemFile& f) {
    TIFFSetErrorHandler(quiet);
    TIFFSetWarningHandler(quiet);
    return TIFFClientOpen("mem", "rm", &f, memRead, memWrite, memSeek, memClose, memSize, memMap, memUnmap);
}

// The first image of a TIFF as RGBA rows from the top; libtiff converts every photometric it knows (RGB, palette,
// grey, CMYK, YCbCr, 16 bit) through TIFFReadRGBAImageOriented. Caller frees with free().
unsigned char* decodeTiff(const std::vector<unsigned char>& d, int* w, int* h) {
    MemFile f{d.data(), static_cast<toff_t>(d.size()), 0};
    TIFF* t = openTiff(f);
    if (!t) return nullptr;
    uint32_t tw = 0, th = 0;
    TIFFGetField(t, TIFFTAG_IMAGEWIDTH, &tw);
    TIFFGetField(t, TIFFTAG_IMAGELENGTH, &th);
    unsigned char* px = nullptr;
    if (tw > 0 && th > 0) {
        px = static_cast<unsigned char*>(malloc(static_cast<size_t>(tw) * th * 4));
        // Each pixel is a uint32 A<<24|B<<16|G<<8|R: on little-endian ARM the bytes already lie as R, G, B, A.
        if (px && !TIFFReadRGBAImageOriented(t, tw, th, reinterpret_cast<uint32_t*>(px), ORIENTATION_TOPLEFT, 0)) {
            free(px);
            px = nullptr;
        }
    }
    TIFFClose(t);
    *w = static_cast<int>(tw);
    *h = static_cast<int>(th);
    return px;
}

// Reads the whole file behind [fd] from its start; the caller keeps owning the descriptor.
bool readAll(int fd, std::vector<unsigned char>& out) {
    if (lseek(fd, 0, SEEK_SET) < 0) return false;
    out.clear();
    unsigned char buf[1 << 16];
    for (;;) {
        ssize_t n = read(fd, buf, sizeof buf);
        if (n < 0) return false;
        if (n == 0) break;
        out.insert(out.end(), buf, buf + n);
    }
    return !out.empty();
}

bool isQoi(const std::vector<unsigned char>& d) { return d.size() >= 14 && std::memcmp(d.data(), "qoif", 4) == 0; }

uint32_t be32(const unsigned char* p) { return (uint32_t(p[0]) << 24) | (uint32_t(p[1]) << 16) | (uint32_t(p[2]) << 8) | p[3]; }

}  // namespace

// [width, height] of the picture, or null when neither stb nor QOI knows it.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_kriniks_kartun_engine_ImageCodec_nativeSize(JNIEnv* env, jclass, jint fd) {
    std::vector<unsigned char> d;
    if (!readAll(fd, d)) return nullptr;
    int w = 0, h = 0, n = 0;
    if (isQoi(d)) {
        w = static_cast<int>(be32(d.data() + 4));
        h = static_cast<int>(be32(d.data() + 8));
    } else if (isTiff(d)) {
        MemFile f{d.data(), static_cast<toff_t>(d.size()), 0};
        TIFF* t = openTiff(f);
        if (!t) return nullptr;
        uint32_t tw = 0, th = 0;
        TIFFGetField(t, TIFFTAG_IMAGEWIDTH, &tw);
        TIFFGetField(t, TIFFTAG_IMAGELENGTH, &th);
        TIFFClose(t);
        w = static_cast<int>(tw);
        h = static_cast<int>(th);
    } else if (!stbi_info_from_memory(d.data(), static_cast<int>(d.size()), &w, &h, &n)) {
        return nullptr;
    }
    if (w <= 0 || h <= 0) return nullptr;
    jintArray r = env->NewIntArray(2);
    jint v[2] = {w, h};
    env->SetIntArrayRegion(r, 0, 2, v);
    return r;
}

// Decodes the file into [bitmap] (ARGB_8888, exactly the size nativeSize returned): RGBA with alpha premultiplied, as
// Android keeps ARGB_8888. Returns false on any failure; the bitmap is then left unspecified.
extern "C" JNIEXPORT jboolean JNICALL
Java_com_kriniks_kartun_engine_ImageCodec_nativeDecodeInto(JNIEnv* env, jclass, jint fd, jobject bitmap) {
    std::vector<unsigned char> d;
    if (!readAll(fd, d)) return JNI_FALSE;
    int w = 0, h = 0;
    unsigned char* px = nullptr;
    const bool qoi = isQoi(d), tiff = !qoi && isTiff(d);
    if (qoi) {
        qoi_desc desc;
        px = static_cast<unsigned char*>(qoi_decode(d.data(), static_cast<int>(d.size()), &desc, 4));
        w = static_cast<int>(desc.width);
        h = static_cast<int>(desc.height);
    } else if (tiff) {
        px = decodeTiff(d, &w, &h);
    } else {
        int n = 0;
        px = stbi_load_from_memory(d.data(), static_cast<int>(d.size()), &w, &h, &n, 4);
    }
    if (!px) return JNI_FALSE;
    std::vector<unsigned char>().swap(d);  // the file bytes are no longer needed while copying

    AndroidBitmapInfo info;
    void* dst = nullptr;
    bool ok = AndroidBitmap_getInfo(env, bitmap, &info) == ANDROID_BITMAP_RESULT_SUCCESS &&
              info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 && static_cast<int>(info.width) == w &&
              static_cast<int>(info.height) == h && AndroidBitmap_lockPixels(env, bitmap, &dst) == ANDROID_BITMAP_RESULT_SUCCESS;
    if (ok) {
        for (int y = 0; y < h; ++y) {
            const unsigned char* s = px + static_cast<size_t>(y) * w * 4;
            unsigned char* t = static_cast<unsigned char*>(dst) + static_cast<size_t>(y) * info.stride;
            for (int x = 0; x < w; ++x, s += 4, t += 4) {
                const unsigned a = s[3];
                if (a == 255) {
                    std::memcpy(t, s, 4);
                } else {  // premultiply, rounding to nearest
                    t[0] = static_cast<unsigned char>((s[0] * a + 127) / 255);
                    t[1] = static_cast<unsigned char>((s[1] * a + 127) / 255);
                    t[2] = static_cast<unsigned char>((s[2] * a + 127) / 255);
                    t[3] = static_cast<unsigned char>(a);
                }
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
    if (qoi || tiff) free(px); else stbi_image_free(px);
    return ok ? JNI_TRUE : JNI_FALSE;
}
