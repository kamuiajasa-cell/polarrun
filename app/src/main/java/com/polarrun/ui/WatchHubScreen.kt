package com.polarrun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polarrun.AppState

/** Tab Jam: "Sambungan" (scan, ikat, sinkron) dan "Data" (isi database jam). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHubScreen(st: AppState, modifier: Modifier = Modifier) {
    var page by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = page == 0, onClick = { page = 0 }, label = { Text("Sambungan") })
            FilterChip(selected = page == 1, onClick = { page = 1 }, label = { Text("Data") })
        }
        if (page == 0) WatchScreen(st, Modifier.weight(1f)) else WatchDataScreen(st, Modifier.weight(1f))
    }
}
