package com.hellreaver.barcodepages

import com.google.zxing.EncodeHintType
import com.google.zxing.oned.Code128Writer

/**
 * Code 128 encoding and pixel layout. Code 128 carries upper and lower case letters and
 * digits, so a tote code such as "Z13334" scans back as exactly "Z13334".
 */
object Barcode {
    /** Blank modules kept on each side. The Code 128 spec asks for at least 10. */
    const val QUIET_MODULES = 10

    fun isEncodable(text: String): Boolean =
        text.isNotEmpty() && text.all { it.code in 32..126 }

    /** One entry per module, true = black bar. Null when the text can't be encoded. */
    fun modules(text: String): BooleanArray? {
        if (!isEncodable(text)) return null
        // COMPACT picks code sets B and C for the fewest symbols, which gives the widest bars.
        return runCatching {
            Code128Writer().encode(text, mapOf(EncodeHintType.CODE128_COMPACT to true))
        }.getOrNull()
    }

    /**
     * Whole-pixel module width, so every bar lands on device pixels with no blurred edges.
     * [left] is the pixel offset of the first module.
     */
    data class Layout(val modulePx: Int, val left: Int)

    fun layout(widthPx: Int, moduleCount: Int): Layout {
        val modulePx = maxOf(1, widthPx / (moduleCount + 2 * QUIET_MODULES))
        return Layout(modulePx, (widthPx - modulePx * moduleCount) / 2)
    }

    /** Runs of black modules as [start, endExclusive) module index pairs. */
    fun bars(modules: BooleanArray): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (i < modules.size) {
            if (modules[i]) {
                val start = i
                while (i < modules.size && modules[i]) i++
                out += start to i
            } else {
                i++
            }
        }
        return out
    }

    /** ARGB pixels, black bars on white, for tests that feed the image to a decoder. */
    fun render(text: String, widthPx: Int, heightPx: Int): IntArray? {
        val modules = modules(text) ?: return null
        val (modulePx, left) = layout(widthPx, modules.size)
        val row = IntArray(widthPx) { 0xFFFFFFFF.toInt() }
        for ((start, end) in bars(modules)) {
            for (x in left + start * modulePx until left + end * modulePx) row[x] = 0xFF000000.toInt()
        }
        val pixels = IntArray(widthPx * heightPx)
        for (y in 0 until heightPx) row.copyInto(pixels, y * widthPx)
        return pixels
    }
}
