# 🤝 AGENT HANDOFF: Panduan Arsitektur Sboard v1.0.6

Dokumen ini memuat panduan perbaikan bug dan spesifikasi fitur baru yang telah diaudit dan diselaraskan dengan cetak biru `SPEC.md`.

---

## 1. Perbaikan Prioritas: Bug Kritis & UX Keyboard (Wajib Selesai Dulu)
1. **Perbaiki Crash `BadTokenException`:**
   - Hapus pemanggilan `AlertDialog` di `ClipboardScreen.kt`.
   - Ganti dialog pop-up dengan **Inline Modal Overlay** di dalam hierarki ComposeView keyboard (misal: `Box` dengan latar semi-transparan `Color.Black.copy(0.45f)` yang memunculkan kartu aksi Pin & Hapus di dalam area keyboard).
2. **Tombol Hapus (Backspace):**
   - Hapus `.clickable` kosong di child `KeyView` yang memakan touch event pembungkus luar.
   - Tambahkan fallback resmi `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)` di method `performBackspace()`.
3. **Ketinggian ClipboardScreen:**
   - Kunci tinggi `ClipboardScreen` sama persis dengan tinggi keyboard (`Modifier.fillMaxWidth().height(keyboardHeightDp)`).
   - Ubah tampilan menjadi **2 Kolom (Grid)** menggunakan `LazyVerticalGrid(columns = GridCells.Fixed(2))` dengan `maxLines = 2` dan `TextOverflow.Ellipsis`.
4. **Ketinggian ResizeOverlayView:**
   - Kunci tinggi `ResizeOverlayView` setinggi keyboard (jangan gunakan `fillMaxSize()` yang membuat jendela IME melayang ke atas layar).

---

## 2. Fitur Baru 1: Floating Key Preview (Pratinjau Huruf Melayang saat Mengetik)
* **Kebutuhan:** Saat pengguna menekan tombol huruf/angka (touch down), muncul gelembung pratinjau (*key popup magnifier*) melayang di atas tombol tersebut setinggi `-50.dp` dengan huruf besar (26sp, bold). Gelembung hilang seketika saat jari diangkat (touch up).
* **Implementasi:**
  - Simpan state tombol yang sedang ditekan: `activePressedKey: KeyModel?`.
  - Pada [`SboardKeyboardView.kt`](file:///home/coder/projects/sahalOS/sboard/app/src/main/java/com/sahalos/sboard/ui/keyboard/SboardKeyboardView.kt), jika `activePressedKey != null`, render `Box` gelembung pratinjau melayang di posisi koordinat tombol dengan bayangan halus (`shadow(4.dp, shape = RoundedCornerShape(8.dp))`), warna latar tema sedikit lebih terang, dan huruf kontras.
  - Sediakan master toggle `enableKeyPopupPreview` (default: `true`) di `SboardPreferences.kt`.

---

## 3. Fitur Baru 2: Redesain Halaman Pengaturan Profesional (`SettingsScreen.kt`)
Strukturkan halaman pengaturan menjadi kartu-kartu Material 3 yang rapi, elegan, dan terkelompok:
1. **Hero Header (Status Aktivasi IME):**
   - Kartu status: Menampilkan apakah Sboard sudah aktif dan dipilih sebagai keyboard utama atau belum, disertai tombol cepat: *"Aktifkan di Pengaturan"* dan *"Pilih Keyboard Utama"*.
2. **Seksi Tampilan & Tema:**
   - Pemilih tema horizontal (Nord Dark, Cyberpunk, AMOLED Black, Dracula, Monokai) dengan swatch warna riil.
   - Slider Ketinggian Keyboard (75% s/d 135%) dengan indikator persentase real-time.
   - Slider Jarak Dagu Bawah (0 s/d 36 dp) untuk ergonomi jempol.
   - Switch: *"Pratinjau Huruf Melayang (Key Popup)"*.
3. **Seksi Pengetikan & Gestur:**
   - Switch: *"Saran Kata Pasif (Non-agresif)"*.
   - Switch: *"Getaran Saat Mengetik (Haptic)"*.
   - Switch: *"Tekan Lama Simbol"* + Slider durasi (150 ms - 450 ms).
   - Switch: *"Navigasi Kursor Spasi (Cursor Glide)"*.
4. **Seksi Papan Klip (Clipboard):**
   - Switch: *"Papan Klip Aktif (FIFO 30 klip)"*.
   - Tombol outline: *"Bersihkan Riwayat Klip Tidak Disematkan"*.
5. **Seksi Pembaruan Aplikasi (In-App Updater):**
   - Kartu terdedikasi: Menampilkan versi aplikasi terpasang (`v1.0.x`), status update, dan tombol *"Periksa Pembaruan"*.

---

## 4. Fitur Baru 3: In-App Updater via GitHub Releases (Non-Play Store Distribution)
Karena Sboard didistribusikan mandiri tanpa Google Play Store, aplikasi memerlukan kemampuan update langsung dari GitHub Releases:

1. **Manifest & Izin Jaringan:**
   - Di `AndroidManifest.xml`, tambahkan izin:
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     ```
   - Tambahkan `FileProvider`:
     ```xml
     <provider
         android:name="androidx.core.content.FileProvider"
         android:authorities="${applicationId}.fileprovider"
         android:exported="false"
         android:grantUriPermissions="true">
         <meta-data
             android:name="android.support.FILE_PROVIDER_PATHS"
             android:resource="@xml/file_paths" />
     </provider>
     ```
   - Buat `app/src/main/res/xml/file_paths.xml`:
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <paths>
         <external-files-path name="apk_downloads" path="Download/" />
         <cache-path name="apk_cache" path="updates/" />
     </paths>
     ```
2. **UpdateManager (`data/UpdateManager.kt`):**
   - Panggil GitHub API: `https://api.github.com/repos/arbdevai/sboard/releases/latest`.
   - Parse respons JSON menggunakan `kotlinx.serialization.json`: ambil `tag_name`, `body` (Changelog Markdown formal tanpa emoji lebay), dan URL file `.apk` dari `assets`.
   - Bandingkan tag remote (misal `v1.0.6`) dengan `BuildConfig.VERSION_NAME`.
   - Jika versi baru tersedia:
     - Tampilkan modal konfirmasi dengan catatan rilis.
     - Unduh berkas APK ke cache aplikasi atau unduhan via `HttpURLConnection` / `DownloadManager`.
     - Luncurkan Intent instalasi paket:
       ```kotlin
       val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
       val intent = Intent(Intent.ACTION_VIEW).apply {
           setDataAndType(uri, "application/vnd.android.package-archive")
           addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
       }
       context.startActivity(intent)
       ```
3. **Komitmen Privasi:**
   - Izin internet diisolasi ketat **hanya** untuk modul pembaruan di `MainActivity`.
   - Kelas `SboardIME` **100% offline**, tidak ada telemetri dan data ketikan tidak pernah dikirim ke jaringan.
