package com.kriniks.kartun.engine

import android.graphics.Bitmap

/**
 * The second decoder (plans/08 step 3): opens the formats Android's ImageDecoder refuses - TGA, PSD, PPM/PGM, HDR, QOI,
 * TIFF (measured on the Titan 2026-10-16: «unimplemented») - with stb_image, qoi.h and libtiff inside libkartun_engine
 * (image_decode.cpp). The file comes as a file descriptor, so a content URI works the same as a path and no copy of the
 * file lands on the Java heap. [TESTED: 2026-10-16 · Titan, agent door - testcases/reports/2026-10-16_input_formats_titan.md]
 */
object ImageCodec {
    init { System.loadLibrary("kartun_engine") }

    /**
     * Decodes the file behind [fd] (the caller keeps owning it) as an ARGB_8888 bitmap, or returns null when the format
     * is unknown or the file is broken. [check] sees width and height before the pixels are allocated and may throw
     * (the app's size limit).
     */
    fun decode(fd: Int, check: (width: Int, height: Int) -> Unit = { _, _ -> }): Bitmap? {
        val size = nativeSize(fd) ?: return null
        check(size[0], size[1])
        val bmp = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888)
        if (nativeDecodeInto(fd, bmp)) return bmp
        bmp.recycle()
        return null
    }

    @JvmStatic private external fun nativeSize(fd: Int): IntArray?
    @JvmStatic private external fun nativeDecodeInto(fd: Int, bitmap: Bitmap): Boolean
}
