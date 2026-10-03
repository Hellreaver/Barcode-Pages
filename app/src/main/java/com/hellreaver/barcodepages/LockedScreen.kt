package com.hellreaver.barcodepages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown instead of the app while the owner's off switch is on, or GitHub has been silent a week. */
@Composable
fun LockedScreen(lock: Lock, checking: Boolean, onRetry: () -> Unit) {
    val (title, body) = when (lock) {
        is Lock.TurnedOff -> "App turned off" to
            lock.message.ifEmpty { "The owner has turned this app off for now." }
        else -> "Can't reach GitHub" to
            "This app checks in with GitHub at least once a week, and it hasn't been able to for " +
            "over 7 days. Connect to the internet and tap Check again."
    }
    Column(Modifier.fillMaxSize().background(Lemon.bg).testTag("locked")) {
        TopBar(eyebrow = "Barcode-Pages v${BuildConfig.VERSION_NAME}", title = title)
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(body, style = TextStyle(fontFamily = Lemon.body, fontSize = 17.sp, color = Lemon.text))
            LemonButton(
                text = if (checking) "Checking…" else "Check again",
                kind = ButtonKind.Primary,
                enabled = !checking,
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
