package com.kriniks.kartun.agent

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.ImageDecoder
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.kriniks.kartun.BuildConfig
import com.kriniks.kartun.ModelCatalog
import com.kriniks.kartun.ModelStore
import com.kriniks.kartun.R
import com.kriniks.kartun.ResultStore
import com.kriniks.kartun.UpscaleEngine
import com.kriniks.kartun.UpscaleService
import com.kriniks.kartun.UpscaleViewModel
import com.kriniks.kartun.engine.NcnnUpscaler
import com.kriniks.kartun.engine.UpscaleCancelledException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The ADB door for AI agents (plans/06 §4.2–4.4, plans/07 steps 1 and 4): an exported service guarded by the `DUMP`
 * permission, which only the adb shell holds, so an app on the device cannot call it (criterion A7). An agent on the PC
 * talks to it with `am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService -a <action> --es …` and
 * reads the answers as files in `files/agent/` (the protocol block of plans/07, published in docs/agent-api.md):
 *
 * - `UPSCALE` (`job`, `in`, `model`, `scale`, `format`, `quality`, `gpu`) → `<job>.json` goes queued → running →
 *   succeeded (`<job>.out.<ext>` written) / failed / canceled; jobs run one after another, at most [QUEUE_CAP] waiting;
 * - `CANCEL` (`job`) → a queued job is canceled at once, a running one after its current tile;
 * - `INFO` → `info.json`: the app, the device, the GPU, the models, the limits.
 *
 * Every start keeps the 10-second foreground promise first; the notification «Задание агента · 42 %» (with «Отменить»)
 * appears only for a job long enough to outlive the platform's deferral. The runs go through the shared [UpscaleEngine],
 * so the screens and the agent never hold two nets. Step 1 (INFO, the door) was proved on the Titan
 * (testcases/reports/2026-10-16_epic06_A_step1_door_spike.md); the rest [NOT-TESTED].
 */
