package com.hellreaver.barcodepages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real org.json that Android ships.
@RunWith(RobolectricTestRunner::class)
class UpdatesTest {
    private val latest = """
        {"tag_name": "v1.15",
         "html_url": "https://github.com/Hellreaver/Barcode-Pages/releases/tag/v1.15",
         "assets": [
           {"name": "notes.txt", "browser_download_url": "https://example.invalid/notes.txt"},
           {"name": "Barcode-Pages-1.15.apk",
            "browser_download_url": "https://github.com/Hellreaver/Barcode-Pages/releases/download/v1.15/Barcode-Pages-1.15.apk"}
         ]}
    """.trimIndent()

    @Test
    fun readsVersionAndApkLinkFromTheLatestRelease() {
        val release = Updates.parse(latest)!!
        assertEquals(115, release.versionCode)
        assertEquals("1.15", release.versionName)
        assertEquals(
            "https://github.com/Hellreaver/Barcode-Pages/releases/download/v1.15/Barcode-Pages-1.15.apk",
            release.apkUrl,
        )
    }

    @Test
    fun offersTheUpdateOnlyWhenTheReleaseIsNewer() {
        val release = Updates.parse(latest)!!
        // Phones on the old 1.0.x builds (codes 1 to 14) are offered 1.15.
        assertEquals(UpdateState.Available(release), Updates.stateFor(release, installedCode = 14))
        assertEquals(UpdateState.Available(release), Updates.stateFor(release, installedCode = 114))
        assertEquals(UpdateState.UpToDate, Updates.stateFor(release, installedCode = 115))
        assertEquals(UpdateState.UpToDate, Updates.stateFor(release, installedCode = 120))
    }

    @Test
    fun readsBothVersionStyles() {
        assertEquals(115, Updates.versionCodeOf("v1.15"))
        assertEquals(109, Updates.versionCodeOf("v1.09"))
        assertEquals(120, Updates.versionCodeOf("v1.20"))
        assertEquals(200, Updates.versionCodeOf("v2.00"))
        assertEquals(14, Updates.versionCodeOf("v1.0.14"))
        assertEquals(null, Updates.versionCodeOf("nightly"))
    }

    @Test
    fun releaseWithoutAnApkFallsBackToItsPage() {
        val release = Updates.parse("""{"tag_name": "v1.18", "html_url": "https://github.com/x", "assets": []}""")!!
        assertNull(release.apkUrl)
        assertEquals("https://github.com/x", release.pageUrl)
    }

    @Test
    fun tagWithoutANumberIsIgnored() {
        assertNull(Updates.parse("""{"tag_name": "nightly", "assets": []}"""))
    }
}
