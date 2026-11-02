package com.polarrun.engine

import com.polarrun.data.ActivityRec
import com.polarrun.data.Settings
import java.time.LocalDate
import kotlin.math.roundToInt

data class WeekAgg(
    val monday: LocalDate,
    val min: Int,
    val km: Double,
    val easy: Int,
    val mod: Int,
    val hard: Int
) {
    val zoneTotal: Int get() = easy + mod + hard
}

data class Compliance(val planned: Int, val done: Int, val marks: List<Boolean>) {
    val pct: Int get() = if (planned == 0) 0 else (done * 100.0 / planned).roundToInt()
}

object Analytics {
    // n minggu terakhir, urut dari yang terlama ke minggu ini
    fun weeks(acts: List<ActivityRec>, n: Int, today: LocalDate): List<WeekAgg> {
        val thisMon = Planner.weekStart(today)
        return (n - 1 downTo 0).map { k ->
            val mon = thisMon.minusWeeks(k.toLong())
            val lo = mon.toString()
            val hi = mon.plusDays(6).toString()
            val sel = acts.filter { it.date >= lo && it.date <= hi }
            WeekAgg(
                mon,
                sel.sumOf { it.min },
                sel.sumOf { it.km },
                sel.sumOf { it.easySec },
                sel.sumOf { it.modSec },
                sel.sumOf { it.hardSec }
            )
        }
    }

    // Sesi terencana yang sudah lewat (4 minggu terakhir): dianggap terlaksana kalau ada lari >= 50% durasi rencana
    fun compliance(s: Settings, acts: List<ActivityRec>, today: LocalDate): Compliance {
        val start = Planner.weekStart(LocalDate.parse(s.planStart))
        val thisMon = Planner.weekStart(today)
        var mon = thisMon.minusWeeks(3)
        if (mon.isBefore(start)) mon = start
        val dayMin = acts.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.min } }
        val marks = ArrayList<Boolean>()
        while (!mon.isAfter(thisMon)) {
            for (x in Planner.buildWeek(s, mon)) {
                if (x.date.isBefore(today) && !x.date.isBefore(start)) {
                    marks.add((dayMin[x.date.toString()] ?: 0) >= x.totalMin * 0.5)
                }
            }
            mon = mon.plusWeeks(1)
        }
        return Compliance(marks.size, marks.count { it }, marks.takeLast(10))
    }
}
