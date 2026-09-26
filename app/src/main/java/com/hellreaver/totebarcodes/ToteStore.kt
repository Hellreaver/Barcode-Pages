package com.hellreaver.totebarcodes

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class Tote(val id: Long, val label: String)

enum class Screen { Entry, Barcodes }

/**
 * The tote list, which totes are marked scanned, and which screen is open. Every change is
 * written to SharedPreferences, so a crash, a reboot or a swipe-away brings back the same list.
 * The list always ends with one blank row; typing into it adds the next blank row.
 */
class ToteStore(private val prefs: SharedPreferences) {
    var totes by mutableStateOf(listOf<Tote>())
        private set
    var scanned by mutableStateOf(setOf<Long>())
        private set
    var screen by mutableStateOf(Screen.Entry)
        private set

    private var nextId = 1L

    init {
        load()
        ensureTrailingBlank()
    }

    /** Totes with a label, in list order. These get barcodes. */
    val filled: List<Tote> get() = totes.filter { it.label.isNotBlank() }

    /**
     * Sets a row's text. Spaces, commas, semicolons and line breaks split the text into
     * several totes, so a pasted list fills several rows at once.
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
        if (list[index].label != first) scanned = scanned - id
        list[index] = Tote(id, first)
        list.addAll(index + 1, added)
        totes = list
        ensureTrailingBlank()
        save()

        if (!hasSeparator) return null
        val lastTouched = added.lastOrNull()?.id ?: id
        return totes.getOrNull(totes.indexOfFirst { it.id == lastTouched } + 1)?.id
    }

    fun remove(id: Long) {
        totes = totes.filterNot { it.id == id }
        scanned = scanned - id
        ensureTrailingBlank()
        save()
    }

    /** Drops blank rows other than the last one and [keep]. */
    fun pruneBlanks(keep: Long? = null) {
        val last = totes.lastOrNull()?.id
        val pruned = totes.filter { it.label.isNotBlank() || it.id == last || it.id == keep }
        if (pruned.size != totes.size) {
            totes = pruned
            save()
        }
    }

    fun clearAll() {
        totes = emptyList()
        scanned = emptySet()
        screen = Screen.Entry
        ensureTrailingBlank()
        save()
    }

    fun toggleScanned(id: Long) {
        scanned = if (id in scanned) scanned - id else scanned + id
        save()
    }

    fun showBarcodes() {
        if (filled.isEmpty()) return
        pruneBlanks()
        screen = Screen.Barcodes
        save()
    }

    fun showEntry() {
        screen = Screen.Entry
        save()
    }

    fun isDuplicate(tote: Tote): Boolean =
        tote.label.isNotBlank() && totes.count { it.label == tote.label } > 1

    private fun ensureTrailingBlank() {
        if (totes.lastOrNull()?.label?.isBlank() != true) {
            totes = totes + Tote(nextId++, "")
        }
    }

    private fun save() {
        val json = JSONObject()
            .put("nextId", nextId)
            .put("screen", screen.name)
            .put("totes", JSONArray().apply {
                totes.forEach { put(JSONObject().put("id", it.id).put("label", it.label)) }
            })
            .put("scanned", JSONArray().apply { scanned.forEach { put(it) } })
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString(KEY, null) ?: return
        runCatching {
            val json = JSONObject(raw)
            val list = json.getJSONArray("totes")
            totes = (0 until list.length()).map {
                val o = list.getJSONObject(it)
                Tote(o.getLong("id"), o.getString("label"))
            }
            val ids = json.getJSONArray("scanned")
            scanned = (0 until ids.length()).map { ids.getLong(it) }.toSet()
            nextId = maxOf(json.getLong("nextId"), (totes.maxOfOrNull { it.id } ?: 0L) + 1)
            screen = if (json.optString("screen") == Screen.Barcodes.name && filled.isNotEmpty()) {
                Screen.Barcodes
            } else {
                Screen.Entry
            }
        }
    }

    private companion object {
        const val KEY = "state"
        val SEPARATORS = Regex("[\\s,;]+")
    }
}
