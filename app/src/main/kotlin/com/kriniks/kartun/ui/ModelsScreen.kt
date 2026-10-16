package com.kriniks.kartun.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriniks.kartun.Model
import com.kriniks.kartun.R
import com.kriniks.kartun.ui.theme.NonCommercialViolet
import com.kriniks.kartun.UpscaleViewModel
import java.util.Locale

/**
 * «Все модели» (plans/05 step 5; mockup 'models' of design/kartun-mockups.html, accepted in interview #002; the cards as
 * wide as Upscayl's after the owner's «у апскейли они красивые»): the filter chips «Все · Фото · Аниме · Для коммерческого
 * использования» and one card per catalogue model - the name, what it is for and its size, the wide before/after
 * picture, the licence badge, the author line (the owner, plan 03 step 40: «помечаем, что
 * модели принадлежит их авторам»), and on the right its state: the chosen tick, a green tick when it is on the device,
 * a percent ring with «12 из 33 МБ» while it downloads, or the pink «⤓ 33 МБ» to download it (the owner's words, plans/05
 * step 5). A card on the device chooses its model and returns to «Улучшение»; a downloaded one carries «Удалить».
 * [NOT-TESTED]
 */
@Composable
fun ModelsScreen(vm: UpscaleViewModel) {
    val c = MaterialTheme.colorScheme
    BackHandler(onBack = vm::back)
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    val shown = vm.catalogue.byAuthor.filter { filter.keeps(it) }

    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_back, onClick = vm::back)
            Text(stringResource(R.string.models_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.size(44.dp))
        }
        Row(
            // A fixed gap under the chips: the scrolled cards stopped flush against them (the owner, 2026-10-16 ≈ 20:5x: «когда их
            // скролишь вверху они прикасаются вплотную к чипсам фильтра - некрасиво»); the list's own top padding shrank by the
            // same amount, so the unscrolled look is unchanged. [NOT-TESTED]
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = CHIPS_GAP),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Filter.entries.forEach { f -> Pill(stringResource(f.label), on = f == filter, onClick = { filter = f }) }
        }
        vm.modelsMessage?.let { Notice(stringResource(it), onClick = {}, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) }
        LazyColumn(
            Modifier.fillMaxWidth().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp - CHIPS_GAP, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { m ->
                ModelRow(
                    m, chosen = m == vm.model, onDevice = m.id in vm.onDevice, download = vm.downloads[m.id],
                    onPick = { vm.pickFromCatalogue(m) }, onDownload = { vm.download(m) }, onDelete = { vm.deleteModel(m) },
                )
            }
        }
    }
}

/** The space kept under the filter chips while the list scrolls. */
private val CHIPS_GAP = 10.dp

/** The chips of the mockup; «Для коммерческого использования» keeps the models whose licence allows it. */
private enum class Filter(@param:StringRes val label: Int) {
    All(R.string.filter_all), Photo(R.string.filter_photo), Anime(R.string.filter_anime), Commercial(R.string.filter_commercial);

    fun keeps(m: Model): Boolean = when (this) {
        All -> true
        Photo -> m.kind == "photo"
        Anime -> m.kind == "anime"
        Commercial -> m.commercial
    }
}

