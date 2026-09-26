package com.hellreaver.barcodepages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensTestBase(private val device: String) {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/screenshots/$device-$name.png")

    @Test
    fun enterTotesShowBarcodesAndShare() {
        val store = ToteStore()
        var update by mutableStateOf<UpdateState>(UpdateState.UpToDate)
        compose.setContent { ToteApp(store, update, autoFocus = false) }
        shot("1-empty")
        compose.onNodeWithTag("next").assertIsNotEnabled()

        compose.onNodeWithTag("tote-1").performTextInput("e3397")
        compose.onNodeWithTag("tote-2").performTextInput("2965")
        compose.onNodeWithTag("tote-3").performTextInput("e22560")
        compose.onNodeWithTag("tote-4").performTextInput("e22560")
        compose.onNodeWithTag("tote-5").assertExists()
        compose.onNodeWithTag("next").assertIsEnabled()
        shot("2-entered")

        compose.onNodeWithTag("next").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Barcodes, store.screen)
        shot("3-barcodes")

        compose.onAllNodesWithText("Mark scanned")[0].performClick()
        compose.waitForIdle()
        assertEquals(1, store.scanned.size)
        shot("4-marked")

        store.show(Screen.Entry)
        compose.onNodeWithText("Share").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Share, store.screen)
        shot("5-share")

        // A newer release turns the Share chip into a highlighted Update chip.
        update = UpdateState.Available(Release(99, "1.0.99", "https://example.invalid/Barcode-Pages-1.0.99.apk", RELEASES_URL))
        store.show(Screen.Entry)
        compose.waitForIdle()
        compose.onNodeWithText("Update").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Update to 1.0.99").assertExists()
        shot("6-update")
    }
}

@Config(qualifiers = "w411dp-h914dp-420dpi")
class Pixel8aScreensTest : ScreensTestBase("pixel-8a")

@Config(qualifiers = "w448dp-h997dp-xxhdpi")
class Pixel8ProScreensTest : ScreensTestBase("pixel-8-pro")