class AgentService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // Main-thread state: the waiting jobs, the running one and the cancel flags of both.
    private val queue = ArrayDeque<JobSpec>()
    private var running: JobSpec? = null
    private val cancels = HashMap<String, AtomicBoolean>()
    private var percent = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The 10-second promise of startForegroundService is kept first, before any work (researches/10 §1.4). The
        // default behaviour defers the notification by ~10 s, so a short start shows nothing on the screen.
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        scope.launch(Dispatchers.IO) { runCatching { sweep() } }
        when (intent?.action) {
            ACTION_INFO -> scope.launch {
                runCatching { writeInfo() }.onFailure { Log.w(TAG, "info failed", it) }
                stopIfIdle()
            }
            ACTION_UPSCALE -> submit(intent)
            ACTION_CANCEL -> cancel(intent.getStringExtra("job"))
            else -> stopIfIdle()
        }
        return START_NOT_STICKY
    }

    /** Validates the request, writes `queued` (or `failed`) at once, and starts the worker if it is idle. */
    private fun submit(intent: Intent) {
        val extras = intent.extras?.let { b -> b.keySet().associateWith { @Suppress("DEPRECATION") b.get(it) } } ?: emptyMap()
        when (val parsed = JobSpec.parse(extras)) {
            is JobSpec.Parsed.Bad -> {
                // Without a valid job id there is no status file to answer in: the CLI reads the logcat line instead.
                val job = extras["job"] as? String
                Log.w(TAG, "agent: bad request ${job ?: "(no job)"}: ${parsed.message}")
                if (job != null && Regex("[A-Za-z0-9_-]{8,64}").matches(job)) {
                    write(failedStatus(job, extras, ErrorCode.BAD_REQUEST, parsed.message))
                }
                stopIfIdle()
            }
            is JobSpec.Parsed.Ok -> {
                val spec = parsed.spec
                if (queue.size >= QUEUE_CAP) {
                    write(status(spec, JobState.Failed, error = JobStatus.Error(ErrorCode.BUSY, "the queue holds $QUEUE_CAP jobs; try later")))
                    stopIfIdle()
                    return
                }
                cancels[spec.job] = AtomicBoolean(false)
                queue.addLast(spec)
                write(status(spec, JobState.Queued))
                if (running == null) next()
            }
        }
    }

    private fun cancel(job: String?) {
        job ?: return stopIfIdle()
        cancels[job]?.set(true)
        val waiting = queue.firstOrNull { it.job == job }
        if (waiting != null) {
            queue.remove(waiting)
            cancels.remove(job)
            write(status(waiting, JobState.Canceled))
        }
        stopIfIdle()
    }

    /** Takes the next waiting job; stops the service when there is none. */
    private fun next() {
        val spec = queue.removeFirstOrNull() ?: return stopIfIdle()
        running = spec
        percent = 0
        scope.launch {
            runCatching { run(spec) }.onFailure { Log.w(TAG, "agent job ${spec.job} crashed", it) }
            cancels.remove(spec.job)
            running = null
            next()
        }
    }

    /** One job end to end: input → decode → the shared engine → the output file → `succeeded`; any failure → its code. */
    private suspend fun run(spec: JobSpec) {
        val dir = agentDir()
        val cancel = cancels[spec.job] ?: AtomicBoolean(false)
        val catalogue = ModelCatalog.load(this)
        val model = if (spec.model == JobSpec.DEFAULT_MODEL) catalogue.default else catalogue.byId(spec.model)
        val gpu = spec.gpu ?: prefs().getBoolean(UpscaleViewModel.PREF_GPU, true)
        val base = status(spec, JobState.Running, model = model?.id ?: spec.model, gpu = gpu)
        fun fail(code: String, message: String) = write(base.copy(state = JobState.Failed, error = JobStatus.Error(code, message)))

        if (model == null || !ModelStore.isAvailable(this, model)) return fail(ErrorCode.MODEL_NOT_INSTALLED, "model '${spec.model}' is not on the device; INFO lists the installed ones")
        val input = File(dir, spec.input)
        if (!input.isFile) return fail(ErrorCode.INPUT_MISSING, "no file '${spec.input}' in files/agent/")
        val bitmap = try {
            withContext(UpscaleEngine.dispatcher) { UpscaleEngine.decode(ImageDecoder.createSource(input)) }
        } catch (e: UpscaleEngine.TooBig) {
            return fail(ErrorCode.TOO_BIG, "the photo is over ${UpscaleViewModel.mpLimitText()} MP")
        } catch (e: Exception) {
            return fail(ErrorCode.DECODE_FAILED, "cannot decode '${spec.input}': ${e.message ?: e.javaClass.simpleName}")
        }
        val mp = bitmap.width.toFloat() * bitmap.height / 1_000_000f
        val rate = prefs().getFloat(UpscaleViewModel.rateKey(model, gpu), UpscaleViewModel.startRate(model, gpu))
        val estimate = (mp * UpscaleViewModel.passFactor(spec.scale) * rate).toLong()
        val t0 = SystemClock.elapsedRealtime()
        var last = 0L
        var current = base.copy(estimateMs = estimate, inW = bitmap.width, inH = bitmap.height)
        write(current)
        val out = try {
            UpscaleEngine.run(this, bitmap, model, spec.scale, gpu,
                onProgress = { done, total, usedGpu ->
                    val now = SystemClock.elapsedRealtime()
                    current = current.copy(gpu = usedGpu, tilesDone = done, tilesTotal = total, elapsedMs = now - t0,
                        progress = if (total > 0) done.toFloat() / total else 0f)
                    // At most every STATUS_EVERY_MS while running (the engine thread calls this once per tile).
                    if (now - last >= STATUS_EVERY_MS || done == total) { last = now; write(current); showProgress(current.progress) }
                },
                cancelled = { cancel.get() }, fromApi = true)
        } catch (e: UpscaleCancelledException) {
            write(current.copy(state = JobState.Canceled)); return
        } catch (e: UpscaleEngine.OutputTooBig) {
            return fail(ErrorCode.TOO_BIG, "the result would be over ${UpscaleViewModel.MAX_OUTPUT_MP.toInt()} MP; take a smaller scale")
        } catch (e: UpscaleEngine.ModelLoadFailed) {
            return fail(ErrorCode.MODEL_LOAD_FAILED, "model '${model.id}' did not load")
        } catch (e: Exception) {
            return fail(ErrorCode.ENGINE_FAILED, "the network failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            bitmap.recycle()
        }
        val format = ResultStore.Format.entries.first { it.ext == spec.format }
        val file = File(dir, JobFiles.output(spec.job, spec.format))
        val (outW, outH) = out.bitmap.width to out.bitmap.height
        val bytes = try {
            withContext(Dispatchers.IO) { ResultStore.writeFile(out.bitmap, file, format, spec.quality, cancel) }
        } catch (e: ResultStore.RenderCancelled) {
            write(current.copy(state = JobState.Canceled)); return
        } catch (e: IllegalArgumentException) {
            return fail(ErrorCode.BAD_REQUEST, e.message ?: "format")
        } catch (e: Exception) {
            return fail(ErrorCode.INTERNAL, "writing the result failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            out.bitmap.recycle()
        }
        dir.listFiles()?.filter { JobFiles.isInputOf(it.name, spec.job) }?.forEach { it.delete() }
        write(current.copy(state = JobState.Succeeded, gpu = out.gpuUsed, progress = 1f, elapsedMs = SystemClock.elapsedRealtime() - t0,
            out = JobStatus.Out(file.name, outW, outH, bytes)))
    }

    /** The daily limit of mediaProcessing is reached: the running job fails with `quota`, the waiting ones too. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        running?.let { cancels[it.job]?.set(true); write(status(it, JobState.Failed, error = JobStatus.Error(ErrorCode.QUOTA, "Android's 6 h per day for media processing is used up"))) }
        queue.forEach { write(status(it, JobState.Failed, error = JobStatus.Error(ErrorCode.QUOTA, "Android's 6 h per day for media processing is used up"))) }
        queue.clear()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun stopIfIdle() {
        if (running == null && queue.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun status(spec: JobSpec, state: JobState, model: String = spec.model, gpu: Boolean = spec.gpu ?: true, error: JobStatus.Error? = null) =
        JobStatus(spec.job, state, model, spec.scale, spec.format, gpu, error = error)

    /** A `failed`/`bad_request` status for extras that did not parse - whatever of them is readable. */
    private fun failedStatus(job: String, extras: Map<String, Any?>, code: String, message: String) = JobStatus(
        job, JobState.Failed, extras["model"] as? String ?: JobSpec.DEFAULT_MODEL, extras["scale"] as? Int ?: 0,
        extras["format"] as? String ?: "", extras["gpu"] as? Boolean ?: true, error = JobStatus.Error(code, message),
    )

    /** Writes `<job>.json` atomically (`.tmp`, then a rename), so a reader never sees half a file. */
    private fun write(s: JobStatus) = writeAtomic(File(agentDir(), JobFiles.status(s.job)), s.toJson())

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }

    /** `info.json`: the app, the device, the GPU, the models with what is installed, the limits (plans/07 protocol). */
    private suspend fun writeInfo() {
        val catalogue = ModelCatalog.load(this)
        val gpu = withContext(UpscaleEngine.dispatcher) { NcnnUpscaler.gpuInfo() }
        val battery = getSystemService(BatteryManager::class.java)
        val models = catalogue.models.joinToString(",") { m ->
            "{\"id\":${q(m.id)},\"name\":${q(m.name)},\"scale\":${UpscaleViewModel.ENGINE_SCALE},\"installed\":${ModelStore.isAvailable(this, m)}," +
                "\"default\":${m.isDefault},\"licence\":${q(m.licenceLabel)},\"commercial\":${m.commercial}}"
        }
        val json = buildString {
            append("{\"v\":1")
            append(",\"app\":{\"package\":").append(q(packageName))
            append(",\"version\":").append(q(BuildConfig.VERSION_NAME))
            append(",\"build\":").append(q(BuildConfig.BUILD_TYPE)).append('}')
            append(",\"device\":{\"manufacturer\":").append(q(Build.MANUFACTURER))
            append(",\"model\":").append(q(Build.MODEL))
            append(",\"sdk\":").append(Build.VERSION.SDK_INT).append('}')
            append(",\"gpu\":").append(q(gpu))
            append(",\"gpu_default\":").append(prefs().getBoolean(UpscaleViewModel.PREF_GPU, true))
            append(",\"charging\":").append(battery?.isCharging ?: false)
            append(",\"queue\":").append(queue.size + if (running != null) 1 else 0)
            append(",\"models\":[").append(models).append(']')
            append(",\"limits\":{\"max_input_mp\":").append(UpscaleViewModel.mpLimitText())
            append(",\"max_output_mp\":").append(UpscaleViewModel.MAX_OUTPUT_MP.toInt())
            append(",\"scales\":[").append(JobSpec.SCALES.joinToString(",")).append(']')
            append(",\"formats\":[").append(JobSpec.FORMATS.joinToString(",") { q(it) }).append("]}")
            append('}')
        }
        withContext(Dispatchers.IO) { writeAtomic(File(agentDir(), INFO_FILE), json) }
    }

    /**
     * R1 - the agent's files do not pile up on the device: a job whose status file the APP last wrote over [KEEP_MS] ago
     * leaves with all its files. Keyed on the status file, never on an input's mtime: `adb push` keeps the PC file's date
     * (step 1 found a fresh input dated two days back).
     */
    private fun sweep() {
        val dir = agentDir()
        val now = System.currentTimeMillis()
        val old = dir.listFiles { f -> f.name.endsWith(".json") && f.name != INFO_FILE && now - f.lastModified() > KEEP_MS }.orEmpty()
        for (status in old) {
            val job = status.name.removeSuffix(".json")
            if (job == running?.job || queue.any { it.job == job }) continue
            dir.listFiles { f -> f.name.startsWith("$job.") }?.forEach { it.delete() }
        }
    }

    private fun prefs() = getSharedPreferences(UpscaleEngine.PREFS, Context.MODE_PRIVATE)

    private fun agentDir(): File = (getExternalFilesDir(DIR) ?: File(filesDir, DIR)).apply { mkdirs() }

    private fun showProgress(p: Float) {
        val pct = (p * 100).toInt()
        if (pct == percent) return
        percent = pct
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
    }

    /** «Задание агента · 42 %» with «Отменить» (cancels the running job); reuses the processing channel. */
    private fun notification(): Notification {
        UpscaleService.ensureChannel(this)
        val b = Notification.Builder(this, UpscaleService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_heart_mono)
            .setContentTitle(getString(R.string.agent_job_progress, percent))
            .setProgress(PERCENT, percent, running == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        running?.let { job ->
            val cancel = PendingIntent.getService(this, 0, Intent(this, AgentService::class.java).setAction(ACTION_CANCEL).putExtra("job", job.job),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(Notification.Action.Builder(null, getString(R.string.cancel), cancel).build())
        }
        return b.build()
    }

    companion object {
        const val ACTION_INFO = "com.kriniks.kartun.agent.INFO"
        const val ACTION_UPSCALE = "com.kriniks.kartun.agent.UPSCALE"
        const val ACTION_CANCEL = "com.kriniks.kartun.agent.CANCEL"
        private const val TAG = "KARTUN"
        private const val NOTIFICATION_ID = 3
        private const val DIR = "agent"
        private const val INFO_FILE = "info.json"
        /** `[AI]` jobs waiting at most; above it a new one fails with `busy` (plans/07 step 4). */
        private const val QUEUE_CAP = 8
        /** The status file is rewritten at most this often while a job runs (plans/07 protocol: «at most every 500 ms»). */
        private const val STATUS_EVERY_MS = 500L
        /** A finished job's files stay this long (R1: 24 h). */
        private const val KEEP_MS = 24 * 60 * 60 * 1000L
        private const val PERCENT = 100
    }
}
