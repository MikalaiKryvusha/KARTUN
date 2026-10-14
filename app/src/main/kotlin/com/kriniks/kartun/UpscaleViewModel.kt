package com.kriniks.kartun

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kriniks.kartun.engine.NcnnUpscaler
import com.kriniks.kartun.engine.TileListener
import com.kriniks.kartun.engine.UpscaleCancelledException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The main path of phase 2 (plans/03): pick → set up → upscale with progress → compare. Holds the screen, the picked
 * photo, the choices and the result; runs the engine on ONE dedicated thread (research 03 §B, lesson 7). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
class UpscaleViewModel(app: Application) : AndroidViewModel(app) {

    enum class Screen { Home, Setup, Working, Result, Settings, Manual, About }

    /**
     * The two models shipped in the APK. [msPerMp] is the starting estimate for the time per input megapixel, measured
     * on the Titan (testcases/reports/2026-10-14_F0_own_fast_model.md run 9: 5 777 ms for 1 MP;
     * 2026-10-14_F0_engine_probe.md run 6: 88 717 ms for 1 MP); each device then learns its own rate from its runs.
     */
    enum class Model(val file: String, @param:StringRes val title: Int, @param:StringRes val desc: Int, val msPerMp: Float) {
        Fast("realesr-general-x4v3", R.string.model_fast_name, R.string.model_fast_desc, 5_777f),
        Max("realesrgan-x4plus", R.string.model_max_name, R.string.model_max_desc, 88_717f),
    }

    class Source(val uri: Uri, val name: String, val bitmap: Bitmap) {
        val width get() = bitmap.width
        val height get() = bitmap.height
        val megapixels get() = width.toFloat() * height / 1_000_000f
    }

    class Result(val full: Bitmap, val display: Bitmap, val before: Bitmap, val ms: Long, val gpu: Boolean, val model: Model, val scale: Int)

    /** Tile progress of the running job; [total] is 0 until the engine reports its first tile. */
    data class Progress(val done: Int = 0, val total: Int = 0, val elapsedMs: Long = 0, val estimateMs: Long = 0, val gpu: Boolean = true)

    var screen by mutableStateOf(Screen.Home); private set
    var source by mutableStateOf<Source?>(null); private set
    /** The chosen model; choosing one starts loading it right away (the owner: «модель качать на экране улучшение»). */
    var model by mutableStateOf(Model.Fast); private set
    /** True while the chosen model is being copied out of the APK and loaded into the engine. */
    var modelLoading by mutableStateOf(false); private set
    var scaleIndex by mutableStateOf(DEFAULT_SCALE_INDEX)
    var result by mutableStateOf<Result?>(null); private set
    /** A user-facing error: the string resource and its argument (or null). */
    var error by mutableStateOf<Pair<Int, String?>?>(null); private set

    /** Save settings (mockup 'save'): the format and the quality of JPEG/WebP; 92 is the mockup's value. */
    var saveFormat by mutableStateOf(ResultStore.Format.Png)
    var saveQuality by mutableStateOf(DEFAULT_QUALITY)
    /** True while a save or a share copy is being encoded. */
    var saving by mutableStateOf(false); private set
    /** One-line outcome of the last save for the result screen («Сохранено в Pictures/KARTUN»), as a string resource. */
    var saveMessage by mutableStateOf<Int?>(null); private set

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    val scale get() = SCALES[scaleIndex]
    /** The result of the chosen scale fits the output limit (plans/04 step 1). */
    val scaleSupported get() = source?.let { fits(it.megapixels, scale) } ?: false
    /** The largest scale of the slider whose result fits the limit for the picked photo (for the notice). */
    val largestFittingScale get() = source?.let { s -> SCALES.lastOrNull { fits(s.megapixels, it) } } ?: SCALES.first()

    private val engineThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    // One preferences file for the learned speed AND the remembered settings (its name predates the settings).
    private val prefs = app.getSharedPreferences("estimates", Context.MODE_PRIVATE)
    private var job: Job? = null
    private var preload: Job? = null
    // The loaded net lives on the engine thread only (touched inside withContext(engineThread)).
    private var loaded: NcnnUpscaler? = null
    private var loadedModel: Model? = null

