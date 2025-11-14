package com.example.nobet.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nobet.ui.calendar.AnnualLeaveSettings
import com.example.nobet.ui.calendar.ScheduleViewModel
import com.example.nobet.ui.calendar.ShiftType
import com.example.nobet.utils.PdfExporter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun PeriodSelectionCard(
    selectedMode: StatisticsMode,
    selectedMonth: YearMonth,
    selectedYear: Int,
    locale: Locale,
    onShowMonthPicker: () -> Unit,
    onShowYearPicker: () -> Unit,
    showExportButtons: Boolean = true,
    onExportMonthly: ((Boolean) -> Unit)? = null,
    onExportYearly: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedMode) {
                StatisticsMode.MONTHLY -> {
                    // Month/Year selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Seçilen Dönem:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = onShowMonthPicker) {
                            Icon(Icons.Default.DateRange, null)
                            Spacer(Modifier.width(8.dp))
                            val monthName = selectedMonth.month.getDisplayName(TextStyle.FULL, locale)
                            Text("${monthName.replaceFirstChar { it.titlecase(locale) }} ${selectedMonth.year}")
                        }
                    }
                    
                    // Export buttons (only if showExportButtons is true)
                    if (showExportButtons && onExportMonthly != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onExportMonthly(true) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(4.dp))
                                Text("PDF Paylaş")
                            }
                            
                            OutlinedButton(
                                onClick = { onExportMonthly(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(4.dp))
                                Text("PDF Kaydet")
                            }
                        }
                    }
                }
                
                StatisticsMode.YEARLY -> {
                    // Year selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Seçilen Yıl:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = onShowYearPicker) {
                            Icon(Icons.Default.DateRange, null)
                            Spacer(Modifier.width(8.dp))
                            Text(selectedYear.toString())
                        }
                    }
                    
                    // Export buttons (only if showExportButtons is true)
                    if (showExportButtons && onExportYearly != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onExportYearly(true) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(4.dp))
                                Text("PDF Paylaş")
                            }
                            
                            OutlinedButton(
                                onClick = { onExportYearly(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(4.dp))
                                Text("PDF Kaydet")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyStatisticsContent(
    month: YearMonth,
    vm: ScheduleViewModel,
    locale: Locale
) {
    val holidays = remember(month) { vm.getHolidaysForMonth(month) }
    val overtimeResult = vm.calculateOvertime(month)
    val missingHours = max(0, overtimeResult.totalExpectedHours - overtimeResult.workedHours)
    
    val monthText = month.month.getDisplayName(TextStyle.FULL, locale)
    val header = "${monthText.replaceFirstChar { it.titlecase(locale) }} ${month.year} İstatistik"
    
    val currentMonthShifts = vm.schedule.filter { (date, _) -> YearMonth.from(date) == month }
    
    val counts = mapOf(
        ShiftType.MORNING to currentMonthShifts.filter { (_, type) -> type == ShiftType.MORNING }.size,
        ShiftType.NIGHT to currentMonthShifts.filter { (_, type) -> type == ShiftType.NIGHT }.size,
        ShiftType.FULL to currentMonthShifts.filter { (_, type) -> type == ShiftType.FULL }.size
    )
    
    // Count annual leave and report days
    val annualLeaveCount = currentMonthShifts.filter { (_, type) -> type == ShiftType.ANNUAL_LEAVE }.size
    val reportCount = currentMonthShifts.filter { (_, type) -> type == ShiftType.REPORT }.size
    
    val workingDayDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
        currentMonthShifts.filter { (date, _) -> date.dayOfWeek == dayOfWeek }.size
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Statistics
        SummaryCard(
            totalHours = vm.totalFor(month),
            overtimeResult = overtimeResult,
            missingHours = missingHours
        )
        
        // Shift Types
        ShiftTypesCard(counts = counts)
        
        // Annual Leave and Report Cards
        if (annualLeaveCount > 0) {
            AnnualLeaveCard(count = annualLeaveCount, annualLeaveSettings = vm.annualLeaveSettings)
        }
        
        if (reportCount > 0) {
            ReportCard(count = reportCount)
        }
        
        // Day Shift Distribution (08-16)
        val dayShiftDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
            currentMonthShifts.filter { (date, type) -> 
                date.dayOfWeek == dayOfWeek && type == ShiftType.MORNING 
            }.size
        }
        DayShiftDistributionCard(distribution = dayShiftDistribution)
        
        // Night Shift Distribution (16-08 & 08-08)
        val nightShiftDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
            currentMonthShifts.filter { (date, type) -> 
                date.dayOfWeek == dayOfWeek && (type == ShiftType.NIGHT || type == ShiftType.FULL)
            }.size
        }
        NightShiftDistributionCard(distribution = nightShiftDistribution)
        
        // Holidays
        if (holidays.isNotEmpty()) {
            HolidaysCard(holidays = holidays)
        }
        
        // Arife Day Adjustments
        val arifeDayAdjustments = getArifeDayAdjustments(vm, month)
        if (arifeDayAdjustments.isNotEmpty()) {
            ArifeDayAdjustmentsCard(adjustments = arifeDayAdjustments, vm = vm)
        }
    }
}

