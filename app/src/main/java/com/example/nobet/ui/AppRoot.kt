package com.example.nobet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.BarChart
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
import com.example.nobet.ui.calendar.CalendarScreen
import com.example.nobet.ui.data.VerilerimScreen
import com.example.nobet.ui.notification.NotificationSettingsScreen
import com.example.nobet.ui.stats.StatisticsScreen
import com.example.nobet.ui.about.AboutScreen
import java.time.YearMonth

@Composable
fun AppRoot() {
    var selectedIndex by remember { mutableStateOf(0) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) } // Shared current month state

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { 
                        try {
                            selectedIndex = 0 
                        } catch (e: Exception) {
                            // Handle navigation error gracefully
                        }
                    },
                    icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                    label = { Text("Takvim") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { 
                        try {
                            selectedIndex = 1 
                        } catch (e: Exception) {
                            // Handle navigation error gracefully
                        }
                    },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text("İstatistik") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { 
                        try {
                            selectedIndex = 2 
                        } catch (e: Exception) {
                            // Handle navigation error gracefully
                        }
                    },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    label = { Text("Bildirim") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 3,
                    onClick = { 
                        try {
                            selectedIndex = 3 
                        } catch (e: Exception) {
                            // Handle navigation error gracefully
                        }
                    },
                    icon = { Icon(Icons.Filled.Storage, contentDescription = null) },
                    label = { Text("Verilerim") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 4,
                    onClick = { 
                        try {
                            selectedIndex = 4 
                        } catch (e: Exception) {
                            // Handle navigation error gracefully
                        }
                    },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Hakkında") }
                )
            }
        }
    ) { padding ->
        when (selectedIndex) {
            0 -> CalendarScreen(
                padding = padding,
                currentMonth = currentMonth,
                onMonthChanged = { currentMonth = it }
            )
            1 -> StatisticsScreen(
                padding = padding,
                currentMonth = currentMonth
            )
            2 -> NotificationSettingsScreen(padding)
            3 -> VerilerimScreen(padding)
            else -> AboutScreen(padding)
        }
    }
}