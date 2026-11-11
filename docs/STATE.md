# Sboard Project State & Evolution Log

## Current Status (v1.0.24 - Production Grade)
- **Active Version:** `1.0.24` (`versionCode = 10024`)
- **Git Branch:** `main`
- **NDK Architectures:** `arm64-v8a`, `x86_64`
- **Target Android SDK:** 36 (Min SDK: 30)

---

## Logika & Modul yang Telah Beroperasi Penuh

1. **Penguncian Tinggi Keyboard Konstan (Lock Frame Height Antara QWERTY & Simbol):**
   - File: `app/src/main/java/com/sahalos/sboard/service/SboardIME.kt` dan `SboardKeyboardView.kt`.
   - Mengunci total tinggi jendela keyboard (`totalKeyboardHeightDp`) agar tidak pernah berubah/melompat saat pengguna beralih antara QWERTY dan Simbol `?123` / `=\<`.
   - Mengisi ruang area tombol secara proporsional: saat 5 baris tiap baris berukuran 42dp, dan saat 4 baris simbol tiap baris otomatis membesar ke 52.5dp sehingga ketinggian jendela keyboard di layar tetap stabil dan presisi seperti Gboard/FlorisBoard.

2. **Stabilitas Sesi Riwayat Emoji (Session-Stable Recent Emojis Anti-Dancing Grid):**
   - File: `app/src/main/java/com/sahalos/sboard/ui/keyboard/EmojiScreen.kt`.
   - Membekukan tata letak kisi (*grid*) riwayat emoji selama layar emoji dibuka dengan `stableRecentList`.
   - Mengizinkan pengguna melakukan spam pengetikan emoji berulang kali di koordinat yang sama tanpa ada emoji yang melompat ke posisi indeks 0 secara mendadak. Pembaruan urutan riwayat tetap tersimpan di DataStore dan baru diaplikasikan pada sesi baru berikutnya.

3. **Pemulihan & Stabilisasi Backspace Repeat (Anti Infinite-Delete Loop):**
   - File: `app/src/main/java/com/sahalos/sboard/ui/keyboard/SboardKeyboardView.kt` dan `SboardIME.kt`.
   - Menggantikan `coroutineScope.launch` di dalam `detectTapGestures` yang rentan suspension hang dengan siklus pointer `awaitEachGesture` yang terhubung langsung ke `startBackspaceRepeat()` dan `stopBackspaceRepeat()`.
   - Menambahkan pembersihan mutlak di `onWindowHidden()`, `onFinishInputView()`, dan `onDestroy()` pada `SboardIME` serta `DisposableEffect(Unit)` di Compose sehingga repeat backspace tidak akan pernah bocor (*leak*) atau nyangkut saat berpindah aplikasi.

2. **Sinkronisasi ImeAction & Multiline Real-Time:**
   - File: `app/src/main/java/com/sahalos/sboard/service/SboardIME.kt`.
   - Mengimplementasikan `onStartInput()` agar setiap pergantian fokus kolom teks (meskipun keyboard tetap terbuka) langsung memperbarui ikon dan aksi tombol.
   - Mendeteksi flag `IME_FLAG_NO_ENTER_ACTION` dan `TYPE_TEXT_FLAG_MULTI_LINE` agar kolom catatan atau chat multiline berfungsi murni sebagai baris baru (`KEYCODE_ENTER`), bukan aksi submit.

3. **Inisialisasi Bersih Layer QWERTY Alfabet (Anti-Stuck Simbol):**
   - File: `app/src/main/java/com/sahalos/sboard/service/SboardIME.kt`.
   - Mereset penuh `modifierState` (`isSymActive = false`, `isSymPage2Active = false`, `isShifted = false`, `isCapsLock = false`) pada `onStartInputView()`, `onFinishInputView()`, dan `onWindowHidden()`. Keyboard dijamin selalu terbuka pada tampilan huruf QWERTY standar.

4. **Dedicated Number Row (Baris Angka 1-0) dengan Sakelar Pengaturan:**
   - File: `KeyLayoutProvider.kt`, `SboardPreferences.kt`, `SettingsScreen.kt`, `SboardKeyboardView.kt`, dan `SboardIME.kt`.
   - Menyediakan baris angka mandiri (`1 2 3 4 5 6 7 8 9 0`) di atas baris `qwertyuiop`.
   - Dilengkapi toggle `Baris Angka` di menu Pengaturan (*Tata Letak & Mengetik*) dengan penyimpanan DataStore.
   - Menyesuaikan proporsi tinggi keyboard secara dinamis (42dp per baris untuk 5-row) agar tetap proporsional dan tidak memakan layar berlebihan.

5. **Dukungan Form Input Angka Murni (Numpad 3x4 & Phone Pad):**
   - File: `KeyLayoutProvider.kt`, `SboardIME.kt`, dan `SboardKeyboardView.kt`.
   - Deteksi otomatis `InputType.TYPE_CLASS_NUMBER`, `InputType.TYPE_CLASS_PHONE`, `InputType.TYPE_CLASS_DATETIME`, flag desimal (`TYPE_NUMBER_FLAG_DECIMAL`), dan signed (`TYPE_NUMBER_FLAG_SIGNED`).
   - Menyajikan papan angka 4x4 kalkulator/PIN/telepon dengan tombol `DEL` dan `ENTER` yang presisi.
   - Tombol `ABC` di sudut kiri bawah memungkinkan pengguna beralih dengan mudah ke keyboard huruf alfabet QWERTY penuh kapan saja jika diperlukan.
   - Mode password numerik (`TYPE_NUMBER_VARIATION_PASSWORD`) otomatis mematikan bilah saran kata dan mencegah perekaman ke kamus pembelajaran kata.

