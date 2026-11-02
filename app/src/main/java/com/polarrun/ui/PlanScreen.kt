package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polarrun.AppState
import com.polarrun.engine.Planner
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private val DAY_NAMES = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@Composable
fun PlanScreen(st: AppState, modifier: Modifier = Modifier) {
    val s = st.settings
    val monday = Planner.weekStart(LocalDate.now())
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Rencana 4 minggu", style = MaterialTheme.typography.headlineSmall)

        val ws = st.weekStat
        if (ws != null && ws.week == monday.toString()) {
            Text(
                "Realisasi minggu ini: ${ws.min} menit, ${"%.1f".format(ws.km)} km",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        for (w in 0..3) {
            val m = monday.plusWeeks(w.toLong())
            val list = Planner.buildWeek(s, m)
            val idx = Planner.weekIndex(s, m)
            val pct = (Planner.hardShare(list) * 100).roundToInt()
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Minggu ke-${idx + 1}" + (if (Planner.isDeload(idx)) " (pemulihan)" else ""),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${list.sumOf { it.totalMin }} menit, porsi keras $pct%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (x in list) {
                        Text(
                            "${DAY_NAMES[x.date.dayOfWeek.value - 1]} ${x.date.dayOfMonth}/${x.date.monthValue}: " +
                                "${x.kind.label}, ${x.totalMin} menit" +
                                (if (x.reps > 0) " (${x.reps} rep keras)" else "")
                        )
                    }
                }
            }
        }

        Button(
            enabled = !st.busy,
            onClick = { scope.launch { st.pushPlan() } }
        ) { Text("Kirim 2 minggu ke kalender intervals.icu") }

        if (st.status.isNotBlank()) {
            Text(st.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
