package com.hellreaver.barcodepages

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode as MlBarcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Two kinds of label. Tote labels: the entered totes, each with a name (a blank name uses the
 * one above it, so a customer's run of totes needs it typed once). Text label: just text, as
 * big as fits, for marking a tote "WATER". With no totes entered only the text label is offered.
 */
@Composable
fun PrintScreen(store: ToteStore) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("printing", Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val totes = store.filled
    BackHandler { store.closePrint() }
    var textMode by rememberSaveable { mutableStateOf(totes.isEmpty()) }
    if (totes.isEmpty()) textMode = true
    var labelText by rememberSaveable { mutableStateOf("") }

    var address by remember { mutableStateOf(prefs.getString("address", "").orEmpty()) }
    var useQr by remember { mutableStateOf(prefs.getBoolean("qr", false)) }
    var qrLevel by remember {
        mutableStateOf(runCatching { LabelZpl.QrLevel.valueOf(prefs.getString("qr_level", "")!!) }
            .getOrDefault(LabelZpl.DEFAULT_QR_LEVEL))
    }
    var copies by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var confirmCalibrate by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    val requesters = remember { mutableMapOf<Long, FocusRequester>() }

    fun say(msg: String) = scope.launch(Dispatchers.Main) {
        log.add(msg)
        while (log.size > 6) log.removeAt(0)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        say(if (ok) "Bluetooth allowed. Tap Print again." else "Bluetooth permission is needed to print")
    }

    LaunchedEffect(confirmCalibrate) {
        if (confirmCalibrate) {
            delay(3000)
            confirmCalibrate = false
        }
    }

    /** Checks permission and the address, then runs [build] for the printer on a background thread. */
    fun sendToPrinter(what: String, askSize: Boolean, build: (LabelZpl.LabelSize) -> ByteArray) {
        if (busy) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        val mac = LabelZpl.bluetoothAddress(address) ?: run {
            say("Enter the printer's Bluetooth address, or Scan its Wi-Fi sticker")
            return
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            say("Bluetooth is off")
            return
        }
        busy = true
        log.clear()
        say(what)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BluetoothPrinter.send(adapter.getRemoteDevice(mac), { say(it) }, askSize, build)
                }
                say("Done")
            } catch (e: Exception) {
                say("Failed: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                busy = false
            }
        }
    }

    fun scanSticker() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(MlBarcode.FORMAT_CODE_128, MlBarcode.FORMAT_CODE_39, MlBarcode.FORMAT_CODE_93)
            .enableAutoZoom()
            .allowManualInput()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { code ->
                val bt = LabelZpl.bluetoothFromWifi(code.rawValue.orEmpty())
                if (bt == null) {
                    say("That isn't a Wi-Fi address. Scan the sticker with the Wi-Fi symbol.")
                } else {
                    address = bt.replace(":", "")
                    prefs.edit().putString("address", address).apply()
                    say("Printer $address")
                }
            }
            .addOnFailureListener { say("Scanner failed: ${it.message}") }
    }

    Column(Modifier.fillMaxSize().background(Lemon.bg)) {
        TopBar(
            eyebrow = if (textMode) "Print a text label" else "Print tote labels",
            title = if (textMode) "Text label" else if (totes.size == 1) "1 tote label" else "${totes.size} tote labels",
            leading = {
                HeaderChip(
                    if (store.printReturn == Screen.Barcodes) "‹ Barcodes" else "‹ Totes",
                    onClick = { store.closePrint() },
                )
            },
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (totes.isNotEmpty()) {
                Row {
                    Toggle(if (totes.size == 1) "1 tote" else "${totes.size} totes", selected = !textMode, modifier = Modifier.weight(1f)) {
                        textMode = false
                    }
                    Spacer(Modifier.width(8.dp))
                    Toggle("Text label", selected = textMode, modifier = Modifier.weight(1f)) {
                        focusManager.clearFocus()
                        textMode = true
                    }
                }
            }
            if (textMode) {
                Note(
                    if (totes.isEmpty()) "No totes entered, so this prints a text label: words only, as big as fits. " +
                        "Enter totes on the first screen to print tote labels."
                    else "Words only, as big as fits, up to 3 lines. For marking a tote WATER and the like.",
                )
                Box(Modifier.fillMaxWidth().stripeCard(if (labelText.isNotBlank()) Lemon.accent else Lemon.line).padding(start = 17.dp, end = 10.dp, top = 14.dp, bottom = 14.dp)) {
                    if (labelText.isEmpty()) Placeholder("Label text, e.g. WATER")
                    BasicTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        cursorBrush = SolidColor(Lemon.accent),
                        textStyle = TextStyle(fontFamily = Lemon.body, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = Lemon.text),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().testTag("label-text"),
                    )
                }
            } else {
                Note("Name on each label (optional). Leave one blank to use the name above it, so a customer's totes need it once.")
            }
            if (!textMode) totes.forEachIndexed { index, tote ->
                key(tote.id) {
                    val requester = requesters.getOrPut(tote.id) { FocusRequester() }
                    NameRow(
                        number = index + 1,
                        code = tote.label,
                        typed = store.names[tote.id].orEmpty(),
                        inherited = if (store.ownName(tote).isEmpty()) store.nameFor(tote) else "",
                        requester = requester,
                        onChange = { store.setName(tote.id, it) },
                        onNext = {
                            val next = totes.getOrNull(index + 1)
                            if (next != null) requesters[next.id]?.requestFocus() else focusManager.clearFocus()
                        },
                    )
                }
            }

            SectionLabel("Printer")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).stripeCard(Lemon.line).padding(start = 17.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)) {
                    if (address.isEmpty()) Placeholder("Bluetooth address")
                    BasicTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            prefs.edit().putString("address", it).apply()
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(Lemon.accent),
                        textStyle = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Lemon.text),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().testTag("printer-address"),
                    )
                }
                Spacer(Modifier.width(8.dp))
                LemonButton("Scan", onClick = { scanSticker() }, padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp))
            }
            Note("The printer's BT Addr. Scan reads the Wi-Fi sticker on its back and adds one. A new printer asks to pair: confirm the code on both screens.")

            if (!textMode) SectionLabel("Code")
            if (!textMode) Row {
                Toggle("Barcode", selected = !useQr, modifier = Modifier.weight(1f)) {
                    useQr = false
                    prefs.edit().putBoolean("qr", false).apply()
                }
                Spacer(Modifier.width(8.dp))
                Toggle("QR code", selected = useQr, modifier = Modifier.weight(1f)) {
                    useQr = true
                    prefs.edit().putBoolean("qr", true).apply()
                }
            }
            if (useQr && !textMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelZpl.QrLevel.values().forEachIndexed { i, level ->
                        if (i > 0) Spacer(Modifier.width(6.dp))
                        Toggle("${level.name} ${level.percent}%", selected = level == qrLevel, modifier = Modifier.weight(1f)) {
                            qrLevel = level
                            prefs.edit().putString("qr_level", level.name).apply()
                        }
                    }
                }
                Note("How much of the QR code can be scuffed off and still scan. Q fits tote and trip codes in the smallest QR size.")
            }

            SectionLabel("Copies of each label")
            Row(verticalAlignment = Alignment.CenterVertically) {
                LemonButton("−", onClick = { if (copies > 1) copies-- }, padding = PaddingValues(horizontal = 22.dp, vertical = 12.dp))
                Text(
                    "$copies",
                    modifier = Modifier.widthIn(min = 64.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = Lemon.text),
                )
                LemonButton("+", onClick = { if (copies < 99) copies++ }, padding = PaddingValues(horizontal = 22.dp, vertical = 12.dp))
            }

            if (log.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().stripeCard(Lemon.accent).padding(start = 17.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)) {
                    log.forEach {
                        Text(it, style = TextStyle(fontFamily = Lemon.mono, fontSize = 13.sp, color = Lemon.text))
                    }
                }
            }

            SectionLabel("Printer tools")
            Row {
                LemonButton(
                    "Config label",
                    onClick = { sendToPrinter("Printing the printer's settings label", false) { LabelZpl.CONFIG_LABEL.toByteArray() } },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                LemonButton(
                    if (confirmCalibrate) "Tap again" else "Calibrate",
                    kind = ButtonKind.Danger,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (confirmCalibrate) {
                            confirmCalibrate = false
                            sendToPrinter("Calibrating: it feeds a few labels. Turn it off and on when it stops.", false) {
                                LabelZpl.CALIBRATE_MARK.toByteArray()
                            }
                        } else {
                            confirmCalibrate = true
                        }
                    },
                )
            }
            Note("Calibrate if labels come out cut off: the printer measures its labels again and saves it.")
        }

        BottomBar(Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
            val n = (if (textMode) 1 else totes.size) * copies
            LemonButton(
                text = if (busy) "Printing…" else if (n == 1) "Print 1 label" else "Print $n labels",
                kind = ButtonKind.Primary,
                enabled = !busy && (if (textMode) labelText.isNotBlank() else totes.isNotEmpty()),
                modifier = Modifier.weight(1f).testTag("print"),
                onClick = {
                    focusManager.clearFocus()
                    if (textMode) {
                        val text = labelText
                        sendToPrinter("Printing \"${text.trim()}\" x$copies", askSize = true) { size ->
                            LabelZpl.textLabel(text, size, LabelText, copies).toByteArray(Charsets.US_ASCII)
                        }
                        return@LemonButton
                    }
                    val qr = if (useQr) qrLevel else null
                    if (qr == null) totes.firstNotNullOfOrNull { LabelZpl.problem(it.label) }?.let {
                        say(it)
                        return@LemonButton
                    }
                    val labels = totes.map { it.label to store.nameFor(it) }
                    sendToPrinter("Printing $n ${if (n == 1) "label" else "labels"}", askSize = true) { size ->
                        labels.joinToString("\n") { (code, name) -> LabelZpl.label(code, name, size, LabelText, copies, qr) }
                            .toByteArray(Charsets.US_ASCII)
                    }
                },
            )
        }
    }
}

