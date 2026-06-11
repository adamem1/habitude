package com.adam.habituator.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adam.habituator.ui.analytics.AnalyticsScreen
import com.adam.habituator.ui.track.TrackScreen

private enum class HabituatorTab(val label: String) {
    Track("Track"),
    Analytics("Analytics"),
}

@Composable
fun HabituatorApp() {
    var selectedTab by remember { mutableStateOf(HabituatorTab.Track) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == HabituatorTab.Track,
                    onClick = { selectedTab = HabituatorTab.Track },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text(HabituatorTab.Track.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == HabituatorTab.Analytics,
                    onClick = { selectedTab = HabituatorTab.Analytics },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text(HabituatorTab.Analytics.label) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HabituatorTab.Track -> TrackScreen(modifier = Modifier.padding(innerPadding))
            HabituatorTab.Analytics -> AnalyticsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
