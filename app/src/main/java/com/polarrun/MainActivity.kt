package com.polarrun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.polarrun.ui.JournalScreen
import com.polarrun.ui.PlanScreen
import com.polarrun.ui.SettingsScreen
import com.polarrun.ui.TodayScreen

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF00F19F),
    onPrimary = Color(0xFF00110A),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF16161C),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF1F1F27),
    onSurfaceVariant = Color(0xFFB8B8C4)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            MaterialTheme(colorScheme = DarkScheme) {
                Root(state)
            }
        }
    }
}

@Composable
fun Root(st: AppState) {
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf("Hari Ini", "Rencana", "Jurnal", "Setelan")
    val marks = listOf("●", "▤", "✎", "⚙")
    Scaffold(
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { i, l ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(marks[i]) },
                        label = { Text(l) }
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
            else -> SettingsScreen(st, m)
        }
    }
}
