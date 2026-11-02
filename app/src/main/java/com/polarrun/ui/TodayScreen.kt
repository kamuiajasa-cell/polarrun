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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polarrun.AppState
import com.polarrun.engine.Coach
import com.polarrun.engine.Kind
import com.polarrun.engine.Planner
import com.polarrun.engine.Readiness
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun TodayScreen(st: AppState, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val s = st.settings
    val week = Planner.buildWeek(s, Planner.weekStart(today))
    val planned = week.firstOrNull { it.date == today }
    val score = Readiness.score(st.journal, st.wellness)
    val adv = Coach.advise(planned, score, st.journal)
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.athleteId, s.apiKey) {
        if (s.athleteId.isNotBlank() && s.apiKey.isNotBlank()) st.syncPull()
    }

    val title = when (adv.kind) {
        Kind.REST -> "Istirahat"
        Kind.HARD -> "Hard (interval)"
        else -> "${adv.kind.label} · ${adv.totalMin} menit"
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Sesi hari ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)

        if (adv.kind != Kind.REST) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    Planner.describe(adv.kind, adv.totalMin, adv.reps, adv.workSec, adv.restSec, s),
                    Modifier.padding(16.dp)
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(adv.message, Modifier.padding(16.dp))
        }

        Text(
            if (score != null) "Skor kesiapan: $score/100" else "Skor kesiapan: belum ada (isi Jurnal)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        if (week.isNotEmpty()) {
            val pct = (Planner.hardShare(week) * 100).roundToInt()
            Text(
                "Minggu ini: ${week.sumOf { it.totalMin }} menit, porsi keras $pct%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            enabled = !st.busy,
            onClick = { scope.launch { st.syncPull() } }
        ) { Text(if (st.busy) "Menyinkronkan..." else "Sinkron intervals.icu") }

        if (st.status.isNotBlank()) {
            Text(st.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
