package com.kriniks.kartun

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import com.kriniks.kartun.engine.NcnnUpscaler
import java.io.File
import java.util.concurrent.Executors

/**
 * Phase 0 engine probe (plans/02), kept for tools/titan-probe.mjs: an adb `am start` with the extra `in` runs one engine
 * upscale on files in the app's external files dir and logs a JSON line (tag KARTUN_PROBE). Without that extra the app
 * shows its normal screens. Debuggable builds only: MainActivity calls it under BuildConfig.DEBUG (plans/03 step 12).
 * [TESTED: 2026-10-14 · 13 probe runs on the Titan - testcases/reports/2026-10-14_F0_own_fast_model.md]
 *
 * adb shell am start -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity \
 *   --es in test256.png --es model realesrgan-x4plus --ez gpu true --ei tile 128 --ei scale 4
 */
object EngineProbe {
    // One dedicated thread for every engine call (research 03, section B, lesson 7).
    private val engineThread = Executors.newSingleThreadExecutor()

    /** Starts the probe when [intent] carries the `in` extra; returns false for a normal launch. */
    fun runIfRequested(context: Context, intent: Intent): Boolean {
        val input = intent.getStringExtra("in") ?: return false
        val model = intent.getStringExtra("model") ?: "realesrgan-x4plus"
        val gpu = intent.getBooleanExtra("gpu", true)
        val tile = intent.getIntExtra("tile", 128)
        val scale = intent.getIntExtra("scale", 4)
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        engineThread.execute { run(dir, input, model, gpu, tile, scale) }
        return true
    }

    private fun run(dir: File, input: String, model: String, gpu: Boolean, tile: Int, scale: Int) {
        val tag = "KARTUN_PROBE"
        try {
            val info = NcnnUpscaler.gpuInfo()
            val bmp: Bitmap = BitmapFactory.decodeFile(File(dir, input).path) ?: error("cannot decode ${File(dir, input).path}")
            val t0 = SystemClock.elapsedRealtime()
            val up = NcnnUpscaler.load(File(dir, "$model.param").path, File(dir, "$model.bin").path, gpu)
                ?: error("model load failed: $model")
            val t1 = SystemClock.elapsedRealtime()
            val out = up.use { it.upscale(bmp, scale, tile) }
            val t2 = SystemClock.elapsedRealtime()
            val name = "out_${model}_${if (gpu) "gpu" else "cpu"}_t${tile}_${input.substringBeforeLast('.')}.png"
            File(dir, name).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Log.i(tag, """{"ok":true,"in":"$input","w":${bmp.width},"h":${bmp.height},"out":"$name","ow":${out.width},"oh":${out.height},"model":"$model","gpu_requested":$gpu,"gpu_used":${up.usesGpu},"tile":$tile,"load_ms":${t1 - t0},"upscale_ms":${t2 - t1},"info":"$info"}""")
        } catch (t: Throwable) {
            Log.e(tag, """{"ok":false,"error":"${t.message}"}""", t)
        }
    }
}
