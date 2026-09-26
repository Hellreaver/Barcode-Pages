package com.hellreaver.totebarcodes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToteStoreTest {
    private fun ToteStore.labels() = totes.map { it.label }

    @Test
    fun typingInTheLastRowAddsAnotherBlankRow() {
        val store = ToteStore()
        assertEquals(listOf(""), store.labels())
        store.edit(store.totes.last().id, "e")
        assertEquals(listOf("e", ""), store.labels())
        store.edit(store.totes[0].id, "e3397")
        assertEquals(listOf("e3397", ""), store.labels())
        store.edit(store.totes.last().id, "2965")
        assertEquals(listOf("e3397", "2965", ""), store.labels())
    }

    @Test
    fun pastedListSplitsIntoRowsAndFocusMovesPastIt() {
        val store = ToteStore()
        val focus = store.edit(store.totes.last().id, "e3397 2965,e22560\n")
        assertEquals(listOf("e3397", "2965", "e22560", ""), store.labels())
        assertEquals(store.totes.last().id, focus)
    }

    @Test
    fun freshStoreStartsEmpty() {
        val first = ToteStore()
        first.edit(first.totes.last().id, "e3397")
        assertEquals(listOf(""), ToteStore().labels())
    }

    @Test
    fun snapshotRestoresListMarksAndScreen() {
        var saved: Snapshot? = null
        val first = ToteStore { saved = it }
        first.edit(first.totes.last().id, "e3397")
        first.edit(first.totes.last().id, "2965")
        first.toggleScanned(first.totes[1].id)
        first.showBarcodes()

        val second = ToteStore(saved)
        assertEquals(listOf("e3397", "2965", ""), second.labels())
        assertEquals(setOf(second.totes[1].id), second.scanned)
        assertEquals(Screen.Barcodes, second.screen)
        second.edit(second.totes.last().id, "e22560")
        assertEquals(second.totes.size, second.totes.map { it.id }.toSet().size)
    }

    @Test
    fun editingALabelClearsItsScannedMark() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "e3397")
        val id = store.totes[0].id
        store.toggleScanned(id)
        store.edit(id, "e3398")
        assertFalse(id in store.scanned)
    }

    @Test
    fun blankMiddleRowsArePrunedButTheTrailingOneStays() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "a")
        store.edit(store.totes.last().id, "b")
        store.edit(store.totes[0].id, "")
        assertEquals(listOf("", "b", ""), store.labels())
        store.pruneBlanks()
        assertEquals(listOf("b", ""), store.labels())
    }

    @Test
    fun nextNeedsOneToteAndClearStartsOver() {
        val store = ToteStore()
        store.showBarcodes()
        assertEquals(Screen.Entry, store.screen)
        store.edit(store.totes.last().id, "e3397")
        store.showBarcodes()
        assertEquals(Screen.Barcodes, store.screen)
        store.clearAll()
        assertEquals(listOf(""), store.labels())
        assertEquals(Screen.Entry, store.screen)
        assertTrue(store.scanned.isEmpty())
    }

    @Test
    fun duplicatesAreFlagged() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "e3397 e3397 2965")
        assertTrue(store.isDuplicate(store.totes[0]))
        assertFalse(store.isDuplicate(store.totes[2]))
    }
}
