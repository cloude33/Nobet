package com.example.nobet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.nobet.ui.theme.NobetTheme
import com.example.nobet.ui.AppRoot
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set Turkish locale for calendar (month/day names)
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        setContent {
            NobetTheme { AppRoot() }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PreviewApp() { NobetTheme { AppRoot() } }