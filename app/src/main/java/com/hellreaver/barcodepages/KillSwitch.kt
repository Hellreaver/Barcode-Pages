package com.hellreaver.barcodepages

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** What status.json on GitHub said at the last successful check. */
data class SwitchAnswer(val enabled: Boolean, val message: String)

/**
 * Stored on the phone between launches. [firstSeen] starts the clock for a copy that has never
 * reached GitHub; [lastOk] is the last time status.json was read.
 */
data class SwitchState(val firstSeen: Long, val lastOk: Long?, val answer: SwitchAnswer?)

sealed interface Lock {
    data object Open : Lock
    /** status.json says enabled: false. */
    data class TurnedOff(val message: String) : Lock
    /** No good answer from GitHub for longer than [KillSwitch.MAX_SILENCE_MS]. */
    data object NoContact : Lock
}

/**
 * The owner's off switch. The app reads status.json from the repo's main branch: "enabled": false
 * locks every copy at its next check, and a copy that hasn't read the file for 7 days locks
 * itself until it can, so going offline, or the repo going private or away, also ends it.
 */
object KillSwitch {
    const val STATUS_URL = "https://raw.githubusercontent.com/Hellreaver/Barcode-Pages/main/status.json"
    const val MAX_SILENCE_MS = 7L * 24 * 60 * 60 * 1000
    /** A phone clock this far behind the last check counts as rolled back. */
    const val CLOCK_SLACK_MS = 24L * 60 * 60 * 1000

    /** Null when the text isn't a JSON object, which counts as no answer. */
    fun parse(json: String): SwitchAnswer? = runCatching {
        val o = JSONObject(json)
        SwitchAnswer(enabled = o.optBoolean("enabled", true), message = o.optString("message", "").trim())
    }.getOrNull()

    fun lockFor(state: SwitchState, now: Long): Lock {
        state.answer?.let { if (!it.enabled) return Lock.TurnedOff(it.message) }
        val since = state.lastOk ?: state.firstSeen
        if (now < since - CLOCK_SLACK_MS) return Lock.NoContact
        if (now - since > MAX_SILENCE_MS) return Lock.NoContact
        return Lock.Open
    }

    /** Folds a check's result into the stored state. A failed check (null) changes nothing. */
    fun afterCheck(state: SwitchState, answer: SwitchAnswer?, now: Long): SwitchState =
        if (answer == null) state else state.copy(lastOk = now, answer = answer)

    suspend fun fetch(): SwitchAnswer? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$STATUS_URL?t=${System.currentTimeMillis() / 60_000}").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.useCaches = false
            try {
                if (conn.responseCode != 200) return@withContext null
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        } catch (e: java.io.IOException) {
            null
        }
    }
}

/** Keeps [SwitchState] in SharedPreferences, so the 7-day clock survives restarts. */
class SwitchStore(private val prefs: SharedPreferences, private val now: () -> Long = System::currentTimeMillis) {
    fun load(): SwitchState {
        if (!prefs.contains(FIRST_SEEN)) prefs.edit().putLong(FIRST_SEEN, now()).apply()
        val answer = if (prefs.contains(ENABLED)) {
            SwitchAnswer(prefs.getBoolean(ENABLED, true), prefs.getString(MESSAGE, "").orEmpty())
        } else {
            null
        }
        return SwitchState(
            firstSeen = prefs.getLong(FIRST_SEEN, now()),
            lastOk = if (prefs.contains(LAST_OK)) prefs.getLong(LAST_OK, 0) else null,
            answer = answer,
        )
    }

    fun save(state: SwitchState) {
        prefs.edit().apply {
            putLong(FIRST_SEEN, state.firstSeen)
            state.lastOk?.let { putLong(LAST_OK, it) }
            state.answer?.let {
                putBoolean(ENABLED, it.enabled)
                putString(MESSAGE, it.message)
            }
        }.apply()
    }

    private companion object {
        const val FIRST_SEEN = "first_seen"
        const val LAST_OK = "last_ok"
        const val ENABLED = "enabled"
        const val MESSAGE = "message"
    }
}
