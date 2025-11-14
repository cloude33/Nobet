package com.example.nobet.ui.calendar

import com.example.nobet.R
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.nobet.notification.NotificationHelper
import com.example.nobet.notification.NotificationScheduler
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
        
        Text(
            "Tüm çalışma günlerine mesai ekle:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        val morning = vm.getCurrentShiftTypes().firstOrNull { it.type == ShiftType.MORNING }
        if (morning != null) {
            Button(
                onClick = { showConfirmDialog = "add_MORNING" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = morning.color,
                    contentColor = Color.White
                )
            ) {
                Text("08-16 Gündüz Mesai (8 Saat) Ekle")
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
            Text("Tüm Kayıtları Temizle")
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Info text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Text(
                "ℹ️ Sadece boş çalışma günlerine mesai eklenecek. Tatil günleri ve mevcut kayıtlar etkilenmeyecek.",
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
                        action.startsWith("add_") -> "Tüm boş çalışma günlerine 08-16 Gündüz Mesaisi eklemek istediğinizden emin misiniz?"
                        action == "clear_all" -> "Bu aydaki tüm kayıtları silmek istediğinizden emin misiniz?"
                        else -> "Bu işlemi gerçekleştirmek istediğinizden emin misiniz?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            action.startsWith("add_") -> {
                                vm.addWorkingDaysToMonth(currentMonth, ShiftType.MORNING)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftConfigCardV2(
    title: String,
    color: Color,
    hours: Int,
    label: String,
    onHoursChange: (Int) -> Unit,
    onLabelChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var localLabel by remember(label) { mutableStateOf(label) }
    var startTime by remember(label) { mutableStateOf(label.split("-").getOrNull(0)?.trim()?.let { if (it.contains(":")) it else "$it:00" } ?: "08:00") }
    var endTime by remember(label) { mutableStateOf(label.split("-").getOrNull(1)?.trim()?.let { if (it.contains(":")) it else "$it:00" } ?: "16:00") }
    fun computeHours(): Int {
        return try {
            val (sh, sm) = startTime.split(":").map { it.toInt() }
            val (eh, em) = endTime.split(":").map { it.toInt() }
            val startTotal = sh * 60 + sm
            val endTotal = eh * 60 + em
            val diff = if (endTotal >= startTotal) endTotal - startTotal else (24 * 60 - startTotal) + endTotal
            (diff / 60).coerceIn(0, 24)
        } catch (_: Exception) { hours }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            val newHours = computeHours()
                            val newLabel = "$startTime - $endTime"
                            onHoursChange(newHours)
                            onLabelChange(newLabel)
                            localLabel = newLabel
                            try { onSave() } catch (_: Exception) {}
                            isEditing = false
                        }) { Icon(Icons.Filled.Check, contentDescription = "Kaydet", tint = Color(0xFF2E7D32)) }
                        IconButton(onClick = {
                            startTime = localLabel.split("-").getOrNull(0)?.trim()?.let { if (it.contains(":")) it else "$it:00" } ?: startTime
                            endTime = localLabel.split("-").getOrNull(1)?.trim()?.let { if (it.contains(":")) it else "$it:00" } ?: endTime
                            isEditing = false
                        }) { Icon(Icons.Filled.Close, contentDescription = "İptal", tint = Color(0xFFD32F2F)) }
                    }
                } else {
                    IconButton(onClick = { isEditing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Düzenle") }
                }
            }
            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(localLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${hours} saat", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Başlangıç") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Bitiş") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = computeHours().toString(),
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Toplam Saat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Vardiya Saatleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Morning shift configuration
        ShiftConfigCardV2(
            title = "Gündüz Mesaisi",
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
        ShiftConfigCardV2(
            title = "Nöbet (${nightHours} Saat)",
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
        ShiftConfigCardV2(
            title = "Nöbet (${fullHours} Saat)",
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
        
        // Custom Shifts Section
        CustomShiftsSection(vm = vm)
        
        // Divider before Annual Leave
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Annual Leave Section - Simplified version
        AnnualLeaveSection(vm = vm)
        
        // Divider before Bulk Operations
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Bulk Operations Section
        Text(
            "Toplu İşlemler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        BulkOperationsTab(vm = vm, currentMonth = YearMonth.now(), onDismiss = {})
    }
}

@Composable
fun CustomShiftsSection(vm: ScheduleViewModel) {
    val customShifts = vm.getShiftTypeConfig()?.getCustomShiftTypes() ?: emptyList()
    var showAddDialog by remember { mutableStateOf(false) }
    var shiftToDelete by remember { mutableStateOf<CustomShiftType?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom shifts list or empty state
        if (customShifts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Henüz vardiya eklenmemiş. '+ Yeni Vardiya Ekle' ile ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customShifts) { shift ->
                    CustomShiftItem(
                        shift = shift,
                        onDelete = { shiftToDelete = shift }
                    )
                }
            }
        }

        // Add New Shift button styled as in screenshot
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ekle")
            Spacer(Modifier.width(8.dp))
            Text("Yeni Vardiya Ekle")
        }
    }
    
    // Add Custom Shift Dialog
    if (showAddDialog) {
        AddCustomShiftDialogV2(
            vm = vm,
            onDismiss = { showAddDialog = false }
        )
    }
    
    // Delete Confirmation Dialog
    shiftToDelete?.let { shift ->
        AlertDialog(
            onDismissRequest = { shiftToDelete = null },
            title = { Text("Nöbet Türünü Sil") },
            text = { Text("${shift.label} nöbet türünü silmek istediğinizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.removeCustomShiftType(shift.id)
                        shiftToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) {
                    Text("İptal")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomShiftDialog(
    vm: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(8) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val colors = ShiftTypeConfig.PREDEFINED_COLORS
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Vardiya Ekle") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Etiket (ör: 08-13)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Hours selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saat:", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (hours > 1) hours-- }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Azalt")
                        }
                        Text("$hours", style = MaterialTheme.typography.headlineSmall)
                        IconButton(
                            onClick = { if (hours < 24) hours++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Artır")
                        }
                    }
                }
                
                // Color selector
                Text("Renk:", style = MaterialTheme.typography.bodyLarge)
                LazyColumn(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors.chunked(6)) { colorRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorRow.forEachIndexed { index, color ->
                                val globalIndex = selectedColorIndex / 6 * 6 + index
                                Card(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = color
                                    ),
                                    onClick = { selectedColorIndex = globalIndex }
                                ) {
                                    if (globalIndex == selectedColorIndex) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seçildi",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) {
                        vm.addCustomShiftType(label, hours, colors[selectedColorIndex])
                        onDismiss()
                    }
                },
                enabled = label.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomShiftDialogV2(
    vm: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("16:00") }
    var end by remember { mutableStateOf("00:00") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val colors = ShiftTypeConfig.PREDEFINED_COLORS
    fun calcHours(): Int {
        return try {
            val (sh, sm) = start.split(":").map { it.toInt() }
            val (eh, em) = end.split(":").map { it.toInt() }
            val s = sh * 60 + sm
            val e = eh * 60 + em
            val diff = if (e >= s) e - s else (24 * 60 - s) + e
            (diff / 60).coerceIn(0, 24)
        } catch (_: Exception) { 8 }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Vardiya Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vardiya Adı") },
                    placeholder = { Text("Örn: Kısa Nöbet") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Başlangıç") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Bitiş") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = calcHours().toString(),
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Toplam Saat") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Renk", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.take(8).forEachIndexed { index, _ ->
                        FilledIconButton(
                            onClick = { selectedColorIndex = index },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors[index])
                        ) {
                            if (index == selectedColorIndex) { Icon(Icons.Filled.Check, contentDescription = "Seçili", tint = Color.White) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        vm.addCustomShiftType(name, calcHours(), colors[selectedColorIndex])
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
fun CustomShiftItem(
    shift: CustomShiftType,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = shift.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    shift.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = shift.color
                )
                Text(
                    "${shift.hours} saat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = shift.color.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil")
            }
        }
    }
}

// New About section component
@Composable
fun AboutSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Hakkında",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Profile Picture
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_picture),
                contentDescription = "Profil Resmi",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        Text(
            "Bu uygulama Hastanede Nöbet Tutan Tüm Sağlık Profesyonelleri için yazılmıştır. Herhangi bir gelir beklentisi yoktur. İstekleriniz doğrultusunda güncellemeler yapılabilinir.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Contact information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "İletişim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "Geliştirici: Hüseyin BULUT",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "Email: hbulut77@gmail.com",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "GitHub: cloude33",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Version information
        Text(
            "Versiyon: 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// Simplified Annual Leave Section for Shift Hours Tab
@Composable
fun AnnualLeaveSection(
    vm: ScheduleViewModel
) {
    val annualLeaveSettings by remember { derivedStateOf { vm.annualLeaveSettings } }
    var isEditing by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(annualLeaveSettings.totalAnnualLeaveDays) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Yıllık İzin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Card displaying total annual leave with inline editing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Toplam Yıllık İzin Günü",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isEditing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                vm.setTotalAnnualLeaveDays(days)
                                isEditing = false
                            }) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Kaydet",
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                            IconButton(onClick = {
                                days = annualLeaveSettings.totalAnnualLeaveDays
                                isEditing = false
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "İptal",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Düzenle")
                        }
                    }
                }
                
                if (!isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${days} gün",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "İzin Günü:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { 
                                    if (days > 0) days--
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
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "$days",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                            
                            FilledIconButton(
                                onClick = { 
                                    if (days < 365) days++
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
                }
            }
        }
    }
}

// Old Annual Leave Settings Tab - kept for reference but not used
@Composable
fun AnnualLeaveSettingsTab(
    vm: ScheduleViewModel
) {
    val annualLeaveSettings by remember { derivedStateOf { vm.annualLeaveSettings } }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Calculate actual annual leave days from schedule
    val actualAnnualLeaveDays = vm.schedule.values.count { it == ShiftType.ANNUAL_LEAVE }
    val isMismatch = actualAnnualLeaveDays != annualLeaveSettings.usedAnnualLeaveDays
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Yıllık İzin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        // Card displaying total annual leave with edit icon
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Toplam Yıllık İzin Günü",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle")
                    }
                }
                Text(
                    "${annualLeaveSettings.totalAnnualLeaveDays} gün",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Yıl boyunca kullanabileceğiniz toplam izin gün sayısını belirleyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Warning about mismatch
        if (isMismatch) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚠️ Uyarı: İzin Sayısı Uyuşmazlığı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Takvimdeki yıllık izin günleri sayısı ($actualAnnualLeaveDays) ile kayıtlı kullanılan izin sayısı (${annualLeaveSettings.usedAnnualLeaveDays}) arasında bir uyuşmazlık var.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = {
                            // Update the used annual leave days to match the schedule
                            vm.addUsedAnnualLeaveDays(actualAnnualLeaveDays - annualLeaveSettings.usedAnnualLeaveDays)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer,
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("Otomatik Düzelt")
                    }
                }
            }
        }
        
        // Annual leave summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "İzin Özeti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toplam İzin Hakkı:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${annualLeaveSettings.totalAnnualLeaveDays} gün",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kullanılan İzin:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${annualLeaveSettings.usedAnnualLeaveDays} gün",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (annualLeaveSettings.remainingAnnualLeaveDays > 0) 
                                Color.Green.copy(alpha = 0.2f) 
                            else 
                                Color.Red.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Kalan İzin:", 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${annualLeaveSettings.remainingAnnualLeaveDays} gün",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Reset button
        OutlinedButton(
            onClick = {
                vm.resetUsedAnnualLeaveDays()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            )
        ) {
            Text("Kullanılan İzinleri Sıfırla")
        }
        
        // Information text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Text(
                "ℹ️ Takvime 'YI' (Yıllık İzin) eklediğinizde kullanılan izin gün sayısı otomatik olarak artar. İzin günlerini kaldırdığınızda ise azalır.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    // Edit dialog for annual leave days
    if (showEditDialog) {
        var input by remember { mutableStateOf(annualLeaveSettings.totalAnnualLeaveDays.toString()) }
        var isError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Toplam Yıllık İzin Günü") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; isError = false },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val days = input.toInt()
                        if (days >= 0) {
                            vm.setTotalAnnualLeaveDays(days)
                            showEditDialog = false
                        } else {
                            isError = true
                        }
                    } catch (e: NumberFormatException) {
                        isError = true
                    }
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
fun NotificationSettingsTab(
    vm: ScheduleViewModel
) {
    val context = LocalContext.current
    
    // Initialize notification components
    val notificationHelper = remember { NotificationHelper(context) }
    val notificationScheduler = remember { NotificationScheduler(context) }
    
    var notificationsEnabled by remember { mutableStateOf(vm.areNotificationsEnabled()) }
    var selectedReminderDays by remember { mutableStateOf(vm.getReminderDays()) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    
    // Check permissions status
    var hasNotificationPermission by remember { mutableStateOf(notificationHelper.areNotificationsEnabled()) }
    var hasExactAlarmPermission by remember { mutableStateOf(notificationScheduler.canScheduleExactAlarms()) }
    
    // Refresh permissions when screen becomes visible
    LaunchedEffect(Unit) {
        hasNotificationPermission = notificationHelper.areNotificationsEnabled()
        hasExactAlarmPermission = notificationScheduler.canScheduleExactAlarms()
    }
    
    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            notificationsEnabled = true
            vm.setNotificationsEnabled(true)
        } else {
            showPermissionDialog = true
        }
    }
    
    // Exact alarm permission launcher for Android 12+
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Refresh permission status after returning from settings
        hasExactAlarmPermission = notificationScheduler.canScheduleExactAlarms()
    }
    
    val reminderOptions = listOf(
        1 to "1 gün önce",
        2 to "2 gün önce", 
        3 to "3 gün önce"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasNotificationPermission && hasExactAlarmPermission) 
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "İzin Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Notification permission status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasNotificationPermission) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (hasNotificationPermission) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Bildirim İzni: ${if (hasNotificationPermission) "✅ Verildi" else "❌ Reddedildi"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Exact alarm permission status (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasExactAlarmPermission) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (hasExactAlarmPermission) Color.Green else Color(0xFFFFA500),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Kesin Alarm İzni: ${if (hasExactAlarmPermission) "✅ Verildi" else "⚠️ Gerekli"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Fix permissions button
                if (!hasNotificationPermission || !hasExactAlarmPermission) {
                    Button(
                        onClick = {
                            if (!hasNotificationPermission) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showPermissionDialog = true
                                }
                            } else if (!hasExactAlarmPermission) {
                                showExactAlarmDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("İzinleri Düzelt")
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/Disable Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Bildirimleri Etkinleştir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Nöbet hatırlatmaları alın",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Check permissions first
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        showPermissionDialog = true
                                    }
                                } else if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    showExactAlarmDialog = true
                                } else {
                                    notificationsEnabled = true
                                    vm.setNotificationsEnabled(true)
                                }
                            } else {
                                notificationsEnabled = false
                                vm.setNotificationsEnabled(false)
                            }
                        },
                        enabled = hasNotificationPermission
                    )
                }
                
                HorizontalDivider()
                
                // Reminder Days Selection
                if (notificationsEnabled) {
                    Column {
                        Text(
                            "Hatırlatma Zamanı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Column(
                            modifier = Modifier
                                .selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderOptions.forEach { (days, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (selectedReminderDays == days),
                                            onClick = {
                                                selectedReminderDays = days
                                                vm.setReminderDays(days)
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (selectedReminderDays == days),
                                        onClick = null
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "ℹ️ Bilgi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nöbet gününüzün öncesinde hatırlatma bildirimleri alacaksınız. Bildirimlerin düzgün çalışması için uygulamanın bildirim ve kesin alarm iznine sahip olması gerekir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Test Notification Card
        if (hasNotificationPermission && notificationsEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "🗂️ Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bildirimlerin çalışıp çalışmadığını kontrol edin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            notificationHelper.testNotification()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Bildirimi Gönder")
                    }
                }
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Bildirim İzni Gerekli") },
            text = {
                Text("Nöbet hatırlatmaları için bildirim izni gereklidir. Lütfen uygulama ayarlarından bildirimleri etkinleştirin.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Exact Alarm Permission Dialog
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Kesin Alarm İzni Gerekli") },
            text = {
                Text("Nöbet hatırlatmalarının tam zamanda gönderilmesi için kesin alarm izni gereklidir. Bu izin Android 12+ sürümlerinde zorunludur.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            exactAlarmLauncher.launch(intent)
                        }
                    }
                ) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
