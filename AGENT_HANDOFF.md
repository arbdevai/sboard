# 🤝 AGENT HANDOFF: Panduan Bugfix Layout QWERTY, Spasi Kursor, & Resize (v1.0.7)

Dokumen ini memuat investigasi teknis dan solusi pasti untuk 3 masalah UX yang dilaporkan pengguna pada `v1.0.6`.

---

## 1. Layout Huruf ASDFGHJKL Mepet Kiri Kanan (Tidak Sesuai Gboard)

### Akar Masalah (Root Cause):
Di `KeyLayoutProvider.kt`:
* Baris 1 (`QWERTYUIOP`): 10 tombol dengan bobot 1.0f (Total bobot = 10.0f).
* Baris 2 (`ASDFGHJKL`): 9 tombol dengan bobot 1.0f (Total bobot = 9.0f) **TANPA SPACER DI KIRI DAN KANAN**.
* Akibatnya:
  1. Lebar tiap tombol baris 2 membesar menjadi `1/9` layar (~11.1%) bukan 10%.
  2. Huruf `A` menempel tepat di tepi kiri layar ($X = 0$), dan huruf `L` menempel tepat di tepi kanan layar ($X = \text{max}$).
  3. Tata letak tidak bersilang (*staggered*) segitiga khas QWERTY standar Gboard/iOS.

### Solusi Wajib:
Di `KeyLayoutProvider.getQwertyRows()`:
Tambahkan spacer `0.5f` di awal dan di akhir Baris 2, sehingga total bobot baris 2 menjadi `0.5 + 9.0 + 0.5 = 10.0f`:
```kotlin
// Pada baris 2 Qwerty:
val row2 = listOf(
    KeyModel(-10, "", null, 0.5f, KeyType.SPACER), // Spacer kiri
    KeyModel('a'.code, "a", "@", 1.0f),
    KeyModel('s'.code, "s", "#", 1.0f),
    KeyModel('d'.code, "d", "$", 1.0f),
    KeyModel('f'.code, "f", "%", 1.0f),
    KeyModel('g'.code, "g", "&", 1.0f),
    KeyModel('h'.code, "h", "*", 1.0f),
    KeyModel('j'.code, "j", "-", 1.0f),
    KeyModel('k'.code, "k", "+", 1.0f),
    KeyModel('l'.code, "l", "/", 1.0f),
    KeyModel(-11, "", null, 0.5f, KeyType.SPACER)  // Spacer kanan
)
```
Pada `SboardKeyboardView.kt`, jika `key.type == KeyType.SPACER`, cukup render `Spacer(modifier = Modifier.weight(key.weight))`.
Hasil: Huruf `A` s/d `L` langsung bergeser rapi ke tengah dengan lekukan 5% di kiri dan kanan, presisi seperti Gboard!

---

## 2. Fitur Kursor pada Spasi Gak Fungsi (Spacebar Glide Cursor)

### Akar Masalah (Root Cause):
1. **Konflik Gesture Compose:**
   Di `SboardKeyboardView.kt`, tombol spasi dibungkus `Box.pointerInput { detectDragGestures }`, namun child `KeyView` di dalamnya memiliki `detectTapGestures(onTap, onLongPress)` sendiri. Di Compose, child `detectTapGestures` menelan sentuhan sehingga `detectDragGestures` pada pembungkus luar tidak pernah menerima event seret (*drag*).
2. **Kelemahan `setSelection` pada `moveCursor`:**
   Method `moveCursor()` di `SboardIME.kt` memakai `getTextBeforeCursor(1000, 0)` yang sering gagal membaca panjang teks riil pada beberapa aplikasi.

