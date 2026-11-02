package com.polarrun.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.math.roundToInt

data class PlanEvent(val date: String, val name: String, val description: String, val seconds: Int)

object IntervalsClient {
    private const val BASE = "https://intervals.icu/api/v1/athlete"

    private fun auth(key: String): String =
        "Basic " + Base64.encodeToString("API_KEY:$key".toByteArray(), Base64.NO_WRAP)

    private fun request(method: String, url: String, key: String, body: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 25000
        c.setRequestProperty("Authorization", auth(key))
        c.setRequestProperty("Accept", "application/json")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        if (code !in 200..299) throw IOException("HTTP $code ${text.take(160)}")
        return text
    }

    suspend fun fetchWellness(id: String, key: String, days: Int = 42): List<Wellness> =
        withContext(Dispatchers.IO) {
            val newest = LocalDate.now()
            val oldest = newest.minusDays(days.toLong())
            val txt = request("GET", "$BASE/$id/wellness?oldest=$oldest&newest=$newest", key, null)
            val arr = JSONArray(txt)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Wellness(
                    o.optString("id", ""),
                    o.dbl("hrv"), o.dbl("restingHR"), o.dbl("sleepSecs"), o.dbl("ctl"), o.dbl("atl")
                )
            }
        }

    // Zona HR: Z1-2 = easy, Z3 = sedang, Z4+ = keras (mengikuti zona HR di intervals.icu)
    suspend fun fetchActivities(id: String, key: String, days: Int = 56): List<ActivityRec> =
        withContext(Dispatchers.IO) {
            val newest = LocalDate.now()
            val oldest = newest.minusDays(days.toLong())
            val txt = request("GET", "$BASE/$id/activities?oldest=$oldest&newest=$newest", key, null)
            val arr = JSONArray(txt)
            val out = ArrayList<ActivityRec>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (!o.optString("type", "").contains("Run")) continue
                val date = o.optString("start_date_local", "").take(10)
                if (date.length != 10) continue
                val min = (o.optDouble("moving_time", 0.0) / 60.0).roundToInt()
                val km = o.optDouble("distance", 0.0) / 1000.0
                var easy = 0
                var mod = 0
                var hard = 0
                val z = o.optJSONArray("icu_hr_zone_times")
                if (z != null) {
                    for (k in 0 until z.length()) {
                        val sec = z.optInt(k, 0)
                        if (k <= 1) easy += sec else if (k == 2) mod += sec else hard += sec
                    }
                }
                out.add(ActivityRec(date, min, km, easy, mod, hard))
            }
            out
        }

    // Hapus event lama berawalan "PolarRun" di rentang tanggal, lalu kirim ulang (tanpa duplikat)
    suspend fun pushPlan(
        id: String, key: String, oldest: String, newest: String, events: List<PlanEvent>
    ) = withContext(Dispatchers.IO) {
        val existing = JSONArray(request("GET", "$BASE/$id/events?oldest=$oldest&newest=$newest", key, null))
        for (i in 0 until existing.length()) {
            val o = existing.getJSONObject(i)
            if (o.optString("name", "").startsWith("PolarRun")) {
                request("DELETE", "$BASE/$id/events/${o.getLong("id")}", key, null)
            }
        }
        val arr = JSONArray()
        for (e in events) {
            val o = JSONObject()
            o.put("category", "WORKOUT")
            o.put("start_date_local", "${e.date}T06:00:00")
            o.put("type", "Run")
            o.put("name", e.name)
            o.put("description", e.description)
            o.put("moving_time", e.seconds)
            arr.put(o)
        }
        if (arr.length() > 0) request("POST", "$BASE/$id/events/bulk", key, arr.toString())
    }
}
