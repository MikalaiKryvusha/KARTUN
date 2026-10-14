package com.kriniks.kartun.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R
import com.kriniks.kartun.engine.NcnnUpscaler

/** The engine version the app is built against (tools/fetch-ncnn.mjs pins it; docs/THIRD_PARTY.md). */
private const val NCNN_VERSION = "ncnn 20260526"

/**
 * «О приложении» (plans/01 §2.14, MASTER_PLAN phase 4: «экран „О приложении“ с лицензиями и диагностикой устройства»;
 * GOAL.md §8 «Диагностика»): the version, the device, Android, the Vulkan GPU line of the engine, the engine version,
 * and the licences of everything KARTUN ships (docs/THIRD_PARTY.md, docs/model-licenses.md). The diagnostics help to
 * read a failure on someone else's phone. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    val version = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.orEmpty() }
    val gpu = remember { NcnnUpscaler.gpuInfo() }

    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_back, onClick = onBack)
            Text(stringResource(R.string.about_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.size(44.dp))
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 40.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                buildAnnotatedString { withStyle(SpanStyle(color = c.primary)) { append("K") }; append("ARTUN") },
                style = MaterialTheme.typography.displaySmall,
            )
            Text(stringResource(R.string.about_tagline, version), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Group(stringResource(R.string.about_device), listOf(
                stringResource(R.string.about_model) to "${Build.MANUFACTURER} ${Build.MODEL}",
                "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                "GPU" to gpu,
                stringResource(R.string.about_engine) to NCNN_VERSION,
            ))
            Group(stringResource(R.string.about_licences), listOf(
                "KARTUN" to "MIT",
                "ncnn (Tencent)" to "BSD 3-Clause",
                "Real-ESRGAN (Xintao Wang)" to "BSD 3-Clause",
                "Unbounded, Manrope" to "SIL Open Font License 1.1",
                "AndroidX, Jetpack Compose" to "Apache 2.0",
            ))
        }
    }
}

/** A titled card of label · value rows. */
@Composable
private fun Group(title: String, rows: List<Pair<String, String>>) {
    val c = MaterialTheme.colorScheme
    Text(title.uppercase(), Modifier.padding(start = 4.dp, top = 10.dp), style = MaterialTheme.typography.labelSmall, color = c.primary)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainer)) {
        rows.forEachIndexed { i, (k, v) ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(k, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                Text(v, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
            }
            if (i < rows.lastIndex) HorizontalDivider(color = c.outline)
        }
    }
}