    /** Opens a picked or shared photo; on success goes to the setup screen. */
    fun open(uri: Uri) {
        viewModelScope.launch {
            error = null
            val ctx = getApplication<Application>()
            val decoded = withContext(engineThread) { runCatching { decode(ctx, uri) } }
            decoded.onSuccess { bmp ->
                source = Source(uri, displayName(ctx, uri), bmp)
                result = null
                screen = Screen.Setup
                preload()
            }.onFailure {
                error = if (it is TooBig) R.string.error_too_big to MAX_INPUT_MP.toString() else R.string.error_decode to null
            }
        }
    }

    /** «Ускорение GPU» of the settings; off = the CPU path. Kept between launches. */
    var useGpu by mutableStateOf(true); private set
    private var beforeSettings = Screen.Home

    // GOAL.md §3: «Сохранять настройки между запусками» - the GPU switch, the model, the scale, the save format and quality
    // come back as the user left them.
    init {
        useGpu = prefs.getBoolean(PREF_GPU, true)
        model = Model.entries.firstOrNull { it.name == prefs.getString(PREF_MODEL, null) } ?: Model.Fast
        scaleIndex = prefs.getInt(PREF_SCALE, DEFAULT_SCALE_INDEX).coerceIn(0, SCALES.lastIndex)
        saveFormat = ResultStore.Format.entries.firstOrNull { it.name == prefs.getString(PREF_FORMAT, null) } ?: ResultStore.Format.Png
        saveQuality = prefs.getInt(PREF_QUALITY, DEFAULT_QUALITY)
    }

    fun chooseGpu(on: Boolean) {
        if (on == useGpu) return
        useGpu = on
        prefs.edit { putBoolean(PREF_GPU, on) }
        // The loaded net was built for the other backend: drop it, the next screen loads the right one.
        viewModelScope.launch { withContext(engineThread) { release() } }
    }

    fun openSettings() { beforeSettings = screen; screen = Screen.Settings }

    private var beforeManual = Screen.Settings

    fun openAbout() { screen = Screen.About }

    /** The guide, from the settings or from an (i) window; Back returns where it was opened. */
    fun openManual() { beforeManual = screen; screen = Screen.Manual }

    /** True while «Вставить ссылку» downloads the picture. */
    var downloading by mutableStateOf(false); private set