6. **Zero Dead-Zone Hitbox, Multi-Touch Rollover & Top-Row Slide Tolerance:**
   - File: `app/src/main/java/com/sahalos/sboard/ui/keyboard/KeyView.kt` dan `SboardKeyboardView.kt`.
   - `pointerInput` ditempatkan pada `Box` terluar sebelum padding (0dp dead-zone di seluruh celah antar tombol).
   - Menggantikan `detectTapGestures` yang kaku dengan pipeline `awaitEachGesture` kustom yang memiliki toleransi geser vertikal 145% pada baris teratas (`qwertyuiop`) serta `KeyRolloverController` yang otomatis mengeksekusi tombol sebelumnya saat jempol kedua menekan huruf baru atau Spasi/Backspace.

2. **Koreksi Spasial & Bigram Native Rust + Filter Anti-Typo Trie:**
   - File: `sboard-core/src/lib.rs`, `Trie.kt`, `PassiveSuggestionEngine.kt`, dan `SboardKeyboardView.kt`.
   - Sentuhan di tepi tombol (`normX < 0.16f` atau `normX > 0.84f`) dikoreksi menggunakan kombinasi probabilitas spasial Gaussian dan frekuensi transisi bigram Bahasa Indonesia (`libsboard_core.so`) serta pengecekan prefix kamus (`hasPrefixMatch`).
   - `Trie.insert` mengakumulasi frekuensi kata dan menyaring kata dengan `frequency < 2` agar salah ketik 1 kali tidak pernah mengotori saran kata.

3. **Mesin Saran Kata (Trie Tree O(L) + Bigram Rust):**
   - File: `app/src/main/java/com/sahalos/sboard/core/Trie.kt` dan `PassiveSuggestionEngine.kt`.
   - Menggantikan flat linear loop dengan Prefix Tree in-memory terindeks frekuensi.
   - Fuzzy distance 1 didukung algoritma rekursif berpemangkasan cabang (*branch pruning*).
   - Skoring kandidat diperkuat probabilitas bigram dari `libsboard_core.so`.
   - Pembelajaran kata real-time saat Space/Enter ke database SQLite lokal (`UserDictionaryDbHelper.kt`).

4. **Penyimpanan Permanen Clipboard Bebas Lag:**
   - File: `app/src/main/java/com/sahalos/sboard/clipboard/SboardClipboardManager.kt`.
   - I/O non-blocking di `Dispatchers.IO` saat startup; pembacaan file tidak memblokir Main Thread IME.
   - Klip yang disematkan (*pinned*) kebal terhadap pembersihan (*clear all*) dan kebal duplikasi.

5. **Katalog Emoji Lengkap Unicode 15.1 + Search Grid:**
   - File: `app/src/main/assets/emojis_catalog.json` & `app/src/main/java/com/sahalos/sboard/core/EmojiProvider.kt`.
   - 1.898 emoji + 269 bendera teranotasi CLDR ID/EN.
   - Tokenized word matching menghilangkan false positive (misal: wortel vs mobil).
   - Grid 8-kolom vertikal scrollable di `EmojiSearchToolbar.kt` di atas QWERTY keyboard.

6. **Toolbar Ergonomis & Terminal Developer Deck (`CTRL`, `ALT`, `FN`):**
   - File: `SuggestionsBar.kt`, `FnRowView.kt`, `KeyLayoutProvider.kt`, dan `SboardIME.kt`.
   - Tombol `[CTRL]` dan `[FN]` di sisi kiri dengan proteksi transisi 350ms (`lastModeSwitchTimeMs`) serta `[Clipboard]`, `[Resize]`, dan `[Settings]` di sisi kanan.
   - Deteksi otomatis konteks terminal (`isTerminalField` untuk DevBox, Termux, ConnectBot, JuiceSSH):
     - `CTRL + [A–Z]` mengirimkan karakter kontrol ASCII murni (`0x01`–`0x1A`, termasuk `Ctrl+C` `SIGINT`, `Ctrl+D` `EOF`, `Ctrl+Z` `SIGTSTP`, `Ctrl+L` clear, `Ctrl+X/O/W/K/U` nano, serta `Ctrl+Enter` / `Shift+Enter` protokol Kitty).
     - `FnRowView` menampilkan **2-Row Developer & Terminal Deck**: Baris 1 (`ESC, TAB, ALT, ←, ↓, ↑, →, HOME, END, PGUP, PGDN, DEL` dengan hold-to-repeat pada panah & DEL) dan Baris 2 (`F1`–`F12` dengan pengiriman urutan escape ANSI/VT100 `\u001BOP`..`\u001B[24~` serta dukungan kombinasi modifier `Ctrl/Alt/Shift + Panah/Fn`).
   - Pada mode non-terminal (Web/Browser/Aplikasi biasa), tetap menggunakan `performContextMenuAction` dan `KeyEvent` standar Android.

7. **Gesture & Animasi:**
   - Spacebar cursor glide (`KEYCODE_DPAD_LEFT`/`RIGHT`).
   - Spring physics bubble preview pada `KeyView.kt`.
   - Resize keyboard real-time overlay.