@Composable
fun YearlyStatisticsContent(
    year: Int,
    vm: ScheduleViewModel,
    locale: Locale
) {
    val yearlyStats = remember(year) { vm.calculateYearlyStatistics(year) }
    val annualLeaveSettings = vm.annualLeaveSettings
    
    // Count annual leave and report days for the year
    val yearStart = YearMonth.of(year, 1)
    val yearEnd = YearMonth.of(year, 12)
    val annualLeaveCount = yearStart.atDay(1).let { start ->
        val end = yearEnd.atEndOfMonth()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { date -> vm.schedule[date] == ShiftType.ANNUAL_LEAVE }
    }
    
    val reportCount = yearStart.atDay(1).let { start ->
        val end = yearEnd.atEndOfMonth()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { date -> vm.schedule[date] == ShiftType.REPORT }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Yearly Summary
        YearlySummaryCard(yearlyStats = yearlyStats)
        
        // Annual Leave Summary Card
        AnnualLeaveSummaryCard(annualLeaveSettings = annualLeaveSettings)
        
        // Yearly Shift Types
        ShiftTypesCard(counts = yearlyStats.yearlyShiftCounts)
        
        // Annual Leave and Report Cards
        if (annualLeaveCount > 0) {
            AnnualLeaveCard(count = annualLeaveCount, annualLeaveSettings = annualLeaveSettings)
        }
        
        if (reportCount > 0) {
            ReportCard(count = reportCount)
        }
        
        // Day Shift Distribution (08-16)
        DayShiftDistributionCard(distribution = yearlyStats.yearlyDayShiftDistribution)
        
        // Night Shift Distribution (16-08 & 08-08)
        NightShiftDistributionCard(distribution = yearlyStats.yearlyNightShiftDistribution)
        
        // Monthly Breakdown
        MonthlyBreakdownCard(monthlyData = yearlyStats.monthlyData, locale = locale)
    }
}

