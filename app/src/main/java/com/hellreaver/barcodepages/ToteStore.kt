package com.hellreaver.barcodepages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * [text] is exactly what the keyboard typed. [label] is the same code in the case the store
 * label prints and encodes (see [labelCase]); the barcode and the screens use it. Keeping the
 * typed text untouched stops the app from rewriting a letter the keyboard is still composing,
 * which made Samsung's keyboard drop the first character.
 */
data class Tote(val id: Long, val text: String) {
    val label: String get() = text.labelCase()
}

/**
 * Puts a typed code in the case its store label uses, one letter at a time so the length
 * never changes (String.uppercase turns "ß" into "SS", which would throw off the cursor).
 *
 * - Trip labels start with TL and carry a lowercase trip id: "TL0a1b-2". Code 128 keeps case,
 *   so the id has to stay lowercase even when it was typed in capitals.
 * - Everything else, tote codes such as "Z13334" included, is capitals.
 */
fun String.labelCase(): String {
    val trip = length >= 2 && this[0].uppercaseChar() == 'T' && this[1].uppercaseChar() == 'L'
    return String(CharArray(length) { i ->
        if (trip && i >= 2) this[i].lowercaseChar() else this[i].uppercaseChar()
    })
}

enum class Screen { Entry, Barcodes, Share }

/** What survives Android killing the app in the background. A fresh launch starts empty. */
data class Snapshot(val labels: List<String>, val scanned: List<Boolean>, val screen: Screen)

/**
 * The tote list, which totes are marked scanned, and which screen is open.
 * The list always ends with one blank row; typing into it adds the next blank row.
 */
class ToteStore(restore: Snapshot? = null, private val onChange: (Snapshot) -> Unit = {}) {
    var totes by mutableStateOf(listOf<Tote>())
        private set
    var scanned by mutableStateOf(setOf<Long>())
        private set
    var screen by mutableStateOf(Screen.Entry)
        private set

    private var nextId = 1L

    init {
        if (restore != null) {
            totes = restore.labels.map { Tote(nextId++, it) }
            scanned = totes.filterIndexed { i, _ -> restore.scanned.getOrElse(i) { false } }.map { it.id }.toSet()
            screen = if (restore.screen == Screen.Barcodes && filled.isEmpty()) Screen.Entry else restore.screen
        }
        ensureTrailingBlank()
    }

    /** Totes with a label, in list order. These get barcodes. */
    val filled: List<Tote> get() = totes.filter { it.label.isNotBlank() }

    /**
     * Sets a row's text. Spaces, commas, semicolons and line breaks split the text into
     * several totes, so a pasted list fills several rows at once. The typed text is stored
     * as is; [Tote.label] capitalizes it, because Code 128 keeps case and tote labels are
     * printed in capitals.
     * Returns the id of the row that should take focus next, or null to leave focus alone.
     */
    fun edit(id: Long, raw: String): Long? {
        val index = totes.indexOfFirst { it.id == id }
        if (index < 0) return null
        val hasSeparator = SEPARATORS.containsMatchIn(raw)
        val parts = if (hasSeparator) raw.split(SEPARATORS).filter { it.isNotEmpty() } else listOf(raw)
        val first = parts.firstOrNull() ?: ""
        val added = parts.drop(1).map { Tote(nextId++, it) }

        val list = totes.toMutableList()
        if (list[index].label != first.labelCase()) scanned = scanned - id
        list[index] = Tote(id, first)
        list.addAll(index + 1, added)
        totes = list
        ensureTrailingBlank()
        changed()

        if (!hasSeparator) return null
        val lastTouched = added.lastOrNull()?.id ?: id
        return totes.getOrNull(totes.indexOfFirst { it.id == lastTouched } + 1)?.id
    }

    fun remove(id: Long) {
        totes = totes.filterNot { it.id == id }
        scanned = scanned - id
        ensureTrailingBlank()
        changed()
    }

    /** Drops blank rows other than the last one. */
    fun pruneBlanks() {
        val last = totes.lastOrNull()?.id
        val pruned = totes.filter { it.label.isNotBlank() || it.id == last }
        if (pruned.size != totes.size) {
            totes = pruned
            changed()
        }
    }

    fun clearAll() {
        totes = emptyList()
        scanned = emptySet()
        screen = Screen.Entry
        ensureTrailingBlank()
        changed()
    }

    fun toggleScanned(id: Long) {
        scanned = if (id in scanned) scanned - id else scanned + id
        changed()
    }

    fun showBarcodes() {
        if (filled.isEmpty()) return
        pruneBlanks()
        show(Screen.Barcodes)
    }

    fun show(target: Screen) {
        screen = target
        changed()
    }

    fun isDuplicate(tote: Tote): Boolean =
        tote.label.isNotBlank() && totes.count { it.label == tote.label } > 1

    fun snapshot() = Snapshot(totes.map { it.text }, totes.map { it.id in scanned }, screen)

    private fun ensureTrailingBlank() {
        if (totes.lastOrNull()?.label?.isBlank() != true) {
            totes = totes + Tote(nextId++, "")
        }
    }

    private fun changed() = onChange(snapshot())

    private companion object {
        val SEPARATORS = Regex("[\\s,;]+")
    }
}
