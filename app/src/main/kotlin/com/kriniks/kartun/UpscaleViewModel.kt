package com.kriniks.kartun

import android.app.Application
import android.os.PowerManager
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kriniks.kartun.engine.UpscaleCancelledException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The main path of phase 2 (plans/03): pick → set up → upscale with progress → compare. Holds the screen, the picked
 * photo, the choices and the result; the runs themselves go through the shared [UpscaleEngine] on the process's one
 * engine thread (research 03 §B, lesson 7; plans/07 step 2). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
/**
 * Why a photo did not open, as a line the user understands (the owner 2026-10-16 ≈ 16:26: «можем в ошибке писать причину
 * понятную? чтобы пользователь понимал примерно, что не так?»). The whole cause chain is read - ImageDecoder and the
 * content resolver wrap their causes. The size limit has its own line (error_too_big) before this is asked.
 * [TESTED: 2026-10-16 · «не нашёл фото», «нет доступа», «обрывается» read on the Titan's screen from broken and cut files;
 * «не хватило памяти» by OpenErrorTest only - testcases/reports/2026-10-16_open_with_titan.md]
 */
@StringRes
internal fun openError(e: Throwable): Int {
    val chain = generateSequence(e) { it.cause }.take(8).toList()
    return when {
        chain.any { it is OutOfMemoryError } -> R.string.error_memory
        chain.any { it is SecurityException || it is java.io.FileNotFoundException } -> R.string.error_no_access
        chain.any { it is ImageDecoder.DecodeException && it.error == ImageDecoder.DecodeException.SOURCE_INCOMPLETE } ->
            R.string.error_incomplete
        // A read error past the decoder's start is a damaged file too: a HEIC cut in half gave «IOException: getPixels
        // failed with error invalid input» on the Titan (testcases/reports/2026-10-16_open_with_titan.md).
        chain.any { it is ImageDecoder.DecodeException || it is java.io.IOException } -> R.string.error_not_photo
        else -> R.string.error_decode
    }
}

class UpscaleViewModel(app: Application) : AndroidViewModel(app) {

    enum class Screen { Home, Setup, Working, Result, Settings, Manual, About, Terms, Models }

    /** The model catalogue of the APK (ModelCatalog.kt): the built-in models and the ones downloaded on demand; the
     *  starting time estimates per model stand in it (docs/model-catalogue.json -> gpuMsPerMp, cpuMsPerMp, rateSource). */
    val catalogue: Catalogue = ModelCatalog.load(app)

    /** The picked photo: [bitmap] at full size for the engine, [preview] at most DISPLAY_MAX_SIDE for every screen -
     *  Android refuses to draw a bitmap over ~100 MB, and a 100 MP photo is 400 MB. */
    class Source(val uri: Uri, val name: String, val bitmap: Bitmap) {
        val preview: Bitmap = fitForDisplay(bitmap)
        val width get() = bitmap.width
        val height get() = bitmap.height
        val megapixels get() = width.toFloat() * height / 1_000_000f
    }

    class Result(val full: Bitmap, val display: Bitmap, val before: Bitmap, val ms: Long, val gpu: Boolean, val model: Model, val scale: Int)

    /** Tile progress of the running job; [total] is 0 until the engine reports its first tile. [started] turns true when the
     *  engine takes this run - before that it may wait behind a job of the API (UpscaleEngine.apiBusy). */
    data class Progress(val done: Int = 0, val total: Int = 0, val elapsedMs: Long = 0, val estimateMs: Long = 0, val gpu: Boolean = true,
        val started: Boolean = false)

    /** True while a job of the API holds the engine (the Working screen's «Ждёт очереди», variant C). */
    val apiBusy: StateFlow<Boolean> = UpscaleEngine.apiBusy

    /** «Остановить задание по API» on the Working screen. */
    fun stopApiJob() = UpscaleEngine.stopApiJob()

    var screen by mutableStateOf(Screen.Home); private set
    var source by mutableStateOf<Source?>(null); private set
    /** The chosen model; choosing one starts loading it right away (the owner: «модель качать на экране улучшение»). */
    var model by mutableStateOf(catalogue.default); private set
    /** True while the chosen model is being copied out of the APK and loaded into the engine. */
    var modelLoading by mutableStateOf(false); private set
    var scaleIndex by mutableIntStateOf(DEFAULT_SCALE_INDEX)
    var result by mutableStateOf<Result?>(null); private set
    /** A user-facing error: the string resource and its argument (or null). */
    var error by mutableStateOf<Pair<Int, String?>?>(null); private set