@Composable
private fun NameRow(
    number: Int,
    code: String,
    typed: String,
    inherited: String,
    requester: FocusRequester,
    onChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val has = typed.isNotBlank()
    Column(Modifier.fillMaxWidth().stripeCard(if (has) Lemon.accent else Lemon.line).padding(start = 17.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$number",
                modifier = Modifier
                    .widthIn(min = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Lemon.accentSoft)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Lemon.accentInk),
            )
            Spacer(Modifier.width(12.dp))
            Text(code, style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Lemon.accentInk))
        }
        Box(Modifier.padding(start = 46.dp, top = 6.dp)) {
            if (typed.isEmpty()) {
                Placeholder(if (inherited.isNotEmpty()) "Same as above: $inherited" else "Name (optional)")
            }
            BasicTextField(
                value = typed,
                onValueChange = onChange,
                singleLine = true,
                cursorBrush = SolidColor(Lemon.accent),
                textStyle = TextStyle(fontFamily = Lemon.body, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Lemon.text),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    // Fixed for the life of the field; changing it while typing restarts the
                    // keyboard connection (see the tote code field).
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .semantics { contentDescription = "Name for tote $number" }
                    .testTag("name-$number"),
            )
        }
    }
}

@Composable
private fun Toggle(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    LemonButton(
        text,
        onClick = onClick,
        kind = if (selected) ButtonKind.Soft else ButtonKind.Plain,
        modifier = modifier,
        padding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), modifier = Modifier.padding(start = 4.dp, top = 10.dp), style = EyebrowStyle)
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        style = TextStyle(fontFamily = Lemon.body, fontSize = 14.sp, color = Lemon.muted),
    )
}

@Composable
private fun Placeholder(text: String) {
    Text(text, maxLines = 1, style = TextStyle(fontFamily = Lemon.body, fontSize = 20.sp, color = Lemon.muted.copy(alpha = 0.7f)))
}
