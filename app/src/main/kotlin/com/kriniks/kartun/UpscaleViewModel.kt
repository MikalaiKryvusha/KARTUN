package com.kriniks.kartun

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kriniks.kartun.engine.NcnnUpscaler
import com.kriniks.kartun.engine.UpscaleCancelledException
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

    enum class Screen { Home, Setup, Working, Result }

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

    class Result(val full: Bitmap, val display: Bitmap, val before: Bitmap, val ms: Long, val gpu: Boolean)

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

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    val scale get() = SCALES[scaleIndex]
    val scaleSupported get() = scale <= ENGINE_SCALE

    private val engineThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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

    fun chooseModel(m: Model) {
        if (m == model) return
        model = m
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
        val up = NcnnUpscaler.load(param.path, bin.path, useGpu = true) ?: throw ModelLoadFailed()
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

    /** Estimated wall time of a run with the current choices: input MP x the learned rate of this device. */
    fun estimateMs(): Long {
        val src = source ?: return 0
        val rate = prefs.getFloat(model.file, model.msPerMp)
        return (src.megapixels * rate).toLong()
    }

    fun start() {
        val src = source ?: return
        if (!scaleSupported) return
        ProcessingState.cancelRequested = false
        error = null
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
                        val tRun = SystemClock.elapsedRealtime()
                        val out4 = u.upscale(src.bitmap, ENGINE_SCALE, TILE_PX) { done, total ->
                            publish(_progress.value.copy(done = done, total = total, elapsedMs = SystemClock.elapsedRealtime() - t0))
                            !ProcessingState.cancelRequested
                        }
                        learnRate(chosen, src.megapixels, SystemClock.elapsedRealtime() - tRun)
                        val full = if (target == ENGINE_SCALE) out4 else downscale(out4, src.width * target, src.height * target)
                        val display = fitForDisplay(full)
                        val before = Bitmap.createScaledBitmap(src.bitmap, display.width, display.height, true)
                        Result(full, display, before, SystemClock.elapsedRealtime() - t0, u.usesGpu)
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
            Screen.Result -> { screen = Screen.Setup; result = null }
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

    companion object {
        /** The owner's scale steps: «x1, x2 (по умолчанию), x4, x8, x16» (interview #002). */
        val SCALES = listOf(1, 2, 4, 8, 16)
        const val DEFAULT_SCALE_INDEX = 1
        /** The shipped models are 4x; 1x and 2x are the 4x result scaled down (plans/01 §4.5.1). */
        const val ENGINE_SCALE = 4
        /** Tile edge in px - the value of every phase 0 run on the Titan (plans/02). */
        private const val TILE_PX = 200
        /** [AI] slice 1 input limit: 12.5 MP keeps the 4x output under 200 MP in one bitmap; phase 1 lifts it. */
        const val MAX_INPUT_MP = 12.5f
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
