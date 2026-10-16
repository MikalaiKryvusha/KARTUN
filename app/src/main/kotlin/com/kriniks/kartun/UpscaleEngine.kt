package com.kriniks.kartun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import com.kriniks.kartun.UpscaleViewModel.Companion.ENGINE_SCALE
import com.kriniks.kartun.UpscaleViewModel.Companion.MAX_INPUT_MP
import com.kriniks.kartun.UpscaleViewModel.Companion.TILE_PX
import com.kriniks.kartun.UpscaleViewModel.Companion.fits
import com.kriniks.kartun.UpscaleViewModel.Companion.rateKey
import com.kriniks.kartun.UpscaleViewModel.Companion.secondPassInput
import com.kriniks.kartun.UpscaleViewModel.Companion.startRate
import com.kriniks.kartun.UpscaleViewModel.Companion.tiles
import com.kriniks.kartun.engine.NcnnUpscaler
import com.kriniks.kartun.engine.TileListener
import kotlinx.coroutines.withContext

/**
 * The ONE engine of the process (plans/06 §4.1, plans/07 step 2): the loaded net, the pass plan of a run, the fallback
 * ladder and the learned speed - shared by the screens ([UpscaleViewModel]) and the agent door (agent/AgentService), so
 * both run the same code on the same thread and never hold two nets. Moved verbatim out of UpscaleViewModel; every
 * engine call runs on [EngineThread] (architecture invariant 4, bugs/01). [TESTED: 2026-10-16 · Titan by touch: 4×, 8×,
 * a mid-run «Отменить», a model switch - testcases/reports/2026-10-16_epic06_A_step2_shared_engine.md]
 */
object UpscaleEngine {
    private const val TAG = "KARTUN"
    private const val MIN_MP_TO_LEARN = 0.2f
    private const val LEARN_WEIGHT = 0.5f
    /** One preferences file for the learned speed AND the screens' remembered settings (its name predates the settings). */
    const val PREFS = "estimates"

    /** The process-wide engine thread, never closed (bugs/01 - see EngineThread). */
    val dispatcher = EngineThread.dispatcher

    // The loaded net lives on the engine thread only (touched inside withContext(dispatcher)).
    private var loaded: NcnnUpscaler? = null
    private var loadedModel: Model? = null
    private var loadedGpu = true

    /** The photo is over the input limit (MAX_INPUT_MP). */
    class TooBig : RuntimeException()
    /** The result would be over the output limit (MAX_OUTPUT_MP). */
    class OutputTooBig : RuntimeException()
    class ModelLoadFailed : RuntimeException()
    class EngineFailed : RuntimeException()

    /** A finished run: the full result and whether the GPU did all of the work. */
    class Output(val bitmap: Bitmap, val gpuUsed: Boolean)

    /** Loads [m] for [useGpu] so the next run starts the network at once; throws [ModelLoadFailed]. */
    suspend fun preload(ctx: Context, m: Model, useGpu: Boolean) {
        withContext(dispatcher) { ensureLoaded(ctx, m, useGpu) }
    }

    /** Drops the loaded net (the GPU switch changed, the screen left). */
    suspend fun release() = withContext(dispatcher) { releaseNow() }

    /** Drops the loaded net only when it is [m] (its files are about to be deleted). */
    suspend fun releaseIf(m: Model) = withContext(dispatcher) { if (loadedModel == m) releaseNow() }

    /** [release] without waiting - for callbacks that cannot suspend (ViewModel.onCleared). */
    fun releaseLater() = EngineThread.executor.execute { releaseNow() }

    /**
     * One run of [m] over [input] at [scale] (1×…16×). [onProgress] gets (done, total, gpu) - total is 0 until the
     * plan is known, then the tiles of all passes, so a ring runs 0..100 % once; [cancelled] is read between tiles.
     * Throws [OutputTooBig], [ModelLoadFailed], [EngineFailed] or the engine's UpscaleCancelledException.
     */
    suspend fun run(
        ctx: Context, input: Bitmap, m: Model, scale: Int, useGpu: Boolean,
        onProgress: (done: Int, total: Int, gpu: Boolean) -> Unit, cancelled: () -> Boolean,
    ): Output = withContext(dispatcher) {
        val mp = input.width.toFloat() * input.height / 1_000_000f
        if (!fits(mp, scale)) throw OutputTooBig()
        // Normally already loaded by preload(); a start during loading waits for it on this same thread.
        val u = ensureLoaded(ctx, m, useGpu)
        var done = 0
        var total = 0
        var gpuUsed = u.usesGpu
        onProgress(done, total, gpuUsed)
        // Tiles of both passes are known up front, so the ring runs 0..100 % once across the whole run.
        val mid = secondPassInput(scale)
        val pass1 = tiles(input.width, input.height)
        val pass2 = mid?.let { tiles(input.width * it, input.height * it) } ?: 0
        val grand = pass1 + pass2
        // The ring counts in 200 px tiles; a retry with smaller tiles reports more of them, so each
        // pass maps its own done/total onto its share of the ring.
        val report = { offset: Int, share: Int ->
            TileListener { d, t ->
                done = offset + d * share / t
                total = grand
                onProgress(done, total, gpuUsed)
                !cancelled()
            }
        }
        val onCpu = { gpuUsed = false; onProgress(done, total, false) }
        val pass = { bmp: Bitmap, outScale: Int, listener: TileListener ->
            withFallback(ctx, m, u, bmp, outScale, listener, onCpu).also { (_, gpu) -> gpuUsed = gpuUsed && gpu }.first
        }
        val tRun = SystemClock.elapsedRealtime()
        // Pass 1 comes out of the engine already at the size it is needed at - the target for 1×/2×/4×,
        // the second pass's input (2× for 8×, 4× for 16×) otherwise: the network's 4× tiles are shrunk
        // inside the engine, so no full 4× image is held (photos up to MAX_INPUT_MP, the owner 11:35).
        val first = pass(input, mid ?: scale, report(0, pass1))
        learnRate(ctx, m, gpuUsed, mp, SystemClock.elapsedRealtime() - tRun)
        // 8×: the 2× result feeds the second 4× pass (2 x 4 = 8); 16×: the 4× result does (4 x 4 = 16).
        // [TESTED: 2026-10-15 · 8× on the Titan - testcases/reports/2026-10-15_F2_guarded_loop_device_run.md run 8, before this change]
        val full = if (mid == null) first else pass(first, ENGINE_SCALE, report(pass1, pass2)).also { first.recycle() }
        Output(full, gpuUsed)
    }

