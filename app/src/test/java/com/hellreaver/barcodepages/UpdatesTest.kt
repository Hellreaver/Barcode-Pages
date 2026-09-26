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
        {"tag_name": "v1.0.5",
         "html_url": "https://github.com/Hellreaver/Barcode-Pages/releases/tag/v1.0.5",
         "assets": [
           {"name": "notes.txt", "browser_download_url": "https://example.invalid/notes.txt"},
           {"name": "Barcode-Pages.apk",
            "browser_download_url": "https://github.com/Hellreaver/Barcode-Pages/releases/download/v1.0.5/Barcode-Pages.apk"}
         ]}
    """.trimIndent()

    @Test
    fun readsVersionAndApkLinkFromTheLatestRelease() {
        val release = Updates.parse(latest)!!
        assertEquals(5, release.versionCode)
        assertEquals("1.0.5", release.versionName)
        assertEquals(
            "https://github.com/Hellreaver/Barcode-Pages/releases/download/v1.0.5/Barcode-Pages.apk",
            release.apkUrl,
        )
    }

    @Test
    fun offersTheUpdateOnlyWhenTheReleaseIsNewer() {
        val release = Updates.parse(latest)!!
        assertEquals(UpdateState.Available(release), Updates.stateFor(release, installedCode = 3))
        assertEquals(UpdateState.UpToDate, Updates.stateFor(release, installedCode = 5))
        assertEquals(UpdateState.UpToDate, Updates.stateFor(release, installedCode = 9))
    }

    @Test
    fun releaseWithoutAnApkFallsBackToItsPage() {
        val release = Updates.parse("""{"tag_name": "v1.0.8", "html_url": "https://github.com/x", "assets": []}""")!!
        assertNull(release.apkUrl)
        assertEquals("https://github.com/x", release.pageUrl)
    }

    @Test
    fun tagWithoutANumberIsIgnored() {
        assertNull(Updates.parse("""{"tag_name": "nightly", "assets": []}"""))
    }
}