    /** Save settings (mockup 'save'): the format and the quality of JPEG/WebP; 92 is the mockup's value. */
    var saveFormat by mutableStateOf(ResultStore.Format.Png)
    var saveQuality by mutableIntStateOf(DEFAULT_QUALITY)
    /** True while a save or a share copy is being encoded. */
    var saving by mutableStateOf(false); private set
    /** One-line outcome of the last save for the result screen («Сохранено в Pictures/KARTUN»), as a string resource. */
    var saveMessage by mutableStateOf<Int?>(null); private set

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    /** The last slider step for the picked photo: the first step whose result does not fit the output limit (it runs at
     *  the largest size that fits - [limited]); the steps above it would give the same result and are not offered. */
    val scaleCap get() = source?.let { s -> SCALES.indexOfFirst { !fits(s.megapixels, it) }.takeIf { it >= 0 } } ?: SCALES.lastIndex
    /** The scale the run uses: the chosen step, at most [scaleCap] (the chosen step itself stays remembered). */
    val scale get() = SCALES[minOf(scaleIndex, scaleCap)]
    /** The result size when [scale] is over the output limit - «4× не поместится, можем 3,2×» (plans/08 step 9); null
     *  when the scale fits. [TESTED: 2026-10-16 · Titan 50 MP - testcases/reports/2026-10-16_fractional_scale_titan.md] */
    val limited get() = source?.let { limitedSize(it.width, it.height, scale) }
    /** A photo is open (every step runs: over the limit at the largest size that fits). */
    val scaleSupported get() = source != null

    // One preferences file for the learned speed AND the remembered settings (its name predates the settings).
    private val prefs = app.getSharedPreferences(UpscaleEngine.PREFS, Context.MODE_PRIVATE)
    private var job: Job? = null
    private var preload: Job? = null

    /** Opens a picked or shared photo; on success goes to the setup screen. */
    fun open(uri: Uri) {
        viewModelScope.launch {
            error = null
            val ctx = getApplication<Application>()
            val decoded = withContext(UpscaleEngine.dispatcher) {
                runCatching {
                    UpscaleEngine.decodeAny(ImageDecoder.createSource(ctx.contentResolver, uri)) {
                        ctx.contentResolver.openFileDescriptor(uri, "r")
                    }
                }
            }
            decoded.onSuccess { bmp ->
                source = Source(uri, displayName(ctx, uri), bmp)
                result = null
                screen = Screen.Setup
                preload()
            }.onFailure {
                // The reason goes to logcat: «Не удалось открыть это фото» alone left a shell-shared photo unexplained
                // (testcases/reports/2026-10-16_epic06_A_step2_shared_engine.md, Found 1).
                Log.w(TAG, "open failed: $uri", it)
                // The owner sees «Не удалось открыть это фото» «часто» (2026-10-16 ≈ 16:22) and logcat had already rolled
                // over: the app's own journal keeps the cause - the source URI, its type and the exception chain.
                val type = runCatching { ctx.contentResolver.getType(uri) }.getOrNull()
                CrashLog.note("open failed: $uri type=$type ${generateSequence(it) { e -> e.cause }.take(3)
                    .joinToString(" <- ") { e -> "${e.javaClass.simpleName}: ${e.message}" }}")
                error = if (it is UpscaleEngine.TooBig) R.string.error_too_big to mpLimitText() else openError(it) to null
            }
        }
    }

    /** The cards of «Улучшение»: the two models on the device used last, the last one on top (the owner, plan 03 step 18:
     *  «показывать две последние использованные модели. Вверху последнюю использованную, ниже предпоследнюю»); the
     *  built-in ones fill the list until two were used. The rest are reached through «Все модели». */
    var recentModels by mutableStateOf(listOf<Model>()); private set

    /** Ids of the models on the device: built in, or downloaded with both files at their catalogue size. */
    var onDevice by mutableStateOf(setOf<String>()); private set

    /** Downloads in flight: model id -> bytes on disk and the model's total (the card's ring and «12 из 33 МБ»). */
    val downloads = mutableStateMapOf<String, Pair<Long, Long>>()

    /** The last download's failure for the «Все модели» screen, as a string resource; null = nothing to say. */
    var modelsMessage by mutableStateOf<Int?>(null); private set

    /** «Ускорение GPU» of the settings; off = the CPU path. Kept between launches. */
    var useGpu by mutableStateOf(true); private set
    private var beforeSettings = Screen.Home

