package com.kriniks.kartun.ui

import android.content.Intent
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
import androidx.core.net.toUri
import com.kriniks.kartun.R
import androidx.compose.ui.platform.LocalConfiguration
import com.kriniks.kartun.ModelCatalog
import com.kriniks.kartun.UpscaleViewModel
import com.kriniks.kartun.UpscaleEngine
import com.kriniks.kartun.engine.NcnnUpscaler

/** The engine the app is built against: OUR ncnn fork at its tag (tools/build-ncnn.mjs pins it; docs/THIRD_PARTY.md). */
private const val NCNN_FORK = "MikalaiKryvusha/ncnn"
private const val NCNN_VERSION = "$NCNN_FORK 20260526-kartun.1"
private const val NCNN_FORK_URL = "https://github.com/MikalaiKryvusha/ncnn/tree/20260526-kartun.1"

/** The 4× result has 16 megapixels per megapixel of input; the learned rates count input megapixels. */
private const val OUTPUT_PER_INPUT_MP = 16f
private const val MS_PER_S = 1000f

/** «0,27 Мп/с» - two significant digits in the device's language; «—» when nothing was measured. */
@Composable
private fun speedText(mps: Float?): String {
    if (mps == null) return "—"
    val locale = LocalConfiguration.current.locales[0]
    val digits = if (mps >= 10f) 0 else if (mps >= 1f) 1 else if (mps >= 0.1f) 2 else 3
    return stringResource(R.string.about_speed_value, ltr(String.format(locale, "%.${digits}f", mps)))
}

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
    // Donations - the address the owner's NDim uses (src/routes/[lang=lang]/menu/donate/+page.svelte; the owner, chat
    // 2026-10-14 ≈ 22:03 +03:00: «найдешь в моем проекте ндим»).
    "DonationAlerts — mikalai_kryvusha" to "https://donationalerts.com/r/mikalai_kryvusha",
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
    // This device's measured speed of the default model, megapixels of the 4× result per second, GPU and CPU: the first-launch
    // check wrote it and every real run refines it (the owner, 2026-10-16 ≈ 22:1x: «в сводку об устройстве ещё пункт добавить
    // условные замеренные бенчем метрики производительности GPU и CPU в мегапикселях в секунду»). Null - nothing measured yet
    // (the check was cancelled and nothing ran). [NOT-TESTED]
    val speed = remember {
        val prefs = ctx.getSharedPreferences(UpscaleEngine.PREFS, android.content.Context.MODE_PRIVATE)
        val m = ModelCatalog.load(ctx).default
        fun mps(gpu: Boolean): Float? = prefs.getFloat(UpscaleViewModel.rateKey(m, gpu), 0f).takeIf { it > 0f }
            ?.let { OUTPUT_PER_INPUT_MP * MS_PER_S / it }
        mps(true) to mps(false)
    }

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
            Text(stringResource(R.string.about_tagline, ltr(version)), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Group(stringResource(R.string.about_device), listOf(
                stringResource(R.string.about_model) to ltr("${Build.MANUFACTURER} ${Build.MODEL}"),
                "Android" to ltr("${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
                "GPU" to ltr(gpuName(gpu)),
                stringResource(R.string.about_engine) to ltr(NCNN_VERSION),
                stringResource(R.string.about_speed_gpu) to speedText(speed.first),
                stringResource(R.string.about_speed_cpu) to speedText(speed.second),
            ))
            LinkGroup(stringResource(R.string.about_project), listOf(stringResource(R.string.about_source) to REPO_URL))
            // The engine first - our fork (the owner, 2026-10-16 ≈ 19:1x: «где движок, пишем MikalaiKryvusha/ncnn - кастомный
            // модифицированный и улучшенный форк»); the original ncnn (Tencent) stays in the list as its source.
            LinkGroup(stringResource(R.string.about_inspired), listOf("$NCNN_FORK — ${stringResource(R.string.about_engine_fork)}" to NCNN_FORK_URL) + INSPIRED)
            LinkGroup(stringResource(R.string.about_author), AUTHOR_LINKS, header = AUTHOR)
            Group(stringResource(R.string.about_licences), listOf(
                "KARTUN" to "MIT",
                // BSD 3-Clause asks the binary to carry the original copyright: our fork keeps Tencent's licence unchanged.
                "$NCNN_FORK · ncnn (Tencent)" to "BSD 3-Clause",
                // The two models inside the APK (docs/model-licenses.md); CC BY 4.0 asks for the author's credit here.
                "4xNomosWebPhoto RealPLKSR (Philip Hofmann)" to "CC BY 4.0",
                "4x NMKD Siax (NMKD)" to "WTFPL",
                "Real-ESRGAN (Xintao Wang)" to "BSD 3-Clause",
                // The second decoder (plans/08 step 3): texts in docs/licenses/ (libtiff, stb_image; QOI's MIT is in qoi.h).
                "libtiff (Sam Leffler, Silicon Graphics)" to "libtiff licence",
                "stb_image (Sean Barrett)" to "Public domain",
                "QOI (Dominic Szablewski)" to "MIT",
                "Unbounded, Manrope" to "SIL Open Font License 1.1",
                "AndroidX, Jetpack Compose" to "Apache 2.0",
            ))
            // The photos the model previews start from, with their authors and licences (docs/model-previews/credits.json).
            val credits = remember { previewCredits(ctx) }
            if (credits.isNotEmpty()) Group(stringResource(R.string.about_previews), credits.map { line ->
                val parts = line.split(" — ")
                parts.take(2).joinToString(" — ") to parts.drop(2).joinToString(" · ")
            })
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
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
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

/** The credit lines of the preview photos: «title — author — licence — source». */
private fun previewCredits(ctx: android.content.Context): List<String> = runCatching {
    val arr = org.json.JSONArray(ctx.assets.open("previews/credits.json").bufferedReader().use { it.readText() })
    (0 until arr.length()).map { arr.getString(it) }
}.getOrDefault(emptyList())

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