### Solusi Wajib:
1. Satukan penanganan gesture spasi: Saat merender `KeyView` untuk spasi, jangan berikan `onLongClick` terpisah ke child jika memakai gesture drag luar. Atau gunakan `detectHorizontalDragGestures` dengan `onDragStart`, `onHorizontalDrag`, dan deteksi tap jika tidak terjadi drag.
2. Di `SboardIME.kt`, ganti navigasi kursor menggunakan KeyEvent resmi sistem Android (**100% universal dan kompatibel di semua aplikasi**):
   ```kotlin
   private fun moveCursor(offset: Int) {
       triggerHaptic()
       val keyCode = if (offset > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
       val count = kotlin.math.abs(offset)
       for (i in 0 until count) {
           sendDownUpKeyEvents(keyCode)
       }
   }
   ```
   Pengiriman `KEYCODE_DPAD_LEFT` dan `KEYCODE_DPAD_RIGHT` dijamin menggerakkan kursor 1 karakter di aplikasi apa pun (termasuk WhatsApp, Chrome, Termux, editor teks).

---

## 3. Fitur Resize "Ngejelly" & Gak Bisa Digeser

### Akar Masalah (Root Cause):
Handle drag atas pada `ResizeOverlayView.kt` mengupdate `previewHeightRatio` pada setiap frame drag delta. Ini memicu `previewTotalHeightDp` di parent Box langsung membesar/mengecil saat jari masih menempel. Karena handle berada di `Alignment.TopCenter`, membesarnya view mengubah posisi koordinat lokal handle di bawah jari pengguna, menciptakan *feedback loop* osilasi bolak-balik yang terasa seperti membal/karet (*jelly*).

### Solusi Wajib:
1. **Stabilkan Drag Gesture:**
   Gunakan akumulasi drag dari posisi awal sentuh (*drag start baseline*) atau gunakan rentang rasio per langkah (*step-based ratio* misal step 0.05).
2. **Sediakan Tombol Preset Cepat (Quick Preset Pills) di Bar Atas Overlay:**
   Tambahkan tombol pill ukuran siap pakai di bar kontrol atas:
   * **Kompak (85%)**
   * **Normal (100%)**
   * **Tinggi (115%)**
   * **Maksimal (125%)**
   Ini memberikan kepastian 1-ketukan bagi pengguna untuk langsung mengubah ukuran tanpa harus bergulat dengan drag gesture di layar sentuh HP.

---

## Ringkasan Eksekusi Rilis v1.0.7:
- [x] Update `KeyLayoutProvider.kt`: Berikan spacer `0.5f` di kiri dan kanan baris 2 (`ASDFGHJKL`) sehingga total bobot baris 2 menjadi 10.0f (tata letak *staggered* segitiga persis Gboard).
- [x] Update `KeyModel.kt`: Tambahkan tipe `SPACER` pada `enum class KeyType`.
- [x] Update `SboardIME.kt`: Ubah `moveCursor` menjadi pengiriman `sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT / RIGHT)` yang 100% universal di seluruh aplikasi Android.
- [x] Update `SboardKeyboardView.kt`: Penanganan gesture spacebar disatukan menggunakan `awaitEachGesture` (tap untuk spasi, drag horizontal untuk navigasi kursor, hold 450ms untuk IME picker) tanpa interupsi child `clickable`.
- [x] Update `ResizeOverlayView.kt`: Stabilkan akumulasi drag gesture dengan `rememberUpdatedState` dan tambahkan tombol preset ukuran cepat (85%, 100%, 115%, 125%).
- [x] Verifikasi sintaks via `verify_kotlin_syntax.py` (0 error, 0 warning).
- [x] Build sukses di GitHub Actions CI (Run ID: `41974586281`) dan file APK `Sboard-v1.0.7.apk` siap di `/home/coder/storage/downloads/`.

---

## Ringkasan Eksekusi Rilis v1.0.8:
- [x] **Redesain Settings Profesional (`SettingsScreen.kt`)**:
  - Hero Header dengan status aktif/default IME Android, dot status dinamis, dan tombol pintasan sistem.
  - Seksi Tampilan & Tema: Pemilih tema horizontal dengan swatch warna riil, Slider Ketinggian Keyboard (75%-135%), Slider Jarak Dagu Bawah (0-36 dp), dan switch Key Popup.
  - Seksi Pengetikan & Gestur: Toggle Saran Kata Pasif, Umpan Balik Getar Haptik, Simbol Tekan Lama + Slider durasi, dan Navigasi Kursor Spasi.
  - Seksi Papan Klip: Toggle Riwayat Papan Klip dan tombol Bersihkan Riwayat Tidak Disematkan.
  - Seksi Pembaruan Aplikasi: Integrasi modul In-App Updater dengan status dan progress bar.
