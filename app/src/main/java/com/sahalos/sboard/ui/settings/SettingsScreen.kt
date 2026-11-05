package com.sahalos.sboard.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahalos.sboard.data.SboardPreferences
import com.sahalos.sboard.model.SboardTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: SboardPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val bottomPadding by preferences.bottomPaddingDp.collectAsStateWithLifecycle(initialValue = 0)
    val heightRatio by preferences.keyboardHeightRatio.collectAsStateWithLifecycle(initialValue = 1.0f)
    val longPressDelay by preferences.longPressDelayMs.collectAsStateWithLifecycle(initialValue = 300L)
    val enableLongPressSymbols by preferences.enableLongPressSymbols.collectAsStateWithLifecycle(initialValue = true)
    val enableVibration by preferences.enableVibration.collectAsStateWithLifecycle(initialValue = true)
    val themeId by preferences.themeId.collectAsStateWithLifecycle(initialValue = "nord_dark")
    val passiveSuggestions by preferences.enablePassiveSuggestions.collectAsStateWithLifecycle(initialValue = true)

    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val isEnabled = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan Sboard",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Activation Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEnabled) "Sboard Siap Digunakan" else "Sboard Belum Diaktifkan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnabled) {
                            "Keyboard telah diaktifkan di pengaturan sistem Android."
                        } else {
                            "Aktifkan Sboard di pengaturan input sistem untuk mulai menggunakannya."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("1. Aktifkan Keyboard")
                        }
                        OutlinedButton(
                            onClick = {
                                imm?.showInputMethodPicker()
                            }
                        ) {
                            Text("2. Pilih Default")
                        }
                    }
                }
            }

            // Ergonomics: Bottom Padding
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Jarak Bawah Layar: ${bottomPadding} dp",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Menaikkan posisi keyboard agar jempol tidak menekuk di tepi bawah layar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = bottomPadding.toFloat(),
                        onValueChange = { newValue ->
                            coroutineScope.launch {
                                preferences.setBottomPaddingDp(newValue.toInt())
                            }
                        },
                        valueRange = 0f..48f,
                        steps = 7
                    )
                }
            }

            // Keyboard Height Ratio
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Skala Tinggi Keyboard: ${(heightRatio * 100).toInt()}%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Mengatur ukuran tinggi baris tombol secara proporsional.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = heightRatio,
                        onValueChange = { newRatio ->
                            coroutineScope.launch {
                                preferences.setKeyboardHeightRatio(newRatio)
                            }
                        },
                        valueRange = 0.8f..1.3f
                    )
                }
            }

            // Long Press Symbol Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simbol Tekan Lama",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Tampilkan dan input simbol sekunder dengan menahan tombol.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLongPressSymbols,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    preferences.setEnableLongPressSymbols(checked)
                                }
                            }
                        )
                    }

                    if (enableLongPressSymbols) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Durasi Tahan: $longPressDelay ms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = longPressDelay.toFloat(),
                            onValueChange = { newDelay ->
                                coroutineScope.launch {
                                    preferences.setLongPressDelayMs(newDelay.toLong())
                                }
                            },
                            valueRange = 150f..500f,
                            steps = 6
                        )
                    }
                }
            }

            // Feedback & Suggestions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Umpan Balik Getar (Haptic)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Getaran lembut saat tombol ditekan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableVibration,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    preferences.setEnableVibration(checked)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saran Kata Pasif",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Menampilkan saran kata di strip atas tanpa mengubah teks otomatis.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = passiveSuggestions,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    preferences.setEnablePassiveSuggestions(checked)
                                }
                            }
                        )
                    }
                }
            }

            // Theme Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilihan Tema",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SboardTheme.AvailableThemes.forEach { itemTheme ->
                        val isSelected = itemTheme.id == themeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        preferences.setThemeId(itemTheme.id)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(itemTheme.backgroundColor))
                                        .border(1.dp, Color(itemTheme.accentColor), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemTheme.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "Aktif",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
