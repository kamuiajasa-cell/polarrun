package com.polarrun

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.polarrun.data.IntervalsClient
import com.polarrun.data.Journal
import com.polarrun.data.PlanEvent
import com.polarrun.data.Prefs
import com.polarrun.data.Settings
import com.polarrun.data.WeekStat
import com.polarrun.data.Wellness
import com.polarrun.engine.Planner
import java.time.LocalDate

class AppState(ctx: Context) {
    private val prefs = Prefs(ctx)

    var settings by mutableStateOf(prefs.loadSettings())
    var wellness by mutableStateOf<List<Wellness>>(prefs.loadWellness())
    var weekStat by mutableStateOf<WeekStat?>(prefs.loadWeekStat())
    var journal by mutableStateOf<Journal?>(prefs.loadJournal(LocalDate.now().toString()))
    var status by mutableStateOf("")
    var busy by mutableStateOf(false)

    fun saveSettings(s: Settings) {
        settings = s
        prefs.saveSettings(s)
    }

    fun submitJournal(j: Journal) {
        journal = j
        prefs.saveJournal(j)
    }

    private fun hasCreds(): Boolean {
        val s = settings
        if (s.athleteId.isBlank() || s.apiKey.isBlank()) {
            status = "Isi Athlete ID dan API key intervals.icu di Setelan dulu."
            return false
        }
        return true
    }

    suspend fun syncPull() {
        if (!hasCreds()) return
        val s = settings
        busy = true
        try {
            val w = IntervalsClient.fetchWellness(s.athleteId.trim(), s.apiKey.trim())
            wellness = w
            prefs.saveWellness(w)
            val monday = Planner.weekStart(LocalDate.now())
            val ws = IntervalsClient.fetchWeekStat(s.athleteId.trim(), s.apiKey.trim(), monday)
            weekStat = ws
            prefs.saveWeekStat(ws)
            status = "Sinkron intervals.icu berhasil (${w.size} hari data)."
        } catch (e: Exception) {
            status = "Gagal sinkron: ${e.message}"
        } finally {
            busy = false
        }
    }

    suspend fun pushPlan() {
        if (!hasCreds()) return
        val s = settings
        busy = true
        try {
            val today = LocalDate.now()
            val monday = Planner.weekStart(today)
            val events = (0..1).flatMap { w ->
                Planner.buildWeek(s, monday.plusWeeks(w.toLong()))
                    .filter { !it.date.isBefore(today) }
                    .map {
                        PlanEvent(
                            it.date.toString(),
                            "PolarRun: ${it.kind.label}",
                            Planner.describe(it.kind, it.totalMin, it.reps, it.workSec, it.restSec, s),
                            it.totalMin * 60
                        )
                    }
            }
            IntervalsClient.pushPlan(
                s.athleteId.trim(), s.apiKey.trim(),
                monday.toString(), monday.plusWeeks(2).minusDays(1).toString(), events
            )
            status = "${events.size} sesi terkirim ke kalender intervals.icu."
        } catch (e: Exception) {
            status = "Gagal kirim rencana: ${e.message}"
        } finally {
            busy = false
        }
    }
}
