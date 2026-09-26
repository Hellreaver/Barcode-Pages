package com.hellreaver.totebarcodes

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarcodeScreen(store: ToteStore) {
    val totes = store.filled
    val done = totes.count { it.id in store.scanned }
    BackHandler { store.show(Screen.Entry) }
    ScreenAtFullBrightness()

    Column(Modifier.fillMaxSize().background(Lemon.bg)) {
        TopBar(
            eyebrow = "$done of ${totes.size} marked scanned",
            title = if (totes.size == 1) "1 tote" else "${totes.size} totes",
            progress = if (totes.isEmpty()) 0f else done.toFloat() / totes.size,
            leading = { HeaderChip("\u2039 Totes", onClick = { store.show(Screen.Entry) }) },
        )
        val nav = WindowInsets.navigationBars.asPaddingValues()
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("barcodes"),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 24.dp + nav.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(totes, key = { _, t -> t.id }) { index, tote ->
                BarcodeCard(
                    position = index + 1,
                    total = totes.size,
                    tote = tote,
                    scanned = tote.id in store.scanned,
                    onToggle = { store.toggleScanned(tote.id) },
                )
            }
            item(key = "end") {
                Text(
                    "End of list · ${totes.size} ${if (totes.size == 1) "tote" else "totes"}",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Lemon.body, fontSize = 13.sp, color = Lemon.muted),
                )
            }
        }
    }
}

@Composable
private fun BarcodeCard(position: Int, total: Int, tote: Tote, scanned: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .stripeCard(if (scanned) Lemon.accent else Lemon.line)
            .padding(start = 17.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$position / $total",
                style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Lemon.muted),
            )
            Spacer(Modifier.weight(1f))
            if (scanned) Chip("SCANNED", Lemon.accentInk, Lemon.accentSoft)
        }
        BasicText(
            tote.label,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 46.sp),
            style = TextStyle(
                fontFamily = Lemon.mono,
                fontWeight = FontWeight.SemiBold,
                color = Lemon.accentInk,
                textAlign = TextAlign.Center,
            ),
        )
        Code128(
            tote.label,
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag("barcode-$position"),
        )
        Spacer(Modifier.height(10.dp))
        LemonButton(
            text = if (scanned) "✓ Scanned · tap to undo" else "Mark scanned",
            kind = if (scanned) ButtonKind.Soft else ButtonKind.Plain,
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** White plate with whole-pixel bars and a 10-module quiet zone on each side. */
@Composable
fun Code128(text: String, modifier: Modifier = Modifier) {
    val modules = remember(text) { Barcode.modules(text) }
    Canvas(modifier.background(Lemon.plate)) {
        if (modules == null) return@Canvas
        val (modulePx, left) = Barcode.layout(size.width.toInt(), modules.size)
        val inset = 12.dp.toPx().toInt().toFloat()
        for ((start, end) in Barcode.bars(modules)) {
            drawRect(
                Color.Black,
                topLeft = Offset((left + start * modulePx).toFloat(), inset),
                size = Size(((end - start) * modulePx).toFloat(), size.height - 2 * inset),
            )
        }
    }
}

/** Keeps the screen on at full brightness while barcodes are showing, so the scanner reads them. */
@Composable
private fun ScreenAtFullBrightness() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
}
