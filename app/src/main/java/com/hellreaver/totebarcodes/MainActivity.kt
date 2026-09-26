package com.hellreaver.totebarcodes

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel

class ToteViewModel(app: Application) : AndroidViewModel(app) {
    val store = ToteStore(app.getSharedPreferences("totes", Context.MODE_PRIVATE))
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
        }
    }
}
