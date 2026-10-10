package com.hellreaver.barcodepages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintingTest {
    private fun storeWith(vararg codes: String): ToteStore {
        val store = ToteStore()
        for (c in codes) store.edit(store.totes.last().id, c)
        return store
    }

    @Test
    fun aBlankNameUsesTheOneAbove() {
        val store = storeWith("Z13334", "Z13027", "Y98760", "TL0a1b-2")
        val (a, b, c, d) = store.filled
        store.setName(a.id, "Pat K.")
        store.setName(c.id, "Sam R.")
        assertEquals(listOf("Pat K.", "Pat K.", "Sam R.", "Sam R."), store.filled.map { store.nameFor(it) })
        assertEquals("", store.ownName(b))
        // nothing typed above: no name
        assertEquals("", storeWith("Z1").let { s -> s.nameFor(s.filled[0]) })
        // clearing a name hands its totes back to the one above
        store.setName(c.id, "")
        assertEquals("Pat K.", store.nameFor(d))
    }

    @Test
    fun namesAreTrimmedForTheLabelButKeptAsTyped() {
        val store = storeWith("Z1")
        val t = store.filled[0]
        store.setName(t.id, "  Pat ")
        assertEquals("  Pat ", store.names[t.id])
        assertEquals("Pat", store.nameFor(t))
    }

    @Test
    fun namesSurviveARestoreAndGoWithTheirTote() {
        var saved: Snapshot? = null
        val store = ToteStore(onChange = { saved = it })
        store.edit(store.totes.last().id, "Z1")
        store.edit(store.totes.last().id, "Z2")
        store.setName(store.filled[1].id, "Pat")
        store.show(Screen.Print)
        val back = ToteStore(saved)
        assertEquals(Screen.Print, back.screen)
        assertEquals(listOf("", "Pat"), back.filled.map { back.ownName(it) })
        // removing a tote drops its name
        back.remove(back.filled[1].id)
        assertTrue(back.names.isEmpty())
    }

    @Test
    fun printScreenStaysOpenWithNoTotesForATextLabel() {
        val back = ToteStore(Snapshot(labels = listOf(""), scanned = listOf(false), screen = Screen.Print))
        assertEquals(Screen.Print, back.screen)
    }

    @Test
    fun backFromPrintGoesWhereItCameFrom() {
        val store = storeWith("Z1")
        store.openPrint(Screen.Entry)
        store.closePrint()
        assertEquals(Screen.Entry, store.screen)
        store.openPrint(Screen.Barcodes)
        store.closePrint()
        assertEquals(Screen.Barcodes, store.screen)
    }

    @Test
    fun textLabelIsOnlyText() {
        val zpl = LabelZpl.textLabel("WATER", LabelZpl.DEFAULT_LABEL, fakeText, copies = 2)
        assertTrue("^BC" !in zpl)
        assertEquals(1, Regex("\\^GFA,").findAll(zpl).count())
        assertTrue(zpl.endsWith("^PQ2^XZ"))
    }

    @Test
    fun bluetoothAddressIsTheWifiStickerPlusOneInHex() {
        // made-up addresses
        assertEquals("01:23:45:67:89:AB", LabelZpl.bluetoothAddress("0123456789ab"))
        assertEquals("01:23:45:67:89:AB", LabelZpl.bluetoothFromWifi("0123456789AA"))
        assertEquals("01:23:45:67:8A:00", LabelZpl.bluetoothFromWifi("01-23-45-67-89-FF"))
        assertNull(LabelZpl.bluetoothFromWifi("not an address"))
    }

    @Test
    fun printerSizeReplyIsParsed() {
        assertEquals(LabelZpl.LabelSize(576, 603), LabelZpl.parseSize("\"576\"\r\n\"603\""))
        assertNull(LabelZpl.parseSize("\"576\""))
    }

    private val fakeText = LabelZpl.TextRenderer { text, w, h ->
        val width = minOf(w, 20 * text.length)
        val rowBytes = (width + 7) / 8
        LabelZpl.Graphic(width, h / 2, rowBytes, "00".repeat(rowBytes * (h / 2)))
    }

    @Test
    fun labelCentersAWideBarcodeUnderTheName() {
        val zpl = LabelZpl.label("Z13334", "PAT K.", LabelZpl.DEFAULT_LABEL, fakeText)
        // 101 modules at 4 dots = 404, centered on 576
        assertTrue(zpl, "^FO86," in zpl && "^BY4,3," in zpl && "^FDZ13334^FS" in zpl)
        assertEquals(2, Regex("\\^GFA,").findAll(zpl).count())
        assertTrue(zpl.startsWith("^XA") && zpl.endsWith("^XZ"))
        // no name: only the code under the barcode is drawn
        assertEquals(1, Regex("\\^GFA,").findAll(LabelZpl.label("Z13334", "", LabelZpl.DEFAULT_LABEL, fakeText)).count())
        assertTrue("^PQ3^XZ" in LabelZpl.label("Z1", "", LabelZpl.DEFAULT_LABEL, fakeText, copies = 3))
    }

    @Test
    fun qrLabelHasNoCode128AndQKeepsCodesSmall() {
        val zpl = LabelZpl.label("TL0a1b-2", "PAT", LabelZpl.DEFAULT_LABEL, fakeText, qr = LabelZpl.QrLevel.Q)
        assertTrue("^BC" !in zpl)
        assertEquals(3, Regex("\\^GFA,").findAll(zpl).count())
        assertEquals(21, LabelZpl.qrModules("TL0a1b-2", LabelZpl.QrLevel.Q))
        assertEquals(25, LabelZpl.qrModules("TL0a1b-2", LabelZpl.QrLevel.H))
    }

    /** Draws the QR picture with a white border and reads it back with ZXing. */
    private fun decodeQr(g: LabelZpl.Graphic, scale: Int): String {
        val border = 4 * scale
        val size = g.width + 2 * border
        val px = IntArray(size * size) { 0xFFFFFFFF.toInt() }
        for (y in 0 until g.height) for (x in 0 until g.width) {
            val i = (y * g.rowBytes + x / 8) * 2
            if (g.hex.substring(i, i + 2).toInt(16) and (0x80 ushr (x % 8)) != 0) {
                px[(y + border) * size + x + border] = 0xFF000000.toInt()
            }
        }
        val bitmap = com.google.zxing.BinaryBitmap(
            com.google.zxing.common.HybridBinarizer(com.google.zxing.RGBLuminanceSource(size, size, px)))
        return com.google.zxing.qrcode.QRCodeReader().decode(bitmap).text
    }

    @Test
    fun qrCodesDecodeBackToTheCode() {
        for (code in listOf("Z13334", "TL0a1b-2", "PUT 1")) for (level in LabelZpl.QrLevel.values()) {
            assertEquals("$code at $level", code, decodeQr(LabelZpl.qrGraphic(code, level, 4), 4))
        }
    }

    @Test
    fun zplCommandCharactersAreRefusedForCode128() {
        assertNull(LabelZpl.problem("Z13334"))
        assertNotNull(LabelZpl.problem("Z1^XA"))
        assertNotNull(LabelZpl.problem("Z1>3"))
    }

    @Test
    fun calibrationIsTheBlackMarkSequence() {
        assertEquals(
            "! U1 setvar \"media.type\" \"label\"\r\n! U1 setvar \"media.sense_mode\" \"bar\"\r\n~JC^XA^JUS^XZ",
            LabelZpl.CALIBRATE_MARK,
        )
    }
}
