package com.kriniks.kartun

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.kriniks.kartun.engine.NcnnUpscaler
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The first-launch speed check (ideas/04; the owner, chat 2026-10-16 ≈ 20:0x +03:00: «Приложение запускается впервые,
 * оценочные ценчи пустые - значит нужно запустить краткий бенч» · «Быстрые тесты по обоим чипам замеряем - примерно
 * понимаем, какая производительность чипов в условных единицах, и используем ее как отправную точку расчетов дальше для
 * эстиматора»).
 *
 * One forward pass of the default model over a small flat square, on the GPU and on the CPU, timed by the engine itself
 * ([NcnnUpscaler.timeToBlob] - the debug probe's per-blob timer). The time against the same bench on the reference device
 * (the Titan, whose speed every catalogue rate holds) is this device's speed factor; each catalogue model's starting rate
 * times that factor is written as this device's first learned rate ([UpscaleViewModel.rateKey]), so the estimate starts
 * near the truth and keeps learning from real runs exactly as before. [NOT-TESTED]
 */
object DeviceBench {
    private const val TAG = "KARTUN"
    /** Set once the first-launch check has run or was cancelled: it is offered once per installation. */
    private const val KEY_DONE = "bench_done"
    private const val OUTPUT_BLOB = "output"
    /** The square's side per backend: big enough to load a six-core GPU (64² did not), small enough for seconds on a slow one. */
    private const val GPU_SIDE = 128
    private const val CPU_SIDE = 32
    /** The first pass builds the Vulkan pipelines and warms the caches; the best of the next ones is the speed. */
    private const val WARMUP_SIDE = 32
    private const val RUNS = 1

    /**
     * The bench's time on the reference device, ms (the catalogue's rates are the Titan's), calibrated 2026-10-16 on TWO
     * devices with real runs of the default model the same evening: per device REF = bench × catalogue rate / real rate,
     * the constant is their geometric mean.
     * GPU (128²): Titan bench 830 ms, real photo 960 × 1280 in 74.1 s = 60 300 ms/MP -> 893; Redmi Note 12 Pro+ COOL bench
     * 2628 / 2682 / 2750 ms (RC smoke), real 262 s = 213 216 ms/MP -> 821; mean 856. (A hot Redmi gave 4698 ms - the
     * first constant 1130 was taken from it.) Out of sample: S21 FE bench 1163 -> estimate 1.13 of real, A52 4439 -> 1.04.
     * CPU (32²): Titan bench 582 ms, real 256 × 256 in 40.4 s = 617 000 ms/MP -> 917; Redmi bench 1118 ms, real 70.0 s =
     * 1 068 206 ms/MP -> 1018; mean 966. testcases/reports/2026-10-16_first_launch_bench.md.
     */
    private const val REF_GPU_MS = 856f
    private const val REF_CPU_MS = 966f

    /**
     * The ESRGAN family slows down more than the default model on weak GPUs (Siax on the Redmi: 0.64 of the real time with
     * one shared factor - the owner, 2026-10-16 ≈ 20:4x: «Чинить наш эстиматор и бенч»). Two short GPU passes of [PROBE_SIDE]²
     * - the default model and Siax (both inside the APK) - give this device's ratio; its quotient by the reference
     * device's [REF_ESRGAN_RATIO] corrects every esrgan-family rate.
     */
    private const val ESRGAN_PROBE = "4x-NMKD-Siax-fp16"
    private const val PROBE_SIDE = 64
    /** Siax / default model at [PROBE_SIDE]² on the reference device (Titan, 2026-10-16: 2.03 and 1.69 over two first
     *  launches - mean 1.85). The Redmi gave 2.41 / 2.59 -> its correction ×1.35. */
    private const val REF_ESRGAN_RATIO = 1.85f
    private const val MIN_CORRECTION = 0.5f
    private const val MAX_CORRECTION = 3f

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(UpscaleEngine.PREFS, Context.MODE_PRIVATE)

    /** True on the first launch: no learned rate yet and the check never ran (an update over a used app never asks). */
    fun needed(ctx: Context): Boolean {
        val p = prefs(ctx)
        return !p.getBoolean(KEY_DONE, false) && p.all.keys.none { it.startsWith("rate_") }
    }

    /** «Отмена»: the estimate stays on the catalogue's rates and learns from real runs; the check is not offered again. */
    fun skip(ctx: Context) = prefs(ctx).edit { putBoolean(KEY_DONE, true) }

    /**
     * Runs the check on the engine thread and writes the rates; [cancelled] is read between passes (a pass itself is
     * short). Returns true when the rates were written.
     */
    suspend fun run(ctx: Context, cancelled: () -> Boolean): Boolean = withContext(UpscaleEngine.dispatcher) {
        val catalogue = ModelCatalog.load(ctx)
        val (param, bin) = ModelStore.files(ctx, catalogue.default)
        // One load of the default model on the GPU serves both its passes: the main square and the probe's short one.
        val gpuTimes = time(param, bin, gpu = true, sides = listOf(GPU_SIDE, PROBE_SIDE), cancelled)
        val gpuMs = gpuTimes?.get(0)
        if (cancelled()) return@withContext false
        // The family probe: both models' short passes on the GPU, measured back to back (same heat, same clocks).
        val probe = catalogue.byId(ESRGAN_PROBE)
        val esrganCorrection = if (gpuTimes == null || probe == null) 1f else runCatching {
            val base = gpuTimes[1]
            val (pp, pb) = ModelStore.files(ctx, probe)
            val heavy = time(pp, pb, gpu = true, sides = listOf(PROBE_SIDE), cancelled)?.get(0)
            if (heavy == null) 1f else ((heavy / base) / REF_ESRGAN_RATIO).coerceIn(MIN_CORRECTION, MAX_CORRECTION)
                .also { Log.i(TAG, """{"bench_probe":true,"base_ms":$base,"esrgan_ms":$heavy,"ratio":${heavy / base}}""") }
        }.getOrDefault(1f)
        if (cancelled()) return@withContext false
        val cpuMs = time(param, bin, gpu = false, sides = listOf(CPU_SIDE), cancelled)?.get(0)
        if (cancelled()) return@withContext false
        val gpuFactor = gpuMs?.let { it / REF_GPU_MS }
        val cpuFactor = cpuMs?.let { it / REF_CPU_MS }
        Log.i(TAG, """{"bench":true,"gpu_ms":${gpuMs ?: -1},"cpu_ms":${cpuMs ?: -1},"gpu_factor":${gpuFactor ?: -1},"cpu_factor":${cpuFactor ?: -1},"esrgan_correction":$esrganCorrection}""")
        prefs(ctx).edit {
            for (m in catalogue.models) {
                val family = if (m.family == "esrgan") esrganCorrection else 1f
                gpuFactor?.let { putFloat(UpscaleViewModel.rateKey(m, true), UpscaleViewModel.startRate(m, true) * it * family) }
                cpuFactor?.let { putFloat(UpscaleViewModel.rateKey(m, false), UpscaleViewModel.startRate(m, false) * it) }
            }
            putBoolean(KEY_DONE, true)
        }
        gpuFactor != null || cpuFactor != null
    }

    /** Engine thread only: for each of [sides], the best of [RUNS] passes of the net over that square after one warm-up,
     *  ms; null when the backend is not there (no Vulkan: the GPU net comes up on the CPU) or a pass failed. */
    private fun time(param: File, bin: File, gpu: Boolean, sides: List<Int>, cancelled: () -> Boolean): List<Float>? {
        val net = NcnnUpscaler.load(param.path, bin.path, useGpu = gpu) ?: return null
        return net.use { u ->
            if (u.usesGpu != gpu || u.timeToBlob(WARMUP_SIDE, WARMUP_SIDE, OUTPUT_BLOB) < 0) return@use null
            sides.map { side ->
                var best = Long.MAX_VALUE
                repeat(RUNS) {
                    if (cancelled()) return@use null
                    val ms = u.timeToBlob(side, side, OUTPUT_BLOB)
                    if (ms < 0) return@use null
                    best = minOf(best, ms)
                }
                best.coerceAtLeast(1).toFloat()
            }
        }
    }
}
