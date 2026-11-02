package com.polarrun.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.polarrun.AppState
import com.polarrun.engine.Planner
import kotlinx.coroutines.launch
import java.time.LocalDate

private val DAY_NAMES = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(st: AppState, modifier: Modifier = Modifier) {
    val s = st.settings
    val scope = rememberCoroutineScope()

    var thr by remember { mutableStateOf(s.thrHr.toString()) }
    var base by remember { mutableStateOf(s.baseWeekMin.toString()) }
    var days by remember { mutableStateOf(s.runDays) }
    var workM by remember { mutableStateOf((s.hardWorkSec / 60).toString()) }
    var workS by remember { mutableStateOf((s.hardWorkSec % 60).toString()) }
    var restM by remember { mutableStateOf((s.hardRestSec / 60).toString()) }
    var restS by remember { mutableStateOf((s.hardRestSec % 60).toString()) }
    var runWalk by remember { mutableStateOf(s.runWalk) }
    var runM by remember { mutableStateOf((s.runSec / 60).toString()) }
    var runS by remember { mutableStateOf((s.runSec % 60).toString()) }
    var walkM by remember { mutableStateOf((s.walkSec / 60).toString()) }
    var walkS by remember { mutableStateOf((s.walkSec % 60).toString()) }
    var athlete by remember { mutableStateOf(s.athleteId) }
    var key by remember { mutableStateOf(s.apiKey) }

    fun n(t: String, def: Int): Int = t.toIntOrNull() ?: def

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setelan", style = MaterialTheme.typography.headlineSmall)

        Text("Hari lari (pilih 2-6)", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DAY_NAMES.forEachIndexed { i, name ->
                val iso = i + 1
                FilterChip(
                    selected = iso in days,
                    onClick = { days = if (iso in days) days - iso else (days + iso).sorted() },
                    label = { Text(name) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("HR threshold (bpm)", thr, { thr = it }, Modifier.weight(1f))
            NumField("Menit/minggu awal", base, { base = it }, Modifier.weight(1f))
        }

        Text("Interval keras", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Keras menit", workM, { workM = it }, Modifier.weight(1f))
            NumField("detik", workS, { workS = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Jeda menit", restM, { restM = it }, Modifier.weight(1f))
            NumField("detik", restS, { restS = it }, Modifier.weight(1f))
        }

        Text("Run-walk", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Pakai pola run-walk di sesi easy", runWalk) { runWalk = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Lari menit", runM, { runM = it }, Modifier.weight(1f))
            NumField("detik", runS, { runS = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Jalan menit", walkM, { walkM = it }, Modifier.weight(1f))
            NumField("detik", walkS, { walkS = it }, Modifier.weight(1f))
        }

        Text("intervals.icu", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = athlete,
            onValueChange = { athlete = it.trim() },
            label = { Text("Athlete ID (contoh i12345)") },
            singleLine = true
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.trim() },
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Button(onClick = {
            if (days.size !in 2..6) {
                st.status = "Pilih 2 sampai 6 hari lari."
            } else {
                val run = n(runM, 0) * 60 + n(runS, 0)
                val walk = n(walkM, 0) * 60 + n(walkS, 0)
                val work = n(workM, 0) * 60 + n(workS, 0)
                val rest = n(restM, 0) * 60 + n(restS, 0)
                if (work < 30 || run + walk <= 0) {
                    st.status = "Durasi interval keras minimal 30 detik dan pola run-walk tidak boleh 0."
                } else {
                    st.saveSettings(
                        s.copy(
                            thrHr = n(thr, s.thrHr).coerceIn(100, 220),
                            baseWeekMin = n(base, s.baseWeekMin).coerceIn(30, 600),
                            runDays = days,
                            hardWorkSec = work,
                            hardRestSec = rest,
                            runWalk = runWalk,
                            runSec = run,
                            walkSec = walk,
                            athleteId = athlete,
                            apiKey = key
                        )
                    )
                    st.status = "Setelan tersimpan."
                }
            }
        }) { Text("Simpan") }

        OutlinedButton(onClick = {
            st.saveSettings(s.copy(planStart = Planner.weekStart(LocalDate.now()).toString()))
            st.status = "Plan dimulai ulang dari minggu ini."
        }) { Text("Mulai plan dari minggu ini") }

        OutlinedButton(
            enabled = !st.busy,
            onClick = { scope.launch { st.syncPull() } }
        ) { Text("Tarik data intervals.icu") }

        if (st.status.isNotBlank()) {
            Text(st.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
