package com.example.nobet.ui.notification

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nobet.notification.NotificationHelper
import com.example.nobet.notification.NotificationScheduler
import com.example.nobet.ui.calendar.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    padding: PaddingValues,
    vm: ScheduleViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        vm.initializeWithContext(context)
    }
    
    var notificationsEnabled by remember { mutableStateOf(vm.areNotificationsEnabled()) }
    var selectedReminderDays by remember { mutableStateOf(vm.getReminderDays()) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    
    val notificationHelper = remember { NotificationHelper(context) }
    val notificationScheduler = remember { NotificationScheduler(context) }
    
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
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "🔔 Bildirim Ayarları",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
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
                        "Bildirim İzni: ${if (hasNotificationPermission) "✅ Verildi" else "❌ Verilmedi"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Exact alarm permission status (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                            modifier = Modifier.selectableGroup()
                        ) {
                            reminderOptions.forEach { (days, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .selectable(
                                            selected = (selectedReminderDays == days),
                                            onClick = {
                                                selectedReminderDays = days
                                                vm.setReminderDays(days)
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 8.dp),
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