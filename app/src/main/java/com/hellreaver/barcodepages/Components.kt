package com.hellreaver.barcodepages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(10.dp)
val ControlShape = RoundedCornerShape(8.dp)

/** Card surface with the 5px left status stripe used by Lemon guide steps. */
fun Modifier.stripeCard(stripe: Color): Modifier = this
    .clip(CardShape)
    .background(Lemon.card)
    .drawBehind { drawRect(stripe, size = Size(5.dp.toPx(), size.height)) }
    .border(1.dp, Lemon.line, CardShape)

/** Sticky header: eyebrow, title, optional progress bar, 3px accent rule under it. */
@Composable
fun TopBar(
    eyebrow: String,
    title: String,
    progress: Float? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Lemon.bg)
            .drawBehind {
                val h = 3.dp.toPx()
                drawRect(Lemon.accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            }
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Box(Modifier.padding(end = 12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(eyebrow.uppercase(), style = EyebrowStyle)
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = Lemon.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        color = Lemon.accentInk,
                    ),
                )
            }
            trailing?.invoke()
        }
        if (progress != null) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Lemon.track)
                    .drawBehind { drawRect(Lemon.accent, size = Size(size.width * progress, size.height)) },
            )
        }
    }
}

val EyebrowStyle = TextStyle(
    fontFamily = Lemon.display,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = 1.3.sp,
    color = Lemon.accent,
)

enum class ButtonKind { Primary, Plain, Danger, Soft }

@Composable
fun LemonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Plain,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
) {
    val (fill, border, ink) = when (kind) {
        ButtonKind.Primary -> Triple(Lemon.accent, Lemon.accent, Lemon.onAccent)
        ButtonKind.Plain -> Triple(Lemon.card, Lemon.line, Lemon.text)
        ButtonKind.Danger -> Triple(Lemon.card, Lemon.badBg, Lemon.bad)
        ButtonKind.Soft -> Triple(Lemon.accentSoft, Lemon.accent, Lemon.accentInk)
    }
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(ControlShape)
            .background(if (enabled) fill else fill.copy(alpha = 0.6f))
            .border(1.dp, border, ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = Lemon.display,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 0.8.sp,
                color = if (enabled) ink else ink.copy(alpha = 0.6f),
            ),
        )
    }
}

@Composable
fun Chip(text: String, fg: Color, bg: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = fg),
    )
}

/** Bottom action bar surface: card fill, 1px line on top. */
@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Lemon.card)
            .drawBehind { drawRect(Lemon.line, size = Size(size.width, 1.dp.toPx())) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun HeaderChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(ControlShape)
            .background(Lemon.card)
            .border(1.dp, Lemon.line, ControlShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = TextStyle(
            fontFamily = Lemon.display,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Lemon.accentInk,
        ),
    )
}
