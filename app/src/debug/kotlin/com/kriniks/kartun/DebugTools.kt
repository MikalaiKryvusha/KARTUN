package com.kriniks.kartun

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.engine.NcnnUpscaler
import com.kriniks.kartun.engine.TileListener
import com.kriniks.kartun.ui.Metric
import com.kriniks.kartun.ui.PixelSpinner
import com.kriniks.kartun.ui.RoundIconButton
import com.kriniks.kartun.ui.SettingRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Debug build only: the «Отладка» group in Settings and the engine probe screen (the owner, chat 2026-10-15 ≈ 15:35 +03:00:
 * «два режима пробы в Настройках приложения сделай дебаг раздел», «пусть там наша анимация рендера крутится, и пишутся
 * человекочитаемые логи, чтобы я понимал, чем занято устройство и КАРТУН», «пусть экран пробы показывает загрузку на
 * устройстве CPU, GPU, RAM», «я остановил, потому что долго висит черный экран»).
 * Replaces the phase 0 black-screen probe: an adb launch with the `in` extra (tools/titan-probe.mjs) now opens this screen
 * and still writes the KARTUN_PROBE JSON line the tool reads. Two modes: the GPU (Vulkan) and the CPU.
 * Load: KARTUN's own CPU share (/proc/self/stat), the fastest core's clock (cpufreq), free memory (/proc/meminfo) and
 * KARTUN's memory (PSS). The Mali load is closed to apps on this device (sysfs "Permission denied", probed 2026-10-15) -
 * the screen shows how long KARTUN computes on the GPU instead. [NOT-TESTED: the first version (one CPU line, the Mali
 * note) was seen on the Titan 2026-10-15 15:44-15:49 over an adb probe - spinner, load tiles, logs, the result picture;
 * 16:07-16:08 the current one (GPU tile, «ОЗУ» on its own row, core clocks as chips, one KARTUN percent) seen by
 * screenshots on the Titan; the `blobs` layer timing read from logcat; the Settings path not walked yet]
 */
object DebugTools {
    private const val TAG = "KARTUN_PROBE"
    private const val PREPADDING = 10  // the engine's context pixels on each side of a tile (kartun_engine.cpp)

    // One dedicated thread for every engine call (research 03, section B, lesson 7); named so the thread list shows it.
    private const val ENGINE_THREAD = "kartun-engine"
    private val engineThread = Executors.newSingleThreadExecutor { r -> Thread(r, ENGINE_THREAD) }
    private val main = Handler(Looper.getMainLooper())

    /** What the probe screen shows; null = the screen is closed. */
    private var screen by mutableStateOf<Probe?>(null)

    private class Probe(val model: String, val input: String, val gpu: Boolean, val tile: Int, val scale: Int,
                        val blobs: List<String> = emptyList()) {
        val log = mutableStateListOf<String>()
        var running by mutableStateOf(false)
        var cancel = false
        var gpuStart by mutableLongStateOf(0L)   // > 0 while the engine computes on the GPU
        var gpuMs by mutableLongStateOf(0L)
        var result by mutableStateOf<Bitmap?>(null)
    }

    private var setup by mutableStateOf(false)  // the model / picture / mode picker opened from Settings

    fun onIntent(context: Context, intent: Intent) {
        val input = intent.getStringExtra("in") ?: return
        val p = Probe(intent.getStringExtra("model") ?: "realesrgan-x4plus", input, intent.getBooleanExtra("gpu", true),
            intent.getIntExtra("tile", 128), intent.getIntExtra("scale", 4),
            intent.getStringExtra("blobs")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty())
        screen = p
        start(context.applicationContext, p)
    }

    @Composable
    fun Overlay(): Boolean {
        val p = screen
        when {
            p != null -> ProbeScreen(p)
            setup -> SetupScreen()
            else -> return false
        }
        return true
    }