- [x] **Floating Key Preview Melayang (`KeyView.kt` & `SboardKeyboardView.kt`)**:
  - Deteksi sentuhan instan pada *touch down* memunculkan gelembung huruf berukuran besar (26sp bold, shadow 8dp) melayang di atas tombol `-52.dp`.
  - Gelembung hilang seketika saat jari diangkat (*touch up*).
  - Terkoneksi dengan master switch `enableKeyPopupPreview` di `SboardPreferences.kt`.
- [x] **In-App Updater via GitHub Releases (`UpdateManager.kt` & `AndroidManifest.xml`)**:
  - Pengecekan versi rilis terbaru dari API GitHub (`arbdevai/sboard/releases/latest`).
  - Pengunduhan berkas APK ke cache internal dengan progress bar real-time.
  - Peluncuran otomatis installer APK via Android `FileProvider` (`content://...fileprovider`).
  - Izin `INTERNET` dan `REQUEST_INSTALL_PACKAGES` terisolasi ketat di `MainActivity`; `SboardIME` tetap 100% offline dan zero telemetry.
- [x] Verifikasi sintaks via `verify_kotlin_syntax.py` (0 error, 0 warning).
- [x] Build sukses di GitHub Actions CI (Run ID: `41978513723`) dan APK `Sboard-v1.0.8.apk` siap di `/home/coder/storage/downloads/`.

---

## Ringkasan Eksekusi Rilis v1.0.10:
- [x] **Pindahkan Ctrl & Fn ke Toolbar Atas (`SuggestionsBar.kt`, `KeyLayoutProvider.kt`)**:
  - Tombol modifier `Ctrl` dan `Fn` dipindahkan dari baris bawah keyboard ke toolbar atas bersebelahan dengan clipboard & resize.
  - Membuka ruang lega pada spasi (weight 4.3f) dan tanda baca bawah persis standar Gboard tanpa sempit.
  - Hapus icon emoji redundan dari toolbar atas karena tombol emoji dedikasi ada di baris bawah.
- [x] **Inline Emoji Search & Quick Chips (`EmojiScreen.kt`)**:
  - Mengatasi limitasi Android `InputMethodService` (`FLAG_NOT_FOCUSABLE`) dengan menghadirkan `InlineEmojiSearchKeyboard` (3 baris QWERTY) langsung di dalam layar emoji.
  - Filter cepat 1-ketukan berbasis chip kata kunci (`senyum, hati, api, ketawa, jempol, kucing, kopi, makan, sedih, pesta, bintang, doa, mobil`).
  - Fitur tab Emoji Terakhir Digunakan (Recent Emojis) tersinkronisasi otomatis dan persisten.
- [x] **Persistensi Pengaturan Anti-Reset saat Pembaruan (`SboardPreferences.kt`, `AndroidManifest.xml`)**:
  - Ditambahkan `android:hasFragileUserData="true"` dan `android:restoreAnyVersion="true"` pada manifest.
  - Dual-layer storage: Sinkronisasi instan ke `SharedPreferences` (`sboard_prefs_backup`) sebagai fail-safe DataStore saat update aplikasi.
- [x] **Tombol Install Eksplisit pada In-App Updater (`UpdateManager.kt`, `SettingsScreen.kt`)**:
  - Transisi state ke `UpdateUiState.Downloaded(apkFile)` setelah pengunduhan tuntas.
  - Tombol hijau tegas "PASANG SEKARANG (INSTALL)" disediakan agar pengguna bisa meluncurkan installer paket kapan saja.
  - Pengecekan izin instalasi aplikasi tidak dikenal (`canRequestPackageInstalls()`) untuk Android 8.0+.
