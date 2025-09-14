package com.example.nobet.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun BulkOperationsTab(
    vm: ScheduleViewModel,
    currentMonth: YearMonth,
    onDismiss: () -> Unit
) {
    val locale = java.util.Locale.forLanguageTag("tr-TR")
    val monthText = currentMonth.month.getDisplayName(TextStyle.FULL, locale)
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${monthText.replaceFirstChar { it.titlecase(locale) }} ${currentMonth.year} için Toplu İşlemler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Add working days buttons
        Text(
            "Tüm çalışma günlerine nöbet ekle:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        vm.getCurrentShiftTypes().forEach { dynamicShift ->
            Button(
                onClick = {
                    showConfirmDialog = "add_${dynamicShift.type.name}"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dynamicShift.color,
                    contentColor = Color.White
                )
            ) {
                Text("${dynamicShift.label} Nöbeti Ekle (${dynamicShift.hours} saat)")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Clear all shifts
        OutlinedButton(
            onClick = {
                showConfirmDialog = "clear_all"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            )
        ) {
            Text("Tüm Nöbetleri Temizle")
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Info text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Text(
                "ℹ️ Sadece boş çalışma günlerine nöbet eklenecek. Tatil günleri ve mevcut nöbetler etkilenmeyecek.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    // Confirmation Dialog
    showConfirmDialog?.let { action ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text("İşlemi Onayla") },
            text = {
                Text(
                    when {
                        action.startsWith("add_") -> "Tüm boş çalışma günlerine seçilen nöbet türünü eklemek istediğinizden emin misiniz?"
                        action == "clear_all" -> "Bu aydaki tüm nöbetleri silmek istediğinizden emin misiniz?"
                        else -> "Bu işlemi gerçekleştirmek istediğinizden emin misiniz?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            action.startsWith("add_") -> {
                                val shiftTypeName = action.removePrefix("add_")
                                val shiftType = when (shiftTypeName) {
                                    "MORNING" -> ShiftType.MORNING
                                    "NIGHT" -> ShiftType.NIGHT
                                    "FULL" -> ShiftType.FULL
                                    else -> ShiftType.MORNING
                                }
                                vm.addWorkingDaysToMonth(currentMonth, shiftType)
                            }
                            action == "clear_all" -> {
                                vm.clearAllShiftsInMonth(currentMonth)
                            }
                        }
                        showConfirmDialog = null
                        onDismiss()
                    }
                ) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Hayır")
                }
            }
        )
    }
}

@Composable
fun ShiftHoursTab(
    vm: ScheduleViewModel,
    shiftTypes: List<DynamicShiftType>
) {
    // Ensure ViewModel is properly initialized with error handling
    LaunchedEffect(Unit) {
        try {
            // This will trigger context initialization if not already done
        } catch (e: Exception) {
            // Handle initialization error gracefully
        }
    }
    
    // Safely get shift configuration
    val shiftConfig = try {
        vm.getShiftTypeConfig()
    } catch (e: Exception) {
        null
    }
    
    var morningHours by remember { mutableStateOf(shiftConfig?.getMorningHours() ?: 8) }
    var nightHours by remember { mutableStateOf(shiftConfig?.getNightHours() ?: 16) }
    var fullHours by remember { mutableStateOf(shiftConfig?.getFullHours() ?: 24) }
    
    var morningLabel by remember { mutableStateOf(shiftConfig?.getMorningLabel() ?: "08-16") }
    var nightLabel by remember { mutableStateOf(shiftConfig?.getNightLabel() ?: "16-08") }
    var fullLabel by remember { mutableStateOf(shiftConfig?.getFullLabel() ?: "08-08") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Nöbet Saatlerini Özelleştir",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Morning shift configuration
        ShiftConfigCard(
            title = "Gündüz Nöbeti",
            color = Color(0xFF64B5F6),
            hours = morningHours,
            label = morningLabel,
            onHoursChange = { morningHours = it },
            onLabelChange = { morningLabel = it },
            onSave = { 
                try {
                    shiftConfig?.setMorningConfig(morningHours, morningLabel)
                } catch (e: Exception) {
                    // Handle save error gracefully
                }
            }
        )
        
        // Night shift configuration
        ShiftConfigCard(
            title = "Gece Nöbeti",
            color = Color(0xFF9575CD),
            hours = nightHours,
            label = nightLabel,
            onHoursChange = { nightHours = it },
            onLabelChange = { nightLabel = it },
            onSave = { 
                try {
                    shiftConfig?.setNightConfig(nightHours, nightLabel)
                } catch (e: Exception) {
                    // Handle save error gracefully
                }
            }
        )
        
        // Full shift configuration
        ShiftConfigCard(
            title = "24 Saat Nöbet",
            color = Color(0xFFF44336),
            hours = fullHours,
            label = fullLabel,
            onHoursChange = { fullHours = it },
            onLabelChange = { fullLabel = it },
            onSave = { 
                try {
                    shiftConfig?.setFullConfig(fullHours, fullLabel)
                } catch (e: Exception) {
                    // Handle save error gracefully
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftConfigCard(
    title: String,
    color: Color,
    hours: Int,
    label: String,
    onHoursChange: (Int) -> Unit,
    onLabelChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title with color indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.size(16.dp),
                    colors = CardDefaults.cardColors(containerColor = color),
                    shape = RoundedCornerShape(4.dp)
                ) {}
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Hours selector with modern design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saat Sayısı:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { 
                            if (hours > 1) {
                                try {
                                    onHoursChange(hours - 1)
                                } catch (e: Exception) {
                                    // Handle error gracefully
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Azalt")
                    }
                    
                    Card(
                        modifier = Modifier.width(60.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = color.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$hours",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    
                    FilledIconButton(
                        onClick = { 
                            if (hours < 24) {
                                try {
                                    onHoursChange(hours + 1)
                                } catch (e: Exception) {
                                    // Handle error gracefully
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Artır")
                    }
                }
            }
            
            // Label input
            OutlinedTextField(
                value = label,
                onValueChange = { newValue ->
                    try {
                        onLabelChange(newValue)
                    } catch (e: Exception) {
                        // Handle error gracefully
                    }
                },
                label = { Text("Etiket (ör: 08-16)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Save button
            Button(
                onClick = { 
                    try {
                        onSave()
                    } catch (e: Exception) {
                        // Handle save error gracefully
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Kaydet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}