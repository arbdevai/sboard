# Sboard Product Requirements Document (PRD)

## 1. Executive Summary
Sboard (Sahal Board) adalah aplikasi papan ketik (Input Method Editor / IME) untuk Android berbasis arsitektur hybrid modern:
- **UI & Presentation Layer:** Kotlin + Jetpack Compose murni dengan animasi fisika pegas (spring physics).
- **Computation & Hitbox Engine:** Native Rust (`sboard-core`) via JNI untuk spatial hitbox Euclidean/Gaussian dan probabilitas transisi bigram.
- **Adaptive Intelligence:** SQLite local database + in-memory Prefix Trie ($O(L)$) untuk pembelajaran kosakata dinamis pengguna (user-learned lexicon).
- **Emoji System:** Standar Unicode UTS #51 (v15.1) dengan 1.898 emoji + 269 bendera teranotasi CLDR ID/EN resmi.
- **Tooling & Persistence:** Double persistence (DataStore + SharedPreferences), persistent JSON clipboard history, dan in-app direct updater via GitHub Releases.

---

## 2. Arsitektur Komponen

### A. Presentation & Interaction (Jetpack Compose)
- `SboardIME.kt`: Service inti `InputMethodService` pengelola lifecycle IME, input connection, dan mode switching.
- `SboardKeyboardView.kt`: Komponen penyusun layout tombol dengan multi-touch gesture detection.
- `KeyView.kt`: Rendering tombol individual dengan spring physics preview saat penekanan lama (long-press).
- `SuggestionsBar.kt`: Toolbar ergonomis terbagi tiga:
  - **Sisi Kiri:** Alat kerja (`[Clipboard]`, `[Resize]`).
  - **Sisi Tengah:** Rekomendasi kata adaptif (`Suggestions`).
  - **Sisi Kanan:** Modifier & pengaturan (`[CTRL]`, `[FN]`, `[Settings]`).
- `EmojiScreen.kt` & `EmojiSearchToolbar.kt`: Tampilan emoji dengan grid 8-kolom yang dapat di-scroll vertikal ala Gboard.
- `ClipboardScreen.kt`: Pengelola riwayat salinan dengan pin status permanen.

### B. Mesin Native Rust (`sboard-core`)
- Sumber: `sboard-core/` (Cargo crate `cdylib`).
- Target ABI: `arm64-v8a` dan `x86_64`.
- Fungsi utama:
  - `nativeResolveHit`: Menghitung probabilitas Gaussian jarak sentuhan pengguna terhadap pusat tombol.
  - `nativeGetBigramScore`: Matriks probabilitas transisi karakter dwigram (bilingual ID/EN).
  - Wrapper fail-safe: `NativeCoreBridge.kt` dengan fallback otomatis ke Kotlin murni jika binary `.so` tidak tersedia.

### C. Mesin Kamus Adaptif (Adaptive Suggestion Engine)
- `TrieTree.kt`: Struktur data pohon awalan (*Prefix Tree*) in-memory dengan traversal $O(L)$ dan fuzzy search Levenshtein jarak 1 dengan branch pruning.
- `PassiveSuggestionEngine.kt`: Koordinator prediksi kata real-time saat pengguna mengetik (`composingText`).
- `UserDictionaryDbHelper.kt`: Database SQLite lokal (`sboard_user_dict.db`) penyimpan kata dan frekuensi dengan pre-seed 3.000 kata dasar Indonesia.

### D. Manajemen Clipboard Permanen
- `SboardClipboardManager.kt`: Pengelola riwayat copy-paste berbasis penyimpanan disk `sboard_clipboard_history.json`.
- Non-blocking I/O pada thread `Dispatchers.IO`.
- Menjaga integritas klip yang disematkan (*pinned*) dari pembersihan otomatis maupun duplikasi.

---

## 3. Spesifikasi Fungsional

1. **Spacebar Cursor Glide:** Navigasi kursor teks kiri-kanan dengan menyeret tombol spasi (DPAD key events).
2. **Double Persistence Settings:** Konfigurasi keyboard (tinggi, padding, haptic, tema) disimpan ganda pada DataStore dan SharedPreferences untuk mencegah reset saat update aplikasi.
3. **Pencarian Emoji Tokenized:** Pencarian berbasis token awalan kata resmi Unicode CLDR Bahasa Indonesia dan Inggris.
4. **EmojiCompat Font Fallback:** Terintegrasi resmi melalui `DefaultEmojiCompatConfig` saat `onCreate()` untuk mencegah kemunculan karakter kotak kosong (*tofu*).
5. **In-App Updater:** Deteksi otomatis rilis GitHub terbaru, pengunduhan aman ke storage lokal, dan instalasi melalui Android `FileProvider`.
