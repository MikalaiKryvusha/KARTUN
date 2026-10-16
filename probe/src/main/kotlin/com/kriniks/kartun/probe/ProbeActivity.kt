package com.kriniks.kartun.probe

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Criterion A7 (plans/06 §2, plans/07 step 9): another app on the device tries to start KARTUN's agent door. Started from
 * the adb shell with `am start -n com.kriniks.kartun.probe/.ProbeActivity [--es target <package>]`; being in the
 * foreground, it may start a foreground service at all, so the only thing that can refuse it is the door's permission.
 * One logcat line under the tag `A7PROBE`: `refused: <exception>` or `STARTED` (the door is open - a defect). [NOT-TESTED]
 */
class ProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra("target") ?: "com.kriniks.kartun.debug"
        val door = Intent("com.kriniks.kartun.agent.INFO").setComponent(ComponentName(target, "com.kriniks.kartun.agent.AgentService"))
        val line = try {
            val started = startForegroundService(door)
            if (started == null) "refused: not found (null component)" else "STARTED $started"
        } catch (e: Exception) {
            "refused: ${e.javaClass.name}: ${e.message}"
        }
        Log.w(TAG, line)
        finish()
    }

    private companion object { const val TAG = "A7PROBE" }
}
