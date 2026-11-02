package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joyodigitama.huaweiwear.WatchData
import com.joyodigitama.huaweiwear.WatchDataOverview
import com.joyodigitama.huaweiwear.WatchDay
import com.joyodigitama.huaweiwear.WatchHrv
import com.joyodigitama.huaweiwear.WatchMetricPoint
import com.joyodigitama.huaweiwear.WatchMetricStatus
import com.joyodigitama.huaweiwear.WatchSleepNight
import com.joyodigitama.huaweiwear.WatchWorkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private class Snapshot(
    val overview: WatchDataOverview,
    val days: List<WatchDay>,
    val nights: List<WatchSleepNight>,
    val workouts: List<WatchWorkout>,
    val hrv: List<WatchHrv>,
    val metrics: List<Pair<WatchMetricStatus, WatchMetricPoint?>>,
    val error: String?
)

private fun load(d: WatchData): Snapshot {
    val overview = d.overview()
    val e1 = d.lastError
    val status = d.metricStatus()
    val metrics = status.map { s ->
        s to (if (s.rows > 0) d.metric(s.type, 60).lastOrNull() else null)
    }
    val days = d.days(7)
    val nights = d.sleepNights(7)
    val workouts = d.workouts(10)
    val hrv = d.hrv(14)
    return Snapshot(overview, days, nights, workouts, hrv, metrics, e1 ?: d.lastError)
}

private val TimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

private fun fmtTime(sec: Long): String =
    Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).format(TimeFmt)

private fun fmtHm(sec: Long): String = "${sec / 3600}j ${(sec % 3600) / 60}m"

private fun fmtKm(m: Int): String = "%.1f km".format(m / 1000.0)

@Composable
fun WatchDataScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val data = remember { WatchData(ctx) }
    var snap by remember { mutableStateOf<Snapshot?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        snap = withContext(Dispatchers.IO) { load(data) }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow("Data dari jam")
        OutlinedButton(onClick = { tick++ }) { Text("Muat ulang") }

        val s = snap
        if (s == null) {
            Text("Membaca…", color = Pal.Sub)
            return@Column
        }

        if (s.error != null) {
            Block("Gagal membaca sebagian data") { Line(s.error) }
        }

        if (!data.available()) {
            Block("Belum ada data") {
                Line("Sambungkan jam di bagian Sambungan, lalu tekan Sinkron.")
            }
            return@Column
        }

        val o = s.overview
        Block("Isi database") {
            if (o.devices.isEmpty()) Line("Jam: belum tercatat")
            o.devices.forEach {
                val sync = it.lastSyncSec?.let { t -> " · sinkron ${fmtTime(t)}" } ?: ""
                Line("${it.name} (${it.model ?: "model ?"}, ${it.software ?: "versi ?"})$sync")
            }
            Line("Sampel harian: ${o.hrSamples} · latihan: ${o.workouts}")
            Line("Segmen tidur: ${o.sleepSegments} · metrik lain: ${o.healthMetricRows} · HRV: ${o.hrvReadings}")
            if (o.firstSampleSec != null && o.lastSampleSec != null) {
                Line("Rentang: ${fmtTime(o.firstSampleSec!!)} sampai ${fmtTime(o.lastSampleSec!!)}")
            }
        }

        Block("7 hari terakhir") {
            if (s.days.isEmpty()) Line("Belum ada data harian.")
            s.days.forEach { d ->
                val parts = mutableListOf("${d.steps} langkah")
                d.restingHr?.let { parts.add("RHR $it") }
                if (d.minHr != null && d.maxHr != null) parts.add("HR ${d.minHr}-${d.maxHr}")
                d.spo2Avg?.let { parts.add("SpO2 $it%") }
                Line("${d.date}: " + parts.joinToString(" · "))
            }
        }

        Block("Tidur") {
            if (s.nights.isEmpty()) Line("Belum ada data tidur.")
            s.nights.forEach { n ->
                val parts = mutableListOf(fmtHm(n.asleepSec))
                if (n.deepSec > 0) parts.add("dalam ${fmtHm(n.deepSec)}")
                if (n.remSec > 0) parts.add("REM ${fmtHm(n.remSec)}")
                n.score?.let { parts.add("skor $it") }
                Line("${fmtTime(n.startSec)} - ${fmtTime(n.endSec)}")
                Line("   " + parts.joinToString(" · "))
            }
        }

        Block("Latihan terbaru") {
            if (s.workouts.isEmpty()) Line("Belum ada latihan.")
            s.workouts.forEach { w ->
                val parts = mutableListOf("${w.durationSec / 60} mnt")
                if (w.distanceM > 0) parts.add(fmtKm(w.distanceM))
                if (w.avgHr != null) parts.add("HR ${w.avgHr}/${w.maxHr ?: "?"}")
                if (!w.complete) parts.add("belum lengkap")
                Line("${w.sportName} · ${fmtTime(w.startSec)}")
                Line("   " + parts.joinToString(" · "))
            }
        }

        Block("HRV") {
            if (s.hrv.isEmpty()) Line("Belum ada data HRV.")
            s.hrv.take(10).forEach { h ->
                Line("${fmtTime(h.startSec)} · ${h.source} · RMSSD ${"%.0f".format(h.rmssd)} ms")
            }
        }

        Block("Metrik kesehatan lain") {
            if (s.metrics.isEmpty()) Line("Belum dicek. Lakukan sinkron dulu.")
            s.metrics.forEach { (m, last) ->
                val text = when {
                    !m.supported -> "tidak didukung jam"
                    m.rows == 0 -> "didukung, belum ada data"
                    else -> "${m.rows} baris" + (last?.value?.let { v -> " · terakhir ${"%.1f".format(v)}" } ?: "")
                }
                Line("${m.title}: $text")
            }
        }
    }
}

@Composable
private fun Block(title: String, content: @Composable ColumnScope.() -> Unit) {
    Panel {
        Eyebrow(title)
        content()
    }
}

@Composable
private fun Line(text: String) {
    Text(text, color = Pal.Text, fontSize = 13.sp)
}
