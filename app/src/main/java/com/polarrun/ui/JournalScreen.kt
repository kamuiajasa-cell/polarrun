package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polarrun.AppState
import com.polarrun.data.Journal
import java.time.LocalDate

@Composable
fun JournalScreen(st: AppState, modifier: Modifier = Modifier) {
    val today = LocalDate.now().toString()
    val saved = st.journal

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Jurnal hari ini", style = MaterialTheme.typography.headlineSmall)

        Choice("Tidur", listOf("Segar", "Biasa", "Kurang"), listOf(3, 2, 1), sleep) { sleep = it }
        Choice("Kaki & badan", listOf("Ringan", "Biasa", "Berat"), listOf(3, 2, 1), legs) { legs = it }
        Choice("Otot pegal", listOf("Tidak ada", "Sedikit", "Banyak"), listOf(3, 2, 1), sore) { sore = it }

        ToggleRow("Tanda bahaya / nyeri cedera", redFlag) { redFlag = it }

        Text("Kebiasaan", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Stres kerja tinggi", stress) { stress = it }
        ToggleRow("Kafein sore", caffeine) { caffeine = it }
        ToggleRow("Hidrasi cukup", hydration) { hydration = it }
        ToggleRow("Nutrisi pemulihan baik", nutrition) { nutrition = it }

        Button(onClick = {
            st.submitJournal(
                Journal(today, sleep, legs, sore, redFlag, stress, caffeine, hydration, nutrition)
            )
            st.status = "Jurnal tersimpan."
        }) { Text(if (saved != null) "Perbarui" else "Submit") }

        if (saved != null) {
            Text(
                "Jurnal hari ini sudah tersimpan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
