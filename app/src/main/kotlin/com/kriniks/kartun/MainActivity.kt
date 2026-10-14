package com.kriniks.kartun

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.engine.NcnnUpscaler
import java.io.File
import java.util.concurrent.Executors

/**
 * Phase 0 probe screen (plans/02): runs one engine upscale driven by intent extras and logs a JSON line to logcat
 * (tag KARTUN_PROBE). Files live in the app's external files dir so adb can push inputs and pull outputs. [NOT-TESTED]
 *
 * adb shell am start -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity \
 *   --es in test256.png --es model realesrgan-x4plus --ez gpu true --ei tile 128 --ei scale 4
 */
class MainActivity : ComponentActivity() {

    private val status = mutableStateOf("KARTUN phase 0 probe")
    // One dedicated thread for every engine call (research 03, section B, lesson 7).
    private val engineThread = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(16.dp)) {
                Text(status.value, color = Color(0xFFFF1A8C))
            }
        }
        val input = intent.getStringExtra("in") ?: return
        val model = intent.getStringExtra("model") ?: "realesrgan-x4plus"
        val gpu = intent.getBooleanExtra("gpu", true)
        val tile = intent.getIntExtra("tile", 128)
        val scale = intent.getIntExtra("scale", 4)
        engineThread.execute { runProbe(input, model, gpu, tile, scale) }
    }

    private fun runProbe(input: String, model: String, gpu: Boolean, tile: Int, scale: Int) {
        val dir = getExternalFilesDir(null) ?: filesDir
        val tag = "KARTUN_PROBE"
        try {
            val info = NcnnUpscaler.gpuInfo()
            val bmp: Bitmap = BitmapFactory.decodeFile(File(dir, input).path)
                ?: error("cannot decode ${File(dir, input).path}")
            val t0 = SystemClock.elapsedRealtime()
            val up = NcnnUpscaler.load(File(dir, "$model.param").path, File(dir, "$model.bin").path, gpu)
                ?: error("model load failed: $model")
            val t1 = SystemClock.elapsedRealtime()
            val out = up.use { it.upscale(bmp, scale, tile) }
            val t2 = SystemClock.elapsedRealtime()
            val name = "out_${model}_${if (gpu) "gpu" else "cpu"}_t${tile}_${input.substringBeforeLast('.')}.png"
            File(dir, name).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val line = """{"ok":true,"in":"$input","w":${bmp.width},"h":${bmp.height},"out":"$name","ow":${out.width},"oh":${out.height},"model":"$model","gpu_requested":$gpu,"gpu_used":${up.usesGpu},"tile":$tile,"load_ms":${t1 - t0},"upscale_ms":${t2 - t1},"info":"$info"}"""
            Log.i(tag, line)
            status.value = line
        } catch (t: Throwable) {
            val line = """{"ok":false,"error":"${t.message}"}"""
            Log.e(tag, line, t)
            status.value = line
        }
    }
}
