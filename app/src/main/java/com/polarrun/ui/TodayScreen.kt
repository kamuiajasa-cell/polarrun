package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polarrun.AppState
import com.polarrun.engine.Coach
import com.polarrun.engine.Kind
import com.polarrun.engine.Planner
import com.polarrun.engine.Readiness
import com.polarrun.engine.mmss
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private val DN = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val MN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun TodayScreen(st: AppState, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val monday = Planner.weekStart(today)
    val s = st.settings
    val week = Planner.buildWeek(s, monday)
    val planned = week.firstOrNull { it.date == today }
    val score = Readiness.score(st.journal, st.wellness)
    val adv = Coach.advise(planned, score, st.journal)
    val scope = rememberCoroutineScope()
    val accent = kindColor(adv.kind)

    LaunchedEffect(s.athleteId, s.apiKey) {
        if (s.athleteId.isNotBlank() && s.apiKey.isNotBlank()) st.syncPull()
    }

    val lo = monday.toString()
    val hi = monday.plusDays(6).toString()
    val doneMin = st.activities.filter { it.date >= lo && it.date <= hi }.sumOf { it.min }
    val planMin = week.sumOf { it.totalMin }
    val volProg = if (planMin > 0) doneMin.toFloat() / planMin else 0f
    val hardPct = (Planner.hardShare(week) * 100).roundToInt()

    val big = if (adv.kind == Kind.REST) "Rest"
    else if (adv.kind == Kind.HARD) "${adv.reps}×${mmss(adv.workSec)}"
    else "${adv.totalMin}"
    val unit = if (adv.kind == Kind.REST) ""
    else if (adv.kind == Kind.HARD) "keras"
    else "menit"

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow("${DN[today.dayOfWeek.value - 1]} · ${today.dayOfMonth} ${MN[today.monthValue - 1]}")

        Panel(accent = accent) {
            Eyebrow("Sesi hari ini", accent)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(big, fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Black, color = accent)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(unit, color = Pal.Sub, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            Text(adv.kind.label, fontSize = 20.sp, fontWeight = FontWeight.Light)
            if (adv.kind != Kind.REST) {
                Text(
                    Planner.describe(adv.kind, adv.totalMin, adv.reps, adv.workSec, adv.restSec, s),
                    color = Pal.Sub,
                    fontSize = 14.sp
                )
            }
            if (adv.adjusted) {
                Text("Disesuaikan coach", color = Pal.Mod, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(1f)) {
                Eyebrow("Kesiapan")
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Ring((score ?: 0) / 100f, scoreColor(score), 104.dp, 10.dp) {
                        Text(score?.toString() ?: "–", fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (score == null) "Isi Jurnal" else "dari 100",
                    color = Pal.Sub, fontSize = 12.sp
                )
            }
            Panel(Modifier.weight(1f)) {
                Eyebrow("Volume minggu ini")
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Ring(volProg, Pal.Long, 104.dp, 10.dp) {
                        Text("$doneMin", fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("dari $planMin menit", color = Pal.Sub, fontSize = 12.sp)
            }
        }

        Panel {
            Eyebrow("Coach", Pal.Lime)
            Text(adv.message)
            Text(
                "Rencana minggu ini: porsi keras $hardPct% (target maks 20%)",
                color = Pal.Sub, fontSize = 12.sp
            )
        }

        OutlinedButton(
            enabled = !st.busy,
            onClick = { scope.launch { st.syncPull() } }
        ) { Text(if (st.busy) "Menyinkronkan..." else "Sinkron intervals.icu") }

        if (st.status.isNotBlank()) {
            Text(st.status, color = Pal.Sub, fontSize = 12.sp)
        }
    }
}
