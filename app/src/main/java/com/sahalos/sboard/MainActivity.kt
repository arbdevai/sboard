package com.sahalos.sboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sahalos.sboard.data.SboardPreferences
import com.sahalos.sboard.ui.settings.SettingsScreen
import com.sahalos.sboard.ui.theme.SboardAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: SboardPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = SboardPreferences(this)

        handleIncomingIntent(intent)

        setContent {
            SboardAppTheme {
                SettingsScreen(preferences = preferences)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data: Uri? = intent?.data ?: return
        if (data.scheme == "sboard" && data.host == "theme") {
            val downloadUrl = data.getQueryParameter("download")
            if (!downloadUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Tautan tema terdeteksi: $downloadUrl", Toast.LENGTH_SHORT).show()
            }
        } else if (intent.action == Intent.ACTION_VIEW && data.path?.endsWith(".sboard-theme") == true) {
            Toast.makeText(this, "Memproses berkas tema Sboard...", Toast.LENGTH_SHORT).show()
        }
    }
}