@Composable
private fun ModelRow(
    m: Model, chosen: Boolean, onDevice: Boolean, download: Pair<Long, Long>?,
    onPick: () -> Unit, onDownload: () -> Unit, onDelete: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (chosen) Brush.verticalGradient(listOf(c.primaryContainer, c.surfaceContainer)) else Brush.linearGradient(listOf(c.surfaceContainer, c.surfaceContainer)))
            .border(2.dp, if (chosen) c.primary else c.surfaceContainer, shape)
            .clickable(enabled = onDevice, onClick = onPick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    ModelInfoButton(m)
                }
                if (m.starred) ChoiceLabel()
                Text(glueDots(stringResource(R.string.model_desc_with_size, stringResource(purposeRes(m.purpose)), sizeMb(m.bytes))),
                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            when {
                chosen -> Box(Modifier.size(22.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_tick), null, Modifier.size(14.dp), tint = c.onPrimary)
                }
                download != null -> DownloadRing(download.first, download.second)
                onDevice -> Box(Modifier.clip(CircleShape).background(c.tertiary.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(painterResource(R.drawable.ic_tick), null, Modifier.size(15.dp), tint = c.tertiary)
                }
                else -> Row(
                    Modifier.clip(CircleShape).background(c.primary).clickable(onClick = onDownload).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_save), stringResource(R.string.model_download), Modifier.size(15.dp), tint = c.onPrimary)
                    Text(stringResource(R.string.model_size_mb, mb(m.bytes)), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(700)), color = c.onPrimary)
                }
            }
        }
        ModelPreview(m)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LicenceBadge(m)
                Text(stringResource(R.string.model_author, m.author), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = c.onSurfaceVariant)
            }
            when {
                m.builtin -> Text(stringResource(R.string.model_builtin), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = c.tertiary)
                onDevice && download == null -> Text(
                    stringResource(R.string.model_delete),
                    Modifier.clip(CircleShape).border(1.5.dp, c.outline, CircleShape).clickable(onClick = onDelete).padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = c.onBackground,
                )
            }
        }
    }
}

/** The small percent ring of a downloading card with «12 из 33 МБ» under it (the owner: «при скачивании модели в ее плашке
 *  нужно крутить кольцо-прогресса с процентами, сколько мегабайт модели скачено»); round ends like the processing ring. */
@Composable
private fun DownloadRing(done: Long, total: Long) {
    val c = MaterialTheme.colorScheme
    val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction }, modifier = Modifier.size(48.dp), color = c.primary,
                strokeWidth = 5.dp, trackColor = c.surfaceContainerHigh, strokeCap = StrokeCap.Round, gapSize = 0.dp,
            )
            Text(stringResource(R.string.percent, (fraction * 100).toInt()),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight(800), fontFeatureSettings = "tnum"))
        }
        Text(stringResource(R.string.model_progress_mb, mb(done), mb(total)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFeatureSettings = "tnum"), color = c.onSurfaceVariant)
    }
}

/** The model's before/after picture across the card (2:1), as Upscayl's model cards show it (the owner, chat 2026-10-16
 *  ≈ 00:30 +03:00: «у апскейли они красивые»): one image per model whose left half is the small source enlarged without a
 *  network and the right half the model's own result (tools/make-previews.py), a pink line between, «До» · «После» on it. */
@Composable
fun ModelPreview(m: Model) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val img = remember(m.id) { loadPreview(ctx, m.id) }
    Box(Modifier.fillMaxWidth().aspectRatio(PREVIEW_ASPECT).clip(RoundedCornerShape(14.dp)).background(c.surfaceContainerHigh)) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().background(c.primary))
        PreviewLabel(stringResource(R.string.before), Modifier.align(Alignment.BottomStart))
        PreviewLabel(stringResource(R.string.after), Modifier.align(Alignment.BottomEnd))
    }
}

/** Width : height of a preview (tools/make-previews.py writes them at this ratio). */
private const val PREVIEW_ASPECT = 2f

