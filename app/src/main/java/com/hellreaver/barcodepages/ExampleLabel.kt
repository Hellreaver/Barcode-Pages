package com.hellreaver.barcodepages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors of the printed store label, only used for this picture of it. */
private val LabelPaper = Color(0xFFFBFAF6)
private val LabelInk = Color(0xFF1E1E1E)
private val LabelOrange = Color(0xFFE8622A)

/** The code the example points at. Same format as store labels (one letter, five digits). */
const val EXAMPLE_CODE = "Z13334"

/** Trip label example: TL, a lowercase trip id, a dash and a number, as printed and encoded. */
const val EXAMPLE_TRIP_CODE = "TL0a1b-2"

/**
 * A drawing of a store tote label with placeholder values, with the one code the app needs
 * highlighted. The bars are decoration and do not form a readable barcode, so nobody can scan
 * the example into the dispense system by mistake.
 */
@Composable
fun ExampleLabel(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("example-label"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "WHERE TO FIND THE CODE",
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
            style = EyebrowStyle,
        )
        Column(
            Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(LabelPaper)
                .clearAndSetSemantics {
                    contentDescription = "Example tote label, with the bag and no-bags symbols shown split in " +
                        "one square. Type the code in its bottom-right corner, like $EXAMPLE_CODE. The long " +
                        "number above the bars is not the tote code."
                },
        ) {
            Box(Modifier.fillMaxWidth().height(7.dp).background(LabelOrange))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(LabelInk),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("1", style = labelText(22, FontWeight.Bold, Color.White))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0000", style = labelText(34, FontWeight.Bold).copy(lineHeight = 36.sp))
                        Text("8:00 AM", style = labelText(18, FontWeight.Bold))
                    }
                    BagIcon()
                }
                Text("SCHEDULED PICKUP", style = labelText(16, FontWeight.Bold))
                Text("FIRST L.", style = labelText(17, FontWeight.Normal))
                Text(
                    "200000000000000",
                    style = TextStyle(fontFamily = Lemon.mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = LabelInk),
                )
                DecorativeBars(Modifier.fillMaxWidth(0.82f).height(46.dp).padding(vertical = 3.dp))
                Text("CHILLED", style = labelText(14, FontWeight.Bold))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("01-Jan", style = labelText(14, FontWeight.Normal))
                    Spacer(Modifier.weight(1f))
                    Text("5:00 AM", style = labelText(14, FontWeight.Normal))
                    Spacer(Modifier.weight(1f))
                    Text(
                        EXAMPLE_CODE,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Lemon.accent)
                            .border(2.dp, Lemon.onAccent, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        style = TextStyle(
                            fontFamily = Lemon.mono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Lemon.onAccent,
                        ),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(7.dp).background(LabelOrange))
        }
        Row(
            Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "▲ Type this code",
                style = TextStyle(fontFamily = Lemon.display, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Lemon.accent),
            )
        }
        Text(
            "It's the code the label's barcode holds. The long number above the bars is the order, not the tote.",
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(top = 4.dp),
            style = TextStyle(fontFamily = Lemon.body, fontSize = 13.sp, color = Lemon.muted),
        )
        // Trip labels are the less common case, so their example starts folded away.
        var showTrip by rememberSaveable { mutableStateOf(false) }
        Row(
            Modifier
                .padding(top = 14.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(ControlShape)
                .border(1.dp, Lemon.line, ControlShape)
                .clickable(role = Role.Button, onClickLabel = if (showTrip) "Hide" else "Show") { showTrip = !showTrip }
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .testTag("trip-toggle"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Trip label example",
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = Lemon.display, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Lemon.accentInk),
            )
            Text(
                if (showTrip) "Hide \u25B4" else "Show \u25BE",
                style = TextStyle(fontFamily = Lemon.display, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Lemon.accent),
            )
        }
        if (showTrip) {
            TripExampleLabel(Modifier.padding(top = 10.dp))
            Text(
                "On a trip label, type the code just above the bars. Its lowercase letters stay lowercase, " +
                    "even if you type capitals.",
                modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(top = 6.dp),
                style = TextStyle(fontFamily = Lemon.body, fontSize = 13.sp, color = Lemon.muted),
            )
        }
    }
}

