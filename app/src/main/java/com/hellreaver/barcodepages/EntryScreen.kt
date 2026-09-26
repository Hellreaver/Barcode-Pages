package com.hellreaver.barcodepages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EntryScreen(store: ToteStore, updateAvailable: Boolean = false, autoFocus: Boolean = true) {
    val requesters = remember { mutableMapOf<Long, FocusRequester>() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var pendingFocus by remember { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var focusedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (autoFocus && store.filled.isEmpty()) pendingFocus = store.totes.last().id
    }
    LaunchedEffect(pendingFocus) {
        val id = pendingFocus ?: return@LaunchedEffect
        requesters[id]?.requestFocus()
        pendingFocus = null
    }
    LaunchedEffect(confirmClear) {
        if (confirmClear) {
            delay(3000)
            confirmClear = false
        }
    }

    val count = store.filled.size

    Column(Modifier.fillMaxSize().background(Lemon.bg)) {
        TopBar(
            eyebrow = "Missed dispense recovery",
            title = "Enter missing totes",
            trailing = {
                HeaderChip(
                    if (updateAvailable) "Update" else "Share",
                    highlighted = updateAvailable,
                    onClick = { store.show(Screen.Share) },
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
            Text(
                "Type the code from the bottom-right corner of each tote label, like Z13334, " +
                    "zeros included. A new box opens as you type, and Next works as soon as there is one.",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = TextStyle(fontFamily = Lemon.body, fontSize = 14.sp, color = Lemon.muted),
            )
            store.totes.forEachIndexed { index, tote ->
                key(tote.id) {
                    val requester = requesters.getOrPut(tote.id) { FocusRequester() }
                    val isLast = index == store.totes.lastIndex
                    ToteRow(
                        number = index + 1,
                        tote = tote,
                        isLast = isLast,
                        duplicate = store.isDuplicate(tote),
                        requester = requester,
                        onChange = { text -> store.edit(tote.id, text)?.let { pendingFocus = it } },
                        onNext = {
                            val next = store.totes.getOrNull(store.totes.indexOfFirst { it.id == tote.id } + 1)
                            if (next != null) {
                                pendingFocus = next.id
                            } else {
                                keyboard?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        onFocus = { isFocused ->
                            if (isFocused) focusedId = tote.id else if (focusedId == tote.id) focusedId = null
                        },
                        onBlur = { store.pruneBlanks() },
                        onRemove = { store.remove(tote.id) },
                    )
                }
            }
            // Stays up while the first code is typed, then goes once the list has moved past it.
            val first = store.filled.firstOrNull()
            if (first == null || (store.filled.size == 1 && focusedId == first.id)) {
                ExampleLabel(Modifier.padding(top = 12.dp))
            }
        }

        BottomBar(Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
            LemonButton(
                text = if (confirmClear) "Tap again" else "Clear all",
                kind = ButtonKind.Danger,
                enabled = count > 0,
                modifier = Modifier.weight(1f).testTag("clear"),
                onClick = {
                    if (confirmClear) {
                        confirmClear = false
                        store.clearAll()
                        pendingFocus = store.totes.last().id
                    } else {
                        confirmClear = true
                    }
                },
            )
            Spacer(Modifier.width(10.dp))
            LemonButton(
                text = when (count) {
                    0 -> "Next ›"
                    1 -> "Next › 1 barcode"
                    else -> "Next › $count barcodes"
                },
                kind = ButtonKind.Primary,
                enabled = count > 0,
                modifier = Modifier.weight(2f).testTag("next"),
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    store.showBarcodes()
                },
            )
        }
    }
}

@Composable
private fun ToteRow(
    number: Int,
    tote: Tote,
    isLast: Boolean,
    duplicate: Boolean,
    requester: FocusRequester,
    onChange: (String) -> Unit,
    onNext: () -> Unit,
    onFocus: (Boolean) -> Unit,
    onBlur: () -> Unit,
    onRemove: () -> Unit,
) {
    val filled = tote.label.isNotBlank()
    val encodable = !filled || Barcode.isEncodable(tote.label)
    var focused by remember { mutableStateOf(false) }
    val stripe = when {
        !encodable -> Lemon.bad
        duplicate -> Lemon.warn
        focused || filled -> Lemon.accent
        else -> Lemon.line
    }

    Column(Modifier.fillMaxWidth().stripeCard(stripe).padding(start = 17.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$number",
                modifier = Modifier
                    .widthIn(min = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (filled) Lemon.accentSoft else Lemon.idleBg)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = Lemon.mono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (filled) Lemon.accentInk else Lemon.muted,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (!filled) {
                    Text(
                        if (isLast && number == 1) "Tote code, e.g. Z13334" else "Next tote",
                        style = TextStyle(fontFamily = Lemon.body, fontSize = 20.sp, color = Lemon.muted.copy(alpha = 0.7f)),
                    )
                }
                BasicTextField(
                    value = tote.text,
                    onValueChange = onChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Lemon.accent),
                    textStyle = TextStyle(
                        fontFamily = Lemon.mono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = Lemon.text,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        // Fixed for the life of the field. Switching it while typing restarts the
                        // keyboard connection, and Samsung's keyboard loses a letter when that happens.
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    visualTransformation = UppercaseTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(requester)
                        .onFocusChanged {
                            if (focused && !it.isFocused) onBlur()
                            focused = it.isFocused
                            onFocus(it.isFocused)
                        }
                        .semantics { contentDescription = "Tote $number" }
                        .testTag("tote-$number"),
                )
            }
            if (filled) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onRemove)
                        .semantics { contentDescription = "Remove tote $number" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", style = TextStyle(fontSize = 26.sp, color = Lemon.muted))
                }
            }
        }
        if (!encodable || duplicate) {
            Row(Modifier.padding(start = 46.dp, bottom = 4.dp)) {
                if (!encodable) {
                    Chip("Only plain letters, digits & symbols", Lemon.bad, Lemon.badBg)
                } else {
                    Chip("Listed twice", Lemon.warn, Lemon.warnBg)
                }
            }
        }
    }
}


/** Shows typed text in capitals without changing what the keyboard sent. Same length, so offsets map 1:1. */
private object UppercaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(AnnotatedString(text.text.caps()), OffsetMapping.Identity)
}
