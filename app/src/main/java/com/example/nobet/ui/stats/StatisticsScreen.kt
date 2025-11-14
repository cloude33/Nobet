package com.example.nobet.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nobet.ui.calendar.ScheduleViewModel
import com.example.nobet.utils.PdfExporter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

enum class StatisticsMode {
    MONTHLY, YEARLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    padding: PaddingValues, 
    vm: ScheduleViewModel = viewModel(),
    currentMonth: YearMonth = YearMonth.now()
) {
    val context = LocalContext.current
    val pdfExporter = remember { PdfExporter(context) }
    
    // Initialize ViewModel with context for data persistence
    LaunchedEffect(Unit) {
        vm.initializeWithContext(context)
    }
    
    var selectedMode by remember { mutableStateOf(StatisticsMode.MONTHLY) }
    var selectedMonth by remember(currentMonth) { mutableStateOf(currentMonth) } // Initialize with current calendar month
    var selectedYear by remember(currentMonth) { mutableIntStateOf(currentMonth.year) } // Initialize with current calendar year
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    
    val locale = java.util.Locale.forLanguageTag("tr-TR")

    Column(
        modifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Başlık Kartı
        val currentPeriodText = when (selectedMode) {
            StatisticsMode.MONTHLY -> {
                val monthName = selectedMonth.month.getDisplayName(TextStyle.FULL, locale)
                "${monthName.replaceFirstChar { it.titlecase(locale) }} ${selectedMonth.year} İstatistik"
            }
            StatisticsMode.YEARLY -> {
                "${selectedYear} Yılı İstatistikleri"
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📊 ",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    currentPeriodText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Mode Selection at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { selectedMode = StatisticsMode.MONTHLY },
                label = { Text("Aylık") },
                selected = selectedMode == StatisticsMode.MONTHLY,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                onClick = { selectedMode = StatisticsMode.YEARLY },
                label = { Text("Yıllık") },
                selected = selectedMode == StatisticsMode.YEARLY,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Period Selection (without export buttons)
        PeriodSelectionCard(
            selectedMode = selectedMode,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            locale = locale,
            onShowMonthPicker = { showMonthPicker = true },
            onShowYearPicker = { showYearPicker = true },
            showExportButtons = false
        )
        
        // Statistics Content
        when (selectedMode) {
            StatisticsMode.MONTHLY -> {
                MonthlyStatisticsContent(
                    month = selectedMonth,
                    vm = vm,
                    locale = locale
                )
            }
            StatisticsMode.YEARLY -> {
                YearlyStatisticsContent(
                    year = selectedYear,
                    vm = vm,
                    locale = locale
                )
            }
        }
        
        // Export buttons at the bottom
        ExportButtonsCard(
            selectedMode = selectedMode,
            onExportMonthly = { shareMode ->
                exportMonthlyStatistics(vm, selectedMonth, pdfExporter, shareMode) { message ->
                    showMessage = message
                }
            },
            onExportYearly = { shareMode ->
                exportYearlyStatistics(vm, selectedYear, pdfExporter, shareMode) { message ->
                    showMessage = message
                }
            }
        )
    }
    
    // Dialogs and Messages
    StatisticsDialogs(
        showMonthPicker = showMonthPicker,
        showYearPicker = showYearPicker,
        selectedMonth = selectedMonth,
        selectedYear = selectedYear,
        locale = locale,
        onMonthSelected = { month ->
            selectedMonth = month
            showMonthPicker = false
        },
        onYearSelected = { year ->
            selectedYear = year
            showYearPicker = false
        },
        onDismissMonth = { showMonthPicker = false },
        onDismissYear = { showYearPicker = false },
        showMessage = showMessage,
        onDismissMessage = { showMessage = null }
    )
}