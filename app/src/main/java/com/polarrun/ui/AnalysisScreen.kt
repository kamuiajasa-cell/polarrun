package com.polarrun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polarrun.AppState
import com.polarrun.engine.Analytics
import com.polarrun.engine.Planner
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column {
        Eyebrow(label)
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun AnalysisScreen(st: AppState, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val s = st.settings
    val weeks8 = Analytics.weeks(st.activities, 8, today)
    val weeks4 = weeks8.takeLast(4)
    val zw = weeks4.filter { it.zoneTotal > 0 }
    val comp = Analytics.compliance(s, st.activities, today)
    val start = Planner.weekStart(LocalDate.parse(s.planStart))

    val wel = st.wellness.sortedBy { it.date }.takeLast(28)
    val ctlS = wel.map { it.ctl?.toFloat() }
    val atlS = wel.map { it.atl?.toFloat() }
    val tsbS = wel.map { w ->
        val c = w.ctl
        val a = w.atl
        if (c != null && a != null) (c - a).toFloat() else null
    }
    val last = wel.lastOrNull()
    val lastCtl = last?.ctl
    val lastAtl = last?.atl

    val days14 = (13 downTo 0).map { today.minusDays(it.toLong()).toString() }
    val scoreS = days14.map { d -> st.scores[d]?.toFloat() }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow("Analisa")
        Text("Performa 8 minggu", fontSize = 26.sp, fontWeight = FontWeight.Light)

        // 1. 80/20 aktual vs target
        Panel {
            Eyebrow("80/20 aktual vs target")
            if (zw.isEmpty()) {
                Text(
                    "Data zona HR belum tersedia. Sinkron intervals.icu dulu.",
                    color = Pal.Sub
                )
            } else {
                val e = zw.sumOf { it.easy }
                val m = zw.sumOf { it.mod }
                val h = zw.sumOf { it.hard }
                val t = (e + m + h).toDouble()
                val hp = (h / t * 100).roundToInt()
                val mp = (m / t * 100).roundToInt()
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$hp%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hp <= 22) Pal.Lime else Pal.Hard
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "keras · target maks 20%",
                        color = Pal.Sub,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                val verdict = when {
                    hp > 22 -> "Terlalu banyak waktu keras. Turunkan intensitas atau kurangi repetisi."
                    mp > 12 -> "Zona sedang (Z3) terlalu banyak. Pelankan sesi easy supaya benar-benar easy."
                    hp < 10 -> "Porsi keras masih di bawah target. Tambah bertahap kalau kesiapan mendukung."
                    else -> "Distribusi sudah sesuai pola polarized."
                }
                Text(verdict, fontSize = 14.sp)
                for (w in zw) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${w.monday.dayOfMonth}/${w.monday.monthValue}",
                            Modifier.width(48.dp),
                            color = Pal.Sub,
                            fontSize = 12.sp
                        )
                        ZoneBar(w.easy, w.mod, w.hard, Modifier.weight(1f))
                        Text(
                            "${(w.hard * 100.0 / w.zoneTotal).roundToInt()}%",
                            Modifier.width(44.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(Pal.Easy, "Easy")
                    LegendDot(Pal.Mod, "Sedang")
                    LegendDot(Pal.Hard, "Keras")
                    LegendDot(Color.White, "Garis = 80%")
                }
            }
        }

        // 2. Kepatuhan jadwal
        Panel {
            Eyebrow("Kepatuhan jadwal")
            if (comp.planned == 0) {
                Text("Belum ada sesi terjadwal yang lewat.", color = Pal.Sub)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(comp.pct / 100f, if (comp.pct >= 80) Pal.Lime else Pal.Mod, 96.dp, 9.dp) {
                        Text("${comp.pct}%", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${comp.done} dari ${comp.planned} sesi terlaksana")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (ok in comp.marks) {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (ok) Pal.Easy else Pal.Hard.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Menit per minggu
        Panel {
            Eyebrow("Menit per minggu")
            BarChart(
                values = weeks8.map { it.min.toFloat() },
                marks = weeks8.map { w ->
                    if (!w.monday.isBefore(start)) {
                        Planner.weekMinutes(s, Planner.weekIndex(s, w.monday)).toFloat()
                    } else null
                },
                labels = weeks8.map { "${it.monday.dayOfMonth}/${it.monday.monthValue}" },
                color = Pal.Long
            )
            Text("Batang = realisasi, garis putih = rencana", color = Pal.Sub, fontSize = 11.sp)
        }

        // 4. Tren skor kesiapan
        Panel {
            Eyebrow("Skor kesiapan 14 hari")
            if (scoreS.all { it == null }) {
                Text("Belum ada riwayat skor. Isi Jurnal tiap hari.", color = Pal.Sub)
            } else {
                LineChart(listOf(scoreS to Pal.Lime), fixedMin = 0f, fixedMax = 100f)
            }
        }

        // 5. CTL / ATL / TSB
        Panel {
            Eyebrow("Fitness · Fatigue · Form")
            if (ctlS.all { it == null }) {
                Text("Belum ada data kebugaran dari intervals.icu.", color = Pal.Sub)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("Fitness", lastCtl?.roundToInt()?.toString() ?: "–", Pal.Long)
                    Stat("Fatigue", lastAtl?.roundToInt()?.toString() ?: "–", Pal.Hard)
                    Stat(
                        "Form",
                        if (lastCtl != null && lastAtl != null) (lastCtl - lastAtl).roundToInt().toString() else "–",
                        Pal.Lime
                    )
                }
                LineChart(listOf(ctlS to Pal.Long, atlS to Pal.Hard))
                Eyebrow("Form (fitness - fatigue)")
                LineChart(listOf(tsbS to Pal.Lime))
            }
        }
    }
}
