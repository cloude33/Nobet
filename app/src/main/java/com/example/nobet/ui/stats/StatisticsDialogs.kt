package com.example.nobet.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsDialogs(
    showMonthPicker: Boolean,
    showYearPicker: Boolean,
    selectedMonth: YearMonth,
    selectedYear: Int,
    locale: Locale,
    onMonthSelected: (YearMonth) -> Unit,
    onYearSelected: (Int) -> Unit,
    onDismissMonth: () -> Unit,
    onDismissYear: () -> Unit,
    showMessage: String?,
    onDismissMessage: () -> Unit
) {
    // Month Picker Dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = selectedMonth,
            onMonthSelected = onMonthSelected,
            onDismiss = onDismissMonth,
            locale = locale
        )
    }
    
    // Year Picker Dialog
    if (showYearPicker) {
        YearPickerDialog(
            currentYear = selectedYear,
            onYearSelected = onYearSelected,
            onDismiss = onDismissYear
        )
    }
    
    // Success/Error message
    showMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            onDismissMessage()
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                TextButton(onClick = onDismissMessage) {
                    Text("TAMAM")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerDialog(
    currentMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    locale: Locale
) {
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth.monthValue) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Ay Seçin",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                // Year selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(
                        selectedYear.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Text("›", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                
                // Month selection grid
                val months = (1..12).chunked(3)
                months.forEach { monthRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        monthRow.forEach { month ->
                            val monthName = java.time.Month.of(month).getDisplayName(TextStyle.SHORT, locale)
                            FilterChip(
                                onClick = { selectedMonth = month },
                                label = { Text(monthName) },
                                selected = selectedMonth == month
                            )
                        }
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onMonthSelected(YearMonth.of(selectedYear, selectedMonth))
                    }) {
                        Text("Seç")
                    }
                }
            }
        }
    }
}

@Composable
fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentDate = LocalDate.now()
    val years = (currentDate.year - 10..currentDate.year + 5).toList()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Yıl Seçin",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(years) { year ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = year == currentYear,
                                    onClick = { onYearSelected(year) }
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = year == currentYear,
                                onClick = { onYearSelected(year) }
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                year.toString(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (year == currentYear) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                }
            }
        }
    }
}