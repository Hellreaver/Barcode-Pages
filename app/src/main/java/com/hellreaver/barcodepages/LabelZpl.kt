package com.hellreaver.barcodepages

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlin.math.roundToInt

/**
 * Tote codes and names in, ZPL for a Zebra label printer out. No Android code here, so the
 * unit tests cover all of it. Built for a ZQ620 (203 dpi, 8 dots per mm) loaded with
 * 72 x 75 mm black-mark labels that have printed bands top and bottom and a peel strip down
 * one side.
 */
object LabelZpl {
    /** Label size in dots. The 72 x 75 mm stock is 576 x 603. */
    data class LabelSize(val width: Int, val length: Int)

    val DEFAULT_LABEL = LabelSize(576, 603)

    /** Asks the printer for its print width and label length (Zebra SGD); one quoted reply each. */
    const val SIZE_QUERY = "! U1 getvar \"ezpl.print_width\"\r\n! U1 getvar \"zpl.label_length\"\r\n"

    /** `"576""603"` -> 576 x 603. Null until both numbers are in and look like a label. */
    fun parseSize(reply: String): LabelSize? {
        val n = Regex("\"(\\d+)\"").findAll(reply).map { it.groupValues[1].toInt() }.toList()
        if (n.size < 2) return null
        val (w, l) = n
        return if (w in 100..2000 && l in 100..32000) LabelSize(w, l) else null
    }

    /**
     * Zebra's media calibration for black-mark labels: label media, mark sensing, then ~JC
     * measures the label length and ^JUS saves it. Zebra says to turn the printer off and on
     * afterwards. Fixes a printer that cuts every label short (one reported 239 dots for this
     * 603-dot stock until it was calibrated).
     */
    const val CALIBRATE_MARK = "! U1 setvar \"media.type\" \"label\"\r\n" +
        "! U1 setvar \"media.sense_mode\" \"bar\"\r\n" +
        "~JC^XA^JUS^XZ"

    /** Prints the printer's own settings label. */
    const val CONFIG_LABEL = "~WC"

    /** Why a code can't go in a Code 128 ^FD field, or null when it can. */
    fun problem(code: String): String? = when {
        code.any { it.code !in 32..126 } -> "$code has a character Code 128 can't carry"
        code.any { it == '^' || it == '~' } -> "$code has ^ or ~, which ZPL reads as a command"
        '>' in code -> "$code has >, which ZPL's Code 128 reads as a code-set switch"
        else -> null
    }

    // ---- Printer address ----

    /** "0123456789AB" in any of 12-hex-digit, dash or colon form -> "XX:XX:…". Null otherwise. */
    fun bluetoothAddress(text: String): String? {
        val hex = text.filter { it.isLetterOrDigit() }.uppercase()
        if (hex.length != 12 || hex.any { it !in "0123456789ABCDEF" }) return null
        return hex.chunked(2).joinToString(":")
    }

    /**
     * The printer's Bluetooth address from the Wi-Fi address on the sticker on its back: that
     * number plus one, counted in hex (…12 -> …13, …4A -> …4B). Held on every printer checked;
     * the printer's Bluetooth settings screen shows the real one as BT Addr.
     */
    fun bluetoothFromWifi(sticker: String): String? {
        val wifi = bluetoothAddress(sticker) ?: return null
        val next = (wifi.replace(":", "").toLong(16) + 1) and 0xFFFFFFFFFFFFL
        return bluetoothAddress("%012X".format(next))
    }

    // ---- Layout ----

    /** A 1-bit picture for ^GFA: [rowBytes] bytes per row, [hex] is every row top to bottom. */
    class Graphic(val width: Int, val height: Int, val rowBytes: Int, val hex: String) {
        val bytes: Int get() = rowBytes * height
    }

    /** Draws text as large as fits in maxWidth x maxHeight dots. Android's is LabelText. */
    fun interface TextRenderer {
        fun render(text: String, maxWidth: Int, maxHeight: Int): Graphic?
    }

    /** Code 128 width in modules as ^BC mode N draws it: 11 per character plus 35. */
    fun code128Modules(code: String): Int = 11 * code.length + 35

    /** Widest bar module (1-10 dots) whose bars plus a 10-module quiet zone each side fit [full]. */
    fun barModule(code: String, full: Int): Int = (full / (code128Modules(code) + 20)).coerceIn(1, 10)

    /** QR error correction, by how much of the code can be lost and still scan. */
    enum class QrLevel(val percent: Int, val zxing: ErrorCorrectionLevel) {
        L(7, ErrorCorrectionLevel.L), M(15, ErrorCorrectionLevel.M),
        Q(25, ErrorCorrectionLevel.Q), H(30, ErrorCorrectionLevel.H),
    }

    /** Q: tote codes and trip codes still fit the smallest QR size (21 x 21) at 25%. */
    val DEFAULT_QR_LEVEL = QrLevel.Q

    fun qrModules(code: String, level: QrLevel): Int = Encoder.encode(code, level.zxing).matrix.width

