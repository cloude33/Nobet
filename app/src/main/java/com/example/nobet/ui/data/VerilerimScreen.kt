package com.example.nobet.ui.data

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nobet.ui.calendar.ScheduleViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun VerilerimScreen(padding: PaddingValues, vm: ScheduleViewModel = viewModel()) {
    val context = LocalContext.current
    
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

    Column(
        modifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Verilerimi Yönet",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Button(
            onClick = { 
                // Create filename with current date: Nöbet_yedek_YYYY-MM-DD
                val currentDate = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val dateString = currentDate.format(formatter)
                val filename = "Nöbet_yedek_${dateString}.json"
                exportLauncher.launch(filename)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Yedekle (JSON Olarak Dışa Aktar)")
        }

        Button(
            onClick = {
                importLauncher.launch(arrayOf("application/json"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📂 Geri Yükle (JSON Olarak İçe Aktar)")
        }

        Text(
            text = "ℹ️ Dosyalar cihazınızın dosya yöneticisi veya bulut (Google Drive, iCloud) üzerinden erişilebilir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp)
        )
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