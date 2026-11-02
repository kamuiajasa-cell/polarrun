package com.polarrun.engine

import com.polarrun.data.Journal
import com.polarrun.data.Wellness
import kotlin.math.roundToInt

object Readiness {
    private fun pts(v: Int): Double = when (v) {
        3 -> 20.0
        2 -> 14.0
        else -> 0.0
    }

    private fun avg(l: List<Double>): Double? = if (l.isEmpty()) null else l.average()

    // Skor 0-100. Null kalau jurnal hari ini belum diisi.
    fun score(j: Journal?, w: List<Wellness>): Int? {
        if (j == null) return null

        var manual = pts(j.sleep) + pts(j.legs) + pts(j.sore)
        if (j.stress) manual -= 5
        if (j.caffeine) manual -= 3
        if (j.hydration) manual += 3
        if (j.nutrition) manual += 3
        manual = manual.coerceIn(0.0, 66.0)

        var raw = manual
        var maxPts = 66.0

        val sorted = w.sortedBy { it.date }
        val latest = sorted.lastOrNull()
        if (latest != null) {
            val prev = sorted.dropLast(1).takeLast(7)

            val hrv = latest.hrv
            val hrvBase = avg(prev.mapNotNull { it.hrv })
            if (hrv != null && hrvBase != null && hrvBase > 0) {
                raw += ((hrv / hrvBase - 0.8) / 0.25).coerceIn(0.0, 1.0) * 20
                maxPts += 20
            }

            val rhr = latest.rhr
            val rhrBase = avg(prev.mapNotNull { it.rhr })
            if (rhr != null && rhrBase != null) {
                raw += (1 - (rhr - rhrBase) / 8.0).coerceIn(0.0, 1.0) * 10
                maxPts += 10
            }

            val sl = latest.sleepSec
            if (sl != null && sl > 0) {
                raw += (sl / 3600.0 / 7.0).coerceIn(0.0, 1.0) * 10
                maxPts += 10
            }
        }

        var pct = raw / maxPts * 100
        val ctl = latest?.ctl
        val atl = latest?.atl
        if (ctl != null && atl != null && ctl > 0) {
            val r = atl / ctl
            if (r > 1.5) pct -= 10 else if (r > 1.3) pct -= 5
        }
        return pct.coerceIn(0.0, 100.0).roundToInt()
    }
}
