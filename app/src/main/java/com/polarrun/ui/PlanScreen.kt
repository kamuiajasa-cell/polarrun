package com.polarrun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polarrun.AppState
import com.polarrun.engine.Planner
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private val DAY_NAMES = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@Composable
fun PlanScreen(st: AppState, modifier: Modifier = Modifier) {
    val s = st.settings
    val today = LocalDate.now()
    val monday = Planner.weekStart(today)
    val scope = rememberCoroutineScope()
    val dayMin = st.activities.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.min } }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow("Rencana")
        Text("4 minggu ke depan", fontSize = 26.sp, fontWeight = FontWeight.Light)

        for (w in 0..3) {
            val m = monday.plusWeeks(w.toLong())
            val list = Planner.buildWeek(s, m)
            val idx = Planner.weekIndex(s, m)
            val deload = Planner.isDeload(idx)
            val pct = (Planner.hardShare(list) * 100).roundToInt()
            Panel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Eyebrow(
                            "Minggu ${idx + 1}" + (if (deload) " · pemulihan" else ""),
                            if (deload) Pal.Mod else Pal.Sub
                        )
                        Text(
                            "${list.sumOf { it.totalMin }} menit",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        "keras $pct%",
                        color = if (pct <= 20) Pal.Lime else Pal.Hard,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                for (x in list) {
                    val isToday = x.date == today
                    val done = !x.date.isAfter(today) && (dayMin[x.date.toString()] ?: 0) >= x.totalMin * 0.5
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isToday) Pal.CardHi else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(kindColor(x.kind))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                x.kind.label + (if (x.reps > 0) " · ${x.reps} rep" else ""),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${DAY_NAMES[x.date.dayOfWeek.value - 1]} ${x.date.dayOfMonth}/${x.date.monthValue}",
                                color = Pal.Sub,
                                fontSize = 12.sp
                            )
                        }
                        if (done) {
                            Text("✓", color = Pal.Easy, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("${x.totalMin} mnt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        PrimaryButton("Kirim 2 minggu ke intervals.icu", enabled = !st.busy) {
            scope.launch { st.pushPlan() }
        }

        if (st.status.isNotBlank()) {
            Text(st.status, color = Pal.Sub, fontSize = 12.sp)
        }
    }
}
