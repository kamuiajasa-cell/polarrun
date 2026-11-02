package com.polarrun.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Settings(
    val thrHr: Int = 169,
    val runDays: List<Int> = listOf(1, 4, 6), // ISO: 1=Senin ... 7=Minggu
    val baseWeekMin: Int = 100,
    val hardWorkSec: Int = 180,
    val hardRestSec: Int = 120,
    val runWalk: Boolean = true,
    val runSec: Int = 225,
    val walkSec: Int = 30,
    val planStart: String = "",
    val athleteId: String = "",
    val apiKey: String = ""
)

data class Journal(
    val date: String,
    val sleep: Int,
    val legs: Int,
    val sore: Int,
    val redFlag: Boolean,
    val stress: Boolean,
    val caffeine: Boolean,
    val hydration: Boolean,
    val nutrition: Boolean
)

data class Wellness(
    val date: String,
    val hrv: Double?,
    val rhr: Double?,
    val sleepSec: Double?,
    val ctl: Double?,
    val atl: Double?
)

data class WeekStat(val week: String, val min: Int, val km: Double)

fun JSONObject.dbl(name: String): Double? =
    if (has(name) && !isNull(name)) getDouble(name) else null

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("polarrun", Context.MODE_PRIVATE)

    private fun thisMonday(): String {
        val d = LocalDate.now()
        return d.minusDays((d.dayOfWeek.value - 1).toLong()).toString()
    }

    fun loadSettings(): Settings {
        val raw = sp.getString("settings", null)
        if (raw == null) {
            val s = Settings(planStart = thisMonday())
            saveSettings(s)
            return s
        }
        val o = JSONObject(raw)
        val d = Settings()
        val arr = o.optJSONArray("runDays")
        val days = if (arr == null) d.runDays else (0 until arr.length()).map { arr.getInt(it) }
        return Settings(
            thrHr = o.optInt("thrHr", d.thrHr),
            runDays = days,
            baseWeekMin = o.optInt("baseWeekMin", d.baseWeekMin),
            hardWorkSec = o.optInt("hardWorkSec", d.hardWorkSec),
            hardRestSec = o.optInt("hardRestSec", d.hardRestSec),
            runWalk = o.optBoolean("runWalk", d.runWalk),
            runSec = o.optInt("runSec", d.runSec),
            walkSec = o.optInt("walkSec", d.walkSec),
            planStart = o.optString("planStart", "").ifBlank { thisMonday() },
            athleteId = o.optString("athleteId", ""),
            apiKey = o.optString("apiKey", "")
        )
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
        o.put("thrHr", s.thrHr)
        o.put("runDays", JSONArray(s.runDays))
        o.put("baseWeekMin", s.baseWeekMin)
        o.put("hardWorkSec", s.hardWorkSec)
        o.put("hardRestSec", s.hardRestSec)
        o.put("runWalk", s.runWalk)
        o.put("runSec", s.runSec)
        o.put("walkSec", s.walkSec)
        o.put("planStart", s.planStart)
        o.put("athleteId", s.athleteId)
        o.put("apiKey", s.apiKey)
        sp.edit().putString("settings", o.toString()).apply()
    }

    fun loadJournal(date: String): Journal? {
        val raw = sp.getString("j_$date", null) ?: return null
        val o = JSONObject(raw)
        return Journal(
            date = date,
            sleep = o.optInt("sleep", 2),
            legs = o.optInt("legs", 2),
            sore = o.optInt("sore", 2),
            redFlag = o.optBoolean("redFlag", false),
            stress = o.optBoolean("stress", false),
            caffeine = o.optBoolean("caffeine", false),
            hydration = o.optBoolean("hydration", false),
            nutrition = o.optBoolean("nutrition", false)
        )
    }

    fun saveJournal(j: Journal) {
        val o = JSONObject()
        o.put("sleep", j.sleep)
        o.put("legs", j.legs)
        o.put("sore", j.sore)
        o.put("redFlag", j.redFlag)
        o.put("stress", j.stress)
        o.put("caffeine", j.caffeine)
        o.put("hydration", j.hydration)
        o.put("nutrition", j.nutrition)
        sp.edit().putString("j_${j.date}", o.toString()).apply()
    }

    fun loadWellness(): List<Wellness> {
        val raw = sp.getString("wellness", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Wellness(
                o.optString("date", ""),
                o.dbl("hrv"), o.dbl("rhr"), o.dbl("sleepSec"), o.dbl("ctl"), o.dbl("atl")
            )
        }
    }

    fun saveWellness(list: List<Wellness>) {
        val arr = JSONArray()
        for (w in list) {
            val o = JSONObject()
            o.put("date", w.date)
            w.hrv?.let { o.put("hrv", it) }
            w.rhr?.let { o.put("rhr", it) }
            w.sleepSec?.let { o.put("sleepSec", it) }
            w.ctl?.let { o.put("ctl", it) }
            w.atl?.let { o.put("atl", it) }
            arr.put(o)
        }
        sp.edit().putString("wellness", arr.toString()).apply()
    }

    fun loadWeekStat(): WeekStat? {
        val raw = sp.getString("weekstat", null) ?: return null
        val o = JSONObject(raw)
        return WeekStat(o.optString("week", ""), o.optInt("min", 0), o.optDouble("km", 0.0))
    }

    fun saveWeekStat(w: WeekStat) {
        val o = JSONObject()
        o.put("week", w.week)
        o.put("min", w.min)
        o.put("km", w.km)
        sp.edit().putString("weekstat", o.toString()).apply()
    }
}
