package com.example.nobet.ui.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nobet.R
import com.example.nobet.ui.calendar.ScheduleViewModel
import com.example.nobet.utils.GoogleDriveBackup
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun VerilerimScreen(padding: PaddingValues, vm: ScheduleViewModel = viewModel()) {
    val context = LocalContext.current
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    
    // Initialize ViewModel with context for data persistence
    LaunchedEffect(Unit) {
        vm.initializeWithContext(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let { writeToUri(context, it, vm.toJson()) } }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { readFromUri(context, it)?.let(vm::fromJson) } }
    )
    
    // Google Drive backup helper
    val googleDriveBackup = remember { GoogleDriveBackup(context) }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    googleDriveBackup.initializeDriveService(it)
                    // Perform backup after successful sign-in
                    if (isBackingUp) {
                        backupToGoogleDrive(googleDriveBackup, vm, context)
                        isBackingUp = false
                    }
                    if (isRestoring) {
                        // For restore, we would need to show file picker from Google Drive
                        isRestoring = false
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, 
                    context.getString(R.string.google_drive_signin_failed, e.message), 
                    Toast.LENGTH_LONG).show()
                isBackingUp = false
                isRestoring = false
            }
        } else {
            isBackingUp = false
            isRestoring = false
        }
    }
    
    Column(
        modifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Başlık Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    "💾 ",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Verilerimi Yönet",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Yerel Yedekleme Bölümü
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "📱 Yerel Yedekleme",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                Button(
                    onClick = { 
                        val currentDate = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val dateString = currentDate.format(formatter)
                        val filename = "Nöbet_yedek_${dateString}.json"
                        exportLauncher.launch(filename)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("💾 Yedekle (JSON)")
                }

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📂 Geri Yükle (JSON)")
                }
            }
        }
        
        // Google Drive Bölümü
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "☁️ Google Drive Yedekleme",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                Button(
                    onClick = {
                        if (googleDriveBackup.isUserSignedIn()) {
                            googleDriveBackup.getCurrentAccount()?.let {
                                googleDriveBackup.initializeDriveService(it)
                                backupToGoogleDrive(googleDriveBackup, vm, context)
                            }
                        } else {
                            isBackingUp = true
                            googleDriveBackup.getGoogleSignInClient()?.signInIntent?.let { intent ->
                                googleSignInLauncher.launch(intent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("☁️ Google Drive'a Yedekle")
                }
                
                OutlinedButton(
                    onClick = {
                        if (googleDriveBackup.isUserSignedIn()) {
                            Toast.makeText(context, R.string.google_drive_restore_not_implemented, Toast.LENGTH_SHORT).show()
                        } else {
                            isRestoring = true
                            googleDriveBackup.getGoogleSignInClient()?.signInIntent?.let { intent ->
                                googleSignInLauncher.launch(intent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("☁️ Google Drive'dan Geri Yükle")
                }
            }
        }

        // Bilgi Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "ℹ️",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Dosyalar cihazınızın dosya yöneticisi veya bulut (Google Drive, iCloud) üzerinden erişilebilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun backupToGoogleDrive(googleDriveBackup: GoogleDriveBackup, vm: ScheduleViewModel, context: Context) {
    Toast.makeText(context, R.string.google_drive_backup_started, Toast.LENGTH_SHORT).show()
    
    googleDriveBackup.backupData(vm.toJson()) { success ->
        if (success) {
            Toast.makeText(context, R.string.google_drive_backup_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.google_drive_backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun writeToUri(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.writer().use { it.write(content) }
    }
}

private fun readFromUri(context: Context, uri: Uri): String? {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.reader().readText()
    }
}