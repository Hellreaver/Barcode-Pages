package com.hellreaver.barcodepages

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Newest GitHub release. GitHub Actions attaches Barcode-Pages-<version>.apk to each one. */
const val RELEASES_URL = "https://github.com/Hellreaver/Barcode-Pages/releases/latest"

@Composable
fun ShareScreen(store: ToteStore, update: UpdateState = UpdateState.UpToDate, onCheckUpdate: () -> Unit = {}) {
    val context = LocalContext.current
    BackHandler { store.show(Screen.Entry) }

    Column(Modifier.fillMaxSize().background(Lemon.bg)) {
        TopBar(
            eyebrow = "Barcode-Pages v${BuildConfig.VERSION_NAME}",
            title = "Share & update",
            leading = { HeaderChip("‹ Totes", onClick = { store.show(Screen.Entry) }) },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UpdateCard(update, onCheckUpdate, open = { context.openInBrowser(it) })
            Text(
                "Point another phone's camera at this code to open the download page.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                style = TextStyle(fontFamily = Lemon.body, fontSize = 15.sp, color = Lemon.text),
            )
            QrCode(
                RELEASES_URL,
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("qr"),
            )
            SelectionContainer {
                Text(
                    RELEASES_URL,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Lemon.accentInk),
                )
            }
            LemonButton(
                text = "Send link",
                kind = ButtonKind.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Barcode-Pages app")
                        .putExtra(Intent.EXTRA_TEXT, RELEASES_URL)
                    context.startActivity(Intent.createChooser(send, "Send link"))
                },
            )
            Text(
                "On the release page, tap the Barcode-Pages .apk file under Assets, open the download, " +
                    "and allow the install when Android asks. Play Protect warns about any app " +
                    "that isn't from the Play Store; tap Install anyway.",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Lemon.accentSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                style = TextStyle(fontFamily = Lemon.body, fontSize = 14.sp, color = Lemon.accentInk),
            )
        }
    }
}

/** QR code on a white plate, whole-pixel modules, 4-module quiet zone. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix: BitMatrix = remember(text) {
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.MARGIN to 0,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            ),
        )
    }
    Canvas(modifier.background(Lemon.plate)) {
        val side = minOf(size.width, size.height).toInt()
        val cell = maxOf(1, side / (matrix.width + 8))
        val left = (size.width.toInt() - cell * matrix.width) / 2
        val top = (size.height.toInt() - cell * matrix.height) / 2
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset((left + x * cell).toFloat(), (top + y * cell).toFloat()),
                        size = Size(cell.toFloat(), cell.toFloat()),
                    )
                }
            }
        }
    }
}

private fun Context.openInBrowser(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/** Installed version against the newest GitHub release, like delivery-tracker's update panel. */
@Composable
private fun UpdateCard(update: UpdateState, onCheck: () -> Unit, open: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .stripeCard(if (update is UpdateState.Available) Lemon.accent else Lemon.line)
            .padding(start = 17.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .testTag("update"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("UPDATES", style = EyebrowStyle)
            Spacer(Modifier.weight(1f))
            Text(
                "Installed ${BuildConfig.VERSION_NAME}",
                style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Lemon.muted),
            )
        }
        val body = TextStyle(fontFamily = Lemon.body, fontSize = 15.sp, color = Lemon.text)
        when (update) {
            UpdateState.Checking -> Text("Checking GitHub for a newer version\u2026", style = body)
            UpdateState.UpToDate -> {
                Text("Up to date.", style = body)
                LemonButton("Check again", onClick = onCheck, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Available -> {
                LemonButton(
                    "Update to ${update.release.versionName}",
                    kind = ButtonKind.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { open(update.release.apkUrl ?: update.release.pageUrl) },
                )
                Text(
                    "Downloads in Chrome. Open the download and install it over this app.",
                    style = TextStyle(fontFamily = Lemon.body, fontSize = 13.sp, color = Lemon.muted),
                )
            }
            is UpdateState.Failed -> {
                Text(update.reason, style = body)
                Row {
                    LemonButton("Releases page", onClick = { open(RELEASES_URL) }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LemonButton("Check again", onClick = onCheck, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
