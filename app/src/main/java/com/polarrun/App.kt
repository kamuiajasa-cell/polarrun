package com.polarrun

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.polarrun.data.ActivityRec
import com.polarrun.data.IntervalsClient
import com.polarrun.data.Journal
import com.polarrun.data.PlanEvent
import com.polarrun.data.Prefs
import com.polarrun.data.Settings
import com.polarrun.data.WatchBridge
import com.polarrun.data.Wellness
import com.polarrun.engine.Planner
import com.polarrun.engine.Readiness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppState(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val prefs = Prefs(ctx)

    var settings by mutableStateOf(prefs.loadSettings())
    var wellness by mutableStateOf<List<Wellness>>(prefs.loadWellness())
    var activities by mutableStateOf<List<ActivityRec>>(prefs.loadActivities())
    var scores by mutableStateOf<Map<String, Int>>(prefs.loadScores())
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
        val sc = Readiness.score(j, wellness)
        if (sc != null) {
            val m = scores + (j.date to sc)
            scores = m
            prefs.saveScores(m)
        }
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
            val fetched = IntervalsClient.fetchWellness(s.athleteId.trim(), s.apiKey.trim())
            val fromWatch = withContext(Dispatchers.IO) { WatchBridge.wellness(appCtx) }
            val w = WatchBridge.merge(fetched, fromWatch)
            wellness = w
            prefs.saveWellness(w)
            val a = IntervalsClient.fetchActivities(s.athleteId.trim(), s.apiKey.trim())
            activities = a
            prefs.saveActivities(a)
            status = "Sinkron intervals.icu berhasil (${w.size} hari wellness, ${a.size} lari)."
        } catch (e: Exception) {
            status = "Gagal sinkron: ${e.message}"
        } finally {
            busy = false
        }
    }

    /** Baca data jam (HRV, HR istirahat, tidur) dari database hasil sinkron dan pakai untuk skor kesiapan. */
    suspend fun pullWatch(): Int {
        busy = true
        try {
            val fresh = withContext(Dispatchers.IO) { WatchBridge.wellness(appCtx) }
            if (fresh.isEmpty()) {
                status = "Belum ada data jam. Sinkronkan jam dulu."
                return 0
            }
            val merged = WatchBridge.merge(wellness, fresh).takeLast(60)
            wellness = merged
            prefs.saveWellness(merged)
            val h = fresh.count { it.hrv != null }
            status = "Data jam dipakai: ${fresh.size} hari (HRV $h hari, tidur ${fresh.count { it.sleepSec != null }} hari)."
            return fresh.size
        } catch (e: Exception) {
            status = "Gagal membaca data jam: ${e.message}"
            return 0
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