    /** Largest module size that fits the QR code and its 4-module quiet zone in a [box]-dot square. */
    fun qrScale(code: String, level: QrLevel, box: Int): Int =
        (box / (qrModules(code, level) + 8)).coerceAtLeast(1)

    /** The QR code as a picture, [scale] dots per module. */
    fun qrGraphic(code: String, level: QrLevel, scale: Int): Graphic {
        val m = Encoder.encode(code, level.zxing).matrix
        val width = m.width * scale
        val rowBytes = (width + 7) / 8
        val hex = StringBuilder(rowBytes * width * 2)
        val row = IntArray(rowBytes)
        for (my in 0 until m.height) {
            row.fill(0)
            for (mx in 0 until m.width) {
                if (m.get(mx, my).toInt() != 1) continue
                for (px in mx * scale until (mx + 1) * scale) row[px / 8] = row[px / 8] or (0x80 ushr (px % 8))
            }
            val line = row.joinToString("") { "%02X".format(it) }
            repeat(scale) { hex.append(line) }
        }
        return Graphic(width, width, rowBytes, hex.toString())
    }

    /**
     * Where things go. Margins keep clear of the stock's printed bands (~57 dots at the top,
     * ~27 at the bottom) and the peel strip (~42 dots), scaled for other label sizes.
     */
    class Plan(val size: LabelSize, hasName: Boolean) {
        val side = (size.width * 0.085).roundToInt()
        val top = (size.length * 0.105).roundToInt()
        private val bottom = (size.length * 0.07).roundToInt()
        val usableWidth = size.width - 2 * side
        private val avail = size.length - top - bottom
        val nameHeight = if (hasName) (avail * 0.30).roundToInt() else 0
        private val gap = if (hasName) (avail * 0.03).roundToInt() else 0
        val codeHeight = (avail * (if (hasName) 0.13 else 0.16)).roundToInt()
        private val codeGap = (avail * 0.02).roundToInt()
        val barcodeTop = top + nameHeight + gap
        val barHeight = avail - nameHeight - gap - codeHeight - codeGap
        val codeTop = barcodeTop + barHeight + codeGap
    }

    private fun graphicAt(x: Int, y: Int, g: Graphic) =
        "^FO$x,$y^GFA,${g.bytes},${g.bytes},${g.rowBytes},${g.hex}^FS"

    /** ^PQ prints the label [copies] times; 1 adds nothing. */
    private fun quantity(copies: Int) = if (copies > 1) "^PQ${copies.coerceAtMost(99)}" else ""

    /**
     * A label with only [text] on it, as large as fits the whole area inside the printed bands
     * and the peel strip, up to 3 lines: for marking a tote "WATER" and the like.
     */
    fun textLabel(text: String, size: LabelSize, render: TextRenderer, copies: Int = 1): String {
        val plan = Plan(size, hasName = true)
        val area = plan.codeTop + plan.codeHeight - plan.top
        val sb = StringBuilder("^XA^LH0,0^LL${size.length}")
        render.render(text.trim(), plan.usableWidth, area)?.let { g ->
            sb.append(graphicAt((size.width - g.width) / 2, plan.top + (area - g.height) / 2, g))
        }
        return sb.append(quantity(copies)).append("^XZ").toString()
    }

    /**
     * One label: [name] (if any) across the top as large as fits, the code as a Code 128 barcode
     * as wide and tall as fits (or a QR code when [qr] is set), and the code in large print
     * under it. Text is drawn on the phone and sent as a picture, so its size is exact.
     */
    fun label(
        code: String, name: String, size: LabelSize, render: TextRenderer,
        copies: Int = 1, qr: QrLevel? = null,
    ): String {
        val plan = Plan(size, name.isNotBlank())
        val sb = StringBuilder("^XA^LH0,0^LL${size.length}")
        if (name.isNotBlank()) {
            render.render(name.trim(), plan.usableWidth, plan.nameHeight)?.let { g ->
                sb.append(graphicAt((size.width - g.width) / 2, plan.top + (plan.nameHeight - g.height) / 2, g))
            }
        }
        if (qr != null) {
            val g = qrGraphic(code, qr, qrScale(code, qr, minOf(size.width, plan.barHeight)))
            sb.append(graphicAt((size.width - g.width) / 2, plan.barcodeTop + (plan.barHeight - g.height) / 2, g))
        } else {
            val w = barModule(code, size.width)
            val x = (size.width - code128Modules(code) * w) / 2
            sb.append("^FO$x,${plan.barcodeTop}^BY$w,3,${plan.barHeight}^BCN,${plan.barHeight},N,N,N,N^FD$code^FS")
        }
        render.render(code, plan.usableWidth, plan.codeHeight)?.let { g ->
            sb.append(graphicAt((size.width - g.width) / 2, plan.codeTop + (plan.codeHeight - g.height) / 2, g))
        }
        return sb.append(quantity(copies)).append("^XZ").toString()
    }
}