    /** «Вставить ссылку на фото»: takes the first link from [clipText], downloads the picture, opens it like a picked one. */
    fun openLink(clipText: CharSequence?) {
        val url = LinkDownloader.findUrl(clipText)
        if (url == null) { error = R.string.link_none to null; return }
        if (downloading) return
        downloading = true
        error = null
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching { LinkDownloader.download(getApplication(), url) }.onFailure { Log.w(TAG, "link download failed", it) }.getOrNull()
            }
            downloading = false
            if (uri == null) error = R.string.link_failed to null else open(uri)
        }
    }

    fun chooseModel(m: Model) {
        if (m == model) return
        model = m
        prefs.edit { putString(PREF_MODEL, m.name) }
        preload()
    }

    /** Loads the chosen model on the setup screen, so «Улучшить» starts the network at once. */
    private fun preload() {
        val m = model
        modelLoading = true
        preload?.cancel()
        preload = viewModelScope.launch {
            val ok = withContext(engineThread) { runCatching { ensureLoaded(m) }.isSuccess }
            if (m == model) {
                modelLoading = false
                if (!ok) error = R.string.error_model to null
            }
        }
    }

    /** Engine thread only: makes [m] the loaded net, releasing the previous one. */
    private fun ensureLoaded(m: Model): NcnnUpscaler {
        loaded?.let { if (loadedModel == m) return it }
        release()
        val (param, bin) = ModelStore.files(getApplication(), m.file)
        val up = NcnnUpscaler.load(param.path, bin.path, useGpu = useGpu) ?: throw ModelLoadFailed()
        loaded = up
        loadedModel = m
        return up
    }

    /** Engine thread only. */
    private fun release() {
        loaded?.close()
        loaded = null
        loadedModel = null
    }

    /**
     * Estimated wall time of a run with the current choices: the megapixels each pass feeds the network x the learned
     * rate of this device. 8× adds a second pass over the 2× image (4 x the input MP), 16× over the 4× image (16 x).
     */
    fun estimateMs(): Long = estimateMs(model)

    /** The same estimate for [m] - the model cards show it next to each model (mockup: «Быстрая · для фото · ≈ 7 с»). */
    fun estimateMs(m: Model): Long {
        val src = source ?: return 0
        val rate = prefs.getFloat(m.file, m.msPerMp)
        return (src.megapixels * passFactor(scale) * rate).toLong()
    }

    fun start() {
        val src = source ?: return
        if (!scaleSupported) return
        ProcessingState.cancelRequested = false
        error = null
        prefs.edit { putInt(PREF_SCALE, scaleIndex) }
        val chosen = model
        val target = scale
        publish(Progress(estimateMs = estimateMs()))
        screen = Screen.Working
        // Keep the process alive and show the notification while the network works (plans/03 slice 2).
        UpscaleService.start(getApplication())
        job = viewModelScope.launch {
            val t0 = SystemClock.elapsedRealtime()
            val outcome = withContext(engineThread) {
                runCatching {
                    // Normally already loaded by preload(); a start during loading waits for it on this same thread.
                    ensureLoaded(chosen).let { u ->
                        publish(_progress.value.copy(gpu = u.usesGpu))
                        // Tiles of both passes are known up front, so the ring runs 0..100 % once across the whole run.
                        val mid = secondPassInput(target)
                        val pass1 = tiles(src.width, src.height)
                        val pass2 = mid?.let { tiles(src.width * it, src.height * it) } ?: 0
                        val grand = pass1 + pass2
                        // The ring counts in 200 px tiles; a retry with smaller tiles reports more of them, so each
                        // pass maps its own done/total onto its share of the ring.
                        val report = { offset: Int, share: Int ->
                            TileListener { done, total ->
                                publish(_progress.value.copy(done = offset + done * share / total, total = grand, elapsedMs = SystemClock.elapsedRealtime() - t0))
                                !ProcessingState.cancelRequested
                            }
                        }
                        var gpuUsed = u.usesGpu
                        val pass = { input: Bitmap, listener: TileListener ->
                            withFallback(chosen, u, input, listener).also { (_, gpu) -> gpuUsed = gpuUsed && gpu }.first
                        }
                        val tRun = SystemClock.elapsedRealtime()
                        val out4 = pass(src.bitmap, report(0, pass1))
                        learnRate(chosen, src.megapixels, SystemClock.elapsedRealtime() - tRun)
                        val full = when {
                            target == ENGINE_SCALE -> out4
                            mid == null -> downscale(out4, src.width * target, src.height * target)
                            else -> {
                                // 8×: the 4× result scaled down to 2× feeds the second 4× pass (2 x 4 = 8);
                                // 16×: the 4× result itself does (4 x 4 = 16). [AI] plans/04 step 1.
                                // [NOT-TESTED: two passes on the device - deferred by the owner; PassPlanTest covers the plan only]
                                val input = if (mid == ENGINE_SCALE) out4 else downscale(out4, src.width * mid, src.height * mid)
                                pass(input, report(pass1, pass2)).also { input.recycle() }
                            }
                        }
                        val display = fitForDisplay(full)
                        val before = Bitmap.createScaledBitmap(src.bitmap, display.width, display.height, true)
                        Result(full, display, before, SystemClock.elapsedRealtime() - t0, gpuUsed, chosen, target)
                    }
                }
            }
            ProcessingState.progress.value = null // the run is over: the service stops itself
            outcome.onSuccess { result = it; screen = Screen.Result }
                .onFailure {
                    screen = Screen.Setup
                    error = when (it) {
                        is UpscaleCancelledException -> null
                        is ModelLoadFailed -> R.string.error_model to null
                        else -> R.string.error_engine to null
                    }
                }
        }
    }

    fun cancel() { ProcessingState.cancelRequested = true }

    /** Saves the result into the gallery as a new file (plans/01 §2.8); the encoder runs off the main thread. */
    fun save() {
        val res = result ?: return
        val src = source ?: return
        if (saving) return
        saving = true
        saveMessage = null
        val format = saveFormat
        val quality = saveQuality
        prefs.edit { putString(PREF_FORMAT, format.name); putInt(PREF_QUALITY, quality) }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ResultStore.saveToGallery(getApplication(), res.full, ResultStore.fileName(src.name, res.model.file, res.scale, format.ext), format, quality)
                }.onFailure { Log.w(TAG, "save failed", it) }.isSuccess
            }
            saving = false
            saveMessage = if (ok) R.string.saved else R.string.save_failed
        }
    }

    /** A PNG copy of the result for the share sheet (plans/01 §2.9); null when it could not be written. */
    suspend fun shareUri(): Uri? {
        val res = result ?: return null
        val src = source ?: return null
        saving = true
        return withContext(Dispatchers.IO) {
            runCatching { ResultStore.shareCopy(getApplication(), res.full, ResultStore.fileName(src.name, res.model.file, res.scale, "png")) }
                .onFailure { Log.w(TAG, "share copy failed", it) }.getOrNull()
        }.also { saving = false }
    }

    fun dismissSaveMessage() { saveMessage = null }

    /**
     * Engine thread only. One 4× pass with the fallback ladder of plans/04 step 2 (research 03 §B lessons 2 and 4: device
     * lost on large tiles is the most common failure of Android upscalers): the loaded net with 200 px tiles → the same
     * net with 100 px tiles → a CPU net → an honest error. A cancel is never retried. Returns the bitmap and whether the
     * GPU did the work. [NOT-TESTED: the forced GPU failure on the device - deferred by the owner]
     */
    private fun withFallback(m: Model, u: NcnnUpscaler, input: Bitmap, listener: TileListener): Pair<Bitmap, Boolean> {
        try {
            return u.upscale(input, ENGINE_SCALE, TILE_PX, listener) to u.usesGpu
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pass failed with tile $TILE_PX (${e.message}); retry with ${TILE_PX / 2}")
        }
        try {
            return u.upscale(input, ENGINE_SCALE, TILE_PX / 2, listener) to u.usesGpu
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pass failed with tile ${TILE_PX / 2} (${e.message}); retry on the CPU")
        }
        if (!u.usesGpu) throw EngineFailed()
        publish(_progress.value.copy(gpu = false))
        val (param, bin) = ModelStore.files(getApplication(), m.file)
        val cpu = NcnnUpscaler.load(param.path, bin.path, useGpu = false) ?: throw EngineFailed()
        return cpu.use {
            try { it.upscale(input, ENGINE_SCALE, TILE_PX, listener) to false }
            catch (e: IllegalStateException) { Log.w(TAG, "CPU pass failed too (${e.message})"); throw EngineFailed() }
        }
    }

    /** One progress value for the screen and for the notification of [UpscaleService]. */
    private fun publish(p: Progress) {
        _progress.value = p
        ProcessingState.progress.value = p
    }

    fun back() {
        when (screen) {
            Screen.Setup -> {
                screen = Screen.Home; source = null; error = null
                viewModelScope.launch { withContext(engineThread) { release() } }
            }
            Screen.Working -> cancel()
            Screen.Result -> { screen = Screen.Setup; result = null; saveMessage = null }
            Screen.Settings -> { screen = beforeSettings; if (screen == Screen.Setup) preload() }
            Screen.Manual -> screen = beforeManual
            Screen.About -> screen = Screen.Settings
            Screen.Home -> Unit
        }
    }

    fun dismissError() { error = null }

    /** Keeps a running average of ms per input MP for each model on THIS device (the estimate learns). */
    private fun learnRate(m: Model, mp: Float, ms: Long) {
        if (mp < MIN_MP_TO_LEARN) return
        val old = prefs.getFloat(m.file, m.msPerMp)
        prefs.edit { putFloat(m.file, old * (1 - LEARN_WEIGHT) + (ms / mp) * LEARN_WEIGHT) }
    }

    override fun onCleared() {
        // The screen is gone for good (Back from the app mid-run): stop the run between tiles and let the service go,
        // so no notification is left behind for a result nobody will see.
        ProcessingState.cancelRequested = true
        ProcessingState.progress.value = null
        // Release the net on its own thread, then stop the thread (ncnn/Vulkan objects must not cross threads).
        engineThread.executor.execute { release() }
        engineThread.close()
    }

    private class TooBig : RuntimeException()
    private class ModelLoadFailed : RuntimeException()
    private class EngineFailed : RuntimeException()

    companion object {
        private const val TAG = "KARTUN"
        private const val PREF_GPU = "use_gpu"
        private const val PREF_MODEL = "model"
        private const val PREF_SCALE = "scale_index"
        private const val PREF_FORMAT = "save_format"
        private const val PREF_QUALITY = "save_quality"
        /** The owner's scale steps: «x1, x2 (по умолчанию), x4, x8, x16» (interview #002). */
        val SCALES = listOf(1, 2, 4, 8, 16)
        const val DEFAULT_SCALE_INDEX = 1
        /** The shipped models are 4x; 1x and 2x are the 4x result scaled down (plans/01 §4.5.1). */
        const val ENGINE_SCALE = 4
        /** Tile edge in px - the value of every phase 0 run on the Titan (plans/02). */
        const val TILE_PX = 200
        /** [AI] slice 1 input limit: 12.5 MP keeps the 4x output under 200 MP in one bitmap; phase 1 lifts it. */
        const val MAX_INPUT_MP = 12.5f
        /** JPEG/WebP quality by default - the value the accepted mockup 'save' shows («Качество · 92»). */
        const val DEFAULT_QUALITY = 92
        /** [AI] output limit, plans/04 step 1: the size the 4× of a 12.5 MP photo already reached in phase 2 slice 1. */
        const val MAX_OUTPUT_MP = 200f

        /** The result of a [mp]-megapixel photo at [scale] fits the output limit. */
        fun fits(mp: Float, scale: Int) = mp * scale * scale <= MAX_OUTPUT_MP

        /** The scale of the image the second 4× pass works on: 2 for 8×, 4 for 16×; null for one-pass scales. */
        fun secondPassInput(scale: Int): Int? = if (scale > ENGINE_SCALE) scale / ENGINE_SCALE else null

        /** Input megapixels of all passes, in units of the photo's own megapixels: 1, plus 4 for 8×, plus 16 for 16×. */
        fun passFactor(scale: Int): Float = 1f + (secondPassInput(scale)?.let { (it * it).toFloat() } ?: 0f)

        /** The tile count of one pass - the same count the native loop reports as `total`. */
        fun tiles(w: Int, h: Int) = ((w + TILE_PX - 1) / TILE_PX) * ((h + TILE_PX - 1) / TILE_PX)
        /** Longest side of the bitmap the compare screen draws (Android refuses to draw bitmaps over ~100 MB). */
        private const val DISPLAY_MAX_SIDE = 4096
        private const val MIN_MP_TO_LEARN = 0.2f
        private const val LEARN_WEIGHT = 0.5f

        private fun decode(ctx: Context, uri: Uri): Bitmap =
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, info, _ ->
                if (info.size.width.toFloat() * info.size.height / 1_000_000f > MAX_INPUT_MP) throw TooBig()
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
            }.let { if (it.config == Bitmap.Config.ARGB_8888) it else it.copy(Bitmap.Config.ARGB_8888, false) }

        private fun displayName(ctx: Context, uri: Uri): String =
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: uri.lastPathSegment.orEmpty()

        private fun downscale(src: Bitmap, w: Int, h: Int): Bitmap =
            Bitmap.createScaledBitmap(src, w, h, true).also { if (it !== src) src.recycle() }

        private fun fitForDisplay(full: Bitmap): Bitmap {
            val side = max(full.width, full.height)
            if (side <= DISPLAY_MAX_SIDE) return full
            val k = DISPLAY_MAX_SIDE.toFloat() / side
            return Bitmap.createScaledBitmap(full, (full.width * k).roundToInt(), (full.height * k).roundToInt(), true)
        }
    }
}
