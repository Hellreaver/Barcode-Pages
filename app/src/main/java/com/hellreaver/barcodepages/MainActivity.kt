package com.hellreaver.barcodepages

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Holds the list in memory. SavedStateHandle brings it back if Android kills the app while
 * it sits in the background; swiping the app away or relaunching it starts a blank list.
 */
class ToteViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
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

    var update by mutableStateOf<UpdateState>(UpdateState.Checking)
        private set

    private val switchStore = SwitchStore(app.getSharedPreferences("killswitch", Context.MODE_PRIVATE))
    private var switchState = switchStore.load()
    private var lastCheckIn = 0L

    /** Worked out from what's stored, so a phone with no signal locks or opens without waiting. */
    var lock by mutableStateOf(KillSwitch.lockFor(switchState, System.currentTimeMillis()))
        private set
    var checkingIn by mutableStateOf(false)
        private set

    init {
        checkForUpdate()
    }

    /** Asks GitHub's status.json whether the app may run. Runs on every start, at most hourly. */
    fun checkIn(force: Boolean = false) {
        val now = System.currentTimeMillis()
        lock = KillSwitch.lockFor(switchState, now)
        if (checkingIn || (!force && now - lastCheckIn < CHECK_IN_EVERY_MS)) return
        lastCheckIn = now
        checkingIn = true
        viewModelScope.launch {
            val answer = KillSwitch.fetch()
            val at = System.currentTimeMillis()
            switchState = KillSwitch.afterCheck(switchState, answer, at)
            if (answer != null) switchStore.save(switchState)
            lock = KillSwitch.lockFor(switchState, at)
            checkingIn = false
        }
    }

    fun checkForUpdate() {
        update = UpdateState.Checking
        viewModelScope.launch { update = Updates.check(BuildConfig.VERSION_CODE) }
    }

    private companion object {
        const val LABELS = "labels"
        const val SCANNED = "scanned"
        const val SCREEN = "screen"
        const val CHECK_IN_EVERY_MS = 60L * 60 * 1000
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
        setContent {
            ToteApp(
                store = model.store,
                update = model.update,
                onCheckUpdate = model::checkForUpdate,
                lock = model.lock,
                checkingIn = model.checkingIn,
                onRetryLock = { model.checkIn(force = true) },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        model.checkIn()
    }
}

@Composable
fun ToteApp(
    store: ToteStore,
    update: UpdateState = UpdateState.UpToDate,
    onCheckUpdate: () -> Unit = {},
    autoFocus: Boolean = true,
    lock: Lock = Lock.Open,
    checkingIn: Boolean = false,
    onRetryLock: () -> Unit = {},
) {
    LemonTheme {
        if (lock != Lock.Open) {
            LockedScreen(lock, checkingIn, onRetryLock)
            return@LemonTheme
        }
        when (store.screen) {
            Screen.Entry -> EntryScreen(store, update is UpdateState.Available, autoFocus)
            Screen.Barcodes -> BarcodeScreen(store)
            Screen.Share -> ShareScreen(store, update, onCheckUpdate)
        }
    }
}
