package com.polarrun.data

import android.content.Context
import com.joyodigitama.huaweiwear.WatchData
import java.time.Instant
import java.time.ZoneId

/** Membaca database jam (hasil sinkron) dan mengubahnya jadi [Wellness] per tanggal lokal. Blocking: panggil dari IO. */
object WatchBridge {
    private fun dateOf(sec: Long): String =
        Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun wellness(ctx: Context, days: Int = 42): List<Wellness> {
        val d = WatchData(ctx.applicationContext)
        if (!d.available()) return emptyList()

        val rhr = d.days(days).filter { it.restingHr != null }.associate { it.date to it.restingHr!!.toDouble() }

        // Tidur: tanggal = hari bangun; potongan di hari yang sama dijumlahkan.
        val sleep = HashMap<String, Double>()
        d.sleepNights(days).filter { it.asleepSec > 0 }.forEach {
            val k = dateOf(it.endSec)
            sleep[k] = (sleep[k] ?: 0.0) + it.asleepSec.toDouble()
        }

        // HRV: rata-rata RMSSD per hari bangun; utamakan hasil malam, kalau tidak ada pakai apa saja.
        val all = d.hrv(days)
        val pool = all.filter { it.source == "night" }.ifEmpty { all }
        val hrv = pool.groupBy { dateOf(it.endSec) }.mapValues { e -> e.value.map { it.rmssd }.average() }

        return (rhr.keys + sleep.keys + hrv.keys).toSortedSet().map {
            Wellness(it, hrv[it], rhr[it], sleep[it], null, null)
        }
    }

    /** Nilai dari jam menimpa hrv/rhr/tidur; ctl/atl dari [base] dipertahankan. */
    fun merge(base: List<Wellness>, watch: List<Wellness>): List<Wellness> {
        val m = base.associateBy { it.date }.toMutableMap()
        for (f in watch) {
            val o = m[f.date]
            m[f.date] = Wellness(f.date, f.hrv ?: o?.hrv, f.rhr ?: o?.rhr, f.sleepSec ?: o?.sleepSec, o?.ctl, o?.atl)
        }
        return m.values.sortedBy { it.date }
    }
}
