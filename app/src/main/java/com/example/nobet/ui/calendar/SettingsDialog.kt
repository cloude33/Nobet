package com.example.nobet.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nobet.ui.calendar.CustomShiftType
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    vm: ScheduleViewModel,
    currentMonth: YearMonth,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shiftTypes = vm.getCurrentShiftTypes()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ayarlar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Nöbet Saatleri") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Toplu İşlemler") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Özel Nöbetler") }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Tab Content
                when (selectedTab) {
                    0 -> ShiftHoursTab(vm = vm, shiftTypes = shiftTypes)
                    1 -> BulkOperationsTab(vm = vm, currentMonth = currentMonth, onDismiss = onDismiss)
                    2 -> CustomShiftsTab(vm = vm, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
fun CustomShiftsTab(
    vm: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val customShifts = vm.getShiftTypeConfig()?.getCustomShiftTypes() ?: emptyList()
    var showAddDialog by remember { mutableStateOf(false) }
    var shiftToDelete by remember { mutableStateOf<CustomShiftType?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Özel Nöbet Türleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }
        
        if (customShifts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Henüz özel nöbet türü eklenmemiş. '+' butonuna basarak yeni nöbet türleri ekleyebilirsiniz.",
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
    }
    
    // Add Custom Shift Dialog
    if (showAddDialog) {
        AddCustomShiftDialog(
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
        title = { Text("Yeni Nöbet Türü Ekle") },
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