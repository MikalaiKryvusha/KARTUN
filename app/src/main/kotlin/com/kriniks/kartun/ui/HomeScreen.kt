package com.kriniks.kartun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R

/**
 * Home screen, variant A v2 of the accepted mockups ('A-home'): the big «Выберите фото» card opens the system photo
 * picker; «Вставить ссылку» downloads the picture whose link is in the clipboard (LinkDownloader, network only here); the share hint explains «Поделиться → KARTUN», which
 * works (MainActivity's ACTION_SEND filter). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
@Composable
fun HomeScreen(onPick: () -> Unit, onPasteLink: () -> Unit, downloading: Boolean, error: String?, onDismissError: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(horizontal = 20.dp)) {
        // Logo: «K» in pink, the rest white (.logo b). The gear (settings) arrives with the settings screen.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString { withStyle(SpanStyle(color = c.primary)) { append("K") }; append("ARTUN") },
                Modifier.weight(1f).padding(vertical = 10.dp), style = MaterialTheme.typography.displaySmall,
            )
        }
        // Portrait: one column as on the mockup. Landscape (the owner, 2026-10-14 ≈ 19:12: «лендскейп нужно поддерживать
        // тоже»): the pick card on the left at full height, the link button, the share hint and the note on the right. [NOT-TESTED: landscape - built only; the device run waits for the owner («потом потестируем»)]
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(Modifier.weight(1f).padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PickCard(onPick, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (error != null) Notice(error, onDismissError)
                    SecondaryButton(stringResource(if (downloading) R.string.link_downloading else R.string.home_paste_link), R.drawable.ic_link, onClick = onPasteLink, enabled = !downloading)
                    ShareHint()
                    Spacer(Modifier.weight(1f))
                    OfflineNote()
                }
            }
            return@Column
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (error != null) Notice(error, onDismissError)
            PickCard(onPick, Modifier.fillMaxWidth().weight(1f, fill = true).heightIn(max = 380.dp))
            SecondaryButton(stringResource(if (downloading) R.string.link_downloading else R.string.home_paste_link), R.drawable.ic_link, onClick = onPasteLink, enabled = !downloading)
            ShareHint()
        }
        Box(Modifier.padding(top = 16.dp, bottom = 24.dp)) { OfflineNote() }
    }
}

/** The pick card (.drop): pink glow from the top, pink border, the 96 dp pink tile with the image icon. */
@Composable
private fun PickCard(onPick: () -> Unit, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
            Column(
                modifier.clip(MaterialTheme.shapes.extraLarge)
                    .drawBehind {
                        // radial-gradient(120% 80% at 50% 0%, pink .18, card 60%) of the mockup: the glow comes from the top edge.
                        drawRect(c.surfaceContainer)
                        drawRect(Brush.radialGradient(listOf(c.primary.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * 0.72f))
                    }
                    .border(1.5.dp, c.primary.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge)
                    .clickable(onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(96.dp).shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = c.primary, spotColor = c.primary)
                        .clip(RoundedCornerShape(32.dp)).background(c.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_image), null, Modifier.size(44.dp), tint = c.onPrimary) }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.home_pick_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.home_pick_sub), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
}

/** Share hint (.share-hint): how to send a photo here from another app. */
@Composable
private fun ShareHint() {
    val c = MaterialTheme.colorScheme
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(c.surfaceContainer).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_share), null, Modifier.size(18.dp), tint = c.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_share_title), style = MaterialTheme.typography.titleSmall)
                }
                Text(stringResource(R.string.home_share_text), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                // Centred in the card - the owner's word in chat 2026-10-14 ≈ 18:50: «Фото - Поделиться - Кртун - выровнять по центру».
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    FlowChip(stringResource(R.string.flow_photo), false); Arrow()
                    FlowChip(stringResource(R.string.flow_share), false); Arrow()
                    FlowChip("KARTUN", true)
                }
            }
}

/** Foot note with the green dot: the promise of on-device processing. */
@Composable
private fun OfflineNote() {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c.tertiary))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.home_offline), style = MaterialTheme.typography.bodySmall, color = c.outlineVariant)
    }
}

@Composable
private fun FlowChip(text: String, key: Boolean) {
    val c = MaterialTheme.colorScheme
    Text(
        text, Modifier.clip(CircleShape).background(if (key) c.primary else c.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
        color = if (key) c.onPrimary else c.onBackground,
    )
}

@Composable
private fun Arrow() = Text(stringResource(R.string.arrow), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