    /** Decodes a photo as ARGB_8888 sRGB in software memory; throws [TooBig] above MAX_INPUT_MP. */
    fun decode(source: ImageDecoder.Source): Bitmap =
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            if (info.size.width.toFloat() * info.size.height / 1_000_000f > MAX_INPUT_MP) throw TooBig()
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
        }.let { if (it.config == Bitmap.Config.ARGB_8888) it else it.copy(Bitmap.Config.ARGB_8888, false) }

    /** Engine thread only: makes [m] on [useGpu] the loaded net, releasing the previous one. */
    private fun ensureLoaded(ctx: Context, m: Model, useGpu: Boolean): NcnnUpscaler {
        loaded?.let { if (loadedModel == m && loadedGpu == useGpu) return it }
        releaseNow()
        val (param, bin) = ModelStore.files(ctx, m)
        // bugs/01: the journal keeps the engine's stderr and the CPU set this load starts on.
        CrashLog.beforeEngine()
        CrashLog.note("load ${m.id} gpu=$useGpu ${CrashLog.cpuState()}")
        val up = NcnnUpscaler.load(param.path, bin.path, useGpu = useGpu) ?: throw ModelLoadFailed()
        loaded = up
        loadedModel = m
        loadedGpu = useGpu
        return up
    }

    /** Engine thread only. */
    private fun releaseNow() {
        loaded?.close()
        loaded = null
        loadedModel = null
    }

    /**
     * Engine thread only. One 4× pass with the fallback ladder of plans/04 step 2 (research 03 §B lessons 2 and 4: device
     * lost on large tiles is the most common failure of Android upscalers): the loaded net with 200 px tiles → the same
     * net with 100 px tiles → a CPU net → an honest error. A cancel is never retried. Returns the bitmap and whether the
     * GPU did the work. [NOT-TESTED: the forced GPU failure on the device - deferred by the owner]
     */
    private fun withFallback(
        ctx: Context, m: Model, u: NcnnUpscaler, input: Bitmap, outScale: Int, listener: TileListener, onCpu: () -> Unit,
    ): Pair<Bitmap, Boolean> {
        try {
            return u.upscale(input, ENGINE_SCALE, TILE_PX, listener, outScale) to u.usesGpu
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pass failed with tile $TILE_PX (${e.message}); retry with ${TILE_PX / 2}")
        }
        try {
            return u.upscale(input, ENGINE_SCALE, TILE_PX / 2, listener, outScale) to u.usesGpu
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pass failed with tile ${TILE_PX / 2} (${e.message}); retry on the CPU")
        }
        if (!u.usesGpu) throw EngineFailed()
        onCpu()
        val (param, bin) = ModelStore.files(ctx, m)
        CrashLog.note("load ${m.id} gpu=false (fallback) ${CrashLog.cpuState()}")
        val cpu = NcnnUpscaler.load(param.path, bin.path, useGpu = false) ?: throw EngineFailed()
        return cpu.use {
            try { it.upscale(input, ENGINE_SCALE, TILE_PX, listener, outScale) to false }
            catch (e: IllegalStateException) { Log.w(TAG, "CPU pass failed too (${e.message})"); throw EngineFailed() }
        }
    }

    /** Keeps a running average of ms per input MP for each model and backend on THIS device (the estimate learns);
     *  [gpu] is the backend that actually ran - a GPU failure that fell back to the CPU teaches the CPU rate. */
    private fun learnRate(ctx: Context, m: Model, gpu: Boolean, mp: Float, ms: Long) {
        if (mp < MIN_MP_TO_LEARN) return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getFloat(rateKey(m, gpu), startRate(m, gpu))
        prefs.edit { putFloat(rateKey(m, gpu), old * (1 - LEARN_WEIGHT) + (ms / mp) * LEARN_WEIGHT) }
    }
}
