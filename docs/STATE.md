# Sboard Project State & Evolution Log

## Current Status (v1.0.17 - Production Grade)
- **Active Version:** `1.0.17` (`versionCode = 10017`)
- **Git Branch:** `main`
- **NDK Architectures:** `arm64-v8a`, `x86_64`
- **Target Android SDK:** 36 (Min SDK: 30)

---

## Logika & Modul yang Telah Beroperasi Penuh

1. **Mesin Saran Kata (Trie Tree O(L) + Bigram Rust):**
   - File: `app/src/main/java/com/sahalos/sboard/core/Trie.kt` dan `PassiveSuggestionEngine.kt`.
   - Menggantikan flat linear loop dengan Prefix Tree in-memory terindeks frekuensi.
   - Fuzzy distance 1 didukung algoritma rekursif berpemangkasan cabang (*branch pruning*).
   - Skoring kandidat diperkuat probabilitas bigram dari `libsboard_core.so`.
   - Pembelajaran kata real-time saat Space/Enter ke database SQLite lokal (`UserDictionaryDbHelper.kt`).

2. **Penyimpanan Permanen Clipboard Bebas Lag:**
   - File: `app/src/main/java/com/sahalos/sboard/clipboard/SboardClipboardManager.kt`.
   - I/O non-blocking di `Dispatchers.IO` saat startup; pembacaan file tidak memblokir Main Thread IME.
   - Klip yang disematkan (*pinned*) kebal terhadap pembersihan (*clear all*) dan kebal duplikasi.

3. **Katalog Emoji Lengkap Unicode 15.1 + Search Grid:**
   - File: `app/src/main/assets/emojis_catalog.json` & `app/src/main/java/com/sahalos/sboard/core/EmojiProvider.kt`.
   - 1.898 emoji + 269 bendera teranotasi CLDR ID/EN.
   - Tokenized word matching menghilangkan false positive (misal: wortel vs mobil).
   - Grid 8-kolom vertikal scrollable di `EmojiSearchToolbar.kt` di atas QWERTY keyboard.

4. **Toolbar Ergonomis Anti-Salah Pencet:**
   - File: `app/src/main/java/com/sahalos/sboard/ui/keyboard/SuggestionsBar.kt`.
   - Tombol `[Clipboard]` dan `[Resize]` di sisi kiri.
   - Tombol `[CTRL]`, `[FN]`, dan `[Settings]` di sisi kanan.
   - Menghilangkan tabrakan koordinat dengan tombol Back di overlay screen.

5. **Gesture & Animasi:**
   - Spacebar cursor glide (`KEYCODE_DPAD_LEFT`/`RIGHT`).
   - Spring physics bubble preview pada `KeyView.kt`.
   - Resize keyboard real-time overlay.