    @Composable
    fun SettingsSection() {
        val c = MaterialTheme.colorScheme
        Text(stringResource(R.string.dbg_section).uppercase(), Modifier.padding(start = 4.dp, top = 8.dp),
            style = MaterialTheme.typography.labelSmall, color = c.primary)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainer)) {
            SettingRow(R.drawable.ic_cpu, stringResource(R.string.dbg_probe), stringResource(R.string.dbg_probe_sub),
                onInfo = null, onClick = { setup = true }) {
                Text("›", style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
            }
        }
    }

    // ---- the run ----

    private fun dir(context: Context) = context.getExternalFilesDir(null) ?: context.filesDir

    /** Model files: the adb folder first, then the models inside the APK (copied out by [ModelStore]). */
    private fun modelFiles(context: Context, model: String): Pair<File, File> {
        val d = dir(context)
        val param = File(d, "$model.param")
        val bin = File(d, "$model.bin")
        return if (param.exists() && bin.exists()) param to bin else ModelStore.files(context, model)
    }

    /** `0=<w>` of the Input line: a fixed-size net (DAT) takes tiles of exactly that size (kartun_engine.cpp). */
    private fun fixedInput(param: File): Int =
        param.useLines { lines -> lines.firstOrNull { it.startsWith("Input") } }
            ?.split(Regex("\\s+"))?.firstOrNull { it.startsWith("0=") }?.substring(2)?.toIntOrNull() ?: 0

    private fun start(context: Context, p: Probe) {
        p.running = true
        val say = { text: String -> main.post { p.log.add(text) } }
        engineThread.execute {
            try {
                val bmp = BitmapFactory.decodeFile(File(dir(context), p.input).path) ?: error("cannot decode ${p.input}")
                val mode = context.getString(if (p.gpu) R.string.dbg_mode_gpu else R.string.dbg_mode_cpu)
                say(context.getString(R.string.dbg_log_start, p.model, p.input, bmp.width, bmp.height, mode))
                val info = NcnnUpscaler.gpuInfo()
                say(context.getString(R.string.dbg_log_device, info.substringAfter("name=", "—").substringBefore(" fp16")))
                say(context.getString(R.string.dbg_log_loading))
                val (param, bin) = modelFiles(context, p.model)
                val t0 = SystemClock.elapsedRealtime()
                val up = NcnnUpscaler.load(param.path, bin.path, p.gpu) ?: error("model load failed: ${p.model}")
                val t1 = SystemClock.elapsedRealtime()
                say(context.getString(if (up.usesGpu) R.string.dbg_log_loaded_gpu else R.string.dbg_log_loaded_cpu, (t1 - t0) / 1000.0))
                val fixed = fixedInput(param)
                if (fixed > 0) {
                    val t = fixed - 2 * PREPADDING
                    say(context.getString(R.string.dbg_log_fixed, fixed, ((bmp.width + t - 1) / t) * ((bmp.height + t - 1) / t)))
                }
                if (p.blobs.isNotEmpty()) {  // where does the time go: the net run up to each named blob, one by one
                    if (up.usesGpu) main.post { p.gpuStart = SystemClock.elapsedRealtime() }
                    up.use { u ->
                        val side = if (fixed > 0) fixed else bmp.width
                        for (b in p.blobs) {
                            if (p.cancel) break
                            say(context.getString(R.string.dbg_log_blob_start, b))
                            val ms = u.timeToBlob(side, side, b)
                            say(context.getString(R.string.dbg_log_blob, b, ms))
                            Log.i(TAG, """{"blob":"$b","ms":$ms}""")
                        }
                    }
                    main.post { if (p.gpuStart > 0) { p.gpuMs += SystemClock.elapsedRealtime() - p.gpuStart; p.gpuStart = 0 } }
                    return@execute
                }
                var last = SystemClock.elapsedRealtime()
                if (up.usesGpu) main.post { p.gpuStart = last }
                val out = up.use {
                    it.upscale(bmp, p.scale, p.tile, TileListener { done, total ->
                        val now = SystemClock.elapsedRealtime()
                        say(context.getString(R.string.dbg_log_tile, done, total, (now - last) / 1000.0))
                        last = now
                        !p.cancel
                    })
                }
                val t2 = SystemClock.elapsedRealtime()
                main.post { if (p.gpuStart > 0) { p.gpuMs += t2 - p.gpuStart; p.gpuStart = 0 } }
                val name = "out_${p.model}_${if (p.gpu) "gpu" else "cpu"}_t${p.tile}_${p.input.substringBeforeLast('.')}.png"
                File(dir(context), name).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                say(context.getString(R.string.dbg_log_done, out.width, out.height, (t2 - t1) / 1000.0, name))
                main.post { p.result = out }
                // The line tools/titan-probe.mjs waits for (unchanged since phase 0).
                Log.i(TAG, """{"ok":true,"in":"${p.input}","w":${bmp.width},"h":${bmp.height},"out":"$name","ow":${out.width},"oh":${out.height},"model":"${p.model}","gpu_requested":${p.gpu},"gpu_used":${up.usesGpu},"tile":${p.tile},"load_ms":${t1 - t0},"upscale_ms":${t2 - t1},"info":"$info"}""")
            } catch (t: Throwable) {
                say(context.getString(if (p.cancel) R.string.dbg_log_cancelled else R.string.dbg_log_error, t.message ?: t.javaClass.simpleName))
                main.post { if (p.gpuStart > 0) { p.gpuMs += SystemClock.elapsedRealtime() - p.gpuStart; p.gpuStart = 0 } }
                Log.e(TAG, """{"ok":false,"error":"${t.message}"}""", t)
            } finally {
                main.post { p.running = false }
            }
        }
    }

    // ---- device load ----

    /** cpuPct: KARTUN's share of the WHOLE processor over the last second (all cores = 100 %), summed over its threads. */
    private class Load(val cpuPct: Int, val coresGhz: List<Double>, val freeGb: Double, val totalGb: Double, val appMb: Int)

    private val clkTck by lazy { Os.sysconf(OsConstants._SC_CLK_TCK).coerceAtLeast(1) }
    private val cores = Runtime.getRuntime().availableProcessors()

    /** Every thread of this process: name -> utime + stime in clock ticks (/proc/self/task/<tid>/stat fields 14 and 15). */
    private fun threadTicks(): Map<String, Pair<String, Long>> =
        File("/proc/self/task").listFiles().orEmpty().mapNotNull { t ->
            runCatching {
                val s = File(t, "stat").readText()
                val name = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'))
                val f = s.substring(s.lastIndexOf(')') + 2).split(' ')
                t.name to (name to f[11].toLong() + f[12].toLong())
            }.getOrNull()
        }.toMap()

    /** Current clock of every core, GHz (cpufreq is readable by apps; the load per core is not - /proc/stat is closed). */
    private fun coresGhz(): List<Double> = (0 until cores).map { i ->
        runCatching { File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1e6 }.getOrDefault(0.0)
    }

    private fun memInfoKb(key: String): Long =
        File("/proc/meminfo").useLines { l -> l.firstOrNull { it.startsWith(key) } }?.split(Regex("\\s+"))?.get(1)?.toLong() ?: 0L

    // ---- screens ----

    @Composable
    private fun ProbeScreen(p: Probe) {
        val c = MaterialTheme.colorScheme
        val ctx = LocalContext.current
        BackHandler { if (!p.running) screen = null }
        var load by remember { mutableStateOf<Load?>(null) }
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            var last = withContext(Dispatchers.IO) { threadTicks() }
            var lastAt = SystemClock.elapsedRealtime()
            while (true) {
                delay(1000)
                load = withContext(Dispatchers.IO) {
                    val now = threadTicks()
                    val at = SystemClock.elapsedRealtime()
                    val sec = (at - lastAt) / 1000.0
                    // One number (the owner, chat 2026-10-15 ≈ 16:08 +03:00: «частоты ядер и один процент суммарный»): the CPU
                    // time of every KARTUN thread over the last second, as a share of all cores.
                    val ticks = now.entries.sumOf { (tid, v) -> v.second - (last[tid]?.second ?: v.second) }
                    last = now; lastAt = at
                    Load((ticks.toDouble() / clkTck / sec / cores * 100).toInt().coerceIn(0, 100), coresGhz(),
                        memInfoKb("MemAvailable") / 1048576.0, memInfoKb("MemTotal") / 1048576.0, (Debug.getPss() / 1024).toInt())
                }
                tick++
            }
        }
        Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(R.drawable.ic_back, onClick = { screen = null }, enabled = !p.running)
                Text(stringResource(R.string.dbg_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.size(44.dp))
            }
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                val r = p.result
                if (r != null && !p.running) Image(r.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                else PixelSpinner(Modifier.size(150.dp))
            }
            Spacer(Modifier.height(12.dp))
            val l = load
            val now = tick.let { SystemClock.elapsedRealtime() }  // re-read every second with the load sample
            val gpuSec = ((p.gpuMs + if (p.gpuStart > 0) now - p.gpuStart else 0) / 1000).toInt()
            Metric(stringResource(R.string.dbg_gpu),
                if (p.gpuStart > 0) stringResource(R.string.dbg_gpu_busy, gpuSec) else stringResource(R.string.dbg_gpu_idle), Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            // RAM on a row of its own: next to the GPU tile its line was cut («· KARTUN … МБ», seen on the Titan 16:07)
            Metric(stringResource(R.string.dbg_ram), if (l == null) "…" else stringResource(R.string.dbg_ram_value, l.freeGb, l.totalGb, l.appMb), Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            // CPU: the clock of every core and KARTUN's share of the whole processor.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.dbg_cpu).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                if (l == null) Text("…")
                // Core clocks first, right under the title, as small fixed-width chips: the row keeps its width while
                // the digits change (the owner, chat 2026-10-15 ≈ 16:00 +03:00: «частота ядер нужно сделать чипсами
                // маленькими и зафиксировать» · «частоты ядра сразу под заголовком Процессор сделай»).
                l?.let {
                    Text(stringResource(R.string.dbg_cores), Modifier.padding(top = 4.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        it.coresGhz.forEach { g ->
                            Text("%.1f".format(g), Modifier.width(38.dp).clip(RoundedCornerShape(8.dp)).background(c.surfaceContainerHigh)
                                .padding(vertical = 3.dp), textAlign = TextAlign.Center, maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
                l?.let {
                    Text(stringResource(R.string.dbg_cpu_total, it.cpuPct),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
                }
            }
            Spacer(Modifier.height(8.dp))
            val state = rememberLazyListState()
            LaunchedEffect(p.log.size) { if (p.log.isNotEmpty()) state.animateScrollToItem(p.log.size - 1) }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceContainer).padding(12.dp), state) {
                items(p.log) { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) }
            }
            Button(
                onClick = { if (p.running) p.cancel = true else { val n = Probe(p.model, p.input, p.gpu, p.tile, p.scale); screen = n; start(ctx.applicationContext, n) } },
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) { Text(stringResource(if (p.running) R.string.dbg_cancel else R.string.dbg_again)) }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun SetupScreen() {
        val c = MaterialTheme.colorScheme
        val ctx = LocalContext.current
        BackHandler { setup = false }
        val d = dir(ctx)
        val models = remember {
            val adb = d.listFiles { f -> f.name.endsWith(".param") }?.map { it.name.removeSuffix(".param") }.orEmpty()
            val apk = ctx.assets.list("models")?.filter { it.endsWith(".param") }?.map { it.removeSuffix(".param") }.orEmpty()
            (adb + apk).distinct().sorted()
        }
        val pictures = remember {
            d.listFiles { f -> f.name.matches(Regex("(?i).+\\.(png|jpe?g|webp)")) && !f.name.startsWith("out_") }
                ?.map { it.name }?.sorted().orEmpty()
        }
        var model by remember { mutableStateOf(models.firstOrNull()) }
        var picture by remember { mutableStateOf(pictures.firstOrNull()) }
        var gpu by remember { mutableStateOf(true) }
        Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(R.drawable.ic_back, onClick = { setup = false })
                Text(stringResource(R.string.dbg_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.size(44.dp))
            }
            @Composable
            fun group(title: Int, items: List<String>, selected: String?, pick: (String) -> Unit) {
                Text(stringResource(title).uppercase(), Modifier.padding(top = 12.dp, bottom = 6.dp), style = MaterialTheme.typography.labelSmall, color = c.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { name ->
                        val on = name == selected
                        Text(name, Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) c.primary else c.surfaceContainer)
                            .clickable { pick(name) }.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (on) c.onPrimary else c.onSurface, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            group(R.string.dbg_model, models, model) { model = it }
            if (pictures.isEmpty()) Text(stringResource(R.string.dbg_no_pictures, d.path), Modifier.padding(top = 12.dp), color = c.onSurfaceVariant)
            else group(R.string.dbg_picture, pictures, picture) { picture = it }
            val gpuLabel = stringResource(R.string.dbg_mode_gpu)
            val cpuLabel = stringResource(R.string.dbg_mode_cpu)
            group(R.string.dbg_mode, listOf(gpuLabel, cpuLabel), if (gpu) gpuLabel else cpuLabel) { gpu = it == gpuLabel }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val m = model ?: return@Button
                    val pic = picture ?: return@Button
                    val p = Probe(m, pic, gpu, 200, 4)
                    setup = false; screen = p; start(ctx.applicationContext, p)
                },
                Modifier.fillMaxWidth().padding(vertical = 12.dp), enabled = model != null && picture != null,
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) { Text(stringResource(R.string.dbg_start)) }
        }
    }
}
