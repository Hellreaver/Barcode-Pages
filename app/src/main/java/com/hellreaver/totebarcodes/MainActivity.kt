package com.hellreaver.totebarcodes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Holds the list in memory. SavedStateHandle brings it back if Android kills the app while
 * it sits in the background; swiping the app away or relaunching it starts a blank list.
 */
class ToteViewModel(handle: SavedStateHandle) : ViewModel() {
    val store = ToteStore(
        restore = handle.get<ArrayList<String>>(LABELS)?.let { labels ->
            Snapshot(
                labels = labels,
                scanned = handle.get<BooleanArray>(SCANNED)?.toList().orEmpty(),
                screen = runCatching { Screen.valueOf(handle.get<String>(SCREEN)!!) }.getOrDefault(Screen.Entry),
            )
        },
        onChange = { snap ->
            handle[LABELS] = ArrayList(snap.labels)
            handle[SCANNED] = snap.scanned.toBooleanArray()
            handle[SCREEN] = snap.screen.name
        },
    )

    private companion object {
        const val LABELS = "labels"
        const val SCANNED = "scanned"
        const val SCREEN = "screen"
    }
}

class MainActivity : ComponentActivity() {
    private val model: ToteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { ToteApp(model.store) }
    }
}

@Composable
fun ToteApp(store: ToteStore, autoFocus: Boolean = true) {
    LemonTheme {
        when (store.screen) {
            Screen.Entry -> EntryScreen(store, autoFocus)
            Screen.Barcodes -> BarcodeScreen(store)
            Screen.Share -> ShareScreen(store)
        }
    }
}