- [x] **Status Alert Default IME Dinamis (`SettingsScreen.kt`)**:
  - Pemantauan status `ON_RESUME` via `LifecycleEventObserver` pada Compose.
  - Banner peringatan langsung hilang dan berganti badge hijau sukses ("✓ Sboard Aktif sebagai Keyboard Utama") seketika pengguna kembali dari pemilih keyboard sistem.
- [x] **Penyesuaian Offset Popup Huruf Atas (`KeyView.kt`, `SboardKeyboardView.kt`)**:
  - Deteksi baris teratas (QWERTYUIOP) dengan offset pop-up terkalibrasi (-36.dp) agar gelembung huruf tidak mentok di plafon window keyboard.

---

## Ringkasan Eksekusi Rilis v1.0.12:
- [x] **Label Huruf Kapital CTRL & FN (`SuggestionsBar.kt`)**:
  - Tombol diubah menjadi huruf kapital penuh: `CTRL` dan `FN` (All Caps).
  - Menggunakan `letterSpacing = 0.5.sp`, `fontSize = 11.sp`, dan `FontWeight.SemiBold`.
  - Geometri tinggi huruf kapital sekarang sejajar rapi secara visual dengan ikon 18x18 dp Clipboard dan Resize di sebelahnya.
- [x] **Animasi Pop-up Long-Press Simbol (`KeyView.kt`)**:
  - Ditambahkan animasi `animateFloatAsState` berbasis spring physics (`Spring.DampingRatioMediumBouncy`) saat tombol ditahan.
  - Gelembung membesar halus (skala 1.0f -> 1.15f), border menyala dengan warna aksen tema, dan simbol alternatif berubah menjadi karakter utama yang disorot jelas.
- [x] **Pencarian Emoji 100% Standar Gboard (`EmojiSearchToolbar.kt`, `SboardIME.kt`, `EmojiScreen.kt`)**:
  - Menghentikan penggunaan mini-keyboard darurat di dalam layar emoji.
  - Saat kotak pencarian diketuk, keyboard beralih ke mode `EMOJI_SEARCH`:
    - Bagian bawah: Keyboard QWERTY asli ukuran penuh dengan seluruh fitur haptik, ukuran standar, dan key preview.
    - Bagian atas (`EmojiSearchToolbar`): Bilah pencarian dengan hasil live emoji yang difilter seketika saat pengguna mengetik huruf.
    - Mengetuk emoji langsung memasukkan karakter ke aplikasi aktif dan menyimpan riwayat terbaru tanpa menutup mode pencarian.
- [x] **Katalog Emoji Dinamis Berbasis Aset JSON (`emojis_catalog.json`, `EmojiProvider.kt`)**:
  - Daftar 816 emoji dipindahkan dari hardcode Kotlin ke berkas `app/src/main/assets/emojis_catalog.json`.
  - Parsed secara terstruktur dan fleksibel untuk pembaharuan jangka panjang tanpa perlu recompile kode Kotlin setiap rilis Unicode baru.
- [x] **Integrasi Mesin Rust Native (`sboard-core`)**:
  - Crate Rust `sboard-core` diimplementasikan dengan modul `spatial.rs` (Euclidean distance + Gaussian probability), `bigram.rs` (tabel frekuensi transisi karakter anti-typo), dan `state.rs` (atomic modifier state machine).
  - Wrapper JNI `NativeCoreBridge.kt` dengan arsitektur fail-safe: otomatis fallback ke mesin Kotlin jika library native tidak tersedia.
  - Otomasi build Rust multi-ABI (`arm64-v8a` & `x86_64`) via `cargo-ndk` di pipeline GitHub Actions CI.

- [x] **Styling Flat Konsisten untuk Ctrl & Fn (`SuggestionsBar.kt`)**:
  - Latar belakang (background) kotak tombol `Ctrl` dan `Fn` di toolbar dihapus total saat tidak aktif.
  - Tampilan visual sekarang 100% konsisten, flat, dan menyatu rapi dengan ikon Clipboard dan Resize (`tint = textColor.copy(alpha = 0.85f)`).
  - Saat aktif, hanya warna teks yang berubah menjadi aksen dan tebal tanpa kotak background yang merusak estetika toolbar.
