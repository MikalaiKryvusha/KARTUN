package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R

/**
 * The user guide (mockups 'manual' / 'manual-models'): a Markdown document laid out like a rendered page - the owner
 * asked for it «в вёрстке Markdown» with points and pictures (interview #002). The text lives in res/raw/manual.md (English) and
 * res/raw-ru/manual.md, one file per language; this renderer knows only what the guide uses: `#`/`##` headings (a leading number painted pink), paragraphs,
 * numbered and bulleted lists, `|` tables (the first row is the header), `>` notes, `**bold**` and pictures
 * `![caption](manual/ru/01_home.webp)` - a file of the APK's assets, drawn across the page with its caption under it (the
 * owner, interview #002: «красочно с пунктами, картинками»; 2026-10-16: «картинки у тебя в руководстве будут? кнопки?
 * примеры интерфейса?»). A picture whose file is missing is skipped. [NOT-TESTED: device run deferred by the owner
 * («потом потестируем»); pictures - no guide page carries one yet]
 */
@Composable
fun ManualScreen(onBack: () -> Unit, raw: Int = R.raw.manual, title: Int = R.string.manual_title) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    // The same reader shows the guide and the terms of use (raw/terms.md - plan 03 step 40).
    val lines = remember(raw) { ctx.resources.openRawResource(raw).bufferedReader().readLines() }

    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_back, onClick = onBack)
            Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.size(44.dp))
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, bottom = 40.dp).widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trimEnd()
                when {
                    line.isBlank() -> Unit
                    line.startsWith("## ") -> H2(line.removePrefix("## "))
                    line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall)
                    line.startsWith("> ") -> Note(line.removePrefix("> "))
                    PICTURE.matches(line) -> PICTURE.find(line)!!.destructured.let { (caption, path) -> Picture(path, caption) }
                    line.startsWith("|") -> {
                        // A table: this line and the following "|" lines; the first row is the header.
                        val rows = mutableListOf<List<String>>()
                        while (i < lines.size && lines[i].startsWith("|")) {
                            rows += lines[i].trim().trim('|').split('|').map { it.trim() }
                            i++
                        }
                        Table(rows)
                        continue
                    }
                    Regex("""^(\d+)\. (.*)""").matches(line) -> ListItem(line.substringBefore(". ") + ".", line.substringAfter(". "))
                    line.startsWith("- ") -> ListItem("•", line.removePrefix("- "))
                    else -> Text(inline(line), style = MaterialTheme.typography.bodyLarge, color = c.onBackground)
                }
                i++
            }
        }
    }
}

/** `![caption](assets path)` on a line of its own. */
internal val PICTURE = Regex("""^!\[(.*)]\((.+)\)$""")

/** A picture of the guide: an asset image across the page, rounded like the cards, its caption under it. */
@Composable
private fun Picture(path: String, caption: String) {
    val ctx = LocalContext.current
    val img = remember(path) {
        runCatching { ctx.assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
    } ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Image(
            img, caption, Modifier.fillMaxWidth().widthIn(max = 420.dp).clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
        )
        if (caption.isNotBlank()) Text(caption, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** `**bold**` spans in the guide's own text colour; the rest stays as written. */
@Composable
private fun inline(s: String): AnnotatedString {
    val bold = MaterialTheme.colorScheme.onBackground
    return buildAnnotatedString {
        s.split("**").forEachIndexed { k, part ->
            if (k % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight(700), color = bold)) { append(part) } else append(part)
        }
    }
}

/** A section heading with its number in pink and a hairline under it (.md h2 of the mockup). */
@Composable
private fun H2(text: String) {
    val c = MaterialTheme.colorScheme
    val num = text.substringBefore(' ').takeIf { it.all(Char::isDigit) }
    Column(Modifier.padding(top = 14.dp)) {
        Text(
            buildAnnotatedString {
                if (num != null) { withStyle(SpanStyle(color = c.primary)) { append(num) }; append("  "); append(text.substringAfter(' ')) } else append(text)
            },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
        )
        HorizontalDivider(Modifier.padding(top = 6.dp), color = c.outline)
    }
}

@Composable
private fun ListItem(marker: String, text: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(marker, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(700)), color = c.primary)
        Text(inline(text), style = MaterialTheme.typography.bodyLarge, color = c.onBackground)
    }
}

/** The pink-edged note block (.md .note). */
@Composable
private fun Note(text: String) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)).background(c.primaryContainer)
            .drawBehind { drawLine(c.primary, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 3.dp.toPx()) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) { Text(inline(text), style = MaterialTheme.typography.bodyMedium, color = c.onBackground) }
}

/** A table with a pink upper-case header row and hairlines between the rows (.md table). */
@Composable
private fun Table(rows: List<List<String>>) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, c.outline, RoundedCornerShape(14.dp))) {
        rows.forEachIndexed { r, cells ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                cells.forEach { cell ->
                    if (r == 0) Text(cell.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = c.primary)
                    else Text(inline(cell), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = c.onBackground)
                }
            }
            if (r < rows.lastIndex) HorizontalDivider(color = if (r == 0) c.primary.copy(alpha = 0.45f) else c.outline)
        }
    }
}
