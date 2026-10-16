package com.kriniks.kartun.agent

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.kriniks.kartun.BuildConfig
import com.kriniks.kartun.EngineThread
import com.kriniks.kartun.R
import com.kriniks.kartun.UpscaleService
import com.kriniks.kartun.engine.NcnnUpscaler
import java.io.File

/**
 * The ADB door for AI agents (plans/06 §4.2, plans/07 step 1 - the device spike): an exported service guarded by the
 * `DUMP` permission, which only the adb shell holds, so an app on the device cannot call it (criterion A7). An agent
 * on the PC starts it with `adb shell am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService -a
 * com.kriniks.kartun.agent.INFO`; the service answers through files in `files/agent/` that adb can read back.
 *
 * Step 1 builds only the skeleton: `INFO` writes `info.json` (app, device, GPU). UPSCALE, CANCEL and the job queue come
 * in step 4. [TESTED: 2026-10-16 · Titan: INFO answered after install without opening the app, after force-stop with
 * the screen asleep, no notification shown; the app read a file the shell pushed - testcases/reports/2026-10-16_epic06_A_step1_door_spike.md]
 */
class AgentService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The 10-second promise of startForegroundService is kept first, before any work (researches/10 §1.4). The
        // default behaviour defers the notification by ~10 s, so a short INFO start shows nothing on the screen.
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        when (intent?.action) {
            // gpuInfo touches ncnn's global Vulkan state: it runs on the one engine thread (architecture invariant 4).
            ACTION_INFO -> EngineThread.executor.execute {
                runCatching { writeInfo() }
                stopSelfResult(startId)
            }
            else -> stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    /** Writes `info.json` atomically: `info.json.tmp`, then a rename, so a reader never sees half a file. */
    private fun writeInfo() {
        val dir = agentDir()
        val json = buildString {
            append("{\"v\":1")
            append(",\"app\":{\"package\":").append(q(packageName))
            append(",\"version\":").append(q(BuildConfig.VERSION_NAME))
            append(",\"build\":").append(q(BuildConfig.BUILD_TYPE)).append('}')
            append(",\"device\":{\"manufacturer\":").append(q(Build.MANUFACTURER))
            append(",\"model\":").append(q(Build.MODEL))
            append(",\"sdk\":").append(Build.VERSION.SDK_INT).append('}')
            append(",\"gpu\":").append(q(NcnnUpscaler.gpuInfo()))
            append('}')
        }
        val tmp = File(dir, "$INFO_FILE.tmp")
        tmp.writeText(json)
        tmp.renameTo(File(dir, INFO_FILE))
    }

    private fun agentDir(): File = (getExternalFilesDir(DIR) ?: File(filesDir, DIR)).apply { mkdirs() }

    /** The foreground notification the platform demands; reuses the processing channel and its strings (plans/07 step 6 adds the agent's own). */
    private fun notification(): Notification {
        UpscaleService.ensureChannel(this)
        return Notification.Builder(this, UpscaleService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_heart_mono)
            .setContentTitle(getString(R.string.working_title))
            .setContentText(getString(R.string.notif_starting))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_INFO = "com.kriniks.kartun.agent.INFO"
        private const val NOTIFICATION_ID = 3
        private const val DIR = "agent"
        private const val INFO_FILE = "info.json"
    }
}
