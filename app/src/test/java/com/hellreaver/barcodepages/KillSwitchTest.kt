package com.hellreaver.barcodepages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies org.json and SharedPreferences.
@RunWith(RobolectricTestRunner::class)
class KillSwitchTest {
    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_800_000_000_000L
    private val on = SwitchAnswer(true, "")

    @Test
    fun freshInstallRunsForSevenDaysWithoutContact() {
        val s = SwitchState(firstSeen = t0, lastOk = null, answer = null)
        assertEquals(Lock.Open, KillSwitch.lockFor(s, t0))
        assertEquals(Lock.Open, KillSwitch.lockFor(s, t0 + 7 * day))
        assertEquals(Lock.NoContact, KillSwitch.lockFor(s, t0 + 7 * day + 1))
    }

    @Test
    fun eachGoodCheckRestartsTheWeek() {
        var s = SwitchState(firstSeen = t0, lastOk = null, answer = null)
        s = KillSwitch.afterCheck(s, on, t0 + 6 * day)
        assertEquals(Lock.Open, KillSwitch.lockFor(s, t0 + 12 * day))
        assertEquals(Lock.NoContact, KillSwitch.lockFor(s, t0 + 14 * day))
        // A failed check changes nothing.
        assertEquals(s, KillSwitch.afterCheck(s, null, t0 + 13 * day))
    }

    @Test
    fun ownerTurnsItOffAndBackOn() {
        var s = KillSwitch.afterCheck(SwitchState(t0, null, null), SwitchAnswer(false, "Paused for now"), t0)
        assertEquals(Lock.TurnedOff("Paused for now"), KillSwitch.lockFor(s, t0 + 1))
        // Off stays off without signal, even within the week.
        assertEquals(Lock.TurnedOff("Paused for now"), KillSwitch.lockFor(KillSwitch.afterCheck(s, null, t0 + day), t0 + day))
        s = KillSwitch.afterCheck(s, on, t0 + 2 * day)
        assertEquals(Lock.Open, KillSwitch.lockFor(s, t0 + 2 * day))
    }

    @Test
    fun clockRolledBackCountsAsNoContact() {
        val s = KillSwitch.afterCheck(SwitchState(t0, null, null), on, t0 + 3 * day)
        assertEquals(Lock.Open, KillSwitch.lockFor(s, t0 + 3 * day - day / 2))
        assertEquals(Lock.NoContact, KillSwitch.lockFor(s, t0 + 3 * day - 2 * day))
    }

    @Test
    fun readsStatusJson() {
        assertEquals(SwitchAnswer(true, ""), KillSwitch.parse("""{"enabled": true, "message": ""}"""))
        assertEquals(SwitchAnswer(false, "Back Monday"), KillSwitch.parse("""{"enabled": false, "message": " Back Monday "}"""))
        assertEquals(SwitchAnswer(true, ""), KillSwitch.parse("{}"))
        // A GitHub error page or anything else that isn't a JSON object is no answer at all.
        assertNull(KillSwitch.parse("404: Not Found"))
        assertNull(KillSwitch.parse(""))
    }

    @Test
    fun theRepoFileParsesAsOn() {
        val text = java.io.File("../status.json").readText()
        assertEquals(SwitchAnswer(true, ""), KillSwitch.parse(text))
    }

    @Test
    fun storedClockSurvivesARestart() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("ks-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        var clock = t0
        val first = SwitchStore(prefs) { clock }.load()
        assertEquals(SwitchState(t0, null, null), first)
        val checked = KillSwitch.afterCheck(first, SwitchAnswer(false, "x"), t0 + day)
        SwitchStore(prefs) { clock }.save(checked)
        clock = t0 + 5 * day
        // A new launch reads the same first-seen time, last check and answer.
        assertEquals(checked, SwitchStore(prefs) { clock }.load())
    }
}
