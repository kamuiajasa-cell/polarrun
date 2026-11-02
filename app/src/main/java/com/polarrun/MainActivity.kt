package com.polarrun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.polarrun.ui.AnalysisScreen
import com.polarrun.ui.JournalScreen
import com.polarrun.ui.Pal
import com.polarrun.ui.PlanScreen
import com.polarrun.ui.PolarScheme
import com.polarrun.ui.SettingsScreen
import com.polarrun.ui.TodayScreen
import com.polarrun.ui.WatchHubScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            MaterialTheme(colorScheme = PolarScheme) {
                Root(state)
            }
        }
    }
}

@Composable
fun Root(st: AppState) {
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf("Hari Ini", "Rencana", "Jurnal", "Analisa", "Jam", "Setelan")
    val marks = listOf("●", "▤", "✎", "◔", "◉", "⚙")
    Scaffold(
        containerColor = Pal.Bg,
        bottomBar = {
            NavigationBar(containerColor = Pal.Card) {
                labels.forEachIndexed { i, l ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(marks[i], fontSize = 18.sp) },
                        label = { Text(l, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Pal.Lime,
                            selectedTextColor = Pal.Lime,
                            indicatorColor = Color(0xFF2B3416),
                            unselectedIconColor = Pal.Sub,
                            unselectedTextColor = Pal.Sub
                        )
                    )
                }
            }
        }
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            0 -> TodayScreen(st, m)
            1 -> PlanScreen(st, m)
            2 -> JournalScreen(st, m)
            3 -> AnalysisScreen(st, m)
            4 -> WatchHubScreen(m)
            else -> SettingsScreen(st, m)
        }
    }
}
