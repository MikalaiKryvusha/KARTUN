package com.kriniks.kartun

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.util.Log
import com.kriniks.kartun.engine.NcnnUpscaler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The local crash journal (the owner, chat 2026-10-15 ≈ 20:58 +03:00: «можно поставить какой-то креш логер, который бы
 * записывал логи крешей, и чтобы они хранились в директории приложения?»; bugs/01 - a native abort whose reason was lost).
 * Everything stays on the device, in `Android/data/<package>/files/crashes/` (adb pull reaches it without root):
 * - on every start, the system's record of how earlier processes ended ([ApplicationExitInfo]): a native crash brings its
 *   tombstone (protobuf, `<moment>_native.pb` beside the `.txt` summary), an ANR its thread dump, a Kotlin crash its line;
 * - a Kotlin crash of this process writes its full stack trace before the system default handler runs;
 * - the native stderr (libomp and ncnn print their fatal errors there) is appended to `engine-stderr.log`, together with
 *   [note] lines written before each model load.
 * Nothing is ever sent anywhere.
 * [TESTED: 2026-10-15 · Titan: an app killed by SIGSEGV left `<moment>_native.txt` + a 296 KB tombstone `.pb` (signal,
 * build, thread stacks readable) on the next start; the engine's own witness line and the load note landed in
 * `engine-stderr.log`. NOT tested: the Kotlin crash handler; a real libomp abort message.]
 */
object CrashLog {
    private const val TAG = "KARTUN_CRASH"
    private const val PREFS = "crash_log"
    private const val PREF_LAST_EXIT = "last_exit_ms"
    /** How many exit records the system is asked for on one start. */
    private const val MAX_EXITS = 16
    /** Older crash files than the newest ones of this count are deleted on start. */
    private const val KEEP_FILES = 40
    /** The stderr log starts over when it grows past this size. */
    private const val MAX_STDERR_BYTES = 1L shl 20

    private var dir: File? = null
    @Volatile private var stderrRedirected = false

    /** Called once from [KartunApp.onCreate]. */
    fun install(app: Application) {
        val d = app.getExternalFilesDir("crashes") ?: return
        dir = d
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { File(d, "${stamp(System.currentTimeMillis())}_kotlin.txt").writeText("thread: ${thread.name}\n${e.stackTraceToString()}") }
            previous?.uncaughtException(thread, e)
        }
        runCatching { collectExits(app, d) }.onFailure { Log.w(TAG, "exit records not read", it) }
        runCatching { trim(d) }
    }

    /** Before the first engine call of the process: native stderr to the journal (the library is loaded by then anyway). */
    fun beforeEngine() {
        val d = dir ?: return
        if (stderrRedirected) return
        stderrRedirected = true
        val f = File(d, "engine-stderr.log")
        if (f.length() > MAX_STDERR_BYTES) f.delete()
        val rc = NcnnUpscaler.redirectStderr(f.path)
        if (rc != 0) Log.w(TAG, "stderr not redirected, errno $rc")
    }

    /** One line into `engine-stderr.log` (and logcat): the state around an engine call, read after a crash. */
    fun note(line: String) {
        Log.i(TAG, line)
        val d = dir ?: return
        runCatching { File(d, "engine-stderr.log").appendText("${stamp(System.currentTimeMillis())} $line\n") }
    }

    /** The CPU set of the calling thread: the cpuset group and the allowed CPUs (bugs/01 - Android moves a background app
     *  to cores 0-3 and back). */
    fun cpuState(): String {
        val cpuset = runCatching { File("/proc/self/cpuset").readText().trim() }.getOrDefault("?")
        val cpus = runCatching {
            File("/proc/thread-self/status").readLines().firstOrNull { it.startsWith("Cpus_allowed_list:") }?.substringAfter(':')?.trim()
        }.getOrNull() ?: "?"
        return "thread=${Thread.currentThread().name} cpuset=$cpuset cpus=$cpus"
    }

    private fun collectExits(context: Context, d: File) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(PREF_LAST_EXIT, 0L)
        val am = context.getSystemService(ActivityManager::class.java)
        val exits = am.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
            .filter { it.timestamp > last && it.reason in KEPT_REASONS }
        for (e in exits) {
            val name = "${stamp(e.timestamp)}_${KEPT_REASONS[e.reason]}"
            File(d, "$name.txt").writeText(
                "reason: ${KEPT_REASONS[e.reason]}\nmoment: ${Date(e.timestamp)}\npid: ${e.pid}\nprocess: ${e.processName}\n" +
                    "importance: ${e.importance}\nstatus: ${e.status}\ndescription: ${e.description}\n"
            )
            runCatching {
                e.traceInputStream?.use { input ->
                    val ext = if (e.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) "pb" else "trace.txt"
                    File(d, "$name.$ext").outputStream().use { input.copyTo(it) }
                }
            }
        }
        exits.maxOfOrNull { it.timestamp }?.let { prefs.edit().putLong(PREF_LAST_EXIT, it).apply() }
    }

    private fun trim(d: File) {
        d.listFiles { f -> f.name != "engine-stderr.log" }?.sortedByDescending { it.name }?.drop(KEEP_FILES)?.forEach { it.delete() }
    }

    private fun stamp(ms: Long) = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date(ms))

    /** The exit reasons worth a file, with their file-name word. */
    private val KEPT_REASONS = mapOf(
        ApplicationExitInfo.REASON_CRASH to "kotlin",
        ApplicationExitInfo.REASON_CRASH_NATIVE to "native",
        ApplicationExitInfo.REASON_ANR to "anr",
    )
}

/** The application object: starts the crash journal before anything else of KARTUN runs. */
class KartunApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
