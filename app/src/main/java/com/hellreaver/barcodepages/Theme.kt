package com.hellreaver.barcodepages

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Color tokens from the Lemon house style (Lemon-Checklists style-guide.json). */
object Lemon {
    val bg = Color(0xFF1B1917)
    val card = Color(0xFF232019)
    val text = Color(0xFFECE8E0)
    val muted = Color(0xFFB6AFA0)
    val line = Color(0xFF3A362F)
    val accent = Color(0xFF7BAFA9)
    val accentSoft = Color(0xFF243B37)
    val accentInk = Color(0xFFBFE0DA)
    val onAccent = Color(0xFF13201E)
    val warn = Color(0xFFD9A95B)
    val warnBg = Color(0xFF3A2F1C)
    val bad = Color(0xFFD97A7A)
    val badBg = Color(0xFF3B2323)
    val idleBg = Color(0xFF2C2923)
    val track = Color(0xFF3A362F)
    val plate = Color.White

    val body = FontFamily(
        Font(R.font.barlow_regular, FontWeight.Normal),
        Font(R.font.barlow_semibold, FontWeight.SemiBold),
    )
    val display = FontFamily(Font(R.font.barlow_condensed_bold, FontWeight.Bold))
    val mono = FontFamily(
        Font(R.font.plex_mono_medium, FontWeight.Medium),
        Font(R.font.plex_mono_semibold, FontWeight.SemiBold),
    )
}

@Composable
fun LemonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lemon.accent,
            onPrimary = Lemon.onAccent,
            background = Lemon.bg,
            onBackground = Lemon.text,
            surface = Lemon.card,
            onSurface = Lemon.text,
            error = Lemon.bad,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = Lemon.accent,
                backgroundColor = Lemon.accentSoft,
            ),
            content = content,
        )
    }
}
