package com.sahalos.sboard.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahalos.sboard.BuildConfig
import com.sahalos.sboard.R
import com.sahalos.sboard.data.SboardPreferences
import com.sahalos.sboard.data.UpdateManager
import com.sahalos.sboard.data.UpdateUiState
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

    // Preferences states
    val bottomPadding by preferences.bottomPaddingDp.collectAsStateWithLifecycle(initialValue = 0)
    val heightRatio by preferences.keyboardHeightRatio.collectAsStateWithLifecycle(initialValue = 1.0f)
    val longPressDelay by preferences.longPressDelayMs.collectAsStateWithLifecycle(initialValue = 300L)
    val enableLongPressSymbols by preferences.enableLongPressSymbols.collectAsStateWithLifecycle(initialValue = true)
    val enableVibration by preferences.enableVibration.collectAsStateWithLifecycle(initialValue = true)
    val themeId by preferences.themeId.collectAsStateWithLifecycle(initialValue = "oled_black")
    val passiveSuggestions by preferences.enablePassiveSuggestions.collectAsStateWithLifecycle(initialValue = true)
    val enableKeyPopup by preferences.enableKeyPopupPreview.collectAsStateWithLifecycle(initialValue = true)
    val enableSpacebarCursorGlide by preferences.enableSpacebarCursorGlide.collectAsStateWithLifecycle(initialValue = true)
    val enableClipboardHistory by preferences.enableClipboardHistory.collectAsStateWithLifecycle(initialValue = true)

    // IME Dynamic Status on Lifecycle ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeDefault by remember { mutableStateOf(false) }

    fun refreshImeStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        isImeEnabled = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
        val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        isImeDefault = currentIme.contains(context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshImeStatus()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update Manager State
    val updateManager = remember { UpdateManager(context) }
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pengaturan Sboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} • Sahal Board",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            // 1. HERO HEADER: Status Aktivasi IME
            if (isImeEnabled && isImeDefault) {
                // When completely configured, show clean compact success card (Solid Material 3)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Column {
                                Text(
                                    text = "Sboard Aktif sebagai Keyboard Utama",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Keyboard siap digunakan di seluruh aplikasi.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ganti", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Setup Alert Card when not active or not default (Solid Material 3)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isImeEnabled) Color(0xFFEAB308) else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (isImeEnabled) {
                                    "Sboard Aktif (Belum Jadi Keyboard Utama)"
                                } else {
                                    "Sboard Belum Diaktifkan"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isImeEnabled) {
                                "Ketuk tombol 'Pilih Default' untuk menjadikan Sboard sebagai metode input utama."
                            } else {
                                "Aktifkan Sboard di pengaturan input sistem Android agar dapat digunakan."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isImeEnabled) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("1. Aktifkan", fontSize = 13.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showInputMethodPicker()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pilih Default", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 2. SEKSI TAMPILAN & TEMA
            SectionTitle(title = "Tampilan & Tata Letak")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Horizontal Selector
                    Column {
                        Text(
                            text = "Tema Keyboard",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Pilih palet visual yang nyaman di mata.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SboardTheme.AvailableThemes.forEach { itemTheme ->
                                val isSelected = itemTheme.id == themeId
                                ThemePreviewCard(
                                    theme = itemTheme,
                                    isSelected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            preferences.setThemeId(itemTheme.id)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Keyboard Height Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skala Tinggi Keyboard",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${(heightRatio * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = heightRatio,
                            onValueChange = { newRatio ->
                                coroutineScope.launch {
                                    preferences.setKeyboardHeightRatio(newRatio)
                                }
                            },
                            valueRange = 0.75f..1.35f,
                            steps = 11
                        )
                    }

                    // Bottom Padding Slider (Ergonomi Jempol)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jarak Dagu Bawah (Bottom Margin)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${bottomPadding} dp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Menaikkan keyboard dari tepi bawah layar agar jempol tidak menekuk.",
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
                            valueRange = 0f..36f,
                            steps = 11
                        )
                    }

                    // Floating Key Popup Switch
                    SettingSwitchRow(
                        title = "Pratinjau Huruf Melayang (Key Popup)",
                        subtitle = "Menampilkan gelembung pembesar huruf di atas tombol saat disentuh.",
                        checked = enableKeyPopup,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnableKeyPopupPreview(checked)
                            }
                        }
                    )
                }
            }

            // 3. SEKSI PENGETIKAN & GESTUR
            SectionTitle(title = "Pengetikan & Gestur")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingSwitchRow(
                        title = "Navigasi Kursor Spasi (Space Glide)",
                        subtitle = "Geser jari ke kiri/kanan pada tombol spasi untuk memindahkan kursor teks.",
                        checked = enableSpacebarCursorGlide,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnableSpacebarCursorGlide(checked)
                            }
                        }
                    )

                    SettingSwitchRow(
                        title = "Umpan Balik Getar (Haptic)",
                        subtitle = "Getaran lembut haptik saat tombol ditekan.",
                        checked = enableVibration,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnableVibration(checked)
                            }
                        }
                    )

                    SettingSwitchRow(
                        title = "Saran Kata Pasif",
                        subtitle = "Menampilkan rekomendasi kata di bilah atas tanpa mengganti kata otomatis.",
                        checked = passiveSuggestions,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnablePassiveSuggestions(checked)
                            }
                        }
                    )

                    SettingSwitchRow(
                        title = "Simbol Tekan Lama",
                        subtitle = "Menampilkan dan menginput simbol sekunder saat tombol ditahan.",
                        checked = enableLongPressSymbols,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnableLongPressSymbols(checked)
                            }
                        }
                    )

                    if (enableLongPressSymbols) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Durasi Tahan Simbol",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$longPressDelay ms",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = longPressDelay.toFloat(),
                                onValueChange = { newDelay ->
                                    coroutineScope.launch {
                                        preferences.setLongPressDelayMs(newDelay.toLong())
                                    }
                                },
                                valueRange = 150f..450f,
                                steps = 5
                            )
                        }
                    }
                }
            }

            // 4. SEKSI PAPAN KLIP (CLIPBOARD)
            SectionTitle(title = "Papan Klip (Clipboard)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingSwitchRow(
                        title = "Riwayat Papan Klip",
                        subtitle = "Menyimpan teks yang disalin secara otomatis (Maks. 30 klip FIFO).",
                        checked = enableClipboardHistory,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setEnableClipboardHistory(checked)
                            }
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Riwayat klip tidak tersemat telah dibersihkan.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clipboard),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Riwayat Tidak Disematkan", fontSize = 13.sp)
                    }
                }
            }

            // 5. SEKSI PEMBARUAN APLIKASI (IN-APP UPDATER)
            SectionTitle(title = "Pembaruan Aplikasi")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Versi Terpasang",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    updateState = UpdateUiState.Checking
                                    updateState = updateManager.checkForUpdates()
                                }
                            },
                            enabled = updateState !is UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (updateState is UpdateUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memeriksa...", fontSize = 13.sp)
                            } else {
                                Text("Periksa Pembaruan", fontSize = 13.sp)
                            }
                        }
                    }

                    // Update Status Feedback Card
                    when (val state = updateState) {
                        is UpdateUiState.Idle -> {
                            Text(
                                text = "Distribusi mandiri langsung dari repositori GitHub tanpa Play Store.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is UpdateUiState.Checking -> {
                            Text(
                                text = "Menghubungkan ke GitHub API...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is UpdateUiState.UpToDate -> {
                            Text(
                                text = "✓ Sboard v${state.currentVersion} sudah merupakan versi terbaru.",
                                fontSize = 12.sp,
                                color = Color(0xFF22C55E),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is UpdateUiState.Error -> {
                            Text(
                                text = "Gagal: ${state.message}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UpdateUiState.UpdateAvailable -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Tersedia Versi Baru: ${state.info.tagName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.info.changelog.take(200),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                updateState = UpdateUiState.Downloading(0f)
                                                val result = updateManager.downloadApk(
                                                    downloadUrl = state.info.downloadUrl,
                                                    onProgress = { progress ->
                                                        downloadProgress = progress
                                                        updateState = UpdateUiState.Downloading(progress)
                                                    }
                                                )
                                                if (result.isSuccess) {
                                                    val apkFile = result.getOrThrow()
                                                    updateState = UpdateUiState.Downloaded(apkFile, state.info)
                                                    val installRes = updateManager.launchApkInstaller(apkFile)
                                                    if (installRes.isFailure) {
                                                        Toast.makeText(context, installRes.exceptionOrNull()?.message ?: "Buka izin pasang APK.", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    updateState = UpdateUiState.Error(result.exceptionOrNull()?.message ?: "Gagal mengunduh APK.")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Unduh Pembaruan", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        is UpdateUiState.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Mengunduh paket rilis...", fontSize = 12.sp)
                                    Text("${(state.progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        is UpdateUiState.Downloaded -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Text(
                                            text = "Paket Rilis Siap Dipasang (${state.info?.tagName ?: "v${BuildConfig.VERSION_NAME}"})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = "File APK pembaruan berhasil diunduh. Ketuk tombol pasang di bawah untuk memperbarui keyboard tanpa menghapus data.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            val res = updateManager.launchApkInstaller(state.apkFile)
                                            if (res.isFailure) {
                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal membuka installer.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF15803D),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "PASANG SEKARANG (INSTALL)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemePreviewCard(
    theme: SboardTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(theme.backgroundColor))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Visual Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Key surface sample
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(theme.keyBackgroundColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 11.sp,
                        color = Color(theme.keyTextColor),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Accent dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(theme.accentColor))
                )
            }

            Text(
                text = theme.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color(theme.keyTextColor)
            )

            if (isSelected) {
                Text(
                    text = "✓ Aktif",
                    fontSize = 10.sp,
                    color = Color(theme.accentColor),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
