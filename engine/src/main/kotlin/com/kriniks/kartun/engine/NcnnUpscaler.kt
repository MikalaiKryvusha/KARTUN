package com.kriniks.kartun.engine

import android.graphics.Bitmap

/**
 * Called by the native engine after every finished tile. Return `false` to stop the run between tiles.
 * Runs on the engine thread - keep it cheap (post the numbers, do not draw). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
fun interface TileListener {
    fun onTile(done: Int, total: Int): Boolean
}

/** Thrown by [NcnnUpscaler.upscale] when the [TileListener] asked to stop. */
class UpscaleCancelledException : RuntimeException("upscale cancelled")

/**
 * Kotlin face of the native NCNN upscaler (`libkartun_engine.so`).
 * [TESTED: 2026-10-14 · probe runs on the Titan, GPU and CPU - testcases/reports/2026-10-14_F0_own_fast_model.md]
 * The tile listener (progress and cancel): [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 *
 * One instance = one loaded model. Every call must come from the same thread: NCNN/Vulkan objects are not
 * thread-safe and some Mali drivers crash on concurrent create/release (research 03, section B, lesson 7).
 */
class NcnnUpscaler private constructor(private var handle: Long, val usesGpu: Boolean) : AutoCloseable {

    /**
     * Upscales [input] (ARGB_8888) by [scale] using tiles of [tile] px, reporting progress to [listener].
     * Returns the new bitmap; throws [UpscaleCancelledException] when the listener stopped the run.
     */
    fun upscale(input: Bitmap, scale: Int, tile: Int, listener: TileListener? = null): Bitmap {
        check(handle != 0L) { "upscaler is closed" }
        val src = if (input.config == Bitmap.Config.ARGB_8888) input else input.copy(Bitmap.Config.ARGB_8888, false)
        val out = Bitmap.createBitmap(src.width * scale, src.height * scale, Bitmap.Config.ARGB_8888)
        val rc = nativeUpscale(handle, src, out, scale, tile, listener)
        if (rc != 0) {
            out.recycle()
            if (rc == ERR_CANCELLED) throw UpscaleCancelledException()
            throw IllegalStateException("native upscale failed, code $rc")
        }
        return out
    }

    override fun close() {
        if (handle != 0L) nativeDestroy(handle)
        handle = 0L
    }

    companion object {
        /** Native return code for a run stopped by the listener (kErrCancelled in kartun_engine.cpp). */
        private const val ERR_CANCELLED = -4

        init { System.loadLibrary("kartun_engine") }

        /** Loads the model from `.param` + `.bin` files; returns null when loading fails. */
        fun load(paramPath: String, binPath: String, useGpu: Boolean, threads: Int = 4): NcnnUpscaler? {
            val h = nativeCreate(paramPath, binPath, useGpu, threads)
            return if (h == 0L) null else NcnnUpscaler(h, useGpu && !gpuInfo().startsWith("gpu_count=0"))
        }

        /** Diagnostics line: GPU count, name, fp16 support, heap budget. */
        fun gpuInfo(): String = nativeGpuInfo()

        @JvmStatic private external fun nativeGpuInfo(): String
        @JvmStatic private external fun nativeCreate(param: String, bin: String, useGpu: Boolean, threads: Int): Long
        @JvmStatic private external fun nativeDestroy(handle: Long)
        @JvmStatic private external fun nativeUpscale(
            handle: Long, input: Bitmap, output: Bitmap, scale: Int, tile: Int, listener: TileListener?,
        ): Int
    }
}