@Composable
private fun SummaryCard(
    totalHours: Int,
    overtimeResult: com.example.nobet.ui.calendar.OvertimeResult,
    missingHours: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Özet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Toplam Çalışılan:", style = MaterialTheme.typography.bodyLarge)
                Text("$totalHours saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Beklenen:", style = MaterialTheme.typography.bodyLarge)
                Text("${overtimeResult.totalExpectedHours} saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            if (overtimeResult.overtimeHours > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fazla Mesai:", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text("${overtimeResult.overtimeHours} saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            
            if (missingHours > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Eksik Saat:", style = MaterialTheme.typography.bodyLarge)
                    Text("$missingHours saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun YearlySummaryCard(yearlyStats: com.example.nobet.ui.calendar.YearlyStatistics) {
    val missingHours = max(0, yearlyStats.totalExpectedHours - yearlyStats.totalWorkedHours)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Yıllık Özet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Toplam Çalışılan:", style = MaterialTheme.typography.bodyLarge)
                Text("${yearlyStats.totalWorkedHours} saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Beklenen:", style = MaterialTheme.typography.bodyLarge)
                Text("${yearlyStats.totalExpectedHours} saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            if (yearlyStats.totalOvertimeHours > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toplam Fazla Mesai:", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text("${yearlyStats.totalOvertimeHours} saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            
            if (missingHours > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toplam Eksik Saat:", style = MaterialTheme.typography.bodyLarge)
                    Text("$missingHours saat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun ShiftTypesCard(counts: Map<ShiftType, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Nöbet Tipleri ve Sayıları",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            counts.forEach { (type, count) ->
                // Skip annual leave and report in this card, they will have their own cards
                if (type != ShiftType.ANNUAL_LEAVE && type != ShiftType.REPORT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        type.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$count Adet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun WorkingDayDistributionCard(distribution: Map<DayOfWeek, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Günlere Göre Dağılım",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            distribution.forEach { (dayOfWeek, count) ->
                val dayName = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> "Pazartesi"
                    DayOfWeek.TUESDAY -> "Salı"
                    DayOfWeek.WEDNESDAY -> "Çarşamba"
                    DayOfWeek.THURSDAY -> "Perşembe"
                    DayOfWeek.FRIDAY -> "Cuma"
                    DayOfWeek.SATURDAY -> "Cumartesi"
                    DayOfWeek.SUNDAY -> "Pazar"
                }
                
                val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                val backgroundColor = if (isWeekend)
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.tertiaryContainer
                val textColor = if (isWeekend)
                    MaterialTheme.colorScheme.onTertiary
                else
                    MaterialTheme.colorScheme.onTertiaryContainer
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor
                    )
                    Text(
                        "$count Nöbet",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DayShiftDistributionCard(distribution: Map<DayOfWeek, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Gündüz Mesaisi (08-16) - Günlere Göre Dağılım",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            distribution.forEach { (dayOfWeek, count) ->
                val dayName = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> "Pazartesi"
                    DayOfWeek.TUESDAY -> "Salı"
                    DayOfWeek.WEDNESDAY -> "Çarşamba"
                    DayOfWeek.THURSDAY -> "Perşembe"
                    DayOfWeek.FRIDAY -> "Cuma"
                    DayOfWeek.SATURDAY -> "Cumartesi"
                    DayOfWeek.SUNDAY -> "Pazar"
                }
                
                val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                val backgroundColor = if (isWeekend)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.primaryContainer
                val textColor = if (isWeekend)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor
                    )
                    Text(
                        "$count Nöbet",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun NightShiftDistributionCard(distribution: Map<DayOfWeek, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Nöbet (16-08 & 08-08) - Günlere Göre Dağılım",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            distribution.forEach { (dayOfWeek, count) ->
                val dayName = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> "Pazartesi"
                    DayOfWeek.TUESDAY -> "Salı"
                    DayOfWeek.WEDNESDAY -> "Çarşamba"
                    DayOfWeek.THURSDAY -> "Perşembe"
                    DayOfWeek.FRIDAY -> "Cuma"
                    DayOfWeek.SATURDAY -> "Cumartesi"
                    DayOfWeek.SUNDAY -> "Pazar"
                }
                
                val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                val backgroundColor = if (isWeekend)
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.secondaryContainer
                val textColor = if (isWeekend)
                    MaterialTheme.colorScheme.onSecondary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor
                    )
                    Text(
                        "$count Nöbet",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HolidaysCard(holidays: List<com.example.nobet.utils.TurkishHolidays.Holiday>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🎆 Bu Aydaki Tatiller",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            holidays.forEach { holiday ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        holiday.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "${holiday.date.dayOfMonth}/${holiday.date.monthValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArifeDayAdjustmentsCard(
    adjustments: List<Pair<LocalDate, Int>>,
    vm: ScheduleViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚠️ Tatil Günü Saat Ayarları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(8.dp))
            adjustments.forEach { (date, effectiveHours) ->
                val shift = vm.schedule[date]!!
                val normalHours = vm.getHoursForShiftType(shift)
                val holidayInfo = vm.getHolidayInfo(date)!!
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${date.dayOfMonth}/${date.monthValue} - ${holidayInfo.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${normalHours}h → ${effectiveHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tatil günlerinde nöbetler maksimum 5 saat olarak hesaplanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AnnualLeaveCard(count: Int, annualLeaveSettings: AnnualLeaveSettings? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Yıllık İzin (Yİ)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShiftType.ANNUAL_LEAVE.color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(ShiftType.ANNUAL_LEAVE.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("$count Gün", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            
            Text(
                "Not: Yıllık izin günleri çalışılan günlerden düşülür.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Show remaining annual leave if settings are provided or use default settings
            val settings = annualLeaveSettings ?: AnnualLeaveSettings()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (settings.remainingAnnualLeaveDays > 0) 
                            Color.Green.copy(alpha = 0.2f) 
                        else 
                            Color.Red.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Kalan İzin:", 
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${settings.remainingAnnualLeaveDays} gün",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// New Annual Leave Summary Card for yearly statistics
@Composable
private fun AnnualLeaveSummaryCard(annualLeaveSettings: AnnualLeaveSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Yıllık İzin Özeti",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Toplam İzin Hakkı:", 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${annualLeaveSettings.totalAnnualLeaveDays} gün",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Kullanılan İzin:", 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${annualLeaveSettings.usedAnnualLeaveDays} gün",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (annualLeaveSettings.remainingAnnualLeaveDays > 0) 
                            Color.Green.copy(alpha = 0.3f) 
                        else 
                            Color.Red.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Kalan İzin:", 
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    "${annualLeaveSettings.remainingAnnualLeaveDays} gün",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ReportCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Rapor (RP)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShiftType.REPORT.color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(ShiftType.REPORT.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("$count adet", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            
            Text(
                "Not: Rapor günleri çalışılan günlerden düşülür.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MonthlyBreakdownCard(
    monthlyData: List<com.example.nobet.ui.calendar.MonthlyStatistics>,
    locale: Locale
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Aylık Detay",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            monthlyData.forEach { monthly ->
                val monthName = monthly.month.month.getDisplayName(TextStyle.SHORT, locale)
                val missingHours = max(0, monthly.overtimeResult.totalExpectedHours - monthly.totalHours)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            monthName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Çalışılan: ${monthly.totalHours}h", style = MaterialTheme.typography.bodySmall)
                            Text("Beklenen: ${monthly.overtimeResult.totalExpectedHours}h", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        if (monthly.overtimeResult.overtimeHours > 0 || missingHours > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (monthly.overtimeResult.overtimeHours > 0) {
                                    Text(
                                        "Fazla: ${monthly.overtimeResult.overtimeHours}h", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Red
                                    )
                                }
                                if (missingHours > 0) {
                                    Text(
                                        "Eksik: ${missingHours}h", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF8800)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getArifeDayAdjustments(vm: ScheduleViewModel, month: YearMonth): List<Pair<LocalDate, Int>> {
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .filter { date ->
            // Only include dates that actually have shifts and have special holiday rules applied
            vm.schedule.containsKey(date) && vm.isSpecialHolidayRule(date)
        }
        .map { date -> date to vm.getEffectiveHoursForDate(date) }
        .toList()
}

// Export helper functions
fun exportMonthlyStatistics(
    vm: ScheduleViewModel,
    month: YearMonth,
    pdfExporter: PdfExporter,
    shareMode: Boolean,
    onResult: (String) -> Unit
) {
    // Create a MonthlyStatistics object manually since the method doesn't exist
    val monthlyStats = createMonthlyStatistics(vm, month)
    val holidays = vm.getHolidaysForMonth(month)
    val arifeDayAdjustments = getArifeDayAdjustments(vm, month)
    
    // Count annual leave and report days for the month
    val currentMonthShifts = vm.schedule.filter { (date, _) -> YearMonth.from(date) == month }
    val annualLeaveCount = currentMonthShifts.filter { (_, type) -> type == ShiftType.ANNUAL_LEAVE }.size
    val reportCount = currentMonthShifts.filter { (_, type) -> type == ShiftType.REPORT }.size
    
    pdfExporter.exportMonthlyStatistics(
        monthlyStats = monthlyStats,
        holidays = holidays,
        arifeDayAdjustments = arifeDayAdjustments,
        annualLeaveCount = annualLeaveCount,
        reportCount = reportCount,
        onSuccess = { uri ->
            if (shareMode) {
                pdfExporter.sharePdf(uri)
                onResult("PDF başarıyla oluşturuldu ve paylaşıldı!")
            } else {
                onResult("PDF başarıyla kaydedildi!")
            }
        },
        onError = { error ->
            onResult(error)
        }
    )
}

fun exportYearlyStatistics(
    vm: ScheduleViewModel,
    year: Int,
    pdfExporter: PdfExporter,
    shareMode: Boolean,
    onResult: (String) -> Unit
) {
    val yearlyStats = vm.calculateYearlyStatistics(year)
    val annualLeaveSettings = vm.annualLeaveSettings
    
    // Count annual leave and report days for the year
    val yearStart = YearMonth.of(year, 1)
    val yearEnd = YearMonth.of(year, 12)
    val annualLeaveCount = yearStart.atDay(1).let { start ->
        val end = yearEnd.atEndOfMonth()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { date -> vm.schedule[date] == ShiftType.ANNUAL_LEAVE }
    }
    
    val reportCount = yearStart.atDay(1).let { start ->
        val end = yearEnd.atEndOfMonth()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { date -> vm.schedule[date] == ShiftType.REPORT }
    }
    
    pdfExporter.exportYearlyStatistics(
        yearlyStats = yearlyStats,
        annualLeaveSettings = annualLeaveSettings,
        annualLeaveCount = annualLeaveCount,
        reportCount = reportCount,
        onSuccess = { uri ->
            if (shareMode) {
                pdfExporter.sharePdf(uri)
                onResult("PDF başarıyla oluşturuldu ve paylaşıldı!")
            } else {
                onResult("PDF başarıyla kaydedildi!")
            }
        },
        onError = { error ->
            onResult(error)
        }
    )
}

// Helper function to create MonthlyStatistics since the method doesn't exist in ScheduleViewModel
private fun createMonthlyStatistics(vm: ScheduleViewModel, month: YearMonth): com.example.nobet.ui.calendar.MonthlyStatistics {
    val currentMonthShifts = vm.schedule.filter { (date, _) -> YearMonth.from(date) == month }
    
    val shiftCounts = mapOf(
        ShiftType.MORNING to currentMonthShifts.filter { (_, type) -> type == ShiftType.MORNING }.size,
        ShiftType.NIGHT to currentMonthShifts.filter { (_, type) -> type == ShiftType.NIGHT }.size,
        ShiftType.FULL to currentMonthShifts.filter { (_, type) -> type == ShiftType.FULL }.size
    )
    
    val workingDayDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
        currentMonthShifts.filter { (date, _) -> date.dayOfWeek == dayOfWeek }.size
    }
    
    return com.example.nobet.ui.calendar.MonthlyStatistics(
        month = month,
        totalHours = vm.totalFor(month),
        overtimeResult = vm.calculateOvertime(month),
        shiftCounts = shiftCounts,
        workingDayDistribution = workingDayDistribution
    )
}

@Composable
fun ExportButtonsCard(
    selectedMode: StatisticsMode,
    onExportMonthly: (Boolean) -> Unit,
    onExportYearly: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "PDF Dışa Aktarma",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when (selectedMode) {
                            StatisticsMode.MONTHLY -> onExportMonthly(true)
                            StatisticsMode.YEARLY -> onExportYearly(true)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF Paylaş")
                }
                
                OutlinedButton(
                    onClick = {
                        when (selectedMode) {
                            StatisticsMode.MONTHLY -> onExportMonthly(false)
                            StatisticsMode.YEARLY -> onExportYearly(false)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF Kaydet")
                }
            }
        }
    }
}