@Composable
private fun PreviewLabel(text: String, modifier: Modifier) {
    Text(
        text, modifier.padding(8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.scrim).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight(700)), color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun loadPreview(ctx: android.content.Context, name: String): ImageBitmap? =
    runCatching { ctx.assets.open("previews/$name.webp").use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()

/** The licence badge in the owner's words (plan 03 step 35): green when commercial use is allowed, blue when it is allowed
 *  with credit (CC BY), amber «Не для коммерческого использования» (mockup .badge.os / .by / .nc). */
@Composable
fun LicenceBadge(m: Model) {
    val c = MaterialTheme.colorScheme
    val (text, color) = when (m.badge) {
        Model.Badge.Free -> stringResource(R.string.licence_commercial, m.licenceLabel) to c.tertiary
        Model.Badge.Credit -> stringResource(R.string.licence_commercial, m.licenceLabel) to c.secondary
        Model.Badge.NonCommercial -> stringResource(R.string.licence_noncommercial) to NonCommercialViolet
    }
    Text(
        glueDots(text), Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing), color = color,
    )
}

/** The card's description: what the model is for - one string per purpose of the catalogue (tools/catalogue.mjs checks
 *  that each purpose has its string). [NOT-TESTED: PurposeTest checks every catalogue purpose maps here] */
@StringRes
fun purposeRes(purpose: String): Int = purposeResOrNull(purpose) ?: R.string.model_purpose_photo

/**
 * The (i) of one model (the owner, 2026-10-16: «Можно кратко в карточке писать и подробнее - в (i) инфо кнопке по модели»):
 * the window shows `model_info_<id>` (what pictures the model is for, what it gives) and `model_info_<id>_list` (the
 * details: weak spots, what people say) - texts from researches/11 and researches/12. A model without these strings shows
 * no button: no placeholder text. [NOT-TESTED]
 */
@Composable
fun ModelInfoButton(m: Model) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val key = "model_info_" + m.id.lowercase().replace(Regex("[^a-z0-9]"), "_")
    @android.annotation.SuppressLint("DiscouragedApi")
    val text = remember(key) { ctx.resources.getIdentifier(key, "string", ctx.packageName) }
    @android.annotation.SuppressLint("DiscouragedApi")
    val list = remember(key) { ctx.resources.getIdentifier(key + "_list", "string", ctx.packageName) }
    if (text == 0) return
    var open by remember { mutableStateOf(false) }
    InfoButton(m.name) { open = true }
    if (open) InfoDialog(m.name, stringResource(text), if (list != 0) stringResource(list) else "") { open = false }
}

@StringRes
fun purposeResOrNull(purpose: String): Int? = when (purpose) {
    "fast_photo" -> R.string.model_purpose_fast_photo
    "detail" -> R.string.model_purpose_detail
    "web_photo" -> R.string.model_purpose_web_photo
    "universal" -> R.string.model_purpose_universal
    "noisy_photo" -> R.string.model_purpose_noisy_photo
    "photo_jpeg" -> R.string.model_purpose_photo_jpeg
    "skin_texture" -> R.string.model_purpose_skin_texture
    "photo" -> R.string.model_purpose_photo
    "balanced" -> R.string.model_purpose_balanced
    "faces" -> R.string.model_purpose_faces
    "anime" -> R.string.model_purpose_anime
    "anime_film" -> R.string.model_purpose_anime_film
    "anime_fast" -> R.string.model_purpose_anime_fast
    "sharp" -> R.string.model_purpose_sharp
    "sharpest" -> R.string.model_purpose_sharpest
    else -> null
}

/** The « · » separators of a card line with no-break spaces on both sides: a line never ends or starts with a hanging
 *  «·» - the line breaks at an ordinary space before it, so «·» always stands between two words (the owner, 2026-10-16
 *  ≈ 14:58: «разделители булет-дотом, которые остаются на строке в конце, и после него перенос … некрасиво»).
 *  [TESTED: GlueDotsTest - the separator loses its breaking spaces; the look on the Titan - see plans/08 step 2] */
fun glueDots(s: String): String = s.replace(" · ", " · ").replace("≈ ", "≈ ")  // «≈» stays with its number too

/** Megabytes as the cards show them: «33» from 1 MB up, «1,2» below 10 MB (the owner: «Там нужно писатьих размер в
 *  мегабайтах»); decimal MB like the release page shows. */
fun mb(bytes: Long): String {
    val v = bytes / 1_000_000.0
    return if (v >= 10) String.format(Locale.getDefault(), "%.0f", v) else String.format(Locale.getDefault(), "%.1f", v)
}

/** «33 МБ» in the user's language. */
@Composable
fun sizeMb(bytes: Long): String = glue(stringResource(R.string.model_size_mb, mb(bytes)))

