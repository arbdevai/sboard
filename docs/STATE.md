# Sboard Project State & Evolution Log

## Current Status (v1.0.22 - Production Grade)
- **Active Version:** `1.0.22` (`versionCode = 10022`)
- **Git Branch:** `main`
- **NDK Architectures:** `arm64-v8a`, `x86_64`
- **Target Android SDK:** 36 (Min SDK: 30)

---

## Logika & Modul yang Telah Beroperasi Penuh

1. **Zero Dead-Zone Hitbox, Multi-Touch Rollover & Top-Row Slide Tolerance:**
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
