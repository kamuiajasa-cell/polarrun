package com.polarrun.engine

import com.polarrun.data.Settings
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Kind(val label: String) {
    EASY("Easy"),
    HARD("Hard (interval)"),
    LONG("Long run"),
    REST("Rest")
}

data class Session(
    val date: LocalDate,
    val kind: Kind,
    val totalMin: Int,
    val reps: Int = 0,
    val workSec: Int = 0,
    val restSec: Int = 0
) {
    val hardMin: Double get() = if (kind == Kind.HARD) reps * workSec / 60.0 else 0.0
}

object Planner {
    private const val WARM = 10
    private const val COOL = 10

    fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    fun weekIndex(s: Settings, monday: LocalDate): Int {
        val start = weekStart(LocalDate.parse(s.planStart))
        return max(0, ChronoUnit.WEEKS.between(start, monday).toInt())
    }

    fun isDeload(idx: Int): Boolean = idx % 4 == 3

    // Naik ~10% per minggu selama 3 minggu, minggu ke-4 turun ke 75% (pemulihan)
    fun weekMinutes(s: Settings, idx: Int): Double {
        val pos = idx % 4
        val g = (idx / 4) * 3 + minOf(pos, 2)
        var v = s.baseWeekMin * 1.10.pow(g.toDouble())
        if (pos == 3) v *= 0.75
        return v
    }

    fun buildWeek(s: Settings, monday: LocalDate): List<Session> {
        val days = s.runDays.distinct().sorted()
        val n = days.size
        if (n == 0) return emptyList()
        val total = weekMinutes(s, weekIndex(s, monday))
        if (n == 1) {
            return listOf(
                Session(monday.plusDays((days[0] - 1).toLong()), Kind.EASY, max(20, total.roundToInt()))
            )
        }
        val hardCount = if (n >= 5) 2 else 1
        val workMin = s.hardWorkSec / 60.0
        // Menit kerja keras per sesi hard, total keras mingguan <= 20% waktu mingguan
        val budget = 0.20 * total / hardCount
        val reps = floor(budget / workMin).toInt().coerceIn(2, 8)
        val longFrac = when (n) {
            2 -> 0.50
            3 -> 0.42
            else -> 0.35
        }
        val longMin = max(20.0, total * longFrac)
        val each = max(20.0, (total - longMin) / (n - 1))
        val hardIdx = if (hardCount == 1) setOf(0) else setOf(0, n / 2)
        val needHard = WARM + COOL +
            ceil((reps * s.hardWorkSec + (reps - 1) * s.hardRestSec) / 60.0).toInt()

        return days.mapIndexed { i, d ->
            val date = monday.plusDays((d - 1).toLong())
            when {
                i == n - 1 -> Session(date, Kind.LONG, longMin.roundToInt())
                i in hardIdx -> Session(
                    date, Kind.HARD, max(each.roundToInt(), needHard),
                    reps, s.hardWorkSec, s.hardRestSec
                )
                else -> Session(date, Kind.EASY, each.roundToInt())
            }
        }
    }

    fun hardShare(list: List<Session>): Double {
        val t = list.sumOf { it.totalMin }
        if (t == 0) return 0.0
        val h = list.sumOf { it.hardMin }
        return h / t
    }

    fun describe(kind: Kind, totalMin: Int, reps: Int, workSec: Int, restSec: Int, s: Settings): String {
        val easyHr = Zones.easyMaxHr(s.thrHr)
        return when (kind) {
            Kind.REST -> "Istirahat penuh."
            Kind.HARD -> {
                val lo = Zones.hardLoHr(s.thrHr)
                val hi = Zones.hardHiHr(s.thrHr)
                "Pemanasan 10 menit easy (HR maks $easyHr bpm), lalu $reps x ${mmss(workSec)} keras " +
                    "di HR $lo-$hi bpm dengan jeda ${mmss(restSec)} jalan/jog pelan, " +
                    "lalu pendinginan 10 menit. Total sekitar $totalMin menit."
            }
            else -> {
                val base = "$totalMin menit santai, HR maks $easyHr bpm (masih bisa ngobrol)."
                if (s.runWalk && s.runSec + s.walkSec > 0) {
                    val cycles = (totalMin * 60) / (s.runSec + s.walkSec)
                    "$base Pola run-walk: lari ${mmss(s.runSec)} / jalan ${mmss(s.walkSec)} x $cycles."
                } else base
            }
        }
    }
}
