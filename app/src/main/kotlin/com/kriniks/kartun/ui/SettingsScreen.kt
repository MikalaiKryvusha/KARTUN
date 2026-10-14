package com.kriniks.kartun.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R
import com.kriniks.kartun.UpscaleViewModel
import com.kriniks.kartun.engine.NcnnUpscaler

/**
 * Settings (mockup 'settings'), only the rows that work today: «Ускорение GPU» (off = the CPU path, for a GPU that
 * misbehaves) with the device's GPU name, and «Язык» opening the system per-app language list (res/xml/locales_config.xml).
 * Each row carries its (i). The manual, the model catalogue and the agent modes join as they are built - no dead rows.
 * [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
@Composable
fun SettingsScreen(vm: UpscaleViewModel, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var info by remember { mutableStateOf<Int?>(null) }
    BackHandler(onBack = onBack)
    // "gpu_count=1 name=Mali-G615 MC6 ..." -> "Vulkan · Mali-G615 MC6"; no GPU -> the CPU line.
    val gpuName = remember { NcnnUpscaler.gpuInfo().substringAfter("name=", "").substringBefore(" fp16").trim() }

    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_back, onClick = onBack)
            Text(stringResource(R.string.settings_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            // «О приложении» - the (i)-in-a-circle icon on the right of the mockup's settings bar.
            RoundIconButton(R.drawable.ic_about, onClick = vm::openAbout)
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // The guide first, as on the mockup's pink hero row (.manual-hero).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.primaryContainer).clickable(onClick = vm::openManual)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(c.primary), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_book), null, Modifier.size(26.dp), tint = c.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_manual), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_manual_sub), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Text("›", style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
            }
            Text(stringResource(R.string.settings_processing).uppercase(), Modifier.padding(start = 4.dp, top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = c.primary)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainer)) {
                SettingRow(
                    R.drawable.ic_cpu, stringResource(R.string.settings_gpu),
                    if (gpuName.isNotEmpty()) stringResource(R.string.settings_gpu_sub, gpuName) else stringResource(R.string.backend_cpu),
                    onInfo = { info = R.string.settings_gpu },
                    onClick = { vm.chooseGpu(!vm.useGpu) },
                ) {
                    Switch(
                        checked = vm.useGpu, onCheckedChange = { vm.chooseGpu(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, uncheckedTrackColor = c.surfaceContainerHigh),
                    )
                }
            }
            Text(stringResource(R.string.settings_app).uppercase(), Modifier.padding(start = 4.dp, top = 8.dp),
                style = MaterialTheme.typography.labelSmall, color = c.primary)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainer)) {
                SettingRow(
                    R.drawable.ic_globe, stringResource(R.string.settings_language), stringResource(R.string.settings_language_sub),
                    onInfo = { info = R.string.settings_language },
                    onClick = {
                        // Android 13+: the system list of this app's languages (the ones locales_config.xml names).
                        ctx.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                    },
                ) { Text("›", style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant) }
            }
        }
    }
    when (info) {
        R.string.settings_gpu -> InfoDialog(stringResource(R.string.settings_gpu), stringResource(R.string.info_gpu_text), stringResource(R.string.info_gpu_list)) { info = null }
        R.string.settings_language -> InfoDialog(stringResource(R.string.settings_language), stringResource(R.string.info_language_text), "") { info = null }
    }
}

/** One row of a settings group (.item): pink icon tile, title and value, the control, the (i). */
@Composable
private fun SettingRow(icon: Int, title: String, sub: String, onInfo: () -> Unit, onClick: () -> Unit, control: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(19.dp), tint = c.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        control()
        InfoButton(onInfo)
    }
    HorizontalDivider(color = c.outline)
}
