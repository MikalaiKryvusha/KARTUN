package com.kriniks.kartun.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
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

/** Our repository; the inspiration and the engine (the owner, plan 03 step 21: «ссылаться на вдохновителей - репозиторий
 *  Upscayl, и репозиторий движка, и моделей»); the author's links as KrinikCam shows them (its AuthorInfoDialog, HEAD d33c055). */
private const val REPO_URL = "https://github.com/MikalaiKryvusha/KARTUN"
private val INSPIRED = listOf(
    "Upscayl" to "https://github.com/upscayl/upscayl",
    "ncnn (Tencent)" to "https://github.com/Tencent/ncnn",
    "Real-ESRGAN (Xintao Wang)" to "https://github.com/xinntao/Real-ESRGAN",
)
private const val AUTHOR = "Mikalai Kryvusha · aka KOT KRINIK"
private val AUTHOR_LINKS = listOf(
    "GitHub — Mikalai Kryvusha" to "https://github.com/MikalaiKryvusha",
    "Telegram — @kotkrinik" to "https://t.me/kotkrinik",
    "YouTube — @kotkrinik" to "https://youtube.com/@kotkrinik",
    "Instagram — @kotkrinik" to "https://instagram.com/kotkrinik",
)

/** The GPU's name out of the engine's diagnostic line («gpu_count=1 name=Mali-G615 MC6 fp16_storage=1 …»); the line itself if it has none. */
private fun gpuName(info: String): String = Regex("""name=(.+?)(?: \w+=|$)""").find(info)?.groupValues?.get(1) ?: info

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
                "GPU" to gpuName(gpu),
                stringResource(R.string.about_engine) to NCNN_VERSION,
            ))
            LinkGroup(stringResource(R.string.about_project), listOf(stringResource(R.string.about_source) to REPO_URL))
            LinkGroup(stringResource(R.string.about_inspired), INSPIRED)
            LinkGroup(stringResource(R.string.about_author), AUTHOR_LINKS, header = AUTHOR)
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

/** A titled card of links: each row opens its page in the browser; [header] is an optional first, plain line. */
@Composable
private fun LinkGroup(title: String, links: List<Pair<String, String>>, header: String? = null) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    Text(title.uppercase(), Modifier.padding(start = 4.dp, top = 10.dp), style = MaterialTheme.typography.labelSmall, color = c.primary)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainer)) {
        header?.let {
            Text(it, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
            HorizontalDivider(color = c.outline)
        }
        links.forEachIndexed { i, (label, url) ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_link), null, Modifier.size(20.dp), tint = c.primary)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
            }
            if (i < links.lastIndex) HorizontalDivider(color = c.outline)
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
