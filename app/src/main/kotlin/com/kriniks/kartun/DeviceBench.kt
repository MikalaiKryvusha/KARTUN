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
    /** The square's side per backend: big enough for a time in tens of ms on a fast chip, small enough for seconds on a slow one. */
    private const val GPU_SIDE = 64
    private const val CPU_SIDE = 32
    /** The first pass builds the Vulkan pipelines and warms the caches; the best of the next ones is the speed. */
    private const val WARMUP_SIDE = 32
    private const val RUNS = 2

    /**
     * The bench's time on the reference device, ms (the catalogue's rates are the Titan's). Calibrated 2026-10-16 on the
     * Redmi Note 12 Pro+ (Mali-G68 MC4), whose real rates of the default model were measured the same day - GPU: photo
     * 960 × 1280 in 262 s = 213 216 ms/MP; CPU: 256 × 256 in 70.0 s = 1 068 206 ms/MP; its bench, three first launches:
     * GPU 734 / 791 / 756 ms, CPU 1121 / 1108 / 1124 ms. REF = bench(Redmi) × catalogue rate / real rate(Redmi):
     * GPU 760 × 64 850 / 213 216, CPU 1118 × 972 750 / 1 068 206 - testcases/reports/2026-10-16_first_launch_bench.md.
     */
    private const val REF_GPU_MS = 231f
    private const val REF_CPU_MS = 1018f

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
        val gpuMs = time(param, bin, gpu = true, side = GPU_SIDE, cancelled)
        if (cancelled()) return@withContext false
        val cpuMs = time(param, bin, gpu = false, side = CPU_SIDE, cancelled)
        if (cancelled()) return@withContext false
        val gpuFactor = gpuMs?.let { it / REF_GPU_MS }
        val cpuFactor = cpuMs?.let { it / REF_CPU_MS }
        Log.i(TAG, """{"bench":true,"gpu_ms":${gpuMs ?: -1},"cpu_ms":${cpuMs ?: -1},"gpu_factor":${gpuFactor ?: -1},"cpu_factor":${cpuFactor ?: -1}}""")
        prefs(ctx).edit {
            for (m in catalogue.models) {
                gpuFactor?.let { putFloat(UpscaleViewModel.rateKey(m, true), UpscaleViewModel.startRate(m, true) * it) }
                cpuFactor?.let { putFloat(UpscaleViewModel.rateKey(m, false), UpscaleViewModel.startRate(m, false) * it) }
            }
            putBoolean(KEY_DONE, true)
        }
        gpuFactor != null || cpuFactor != null
    }

    /** Engine thread only: the best of [RUNS] passes of the net over a [side]² square after a warm-up, ms; null when the
     *  backend is not there (no Vulkan: the GPU net comes up on the CPU) or a pass failed. */
    private fun time(param: File, bin: File, gpu: Boolean, side: Int, cancelled: () -> Boolean): Float? {
        val net = NcnnUpscaler.load(param.path, bin.path, useGpu = gpu) ?: return null
        return net.use { u ->
            if (u.usesGpu != gpu || u.timeToBlob(WARMUP_SIDE, WARMUP_SIDE, OUTPUT_BLOB) < 0) return@use null
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
