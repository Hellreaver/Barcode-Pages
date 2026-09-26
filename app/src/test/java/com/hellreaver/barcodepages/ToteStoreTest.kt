package com.hellreaver.barcodepages

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
        store.edit(store.totes.last().id, "E")
        assertEquals(listOf("E", ""), store.labels())
        store.edit(store.totes[0].id, "Z13334")
        assertEquals(listOf("Z13334", ""), store.labels())
        store.edit(store.totes.last().id, "Z13027")
        assertEquals(listOf("Z13334", "Z13027", ""), store.labels())
    }

    @Test
    fun pastedListSplitsIntoRowsAndFocusMovesPastIt() {
        val store = ToteStore()
        val focus = store.edit(store.totes.last().id, "Z13334 Z13027,Y98760\n")
        assertEquals(listOf("Z13334", "Z13027", "Y98760", ""), store.labels())
        assertEquals(store.totes.last().id, focus)
    }

    @Test
    fun freshStoreStartsEmpty() {
        val first = ToteStore()
        first.edit(first.totes.last().id, "z13334")
        assertEquals(listOf(""), ToteStore().labels())
    }

    @Test
    fun snapshotRestoresListMarksAndScreen() {
        var saved: Snapshot? = null
        val first = ToteStore { saved = it }
        first.edit(first.totes.last().id, "Z13334")
        first.edit(first.totes.last().id, "Z13027")
        first.toggleScanned(first.totes[1].id)
        first.showBarcodes()

        val second = ToteStore(saved)
        assertEquals(listOf("Z13334", "Z13027", ""), second.labels())
        assertEquals(setOf(second.totes[1].id), second.scanned)
        assertEquals(Screen.Barcodes, second.screen)
        second.edit(second.totes.last().id, "y98760")
        assertEquals(second.totes.size, second.totes.map { it.id }.toSet().size)
    }

    @Test
    fun lowercaseTypingAndPastingBecomesCapitals() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "z13334")
        assertEquals(listOf("Z13334", ""), store.labels())
        store.edit(store.totes.last().id, "y98760 tote-ab")
        assertEquals(listOf("Z13334", "Y98760", "TOTE-AB", ""), store.labels())
    }

    @Test
    fun typedTextIsKeptAsTypedAndOnlyTheLabelIsCapitalized() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "z13334")
        assertEquals("z13334", store.totes[0].text)
        assertEquals("Z13334", store.totes[0].label)
        // One char in, one char out, so the on-screen cursor never drifts.
        assertEquals(1, "\u00DF".labelCase().length)
    }

    @Test
    fun tripLabelsKeepTheirLowercaseTripId() {
        // A trip label's barcode holds its code as printed, lowercase letters included: "TL0a1b-2".
        assertEquals("TL0a1b-2", "TL0a1b-2".labelCase())
        assertEquals("TL0a1b-2", "TL0A1B-2".labelCase())
        assertEquals("TL0a1b-2", "tl0a1b-2".labelCase())
        // More trip labels in the same format.
        assertEquals("TL0a1b-1", "TL0A1B-1".labelCase())
        assertEquals("TL4821-1", "tl4821-1".labelCase())
        // Tote codes and anything else are capitals.
        assertEquals("Z13334", "z13334".labelCase())
        assertEquals("Y98760", "y98760".labelCase())
        assertEquals("T12345", "t12345".labelCase())
        assertEquals("T", "t".labelCase())
    }

    @Test
    fun changingOnlyTheCaseKeepsTheScannedMark() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "z13334")
        val id = store.totes[0].id
        store.toggleScanned(id)
        store.edit(id, "Z13334")
        assertTrue(id in store.scanned)
    }

    @Test
    fun editingALabelClearsItsScannedMark() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "z13334")
        val id = store.totes[0].id
        store.toggleScanned(id)
        store.edit(id, "z13335")
        assertFalse(id in store.scanned)
    }

    @Test
    fun blankMiddleRowsArePrunedButTheTrailingOneStays() {
        val store = ToteStore()
        store.edit(store.totes.last().id, "a")
        store.edit(store.totes.last().id, "b")
        store.edit(store.totes[0].id, "")
        assertEquals(listOf("", "B", ""), store.labels())
        store.pruneBlanks()
        assertEquals(listOf("B", ""), store.labels())
    }

    @Test
    fun nextNeedsOneToteAndClearStartsOver() {
        val store = ToteStore()
        store.showBarcodes()
        assertEquals(Screen.Entry, store.screen)
        store.edit(store.totes.last().id, "z13334")
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
        store.edit(store.totes.last().id, "z13334 z13334 Z13027")
        assertTrue(store.isDuplicate(store.totes[0]))
        assertFalse(store.isDuplicate(store.totes[2]))
    }
}