    /** True while the first-launch speed check runs: the home screen shows its dialog (ideas/04, [DeviceBench]). Lives in
     *  the ViewModel so a turn of the screen neither restarts the check nor loses the dialog. Declared above `init`, which starts the check:
     *  a property below it is still null there (the launch crash of 2026-10-16 20:06 on the Redmi). [NOT-TESTED] */
    var benchRunning by mutableStateOf(false); private set

    /** True while battery saver holds the first-launch check back: the home screen warns and offers to turn it off (the
     *  owner, 2026-10-16 ≈ 22:1x: «предупреждать, что он включен на устройстве. Предлагается выключить, а затем запустить
     *  бенч»; ≈ 22:1x «ЗАПУСКАТЬ!» - «Замерить так» runs it anyway). In saver the GPU ran the short check 1.7× slower while a
     *  real run was only 1.1× slower - the estimate came out 0.59 of the real time (RC3 check on the Titan). [NOT-TESTED] */
    var benchSaver by mutableStateOf(false); private set
    private var saverWatch: BroadcastReceiver? = null

    private fun startBenchIfNeeded() {
        val app = getApplication<Application>()
        if (!DeviceBench.needed(app)) return
        if (app.getSystemService(PowerManager::class.java).isPowerSaveMode) {
            benchSaver = true
            // Battery saver turned off (here or in the system settings) -> the check starts by itself.
            val r = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    if (benchSaver && !app.getSystemService(PowerManager::class.java).isPowerSaveMode) runBench()
                }
            }
            app.registerReceiver(r, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            saverWatch = r
            return
        }
        runBench()
    }

    private fun stopSaverWatch() {
        saverWatch?.let { runCatching { getApplication<Application>().unregisterReceiver(it) } }
        saverWatch = null
        benchSaver = false
    }

    /** «Замерить так»: the check runs although battery saver is on. */
    fun benchAnyway() = runBench()

    private fun runBench() {
        stopSaverWatch()
        if (benchRunning) return
        val app = getApplication<Application>()
        benchRunning = true
        viewModelScope.launch {
            runCatching { DeviceBench.run(app) { !benchRunning } }.onFailure { Log.w("KARTUN", "bench failed", it) }
            benchRunning = false
        }
    }

    /** «Отмена» of the check: the dialog closes now, the engine stops after its current pass and nothing is written. */
    fun cancelBench() {
        stopSaverWatch()
        DeviceBench.skip(getApplication())
        benchRunning = false
    }

    fun chooseGpu(on: Boolean) {
        if (on == useGpu) return
        useGpu = on
        prefs.edit { putBoolean(PREF_GPU, on) }
        // The loaded net was built for the other backend: drop it, the next screen loads the right one.
        viewModelScope.launch { UpscaleEngine.release() }
    }

    fun openSettings() { beforeSettings = screen; screen = Screen.Settings }

    private var beforeManual = Screen.Settings

    fun openAbout() { screen = Screen.About }

    /** «Условия использования» (the owner, plan 03 step 40), from the settings; Back returns there. */
    fun openTerms() { screen = Screen.Terms }

    /** The guide, from the settings or from an (i) window; Back returns where it was opened. */
    fun openManual() { beforeManual = screen; screen = Screen.Manual }

    /** True while «Вставить ссылку» downloads the picture. */
    var downloading by mutableStateOf(false); private set

    /** The link window is open: the clipboard held no link, so the user types or pastes one (the owner, chat 2026-10-14
     *  ≈ 21:59 +03:00: «вставить ссылку можно не только из буфера обмена, в просто в текстовое поле» · «если буффер пуст,
     *  то пусть кнопка открывается как поле ввода»). */
    var linkDialog by mutableStateOf(false); private set

    /** «Вставить ссылку на фото»: the first link in [clipText] downloads at once; no link there opens the link window. */
    fun openLink(clipText: CharSequence?) {
        val url = LinkDownloader.findUrl(clipText)
        if (url == null) { error = null; linkDialog = true; return }
        download(url)
    }

    /** «Скачать» in the link window: [text] as typed or pasted. */
    fun downloadLink(text: String) {
        val url = LinkDownloader.findUrl(text) ?: return
        linkDialog = false
        download(url)
    }

    fun closeLinkDialog() { linkDialog = false }

    /** Downloads the picture at [url] and opens it like a picked one. */
    private fun download(url: String) {
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
        if (m == model || m.id !in onDevice) return
        model = m
        prefs.edit { putString(PREF_MODEL, m.id) }
        preload()
    }

    /** «Все модели» from «Улучшение». */
    fun openModels() { modelsMessage = null; screen = Screen.Models }

    /** A card of «Все модели» on the device: it becomes the chosen model in the TOP card of «Улучшение», the model that
     *  was on top moves to the lower card, and the user is back there (the owner, chat 2026-10-16, before 10:48 +03:00:
     *  «Выбранная модель становится выбранной в верхней плиточке. Предыдущая которая была в верхней плиточки смещается в
     *  нижнюю»). [NOT-TESTED] */
    fun pickFromCatalogue(m: Model) {
        if (m.id !in onDevice) return
        putOnTop(m)
        chooseModel(m)
        screen = Screen.Setup
    }

    /** [m] becomes the top card of «Улучшение», the former top card the lower one. */
    private fun putOnTop(m: Model) {
        prefs.edit { putString(PREF_MODEL_ORDER, pickedFirst(prefs.getString(PREF_MODEL_ORDER, null), catalogue, onDevice, m.id)) }
        refreshOnDevice()
    }

    /** «Скачать» on a card: the files come from the release `models` into the app's folder, hash-checked; the card shows
     *  the bytes as they arrive. Several downloads may run at once, one per model. */
    fun download(m: Model) {
        if (m.builtin || m.id in downloads || m.id in onDevice) return
        modelsMessage = null
        downloads[m.id] = 0L to m.bytes
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            var shown = 0L
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    ModelDownloader.download(m, ModelStore.dir(ctx), catalogue::url, progress = { done, total ->
                        // One state write per ~1 % keeps the list from recomposing on every 64 KB buffer.
                        if (done - shown >= total / PROGRESS_STEPS || done == total) {
                            shown = done
                            viewModelScope.launch { if (m.id in downloads) downloads[m.id] = done to total }
                        }
                    })
                }.onFailure { Log.w(TAG, "model download failed: ${m.id}", it) }
            }
            downloads.remove(m.id)
            refreshOnDevice()
            modelsMessage = when (outcome.exceptionOrNull()) {
                null -> null
                is ModelDownloader.Damaged -> R.string.model_damaged
                else -> R.string.model_download_failed
            }
        }
    }

    /** «Удалить» on a downloaded card: its files leave the device; if it was the chosen model, the default takes its place. */
    fun deleteModel(m: Model) {
        if (m.builtin || m.id in downloads) return
        viewModelScope.launch {
            UpscaleEngine.releaseIf(m)
            withContext(Dispatchers.IO) { ModelStore.delete(getApplication(), m) }
            refreshOnDevice()
            if (model == m) {
                model = catalogue.default
                prefs.edit { putString(PREF_MODEL, model.id) }
                // The chosen model always has its card: «Все модели» is a plain button, it shows no model.
                if (model !in recentModels) putOnTop(model)
            }
        }
    }

    private fun refreshOnDevice() {
        val ctx = getApplication<Application>()
        onDevice = catalogue.models.filter { ModelStore.isAvailable(ctx, it) }.map { it.id }.toSet()
        recentModels = recent(prefs.getString(PREF_MODEL_ORDER, null), catalogue, onDevice).mapNotNull { catalogue.byId(it) }
    }

    /** The first builds stored the enum names «Fast» and «Max»; they become catalogue ids once. */
    private fun migrateModelPrefs() {
        val rename = mapOf("Fast" to "realesr-general-x4v3", "Max" to "realesrgan-x4plus")
        val order = prefs.getString(PREF_MODEL_ORDER, null)
        val chosen = prefs.getString(PREF_MODEL, null)
        if (chosen !in rename && order?.split(',')?.none { it in rename } != false) return
        prefs.edit {
            chosen?.let { putString(PREF_MODEL, rename[it] ?: it) }
            order?.let { putString(PREF_MODEL_ORDER, it.split(',').joinToString(",") { n -> rename[n] ?: n }) }
        }
    }

    /** Loads the chosen model on the setup screen, so «Улучшить» starts the network at once. */
    private fun preload() {
        val m = model
        modelLoading = true
        preload?.cancel()
        preload = viewModelScope.launch {
            val ok = runCatching { UpscaleEngine.preload(getApplication(), m, useGpu) }.isSuccess
            if (m == model) {
                modelLoading = false
                if (!ok) error = (if (m.builtin) R.string.error_model else R.string.error_model_downloaded) to null
            }
        }
    }

    /**
     * Estimated wall time of a run with the current choices: the megapixels each pass feeds the network x the learned
     * rate of this device. 8× adds a second pass over the 2× image (4 x the input MP), 16× over the 4× image (16 x).
     */
    fun estimateMs(): Long = estimateMs(model)

    /** The same estimate for [m] - the model cards show it next to each model (mockup: «Быстрая · для фото · ≈ 7 с»). */
    fun estimateMs(m: Model): Long {
        val src = source ?: return 0
        // The rate of the backend that will run: GPU off means the CPU, 15-16 times slower on the Titan (the owner, chat
        // 2026-10-14 ≈ 22:40 +03:00: «эстиматор времени улучшения неверно рабоает, когда ГПУ ускорение выключено» ·
        // «пишет в кнопке 6 секунд» · «а на самом деле больше минуты работа требует»).
        val rate = prefs.getFloat(rateKey(m, useGpu), startRate(m, useGpu))
        return (src.megapixels * passFactor(scale) * rate).toLong()
    }

    fun start() {
        val src = source ?: return
        if (!scaleSupported) return
        ProcessingState.cancelRequested = false
        cancelling = false
        error = null
        prefs.edit { putInt(PREF_SCALE, scaleIndex); putString(PREF_MODEL_ORDER, usedFirst(prefs.getString(PREF_MODEL_ORDER, null), model.id)) }
        refreshOnDevice()
        val chosen = model
        val target = scale
        publish(Progress(estimateMs = estimateMs()))
        screen = Screen.Working
        // Keep the process alive and show the notification while the network works (plans/03 slice 2).
        UpscaleService.start(getApplication())
        job = viewModelScope.launch {
            // The run's clock starts when the engine takes it, never while it waits behind an API job: the wait is shown
            // apart («Ждёт») and stays out of the result's time (the owner's variant C, 2026-10-16).
            var t0 = SystemClock.elapsedRealtime()
            var started = false
            val outcome = runCatching {
                // The shared engine runs the passes (UpscaleEngine.run); the screen only maps its progress and the result.
                val out = UpscaleEngine.run(
                    getApplication(), src.bitmap, chosen, target, useGpu,
                    onProgress = { done, total, gpu ->
                        if (!started) { started = true; t0 = SystemClock.elapsedRealtime() }
                        publish(_progress.value.copy(done = done, total = total, elapsedMs = SystemClock.elapsedRealtime() - t0, gpu = gpu, started = true))
                    },
                    cancelled = { ProcessingState.cancelRequested },
                )
                // The display copies are drawn off the main thread, off the engine thread too: no engine call here.
                withContext(Dispatchers.Default) {
                    val display = fitForDisplay(out.bitmap)
                    val before = Bitmap.createScaledBitmap(src.preview, display.width, display.height, true)
                    Result(out.bitmap, display, before, SystemClock.elapsedRealtime() - t0, out.gpuUsed, chosen, target)
                }
            }
            ProcessingState.progress.value = null // the run is over: the service stops itself
            outcome.onSuccess {
                result = it; screen = Screen.Result
                if (!ProcessingState.appVisible) UpscaleService.notifyDone(getApplication())
            }
                .onFailure {
                    screen = Screen.Setup
                    error = when (it) {
                        is UpscaleCancelledException -> null
                        is UpscaleEngine.ModelLoadFailed -> (if (chosen.builtin) R.string.error_model else R.string.error_model_downloaded) to null
                        else -> R.string.error_engine to null
                    }
                }
        }
    }

    /** «Отменить» was pressed: the processing screen says «Отменяем…» at once - the engine stops only after its current tile,
     *  and for 4-6 s nothing changed on a slow phone (RC smoke on the A52 and Redmi, 2026-10-16). [NOT-TESTED] */
    var cancelling by mutableStateOf(false); private set

    fun cancel() { ProcessingState.cancelRequested = true; cancelling = true }

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
        renderCancel.set(false)
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    ResultStore.saveToGallery(getApplication(), res.full, ResultStore.fileName(src.name, res.model.id, scaleLabel(src, res), format.ext), format, quality, renderCancel)
                }.onFailure { if (it !is ResultStore.RenderCancelled) Log.w(TAG, "save failed", it) }
            }
            saving = false
            // A cancelled render says nothing: the user asked for it, and the half-written file is already deleted.
            saveMessage = when {
                outcome.isSuccess -> R.string.saved
                outcome.exceptionOrNull() is ResultStore.RenderCancelled -> null
                else -> R.string.save_failed
            }
        }
    }

    /** Set by «Отменить» under «Рендер…»; the encoder's stream reads it on every write (ResultStore.Cancellable). */
    private val renderCancel = java.util.concurrent.atomic.AtomicBoolean(false)

    /** «Отменить» under «Рендер…»: stops the file being written for «Сохранить» or «Поделиться» and deletes it. */
    fun cancelRender() { renderCancel.set(true) }

    /** A PNG copy of the result for the share sheet (plans/01 §2.9); null when it could not be written. */
    /** True while the file for «Поделиться» is written: «Сохранить» keeps its own label then (it read «Сохранение…» although
     *  Share was pressed - RC smoke on the Redmi, 2026-10-16). [NOT-TESTED] */
    var sharing by mutableStateOf(false); private set

    suspend fun shareUri(): Uri? {
        val res = result ?: return null
        val src = source ?: return null
        if (saving) return null
        saving = true
        sharing = true
        renderCancel.set(false)
        return withContext(Dispatchers.IO) {
            runCatching { ResultStore.shareCopy(getApplication(), res.full, ResultStore.fileName(src.name, res.model.id, scaleLabel(src, res), "png"), renderCancel) }
                .onFailure { if (it !is ResultStore.RenderCancelled) Log.w(TAG, "share copy failed", it) }.getOrNull()
        }.also { saving = false; sharing = false }
    }

    fun dismissSaveMessage() { saveMessage = null }

    /** One progress value for the screen and for the notification of [UpscaleService]. */
    private fun publish(p: Progress) {
        _progress.value = p
        ProcessingState.progress.value = p
    }

    fun back() {
        when (screen) {
            Screen.Setup -> {
                screen = Screen.Home; source = null; error = null
                viewModelScope.launch { UpscaleEngine.release() }
            }
            Screen.Working -> cancel()
            Screen.Result -> { screen = Screen.Setup; result = null; saveMessage = null }
            Screen.Settings -> { screen = beforeSettings; if (screen == Screen.Setup) preload() }
            Screen.Models -> { screen = Screen.Setup; modelsMessage = null }
            Screen.Manual -> screen = beforeManual
            Screen.About -> screen = Screen.Settings
            Screen.Terms -> screen = Screen.Settings
            Screen.Home -> Unit
        }
    }

    fun dismissError() { error = null }

    override fun onCleared() {
        stopSaverWatch()
        // The screen is gone for good (Back from the app mid-run): stop the run between tiles and let the service go,
        // so no notification is left behind for a result nobody will see.
        ProcessingState.cancelRequested = true
        ProcessingState.progress.value = null
        // Release the net on its own thread (ncnn/Vulkan objects must not cross threads). The thread itself stays: it is
        // the process's one engine thread, and stopping it is what aborted the next screen's model load (bugs/01).
        UpscaleEngine.releaseLater()
    }

    // GOAL.md §3: «Сохранять настройки между запусками» - the GPU switch, the model, the scale, the save format and quality
    // come back as the user left them.
    // The init block stands LAST in the class, below every property: Kotlin runs property initializers and init blocks in
    // source order, so a property declared below `init` is still null while init runs. That killed the first launch twice on
    // 2026-10-16 - `benchRunning` (20:06, Redmi) and `benchSaver` (22:32, RC4 on the Titan and the Redmi, `pm clear` +
    // launch: NPE in the ViewModel constructor). Guard: InitLastGuardTest. [NOT-TESTED]
    init {
        useGpu = prefs.getBoolean(PREF_GPU, true)
        migrateModelPrefs()
        refreshOnDevice()
        model = catalogue.byId(prefs.getString(PREF_MODEL, null))?.takeIf { it.id in onDevice } ?: catalogue.default
        // A model picked in «Все модели» by an older build was not put on the cards: it takes the top one now.
        if (model !in recentModels) putOnTop(model)
        scaleIndex = prefs.getInt(PREF_SCALE, DEFAULT_SCALE_INDEX).coerceIn(0, SCALES.lastIndex)
        saveFormat = ResultStore.Format.entries.firstOrNull { it.name == prefs.getString(PREF_FORMAT, null) } ?: ResultStore.Format.Png
        saveQuality = prefs.getInt(PREF_QUALITY, DEFAULT_QUALITY)
        startBenchIfNeeded()
    }

    companion object {
        private const val TAG = "KARTUN"
        /** «Ускорение GPU»; the agent door reads it too, as the default backend of a job (agent/AgentService). */
        const val PREF_GPU = "use_gpu"
        private const val PREF_MODEL = "model"
        private const val PREF_MODEL_ORDER = "model_order"

        /** How many model cards «Улучшение» shows (the owner, plan 03 step 18: «две последние использованные модели»). */
        const val RECENT_CARDS = 2
        private const val PROGRESS_STEPS = 100

        /** The ids in the stored order (comma-separated, most recent first), duplicates and blanks dropped. */
        fun storedIds(stored: String?): List<String> = stored.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        /** The stored order after a run with [id]: [id] moves to the front. [NOT-TESTED] */
        fun usedFirst(stored: String?, id: String): String = (listOf(id) + storedIds(stored).filter { it != id }).joinToString(",")

        /** The ids of the «Улучшение» cards: the models on the device used last, most recent first, then the built-in ones
         *  (the default first) until there are [RECENT_CARDS]. [NOT-TESTED] */
        fun recent(stored: String?, catalogue: Catalogue, onDevice: Set<String>): List<String> {
            val known = catalogue.models.map { it.id }.toSet()
            val used = storedIds(stored).filter { it in known && it in onDevice }
            val fill = catalogue.models.filter { it.builtin }.sortedByDescending { it.isDefault }.map { it.id }.filter { it !in used }
            return (used + fill).take(RECENT_CARDS)
        }

        /** The stored order after [id] was picked in «Все модели»: [id] on top, then the cards shown until now - so the
         *  former top card becomes the lower one even when it was only a built-in filler, never stored. [NOT-TESTED] */
        fun pickedFirst(stored: String?, catalogue: Catalogue, onDevice: Set<String>, id: String): String =
            usedFirst((recent(stored, catalogue, onDevice) + storedIds(stored)).distinct().joinToString(","), id)
        private const val PREF_SCALE = "scale_index"
        private const val PREF_FORMAT = "save_format"
        private const val PREF_QUALITY = "save_quality"
        /** The owner's scale steps: «x1, x2 (по умолчанию), x4, x8, x16» (interview #002). */
        val SCALES = listOf(1, 2, 4, 8, 16)
        const val DEFAULT_SCALE_INDEX = 2  // 4× (the owner, 2026-10-16 ≈ 15:03: «давай увеличение x4 сделаем на ползунке по умолчанию»)
        /** The shipped models are 4x; 1x and 2x are the 4x result scaled down (plans/01 §4.5.1). */
        const val ENGINE_SCALE = 4
        /** Tile edge in px - the value of every phase 0 run on the Titan (plans/02). */
        const val TILE_PX = 200
        /** Input limit: the owner, chat 2026-10-15 ≈ 11:35 +03:00: «нужно сделать на входе поддержку файлов больше 12 мб.
         *  Думаю, до 100 мб можно давтаь поддержку» - read as 100 MP (the old limit he saw was «12,5 Мп»). With the
         *  output limit below, a 100 MP photo is offered 1×, a 50 MP one up to 2×. */
        // 200 MP since the owner, chat 2026-10-16 ≈ 15:39 +03:00: «давай лимит входных поднимем до 200 мегапикселей».
        const val MAX_INPUT_MP = 200f
        /** JPEG/WebP quality by default - the value the accepted mockup 'save' shows («Качество · 92»). */
        const val DEFAULT_QUALITY = 92
        /** Output limit: Android's ceiling for one Bitmap - 2^31 - 1 bytes, 536.87 MP in ARGB_8888 (the owner, chat 2026-10-16
         *  ≈ 15:13 +03:00: «лимит Android ~2,1 ГБ/536 М - тогда вот такой лимит делаем»; was 500 since 2026-10-15). A scale whose
         *  result is over it is not offered (the same rule: «если более … - то всё, нельзя, выбирай шаг меньше»). */
        const val MAX_OUTPUT_MP = 536f

        /** The input limit as the error line shows it: «100», not «100.0». */
        fun mpLimitText(): String = if (MAX_INPUT_MP % 1f == 0f) MAX_INPUT_MP.toInt().toString() else MAX_INPUT_MP.toString()

        /** The result of a [mp]-megapixel photo at [scale] fits the output limit. */
        fun fits(mp: Float, scale: Int) = mp * scale * scale <= MAX_OUTPUT_MP

        /**
         * The result size when [scale] does not fit the output limit: the largest w × h of the photo's proportions
         * within MAX_OUTPUT_MP (plans/08 step 9 - «4× для этого фото не поместится, можем 3,2×»); null when [scale]
         * fits. [TESTED: 2026-10-16 · LimitedSizeTest; Titan 50 MP - testcases/reports/2026-10-16_fractional_scale_titan.md]
         */
        fun limitedSize(w: Int, h: Int, scale: Int): Pair<Int, Int>? {
            if (fits(w.toFloat() * h / 1_000_000f, scale)) return null
            val f = kotlin.math.sqrt(MAX_OUTPUT_MP * 1_000_000.0 / (w.toDouble() * h))
            return (w * f).toInt().coerceAtLeast(1) to (h * f).toInt().coerceAtLeast(1)
        }

        /** The factor of a limited result, as the slider shows it («3,2×»): one decimal, rounded down. */
        fun limitedFactor(w: Int, limitedW: Int): Float = kotlin.math.floor(limitedW * 10f / w) / 10f

        /** The scale in a file name: «4», or «3.2» when the result was limited by the output size. */
        fun scaleLabel(src: Source, res: Result): String =
            if (res.full.width == src.width * res.scale) "${res.scale}"
            else String.format(java.util.Locale.ROOT, "%.1f", limitedFactor(src.width, res.full.width))

        /** The scale of the image the second 4× pass works on: 2 for 8×, 4 for 16×; null for one-pass scales. */
        fun secondPassInput(scale: Int): Int? = if (scale > ENGINE_SCALE) scale / ENGINE_SCALE else null

        /** Input megapixels of all passes, in units of the photo's own megapixels: 1, plus 4 for 8×, plus 16 for 16×. */
        fun passFactor(scale: Int): Float = 1f + (secondPassInput(scale)?.let { (it * it).toFloat() } ?: 0f)

        /** The tile count of one pass - the same count the native loop reports as `total`. */
        fun tiles(w: Int, h: Int) = ((w + TILE_PX - 1) / TILE_PX) * ((h + TILE_PX - 1) / TILE_PX)
        /** Longest side of the bitmap the compare screen draws (Android refuses to draw bitmaps over ~100 MB). */
        private const val DISPLAY_MAX_SIDE = 4096

        /** The preference key of a learned rate: one per model and backend. New keys on purpose - the old one per model
         *  mixed GPU and CPU runs, so a CPU run had inflated the GPU estimate too. [TESTED: 2026-10-14 · EstimateTest] */
        fun rateKey(m: Model, gpu: Boolean): String = "rate_${m.id}_${if (gpu) "gpu" else "cpu"}"

        /** The starting rate before this device has learned one (the catalogue's, measured on the Titan). */
        fun startRate(m: Model, gpu: Boolean): Float = if (gpu) m.gpuMsPerMp else m.cpuMsPerMp

        /**
         * The photo's name for the screens and the saved result's file name. The system photo picker hides real file names
         * on purpose (privacy): it reports `<media id>.jpg` («1000009282.jpg», seen on the Redmi 2026-10-16 for a file named
         * kartun_smoke_noexif.jpg). A picked photo is therefore named like a camera file by its date taken -
         * «IMG_20261016_194512.jpg»; files that come by «Share» / «Open with» keep their own names (the owner, 2026-10-16
         * ≈ 20:4x: «имя файла из галереи приходит номером … Чинить»). [NOT-TESTED]
         */
        private fun displayName(ctx: Context, uri: Uri): String {
            val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: uri.lastPathSegment.orEmpty()
            val picked = uri.authority == MediaStore.AUTHORITY && uri.pathSegments.any { it.startsWith("picker") }
            if (!picked) return name
            val taken = runCatching {
                ctx.contentResolver.query(uri, arrayOf(MediaStore.PickerMediaColumns.DATE_TAKEN), null, null, null)?.use {
                    if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
                }
            }.getOrNull() ?: return name
            val ext = name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
            return "IMG_" + SimpleDateFormat(CAMERA_NAME_PATTERN, Locale.US).format(Date(taken)) + ext
        }

        /** The camera's file-name stamp (Android camera apps: IMG_20261016_194512.jpg). */
        private const val CAMERA_NAME_PATTERN = "yyyyMMdd_HHmmss"

        fun fitForDisplay(full: Bitmap): Bitmap {
            val side = max(full.width, full.height)
            if (side <= DISPLAY_MAX_SIDE) return full
            val k = DISPLAY_MAX_SIDE.toFloat() / side
            return Bitmap.createScaledBitmap(full, (full.width * k).roundToInt(), (full.height * k).roundToInt(), true)
        }
    }
}
