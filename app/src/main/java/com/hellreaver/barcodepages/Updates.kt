package com.hellreaver.barcodepages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * A published GitHub release. Tags are v1.15, v1.16 ... (versionCode 115, 116 ...); the older
 * v1.0.1 to v1.0.14 tags carry their versionCode as the last number.
 */
data class Release(val versionCode: Int, val versionName: String, val apkUrl: String?, val pageUrl: String)

sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

object Updates {
    const val LATEST_API = "https://api.github.com/repos/Hellreaver/Barcode-Pages/releases/latest"

    /** Reads the GitHub "latest release" JSON. Null when the tag has no version number. */
    fun parse(json: String): Release? {
        val o = JSONObject(json)
        val tag = o.optString("tag_name")
        val code = versionCodeOf(tag) ?: return null
        val assets = o.optJSONArray("assets")
        val apk = assets?.let { list ->
            (0 until list.length()).map { list.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
                ?.optString("browser_download_url")
        }
        return Release(code, tag.removePrefix("v"), apk, o.optString("html_url", RELEASES_URL))
    }

    /** "v1.15" is 115 and "v2.00" is 200; an old three-part tag such as "v1.0.14" is 14. */
    fun versionCodeOf(tag: String): Int? {
        Regex("^v?(\\d+)\\.(\\d{2})$").find(tag)?.let { m ->
            return m.groupValues[1].toInt() * 100 + m.groupValues[2].toInt()
        }
        return Regex("^v?\\d+\\.\\d+\\.(\\d+)$").find(tag)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun stateFor(release: Release, installedCode: Int): UpdateState =
        if (release.versionCode > installedCode) UpdateState.Available(release) else UpdateState.UpToDate

    suspend fun check(installedCode: Int): UpdateState = withContext(Dispatchers.IO) {
        try {
            val conn = URL(LATEST_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                when (val status = conn.responseCode) {
                    200 -> {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        parse(body)?.let { stateFor(it, installedCode) }
                            ?: UpdateState.Failed("The newest release has no version number.")
                    }
                    404 -> UpdateState.Failed(
                        "GitHub answered 404. That happens while the repository is private; " +
                            "the releases page still works in Chrome if you're logged in.",
                    )
                    else -> UpdateState.Failed("GitHub answered HTTP $status.")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: java.io.IOException) {
            UpdateState.Failed("No connection to GitHub.")
        }
    }
}
