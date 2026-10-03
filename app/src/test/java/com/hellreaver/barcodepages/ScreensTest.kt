package com.hellreaver.barcodepages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
        compose.onNodeWithTag("example-label").assertExists()
        // The trip label example starts folded and opens on tap.
        compose.onNodeWithTag("trip-example").assertDoesNotExist()
        compose.onNodeWithTag("trip-toggle").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trip-example").assertExists()
        shot("1a-trip-open")
        compose.onNodeWithTag("trip-toggle").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trip-example").assertDoesNotExist()
        compose.onNodeWithTag("next").assertIsNotEnabled()

        // The Enter key stays "Next" before and after typing. If it flips, Android restarts the
        // keyboard connection and Samsung's keyboard drops the first letter.
        compose.onNodeWithTag("tote-1").assert(hasImeAction(ImeAction.Next))
        compose.onNodeWithTag("tote-1").performTextInput("z13334")
        compose.onNodeWithTag("tote-1").assert(hasImeAction(ImeAction.Next))
        // The field holds exactly what the keyboard sent; only the display is capitalized.
        compose.onNodeWithTag("tote-1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.InputText, AnnotatedString("z13334")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Z13334")))
        assertEquals("Z13334", store.filled[0].label)
        // The example stays while the first code is being typed...
        compose.onNodeWithTag("example-label").assertExists()
        shot("1b-first-code")
        compose.onNodeWithTag("tote-2").performTextInput("Z13027")
        // ...and goes once the list moves on to the second.
        compose.onNodeWithTag("example-label").assertDoesNotExist()
        compose.onNodeWithTag("tote-3").performTextInput("y98760")
        compose.onNodeWithTag("tote-4").performTextInput("y98760")
        // A trip label typed in capitals keeps its lowercase trip id, as trip labels encode it.
        compose.onNodeWithTag("tote-5").performTextInput("TL0A1B-2")
        assertEquals("TL0a1b-2", store.filled[4].label)
        compose.onNodeWithTag("tote-6").assertExists()
        compose.onNodeWithTag("next").assertIsEnabled()
        shot("2-entered")

        compose.onNodeWithTag("next").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Barcodes, store.screen)
        shot("3-barcodes")
        compose.onNodeWithTag("barcodes").performScrollToIndex(4)
        compose.waitForIdle()
        shot("3b-trip-barcode")
        compose.onNodeWithTag("barcodes").performScrollToIndex(0)
        compose.waitForIdle()

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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-420dpi")
class LockedScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lockedScreensReplaceTheApp() {
        val store = ToteStore()
        var lock by mutableStateOf<Lock>(Lock.TurnedOff("Paused until Monday."))
        compose.setContent { ToteApp(store, autoFocus = false, lock = lock) }
        compose.onNodeWithTag("locked").assertExists()
        compose.onNodeWithText("Paused until Monday.").assertExists()
        compose.onNodeWithTag("next").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/screenshots/locked-turned-off.png")

        lock = Lock.NoContact
        compose.waitForIdle()
        compose.onNodeWithText("Can't reach GitHub").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/locked-no-contact.png")

        lock = Lock.Open
        compose.waitForIdle()
        compose.onNodeWithTag("locked").assertDoesNotExist()
        compose.onNodeWithTag("next").assertExists()
    }
}

@Config(qualifiers = "w411dp-h914dp-420dpi")
class Pixel8aScreensTest : ScreensTestBase("pixel-8a")

@Config(qualifiers = "w448dp-h997dp-xxhdpi")
class Pixel8ProScreensTest : ScreensTestBase("pixel-8-pro")
