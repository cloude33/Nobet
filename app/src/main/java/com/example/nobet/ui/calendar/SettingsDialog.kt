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
                
                // Use the shared content component
                SettingsDialogContent(
                    vm = vm,
                    currentMonth = currentMonth,
                    onDismiss = onDismiss
                )
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

// Removed duplicate AddCustomShiftDialog; unified to AddCustomShiftDialogV2 in SettingsComponents.kt

