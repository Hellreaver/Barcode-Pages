package com.hellreaver.barcodepages

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeTest {
    private fun decode(text: String, widthPx: Int): String {
        val pixels = Barcode.render(text, widthPx, 60) ?: error("not encodable: $text")
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(widthPx, 60, pixels)))
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_128))
        val result = MultiFormatReader().decode(bitmap, hints)
        assertEquals(BarcodeFormat.CODE_128, result.barcodeFormat)
        return result.text
    }

    @Test
    fun toteLabelsDecodeToTheSameText() {
        // Plate widths in device pixels: Pixel 8a (363dp x 2.625), Pixel 8 Pro (400dp x 2.25 and x 3).
        for (width in listOf(953, 900, 1200)) {
            // Tote codes in the label format, plus lowercase and odd lengths.
            for (label in listOf("Z13334", "Y98760", "Z13027", "Z133340", "Z99999", "TL0a1b-1", "TL0a1b-2", "TL4821-1", "e3397", "2965", "A1", "7", "tote-0042", "x")) {
                assertEquals(label, decode(label, width))
            }
        }
    }

    @Test
    fun everyPrintableCharacterRoundTrips() {
        val all = (32..126).map { it.toChar() }.joinToString("")
        for (chunk in all.chunked(12)) {
            val label = chunk.trim().ifEmpty { "a b" }
            assertEquals(label, decode(label, 1600))
        }
    }

    @Test
    fun allDigitPairsRoundTrip() {
        // Covers every code set C symbol, 00 through 99.
        val digits = (0..99).joinToString("") { it.toString().padStart(2, '0') }
        for (chunk in digits.chunked(20)) assertEquals(chunk, decode(chunk, 1600))
    }

    @Test
    fun barsLandOnWholePixelsWithQuietZones() {
        val modules = Barcode.modules("e22560")!!
        val layout = Barcode.layout(953, modules.size)
        assertTrue("module ${layout.modulePx}px", layout.modulePx >= 5)
        assertTrue(layout.left >= Barcode.QUIET_MODULES * layout.modulePx)
        val right = 953 - layout.left - modules.size * layout.modulePx
        assertTrue(right >= Barcode.QUIET_MODULES * layout.modulePx)
    }

    @Test
    fun rejectsTextThatCannotBeEncoded() {
        assertFalse(Barcode.isEncodable(""))
        assertFalse(Barcode.isEncodable("café"))
        assertNull(Barcode.modules("tote—"))
    }
}
