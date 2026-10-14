package com.kriniks.kartun

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The run's state shared by the ViewModel (which owns the engine job) and [UpscaleService] (which keeps the process alive
 * and shows the notification). One process-wide object, no binding: the service only READS the progress and can ask
 * for a cancel. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
object ProcessingState {
    /** Progress of the running job, or null when nothing runs (the service stops on null). */
    val progress = MutableStateFlow<UpscaleViewModel.Progress?>(null)
    /** Set by «Отменить» in the app or in the notification; the engine's tile listener reads it between tiles. */
    @Volatile var cancelRequested = false
}

/**
 * Foreground service of type `mediaProcessing` (plans/01 §4.4, plans/03 slice 2): while the network works, the app can be
 * left - the process stays alive and the notification shows the percent, the time left and «Отменить». It owns no work:
 * it mirrors [ProcessingState.progress] and stops itself when the run ends. Android 15+ limits this type to 6 h a day;
 * [onTimeout] cancels the run then. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
class UpscaleService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            ProcessingState.cancelRequested = true
            return START_NOT_STICKY
        }
        ensureChannel()
        startForeground(NOTIFICATION_ID, build(ProcessingState.progress.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        scope.launch {
            ProcessingState.progress.collect { p ->
                if (p == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, build(p))
                }
            }
        }
        return START_NOT_STICKY
    }

    /** The daily limit of mediaProcessing is reached: stop the run between tiles and leave. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        ProcessingState.cancelRequested = true
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            // Low importance: a silent progress notification, no sound or heads-up for a run the user started.
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun build(p: UpscaleViewModel.Progress?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this, 1, Intent(this, UpscaleService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        val total = p?.total ?: 0
        val done = p?.done ?: 0
        val text = if (total > 0) getString(R.string.notif_progress, done * 100 / total) else getString(R.string.notif_starting)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_heart_mono)
            .setContentTitle(getString(R.string.working_title))
            .setContentText(text)
            .setProgress(total.coerceAtLeast(1), done, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.cancel), cancel).build())
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "processing"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_CANCEL = "com.kriniks.kartun.CANCEL"

        fun start(context: Context) = context.startForegroundService(Intent(context, UpscaleService::class.java))
    }
}