---

## Ringkasan Eksekusi Rilis v1.0.13:
- [x] **Katalog Lengkap Unicode Emoji 15.1 (1.898+ Emoji Standar Gboard)**:
  - Berkas `app/src/main/assets/emojis_catalog.json` diperbarui dengan seluruh spesifikasi resmi Unicode UTS #51 (15.1) dalam urutan CLDR standar.
  - Cakupan lengkap semua kategori:
    - Smileys & Emotion: 168 emoji (sebelumnya hanya ~60)
    - People & Body: 385 emoji (sebelumnya hanya ~50)
    - Animals & Nature: 153 emoji (sebelumnya hanya ~40)
    - Food & Drink: 135 emoji (sebelumnya hanya ~70)
    - Travel & Places: 218 emoji (sebelumnya hanya ~50)
    - Activities: 85 emoji
    - Objects: 262 emoji
    - Symbols: 223 emoji
    - Flags: 269 bendera lengkap seluruh negara di dunia (sebelumnya hanya 30)
- [x] **Pre-warming & O(1) Category Indexing (`EmojiProvider.kt`, `SboardIME.kt`)**:
  - `EmojiProvider.load(context)` dipre-warm di thread IO saat `SboardIME.onCreate()` sehingga transisi ke layar emoji berjalan instan tanpa lag.
  - Indeks kategori di-cache dalam `Map<EmojiCategory, List<String>>` untuk waktu akses konstan $O(1)$.

---

## Ringkasan Eksekusi Rilis v1.0.14:
- [x] **Basis Data Emoji Unicode CLDR Resmi (ID & EN) + Tokenized Matching**:
  - 100% dari 1.898 emoji dan 269 bendera di `emojis_catalog.json` diperkaya dengan metadata resmi Unicode CLDR (Bahasa Indonesia & Bahasa Inggris) serta sinonim percakapan sehari-hari (`ketawa`, `ngakak`, `sedih`, `nangis`, `mobil`, `makan`, `uang`, dll.).
  - Mengganti algoritma `contains` naif dengan tokenized word prefix matching: pencarian "mobil" hanya menampilkan kendaraan transportasi nyata (tanpa false-positive seperti wortel atau bola kristal).
- [x] **Tampilan Pencarian Emoji Ala Gboard (`EmojiSearchToolbar.kt`, `SboardIME.kt`)**:
  - Mengubah mode pencarian menjadi **Vertical Scrollable Grid 8-Kolom (tinggi 115dp)** tepat di atas keyboard QWERTY.
  - Toolbar quick action (`CTRL`, `FN`, dll.) otomatis disembunyikan saat mode pencarian emoji aktif agar layar tetap luas dan fokus.
  - Dilengkapi chip pencarian cepat (*Quick Filters*) saat kolom pencarian masih kosong.
- [x] **Inisialisasi `EmojiCompat`**:
  - `EmojiCompat` diinisialisasi otomatis saat `onCreate()` dengan `DefaultEmojiCompatConfig` untuk memastikan perangkat lama tidak menampilkan kotak silang (*tofu*).
- [x] **Mesin Saran Adaptif & Database Pengguna (`UserDictionaryDbHelper.kt`, `PassiveSuggestionEngine.kt`)**:
  - Penyimpanan lokal SQLite `sboard_user_dict.db` dengan tabel `user_words` (`word`, `frequency`, `last_used`).
  - Pre-seeding 3.000 kata dasar Indonesia frekuensi tinggi dari `id_seed_vocabulary.txt`.
  - Auto-learning: kata otomatis dipelajari saat pengguna menekan Space atau Enter.
  - User-boost: kata yang dipilih langsung dari saran mendapat tambahan frekuensi (+5).
  - Koreksi typo berbasis jarak Levenshtein 1 dikombinasikan dengan skor bigram native Rust (`libsboard_core.so`).