/** A drawing of a trip label with placeholder values, the code to type highlighted. */
@Composable
private fun TripExampleLabel(modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(LabelPaper)
            .testTag("trip-example")
            .clearAndSetSemantics {
                contentDescription = "Example trip label. Type the code just above its bars, like $EXAMPLE_TRIP_CODE."
            },
    ) {
        Box(Modifier.fillMaxWidth().height(7.dp).background(LabelOrange))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("Trip Label", style = labelText(14, FontWeight.Normal))
            Text("Trip:0a1b", style = labelText(24, FontWeight.Bold))
            Text(
                EXAMPLE_TRIP_CODE,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Lemon.accent)
                    .border(2.dp, Lemon.onAccent, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                style = TextStyle(
                    fontFamily = Lemon.mono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Lemon.onAccent,
                ),
            )
            DecorativeBars(Modifier.fillMaxWidth(0.82f).height(40.dp).padding(vertical = 3.dp))
            Text("5:00 AM", style = labelText(14, FontWeight.Normal))
        }
        Box(Modifier.fillMaxWidth().height(7.dp).background(LabelOrange))
    }
}

private fun labelText(size: Int, weight: FontWeight, color: Color = LabelInk) = TextStyle(
    fontFamily = Lemon.body,
    fontWeight = weight,
    fontSize = size.sp,
    color = color,
    textAlign = TextAlign.Center,
)

/**
 * The top-right symbol, split corner to corner so one picture covers both label types: the
 * upper-left half is the bag symbol (white bag on black), the lower-right half is the no-bags
 * symbol (black outline bag on white). The dividing line doubles as the no-bags slash.
 */
@Composable
private fun BagIcon() {
    Canvas(Modifier.size(38.dp)) {
        val w = size.width
        val line = w * 0.06f
        fun DrawScope.bag(color: Color) {
            drawRoundRect(
                color,
                topLeft = Offset(w * 0.22f, w * 0.40f),
                size = Size(w * 0.56f, w * 0.40f),
                cornerRadius = CornerRadius(w * 0.04f),
                style = Stroke(width = line),
            )
            drawArc(
                color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.36f, w * 0.20f),
                size = Size(w * 0.28f, w * 0.40f),
                style = Stroke(width = line),
            )
        }
        val upperLeft = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, w); close() }
        val lowerRight = Path().apply { moveTo(w, 0f); lineTo(w, w); lineTo(0f, w); close() }
        clipPath(upperLeft) {
            drawRect(LabelInk)
            bag(Color.White)
        }
        clipPath(lowerRight) {
            drawRect(LabelPaper)
            drawRect(LabelInk, topLeft = Offset(line / 2, line / 2), size = Size(w - line, w - line), style = Stroke(width = line))
            bag(LabelInk)
        }
        clipRect { drawLine(LabelInk, start = Offset(w, 0f), end = Offset(0f, w), strokeWidth = line) }
    }
}

/**
 * Bars that look like a barcode but encode nothing: no start or stop pattern, so scanners
 * and decoders ignore them.
 */
@Composable
private fun DecorativeBars(modifier: Modifier) {
    Canvas(modifier) {
        val widths = intArrayOf(1, 1, 3, 1, 1, 2, 2, 1, 1, 3, 3, 1, 2, 2, 1, 1, 1, 3, 2, 1, 3, 1, 1, 2, 1, 1, 2, 3, 1, 1, 2, 1, 1, 2)
        val unit = size.width / widths.sum()
        var x = 0f
        widths.forEachIndexed { i, w ->
            if (i % 2 == 0) drawRect(LabelInk, topLeft = Offset(x, 0f), size = Size(w * unit, size.height))
            x += w * unit
        }
    }
}

