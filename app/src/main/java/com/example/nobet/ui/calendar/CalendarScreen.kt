package com.example.nobet.ui.calendar

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
    onMonthChanged: (YearMonth) -> Unit = {}
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
    var showSettingsDialog by remember { mutableStateOf(false) }

    val locale = java.util.Locale.forLanguageTag("tr-TR")
    val overtimeResult = vm.calculateOvertime(currentMonth)
    val holidays = remember(currentMonth) { vm.getHolidaysForMonth(currentMonth) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Başlık
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp)) // Balance the settings icon
            
            Row {
                Text(
                    "Nöbet",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "Takvimim",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Ayarlar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Ay navigasyonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                val newMonth = currentMonth.minusMonths(1)
                onMonthChanged(newMonth)
            }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Önceki ay")
            }

            val monthText = currentMonth.month.getDisplayName(TextStyle.FULL, locale)
            Text(
                "${monthText.replaceFirstChar { it.titlecase(locale) }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { 
                val newMonth = currentMonth.plusMonths(1)
                onMonthChanged(newMonth)
            }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Sonraki ay")
            }
        }

        Spacer(Modifier.height(8.dp))

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
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // Legend — SADECE 3 TİP
        Legend()
        
        Spacer(Modifier.height(16.dp))
        
        // Ana Ekran İstatistikleri
        val monthText = currentMonth.month.getDisplayName(TextStyle.FULL, locale)
        val missingHours = max(0, overtimeResult.totalExpectedHours - overtimeResult.workedHours)
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${monthText.replaceFirstChar { it.titlecase(locale) }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Toplam Çalışma Saati
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Toplam Çalışma Saatin:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${overtimeResult.workedHours} Saat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Fazla Çalışma Saati (Kırmızı renkte)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.2f)
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
                        "Fazla Çalışma Saatin:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                    Text(
                        "${overtimeResult.overtimeHours} Saat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                }
            }
            
            // Eksik Mesai
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (missingHours > 0) 
                        Color(0xFFFFA500).copy(alpha = 0.1f) 
                    else 
                        Color.Gray.copy(alpha = 0.05f)
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
                        "Eksik Mesai:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (missingHours > 0) Color.Red else Color.Black
                    )
                    Text(
                        "$missingHours Saat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (missingHours > 0) Color.Red else Color.Black
                    )
                }
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
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            vm = vm,
            currentMonth = currentMonth
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

    val bgColor: Color = when {
        shift != null -> shift.color
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
private fun Legend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
        
        Spacer(Modifier.height(8.dp))
        
        // Second row with FULL
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ShiftType.FULL.LegendBoxCompact()
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
                // SADECE 3 TİP
                listOf(ShiftType.MORNING, ShiftType.NIGHT, ShiftType.FULL).forEach { type ->
                    Button(
                        onClick = { onSelect(type) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = type.color, contentColor = Color.White)
                    ) {
                        Text("${type.label} (${type.hours} Saat)")
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    vm: ScheduleViewModel,
    currentMonth: YearMonth
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Safely get shift types with error handling
    val shiftTypes = remember { 
        try {
            vm.getCurrentShiftTypes()
        } catch (e: Exception) {
            emptyList<DynamicShiftType>()
        }
    }
    
    AlertDialog(
        onDismissRequest = {
            try {
                onDismiss()
            } catch (e: Exception) {
                // Handle dismiss error gracefully
            }
        },
        modifier = Modifier.fillMaxSize(),
        title = {
            Text(
                "⚙️ Ayarlar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Row with error handling
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { 
                            try {
                                selectedTab = 0 
                            } catch (e: Exception) {
                                // Handle tab selection error
                            }
                        },
                        text = { Text("Toplu İşlemler", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { 
                            try {
                                selectedTab = 1 
                            } catch (e: Exception) {
                                // Handle tab selection error
                            }
                        },
                        text = { Text("Nöbet Saatleri", fontSize = 12.sp) }
                    )
                }
                
                // Content without nested scrolling
                when (selectedTab) {
                    0 -> {
                        BulkOperationsTab(
                            vm = vm,
                            currentMonth = currentMonth,
                            onDismiss = onDismiss
                        )
                    }
                    1 -> {
                        ShiftHoursTab(
                            vm = vm,
                            shiftTypes = shiftTypes
                        )
                    }
                    else -> {
                        // Fallback content
                        Text(
                            "Seçenek bulunamadı",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    try {
                        onDismiss()
                    } catch (e: Exception) {
                        // Handle button click error
                    }
                }
            ) {
                Text("Kapat")
            }
        }
    )
}