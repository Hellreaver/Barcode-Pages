package com.hellreaver.barcodepages

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.ceil

/**
 * Draws label text on the phone as a 1-bit picture, as large as fits the box, on 1 to 3
 * balanced lines (whichever gives the biggest letters), in bold condensed type.
 */
object LabelText : LabelZpl.TextRenderer {
    private val paint = Paint().apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        color = Color.BLACK
        isAntiAlias = false   // the printer has no grey: every pixel black or white
    }

    override fun render(text: String, maxWidth: Int, maxHeight: Int): LabelZpl.Graphic? {
        val t = text.trim()
        if (t.isEmpty() || maxWidth < 8 || maxHeight < 8) return null
        val options = (1..3).mapNotNull { wrap(t, it) }.distinct()
        val (lines, size) = options.map { it to fit(it, maxWidth, maxHeight) }.maxBy { it.second }
        paint.textSize = size
        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val width = ceil(lines.maxOf { paint.measureText(it) }).toInt().coerceIn(1, maxWidth)
        val height = ceil(lineHeight * lines.size).toInt().coerceIn(1, maxHeight)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, (width - paint.measureText(line)) / 2, i * lineHeight - fm.ascent, paint)
        }
        return toGraphic(bitmap).also { bitmap.recycle() }
    }

    private fun fit(lines: List<String>, maxWidth: Int, maxHeight: Int): Float {
        var lo = 4f
        var hi = maxHeight.toFloat()
        repeat(24) {
            val mid = (lo + hi) / 2
            paint.textSize = mid
            val fm = paint.fontMetrics
            val fits = (fm.descent - fm.ascent) * lines.size <= maxHeight &&
                lines.all { paint.measureText(it) <= maxWidth }
            if (fits) lo = mid else hi = mid
        }
        return lo
    }

    /** [t] broken at spaces into [lines] lines of about equal length, or null if it can't be. */
    private fun wrap(t: String, lines: Int): List<String>? {
        if (lines == 1) return listOf(t)
        val words = t.split(' ').filter { it.isNotEmpty() }
        if (words.size < lines) return null
        val target = t.length / lines.toDouble()
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        for ((i, w) in words.withIndex()) {
            val wordsLeft = words.size - i
            val linesLeft = lines - out.size
            val wouldBe = if (cur.isEmpty()) w.length else cur.length + 1 + w.length
            if (cur.isNotEmpty() && linesLeft > 1 && (wouldBe > target || wordsLeft < linesLeft)) {
                out += cur.toString()
                cur = StringBuilder()
            }
            if (cur.isNotEmpty()) cur.append(' ')
            cur.append(w)
        }
        out += cur.toString()
        return if (out.size == lines) out else null
    }

    private fun toGraphic(bitmap: Bitmap): LabelZpl.Graphic {
        val w = bitmap.width
        val h = bitmap.height
        val rowBytes = (w + 7) / 8
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val hex = StringBuilder(rowBytes * h * 2)
        for (y in 0 until h) {
            for (b in 0 until rowBytes) {
                var v = 0
                for (bit in 0 until 8) {
                    val x = b * 8 + bit
                    if (x < w && Color.red(pixels[y * w + x]) < 128) v = v or (0x80 ushr bit)
                }
                hex.append(HEX[v ushr 4]).append(HEX[v and 15])
            }
        }
        return LabelZpl.Graphic(w, h, rowBytes, hex.toString())
    }

    private const val HEX = "0123456789ABCDEF"
}
