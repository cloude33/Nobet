package com.example.nobet.ui.calendar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nobet.utils.TurkishHolidays
import com.google.gson.Gson
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.max

@Composable
fun CalendarScreen(
    padding: PaddingValues, 
    vm: ScheduleViewModel = viewModel(),
    currentMonth: YearMonth = YearMonth.now(),
    onMonthChanged: (YearMonth) -> Unit = {},
    showSettingsOnLaunch: Boolean = false
) {
    val context = LocalContext.current
    
    // Initialize ViewModel with context for data persistence
    LaunchedEffect(Unit) {
        vm.initializeWithContext(context)
    }
    
    // Use the shared current month state
    // var currentMonth by remember { mutableStateOf(YearMonth.now()) } // Removed - now comes from parent
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var dialogFor by remember { mutableStateOf<LocalDate?>(null) }
    var selectedShiftColor by remember { mutableStateOf<Color?>(null) }
    var showSettingsDialog by remember { mutableStateOf(showSettingsOnLaunch) }

    val locale = java.util.Locale.forLanguageTag("tr-TR")
    val overtimeResult = vm.calculateOvertime(currentMonth)
    val holidays = remember(currentMonth) { vm.getHolidaysForMonth(currentMonth) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Modern Başlık Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    "Nöbet",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Takvimim",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // Modern Ay Navigasyonu
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    val newMonth = currentMonth.minusMonths(1)
                    onMonthChanged(newMonth)
                }) {
                    Icon(
                        Icons.Filled.ChevronLeft, 
                        contentDescription = "Önceki ay",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                val monthText = currentMonth.month.getDisplayName(TextStyle.FULL, locale)
                Text(
                    "${monthText.replaceFirstChar { it.titlecase(locale) }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { 
                    val newMonth = currentMonth.plusMonths(1)
                    onMonthChanged(newMonth)
                }) {
                    Icon(
                        Icons.Filled.ChevronRight, 
                        contentDescription = "Sonraki ay",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Takvim
        key(currentMonth) {
            SelectableCalendar(
                calendarState = rememberSelectableCalendarState(initialMonth = currentMonth),
                dayContent = { dayState ->
                    DayCell(
                        date = dayState.date,
                        schedule = vm.schedule,
                        isSelected = selectedDate == dayState.date,
                        selectedShiftColor = selectedShiftColor,
                        holidayInfo = vm.getHolidayInfo(dayState.date),
                        onClick = {
                            selectedDate = it
                            dialogFor = it
                            selectedShiftColor = vm.schedule[it]?.color
                        }
                    )
                },
                monthHeader = { monthState -> 
                    val newMonth = monthState.currentMonth
                    if (newMonth != currentMonth) {
                        onMonthChanged(newMonth)
                    }
                },
                firstDayOfWeek = DayOfWeek.MONDAY
            )
        }

        Spacer(Modifier.height(16.dp))

        // Modern Legend Kartı
        /*
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Nöbet Türleri",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Legend()
            }
        }
        */
        
        Spacer(Modifier.height(16.dp))
        
        // Modern İstatistik Kartları
        val monthText = currentMonth.month.getDisplayName(TextStyle.FULL, locale)
        val missingHours = max(0, overtimeResult.totalExpectedHours - overtimeResult.workedHours)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${monthText.replaceFirstChar { it.titlecase(locale) }} ${currentMonth.year} Özeti",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                
                // Toplam Çalışma Saati
                StatisticRow(
                    icon = "⏰",
                    label = "Toplam Çalışma",
                    value = "${overtimeResult.workedHours} Saat",
                    valueColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                )
                
                // Fazla Çalışma Saati
                StatisticRow(
                    icon = "📈",
                    label = "Fazla Çalışma",
                    value = "${overtimeResult.overtimeHours} Saat",
                    valueColor = Color(0xFFD32F2F),
                    backgroundColor = Color(0xFFFFEBEE)
                )
                
                // Eksik Mesai
                StatisticRow(
                    icon = "⚠️",
                    label = "Eksik Mesai",
                    value = "$missingHours Saat",
                    valueColor = if (missingHours > 0) Color(0xFFFF6F00) else MaterialTheme.colorScheme.onSurface,
                    backgroundColor = if (missingHours > 0) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }

    // Nöbet Seçim Diyaloğu — SADECE 3 TİP
    if (dialogFor != null) {
        ShiftDialog(
            selectedDate = dialogFor,
            onDismiss = { dialogFor = null; selectedShiftColor = null },
            onSelect = { type ->
                dialogFor?.let { vm.set(it, type); selectedShiftColor = type.color }
                dialogFor = null
            },
            onClear = {
                dialogFor?.let { vm.set(it, null); selectedShiftColor = null }
                dialogFor = null
            },
            vm = vm
        )
    }
    
    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Ayarlar") },
            text = {
                SettingsDialogContent(
                    vm = vm,
                    currentMonth = currentMonth,
                    onDismiss = { showSettingsDialog = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

}

@Composable
private fun DayCell(
    date: LocalDate,
    schedule: Map<LocalDate, ShiftType>,
    isSelected: Boolean,
    selectedShiftColor: Color?,
    holidayInfo: com.example.nobet.utils.TurkishHolidays.Holiday?,
    onClick: (LocalDate) -> Unit
) {
    val shift = schedule[date]
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val isToday = date == LocalDate.now()
    val isHoliday = holidayInfo != null

    // Özel nöbet rengi kontrolü
    val bgColor: Color = when {
        shift != null -> {
            if (shift == ShiftType.MORNING) {
                // Özel nöbet türü kontrolü
                val customShiftsKey = "custom_shift_$date"
                val sharedPreferences = LocalContext.current.getSharedPreferences("nobet_schedule_prefs", Context.MODE_PRIVATE)
                val customShiftJson = sharedPreferences.getString(customShiftsKey, null)
                if (customShiftJson != null) {
                    try {
                        val gson = Gson()
                        val customShift: CustomShiftType = gson.fromJson(customShiftJson, CustomShiftType::class.java)
                        customShift.color
                    } catch (e: Exception) {
                        shift.color
                    }
                } else {
                    shift.color
                }
            } else {
                shift.color
            }
        }
        isSelected && selectedShiftColor != null -> selectedShiftColor.copy(alpha = 0.7f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        isHoliday -> when (holidayInfo?.type) {
            com.example.nobet.utils.TurkishHolidays.HolidayType.OFFICIAL -> Color.Red.copy(alpha = 0.3f)
            com.example.nobet.utils.TurkishHolidays.HolidayType.RELIGIOUS -> Color.Green.copy(alpha = 0.3f)
            com.example.nobet.utils.TurkishHolidays.HolidayType.HALF_DAY -> Color(0xFFFFA500).copy(alpha = 0.3f) // Orange
            else -> Color.Transparent
        }
        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        isWeekend -> Color.LightGray.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val textColor: Color = when {
        shift != null -> if (shift.color.luminance() < 0.5f) Color.White else Color.Black
        isSelected && selectedShiftColor != null -> if (selectedShiftColor.luminance() < 0.5f) Color.White else Color.Black
        isSelected -> if (MaterialTheme.colorScheme.primary.luminance() < 0.5f) Color.White else Color.Black
        isHoliday -> Color.Black
        isWeekend -> Color.Red
        else -> Color.Black
    }

    Surface(onClick = { onClick(date) }, color = Color.Transparent) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun StatisticRow(
    icon: String,
    label: String,
    value: String,
    valueColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
    }
}

@Composable
private fun Legend() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row with MORNING and NIGHT
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ShiftType.MORNING.LegendBoxCompact()
            ShiftType.NIGHT.LegendBoxCompact()
        }
        
        // Second row with FULL, ANNUAL_LEAVE, and REPORT
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ShiftType.FULL.LegendBoxCompact()
            ShiftType.ANNUAL_LEAVE.LegendBoxCompact()
            ShiftType.REPORT.LegendBoxCompact()
        }
    }
}

@Composable
private fun ShiftType.LegendBoxCompact() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(this@LegendBoxCompact.color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${this@LegendBoxCompact.label} (${this@LegendBoxCompact.hours} Saat)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ShiftType.LegendBoxLarge(showSeparator: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(this@LegendBoxLarge.color)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = this@LegendBoxLarge.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (showSeparator) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "|",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun ShiftDialog(
    selectedDate: LocalDate?,
    onDismiss: () -> Unit, 
    onSelect: (ShiftType) -> Unit, 
    onClear: () -> Unit, 
    vm: ScheduleViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Nöbet Seçiniz") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Get all shift types including custom ones
                val allShiftTypes = vm.getCurrentShiftTypes()
                
                // Display all shift types
                allShiftTypes.forEach { dynamicShift ->
                    Button(
                        onClick = { 
                            // For predefined shifts
                            if (dynamicShift.type != null) {
                                // Standart nöbet türleri için
                                onSelect(dynamicShift.type)
                            } else if (dynamicShift.id != null) {
                                // Özel nöbet türleri için
                                // Özel nöbet türünü ShiftType'a dönüştürüp kaydediyoruz
                                // Burada önemli olan, özel nöbet türünün saatini doğru şekilde kaydetmek
                                selectedDate?.let { date ->
                                    // Özel nöbet türünü kaydet
                                    vm.setCustomShift(date, dynamicShift.id, dynamicShift.label, dynamicShift.hours, dynamicShift.color)
                                    // Diyaloğu kapat
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicShift.color, contentColor = Color.White)
                    ) {
                        Text("${dynamicShift.label} (${dynamicShift.hours} Saat)")
                    }
                }
                
                // Add Annual Leave and Report options
                Button(
                    onClick = { onSelect(ShiftType.ANNUAL_LEAVE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftType.ANNUAL_LEAVE.color, contentColor = Color.White)
                ) {
                    Text("${ShiftType.ANNUAL_LEAVE.label} (${ShiftType.ANNUAL_LEAVE.hours} Saat) - Yıllık İzin")
                }
                
                Button(
                    onClick = { onSelect(ShiftType.REPORT) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftType.REPORT.color, contentColor = Color.White)
                ) {
                    Text("${ShiftType.REPORT.label} (${ShiftType.REPORT.hours} Saat) - Rapor")
                }
                
                // Show arife day warning if applicable
                selectedDate?.let { date ->
                    val holidayInfo = vm.getHolidayInfo(date)
                    if (holidayInfo?.type == com.example.nobet.utils.TurkishHolidays.HolidayType.HALF_DAY) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    "⚠️ Arife Günü Uyarısı",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Bu gün ${holidayInfo.name}. Seçilen nöbet saat sayısı ne olursa olsun maksimum 5 saat olarak hesaplanacaktır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                    Text("Seçimi Temizle")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("İptal")
                }
            }
        }
    )
}

// Add a new composable to show the settings dialog
@Composable
fun SettingsScreen(padding: PaddingValues, vm: ScheduleViewModel) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    // Initialize ViewModel with context for data persistence
    LaunchedEffect(Unit) {
        vm.initializeWithContext(context)
    }
    
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Modern Başlık Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    "⚙️ ",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Ayarlar",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Show the settings dialog content directly in the screen
        com.example.nobet.ui.calendar.SettingsDialogContent(
            vm = vm,
            currentMonth = currentMonth,
            onDismiss = {}
        )
    }
}

// Create a separate composable for the settings dialog content so we can reuse it
@Composable
fun SettingsDialogContent(
    vm: ScheduleViewModel,
    currentMonth: YearMonth,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shiftTypes = vm.getCurrentShiftTypes()
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Scrollable Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Nöbet Saatleri") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Bildirim") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Hakkında") }
            )
        }

        Spacer(Modifier.height(16.dp))
        
        // Tab Content
        when (selectedTab) {
            0 -> ShiftHoursTab(vm = vm, shiftTypes = shiftTypes)
            1 -> NotificationSettingsTab(vm = vm)
            2 -> AboutSection()
        }

    }
}
