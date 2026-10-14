package com.kriniks.kartun.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kriniks.kartun.R

/*
 * The notification permission (Android 13+), by the owner's answer to interview #004 (2026-10-14 20:13): Q1 = A - asked
 * once, at the first «Улучшить»; and his comment - a «Уведомления» row in the settings with a tick when granted, a tap
 * that asks when it may still ask, and otherwise a window explaining the earlier refusal with a button to the system
 * settings of KARTUN. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */

private const val PREFS = "estimates"
private const val PREF_ASKED = "notif_asked"

private fun granted(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun asked(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_ASKED, false)

private fun markAsked(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(PREF_ASKED, true) }

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/**
 * Wraps «Улучшить»: the first tap ever asks for the permission and starts the run after any answer; every later tap
 * just starts it (Q1 = A: one question, at the moment the notification starts to matter).
 */
@Composable
fun rememberStartWithNotificationAsk(start: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }
    return {
        if (granted(ctx) || asked(ctx)) start()
        else { markAsked(ctx); launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}

/** The settings row «Уведомления»: its state follows the system (re-read on every resume), its tap asks or explains. */
@Composable
fun NotificationsRow(onInfo: () -> Unit) {
    val ctx = LocalContext.current
    var isGranted by remember { mutableStateOf(granted(ctx)) }
    var explain by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) isGranted = granted(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted = it }
    val onTap = {
        when {
            isGranted -> Unit
            // The system still lets the app ask: never asked yet, or refused once («rationale» is shown then).
            !asked(ctx) || ctx.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true -> {
                markAsked(ctx); launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> explain = true
        }
    }
    SettingRow(
        R.drawable.ic_bell, stringResource(R.string.settings_notifications),
        stringResource(if (isGranted) R.string.settings_notifications_on else R.string.settings_notifications_off),
        onInfo = onInfo, onClick = onTap,
    ) { StateMark(isGranted) }
    if (explain) {
        // The owner's words for this window: the permission was refused earlier, the app can no longer ask by itself,
        // it is given by hand in the OS settings of the app - and a button that opens those settings.
        InfoDialog(
            stringResource(R.string.notif_denied_title), stringResource(R.string.notif_denied_text), "",
            action = stringResource(R.string.notif_open_settings) to {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                explain = false
            },
            onClose = { explain = false },
        )
    }
}

/** The right side of the row: a pink tick when granted («справа галочка»), a chevron when the tap will ask. */
@Composable
private fun StateMark(on: Boolean) {
    val c = androidx.compose.material3.MaterialTheme.colorScheme
    if (on) androidx.compose.foundation.layout.Box(
        Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.primary),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { androidx.compose.material3.Icon(painterResource(R.drawable.ic_tick), null, Modifier.size(15.dp), tint = c.onPrimary) }
    else androidx.compose.material3.Text("›", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
}
