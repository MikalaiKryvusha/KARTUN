package com.kriniks.kartun

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable

/**
 * Stage and release builds: no «Отладка» section and no engine probe (the real one is src/debug/.../DebugTools.kt).
 * Same API, every call does nothing. [NOT-TESTED]
 */
object DebugTools {
    /** Starts the adb probe when the intent asks for one - never in these builds. */
    fun onIntent(context: Context, intent: Intent) = Unit

    /** Draws the probe screen over the app when it is open; here it never is. */
    @Composable
    fun Overlay(): Boolean = false

    /** The «Отладка» group at the end of Settings - absent here. */
    @Composable
    fun SettingsSection() = Unit
}
