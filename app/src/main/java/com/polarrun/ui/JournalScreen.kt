package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polarrun.AppState
import com.polarrun.data.Journal
import com.polarrun.engine.Coach
import com.polarrun.engine.Kind
import com.polarrun.engine.Planner
import com.polarrun.engine.Readiness
import java.time.LocalDate

private fun lab(v: Int, a: String, b: String, c: String): String =
    if (v == 3) a else if (v == 2) b else c

@Composable
fun JournalScreen(st: AppState, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val saved = st.journal

    var editing by remember { mutableStateOf(saved == null) }
    var step by remember { mutableIntStateOf(0) }
    var sleep by remember { mutableIntStateOf(saved?.sleep ?: 2) }
    var legs by remember { mutableIntStateOf(saved?.legs ?: 2) }
    var sore by remember { mutableIntStateOf(saved?.sore ?: 2) }
    var redFlag by remember { mutableStateOf(saved?.redFlag ?: false) }
    var stress by remember { mutableStateOf(saved?.stress ?: false) }
    var caffeine by remember { mutableStateOf(saved?.caffeine ?: false) }
    var hydration by remember { mutableStateOf(saved?.hydration ?: false) }
    var nutrition by remember { mutableStateOf(saved?.nutrition ?: false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!editing && saved != null) {
            val sc = Readiness.score(saved, st.wellness)
            Eyebrow("Jurnal hari ini")
            Text("Sudah terisi", fontSize = 26.sp, fontWeight = FontWeight.Light)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring((sc ?: 0) / 100f, scoreColor(sc), 96.dp, 9.dp) {
                        Text(sc?.toString() ?: "–", fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tidur: ${lab(saved.sleep, "Segar", "Biasa", "Kurang")}")
                        Text("Kaki & badan: ${lab(saved.legs, "Ringan", "Biasa", "Berat")}")
                        Text("Otot pegal: ${lab(saved.sore, "Tidak ada", "Sedikit", "Banyak")}")
                        if (saved.redFlag) Text("Tanda bahaya: ya", color = Pal.Hard)
                    }
                }
            }
            PrimaryButton("Isi ulang") {
                editing = true
                step = 0
            }
        } else {
            StepBar(step, 6)
            when (step) {
                0 -> {
                    Eyebrow("1 dari 6")
                    Text("Bagaimana tidur semalam?", fontSize = 26.sp, fontWeight = FontWeight.Light)
                    OptionCard("Segar", "Cukup dan nyenyak", sleep == 3, Pal.Lime) { sleep = 3; step = 1 }
                    OptionCard("Biasa", "Lumayan", sleep == 2, Pal.Lime) { sleep = 2; step = 1 }
                    OptionCard("Kurang", "Sedikit atau terganggu", sleep == 1, Pal.Lime) { sleep = 1; step = 1 }
                }
                1 -> {
                    Eyebrow("2 dari 6")
                    Text("Bagaimana rasa kaki dan badan?", fontSize = 26.sp, fontWeight = FontWeight.Light)
                    OptionCard("Ringan", "Siap melangkah", legs == 3, Pal.Lime) { legs = 3; step = 2 }
                    OptionCard("Biasa", "Normal saja", legs == 2, Pal.Lime) { legs = 2; step = 2 }
                    OptionCard("Berat", "Terasa berat atau lelah", legs == 1, Pal.Lime) { legs = 1; step = 2 }
                }
                2 -> {
                    Eyebrow("3 dari 6")
                    Text("Seberapa pegal ototnya?", fontSize = 26.sp, fontWeight = FontWeight.Light)
                    OptionCard("Tidak ada", "Otot terasa segar", sore == 3, Pal.Lime) { sore = 3; step = 3 }
                    OptionCard("Sedikit", "Pegal ringan", sore == 2, Pal.Lime) { sore = 2; step = 3 }
                    OptionCard("Banyak", "Pegal jelas terasa", sore == 1, Pal.Lime) { sore = 1; step = 3 }
                }
                3 -> {
                    Eyebrow("4 dari 6")
                    Text("Ada nyeri yang tidak biasa?", fontSize = 26.sp, fontWeight = FontWeight.Light)
                    OptionCard("Tidak ada", "Tidak ada nyeri tajam atau pincang", !redFlag, Pal.Easy) {
                        redFlag = false
                        step = 4
                    }
                    OptionCard("Ada", "Nyeri tajam, pincang, atau nyeri sendi yang tidak biasa", redFlag, Pal.Hard) {
                        redFlag = true
                        step = 4
                    }
                }
                4 -> {
                    Eyebrow("5 dari 6")
                    Text("Kebiasaan kemarin", fontSize = 26.sp, fontWeight = FontWeight.Light)
                    Panel {
                        ToggleRow("Stres kerja tinggi", stress) { stress = it }
                        ToggleRow("Kafein sore", caffeine) { caffeine = it }
                        ToggleRow("Hidrasi cukup", hydration) { hydration = it }
                        ToggleRow("Nutrisi pemulihan baik", nutrition) { nutrition = it }
                    }
                    PrimaryButton("Lanjut") { step = 5 }
                }
                else -> {
                    val temp = Journal(
                        today.toString(), sleep, legs, sore,
                        redFlag, stress, caffeine, hydration, nutrition
                    )
                    val sc = Readiness.score(temp, st.wellness)
                    val planned = Planner.buildWeek(st.settings, Planner.weekStart(today))
                        .firstOrNull { it.date == today }
                    val adv = Coach.advise(planned, sc, temp)
                    Eyebrow("6 dari 6 · ringkasan")
                    Panel(accent = scoreColor(sc)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Ring((sc ?: 0) / 100f, scoreColor(sc), 96.dp, 9.dp) {
                                Text(sc?.toString() ?: "–", fontSize = 30.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(18.dp))
                            Column {
                                Eyebrow("Sesi hari ini", kindColor(adv.kind))
                                Text(
                                    if (adv.kind == Kind.REST) "Istirahat"
                                    else "${adv.kind.label} · ${adv.totalMin} menit",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Panel {
                        Eyebrow("Coach", Pal.Lime)
                        Text(adv.message)
                    }
                    PrimaryButton("Submit") {
                        st.submitJournal(temp)
                        editing = false
                        st.status = "Jurnal tersimpan."
                    }
                }
            }
            Row {
                if (step > 0) {
                    TextButton(onClick = { step -= 1 }) { Text("‹ Kembali", color = Pal.Sub) }
                }
                if (saved != null) {
                    TextButton(onClick = { editing = false }) { Text("Batal", color = Pal.Sub) }
                }
            }
        }
    }
}
