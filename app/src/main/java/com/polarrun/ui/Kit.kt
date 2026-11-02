package com.polarrun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Card)
            .border(1.dp, accent?.copy(alpha = 0.55f) ?: Pal.Line, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun Eyebrow(text: String, color: Color = Pal.Sub) {
    Text(
        text.uppercase(),
        color = color,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Pal.Lime,
            contentColor = Color.Black,
            disabledContainerColor = Pal.Line,
            disabledContentColor = Pal.Sub
        )
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun Ring(progress: Float, color: Color, dia: Dp, stroke: Dp, center: @Composable () -> Unit) {
    Box(Modifier.size(dia), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(this.size.width - sw, this.size.height - sw)
            drawArc(Pal.Line, -90f, 360f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(color, -90f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

@Composable
fun LineChart(
    series: List<Pair<List<Float?>, Color>>,
    modifier: Modifier = Modifier,
    fixedMin: Float? = null,
    fixedMax: Float? = null
) {
    Canvas(modifier.fillMaxWidth().height(130.dp)) {
        val all = series.flatMap { it.first }.filterNotNull()
        if (all.isEmpty()) return@Canvas
        var lo = fixedMin ?: all.min()
        var hi = fixedMax ?: all.max()
        if (hi - lo < 0.001f) hi = lo + 1f
        if (fixedMin == null) {
            val pad = (hi - lo) * 0.1f
            lo -= pad
            hi += pad
        }
        val w = size.width
        val h = size.height
        val n = series.maxOf { it.first.size }
        fun xAt(i: Int): Float = if (n <= 1) w / 2 else w * i / (n - 1)
        fun yAt(v: Float): Float = h - (v - lo) / (hi - lo) * h

        for (k in 0..2) {
            val y = h * k / 2f
            drawLine(Pal.Line, Offset(0f, y), Offset(w, y), 1.dp.toPx())
        }
        for ((vals, col) in series) {
            val path = Path()
            var pen = false
            vals.forEachIndexed { i, v ->
                if (v == null) {
                    pen = false
                } else {
                    val px = xAt(i)
                    val py = yAt(v)
                    if (!pen) {
                        path.moveTo(px, py)
                        pen = true
                    } else {
                        path.lineTo(px, py)
                    }
                }
            }
            drawPath(path, col, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val li = vals.indexOfLast { it != null }
            val lv = if (li >= 0) vals[li] else null
            if (lv != null) drawCircle(col, 4.dp.toPx(), Offset(xAt(li), yAt(lv)))
        }
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    marks: List<Float?>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val hi = maxOf(values.maxOrNull() ?: 0f, marks.filterNotNull().maxOrNull() ?: 0f, 1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size
            if (n == 0) return@Canvas
            val slot = size.width / n
            val bw = slot * 0.55f
            values.forEachIndexed { i, v ->
                val bh = size.height * (v / hi)
                val x = slot * i + (slot - bw) / 2
                drawRoundRect(
                    color = if (i == n - 1) Pal.Lime else color,
                    topLeft = Offset(x, size.height - bh),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                val m = marks.getOrNull(i)
                if (m != null) {
                    val y = size.height * (1 - m / hi)
                    drawLine(Pal.Text, Offset(x - 4.dp.toPx(), y), Offset(x + bw + 4.dp.toPx(), y), 2.dp.toPx())
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    color = Pal.Sub,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ZoneBar(easy: Int, mod: Int, hard: Int, modifier: Modifier = Modifier) {
    val t = (easy + mod + hard).toFloat()
    Canvas(modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
        drawRect(Pal.Line, Offset.Zero, size)
        if (t > 0f) {
            var x = 0f
            val parts = listOf(easy to Pal.Easy, mod to Pal.Mod, hard to Pal.Hard)
            for ((v, c) in parts) {
                val wd = size.width * v / t
                if (wd > 0f) {
                    drawRect(c, Offset(x, 0f), Size(wd, size.height))
                    x += wd
                }
            }
        }
        val tx = size.width * 0.8f
        drawLine(Color.White, Offset(tx, 0f), Offset(tx, size.height), 2.dp.toPx())
    }
}

@Composable
fun StepBar(step: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until total) {
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= step) Pal.Lime else Pal.Line)
            )
        }
    }
}

@Composable
fun OptionCard(title: String, sub: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Pal.Card)
            .border(1.dp, if (selected) accent else Pal.Line, shape)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Pal.Sub, fontSize = 13.sp)
        }
    }
}

@Composable
fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = Pal.Sub)
    }
}
