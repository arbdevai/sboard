# 📱 Sboard Master Specification & Architecture Blueprint
**Proyek:** Sboard (Sahal Board)  
**Versi Dokumen:** v1.0 (Final Research & Architecture Phase)  
**Tech Stack:** 
* **UI & Android IME Lifecycle:** Kotlin + Jetpack Compose
* **Core Engine (Under-the-Hood):** Rust via JNI / UniFFI (`sboard-core`)
**Paradigma:** Zero Dead-Code, High Performance, Privacy-First (0 Izin Internet), Anti-AI Slop  
**Target Platform:** Android 11+ (API Level 30+)

---

## 1. Identitas, Visi & Filosofi Produk

* **Nama:** **Sboard** (Singkat, tajam, identitas kuat turunan dari Sahal + Speed/Smart).
* **Visi:** Keyboard mobile yang super ringan (RAM < 30 MB), tanpa bloatware, 0 milidetik *cold start*, anti-typo cerdas berbasis probabilitas, dan ramah untuk developer/power-user (tombol `Ctrl`, shortcut navigasi kursor, tombol `Fn` + `F1`–`F12` layaknya laptop).
* **Filosofi Rilis Awal (KISS - Keep It Simple):** Fokus 100% menyempurnakan **QWERTY** terlebih dahulu. Menolak komplikasi dini, namun menjamin struktur data fleksibel (*Data-Driven*) agar siap diekstensi ke layout lain (Colemak, Dvorak, AZERTY) di masa depan tanpa merombak kode UI.
* **Filosofi UI & Anti-AI Slop:** Menolak tombol toolbar ganda yang memakan tempat. Menolak penggunaan font teks emoji sebagai icon tombol (wajib vektor geometris presisi).

---

## 2. Analisis Keluhan & Bug Pasar (Market Pain Points) & Solusi Sboard

Berdasarkan komparasi mendalam terhadap Gboard, SwiftKey, dan FlorisBoard:

| # | Keluhan / Bug di Pasaran | Masalah Teknis | Solusi Nyata di Sboard |
| :--- | :--- | :--- | :--- |
| **1** | **Autocorrect "Sok Tahu" & Agresif** | Keyboard memaksa mengubah kata slang / kodingan / singkatan secara otomatis saat tombol spasi ditekan. | **Passive Autocorrect Only:** Saran hanya ditampilkan di strip atas. **Tidak akan pernah** mengganti kata secara otomatis kecuali user sengaja mengetuk saran tersebut. |
| **2** | **Spam Notifikasi Toast Clipboard (Android 12+)** | Keyboard membaca clipboard sistem secara terus-menerus di background, memicu notifikasi *"App pasted from your clipboard"* tanpa henti. | **On-Demand & Event-Driven Clipboard:** Akses clipboard hanya dipicu saat user membuka tab clipboard, atau memanfaatkan listener sistem resmi tanpa polling kotor di background. |
| **3** | **Jempol Pegal (Tombol Mepet Dagu Layar)** | Pada HP layar penuh dengan navigasi gestur, baris spasi terlalu mepet ke tepi bawah layar, memaksa jempol menekuk tajam ke bawah. | **Adjustable Bottom Padding:** Pengaturan jarak bawah (offset dp) agar posisi keyboard bisa dinaikkan sesuai kenyamanan genggaman tangan. |
| **4** | **Navigasi Kursor yang Bikin Frustrasi** | Memindahkan kursor ke tengah kata dengan jari di layar sangat sulit dan sering meleset. Fitur swipe spasi di Gboard sering macet / kelewat sensitif. | **Spacebar Cursor Glide:** Menggeser jari ke kiri/kanan pada tombol spasi untuk menggerakkan kursor teks dengan halus dan presisi ($\Delta X = 12\text{ dp}$). |
| **5** | **Cold Start Delay & Bloatware** | Keyboard butuh 0.5 - 2 detik untuk muncul karena harus memuat engine AI, tema online, analitik, dan translator. Memakan RAM 150 MB - 300 MB. | **Zero-Bloat Core:** Konsumsi RAM ditargetkan < 30 MB. Tanpa modul internet/analitik, keyboard muncul instan (0 milidetik) saat kolom teks diketuk. |
| **6** | **Kekhawatiran Privasi (Keylogger Anxiety)** | Keyboard Play Store meminta izin internet penuh (`android.permission.INTERNET`) untuk sinkronisasi cloud dan telemetri. | **Zero Internet Permission:** Manifest `AndroidManifest.xml` sama sekali tidak memuat izin internet. Mustahil mengirim data ketikan keluar dari perangkat. |
| **7** | **Ngetik 2 Jari Pincang (Dropped Keystrokes)** | Pada keyboard kustom pemula, ketukan jempol kanan-kiri yang sangat cepat sering saling membatalkan karena event multi-touch tidak ditangani secara independen. | **Independent Multi-Pointer Tracking:** Menggunakan `pointerInput` Compose dengan pelacakan `PointerId` mandiri, diproses instan pada event `ACTION_DOWN`. |
| **8** | **Typo Akibat Jempol Besar (Fat-Finger Problem)** | Target sentuh (*hitbox*) tombol kaku 1:1, sehingga meleset 1 mm ke tepi langsung menghasilkan huruf yang salah. | **Dynamic Hitbox Engine (Rust):** Menghitung jarak Euclidean ke titik tengah tombol terdekat dikombinasikan dengan tabel frekuensi pasangan huruf (Bigram). |
| **9** | **Layout Pecah Saat Diubah Ukuran / Landscape** | Menggunakan ukuran pixel/dp kaku (*hardcoded*), sehingga saat ukuran diubah atau layar berputar ke landscape, tombol terpotong atau menutupi 90% layar. | **Proportional Weight-Based Layout & Landscape Separation:** Skala berbasis rasio bobot (*weight*) dinamis dengan penanganan khusus mode horizontal. |
| **10**| **Ketidakmampuan Mengontrol Terminal / SSH** | Keyboard standar tidak punya tombol `Ctrl` dan baris fungsi `F1`–`F12`, sehingga aplikasi CLI (Termux, Neovim, htop, Midnight Commander) tidak bisa dioperasikan. | **Native PC Keys (Ctrl, Esc, Tab, F1-F12 via Fn):** Pemetaan langsung ke Android `KeyEvent.KEYCODE_F1` s/d `KEYCODE_F12` secara terintegrasi. |
| **11**| **Tombol Enter / Search Macet Tidak Berubah** | Keyboard gagal membaca perubahan `EditorInfo.imeOptions` saat kursor berpindah antar kolom input. | **Dynamic Action Key Listener:** Evaluasi `imeOptions` (Search, Send, Go, Next, Done) secara reaktif pada `onStartInputView`. |
| **12**| **Tampilan Murahan ("AI Slop" Teks Emoji di Tombol)** | Menggunakan karakter emoji teks (🔍, ✈️, ⌫) pada tombol yang menghasilkan rendering tidak sejajar (*misaligned*) dan warna bentrok. | **Curated Vector Asset System (Lucide Design):** Vektor murni beresolusi independen dengan dynamic tinting terikat tema. |
| **13**| **Backspace Nyangkut / Bablas (Runaway Delete)** | Di FlorisBoard/keyboard lain, tekan lama tombol hapus kadang tidak berhenti saat jari diangkat, terus menghapus seluruh teks secara tak terkendali. | **Coroutine Job Cancellation Guard:** Pembatalan instan loop hapus pada `ACTION_UP`, `ACTION_CANCEL`, pointer keluar batas, dan hilangnya fokus IME. |

---

## 3. Gestur Cerdas: Spasi & Backspace Anti-Nyangkut

Menyederhanakan navigasi kursor dan menjamin keamanan penghapusan teks tanpa risiko kehilangan data:

```
[ GESTUR TOMBOL SPASI ]
├─ Geser Kiri / Kanan  ──> Cursor Glide (Menggerakkan kursor teks dengan halus & presisi)
└─ Tekan Lama (Hold)   ──> Tampilkan Dialog Ganti Input Method (IME Switcher)

[ GESTUR TOMBOL BACKSPACE / HAPUS ]
├─ Ketuk Sekali        ──> Hapus 1 karakter di kiri kursor
└─ Tekan Lama (Hold)   ──> Repeat Delete Cepat (DENGAN CANCEL GUARD ANTI-NYANGKUT)
```

### A. Spacebar Cursor Glide & IME Picker
1. **Cursor Glide:** Setiap pergeseran horizontal jari sejauh $\Delta X = 12\text{ dp}$ pada tombol spasi, kursor teks bergeser 1 karakter ke kiri/kanan via `inputConnection.setSelection()`.
2. **IME Switcher:** Menahan tombol spasi selama 400 ms otomatis memicu menu resmi sistem `showInputMethodPicker()`.

### B. Arsitektur Tombol Hapus Anti-Nyangkut (Zero Runaway Delete)
1. **Triple-Cancellation Guard:**
   Loop hapus cepat dijalankan di dalam Coroutine Job yang di-*cancel* seketika jika terjadi salah satu dari 4 kondisi:
   * Event `ACTION_UP` (jari diangkat).
   * Event `ACTION_CANCEL` (sistem Android membatalkan sentuhan).
   * Titik koordinat sentuh bergeser keluar dari area tombol Backspace.
   * Keyboard kehilangan fokus jendela (`onFinishInputView` / `onWindowHidden`).
2. **Pacing Terkendali (Anti-Buffer Bloat):**
   * Penundaan awal (*initial delay*): 350 ms.
   * Interval hapus berulang: 45 ms per karakter.
   * Tidak menumpuk antrian event di `InputConnection`, menjamin penghapusan **berhenti tepat di titik jari diangkat**.

---

## 4. Sistem Tema & Distribusi Web

```
┌─────────────────────────────────────────────────────────────┐
│             File Format: Paket .sboard-theme (ZIP)          │
│  ├─ theme.json      (Skema warna, radius, elevasi, border)  │
│  └─ background.webp (Opsional: gambar latar terkompresi)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┼───────────────┐
               │               │               │
┌──────────────▼──────┐ ┌──────▼──────┐ ┌──────▼──────────────┐
│  Jalur 1: Browser   │ │  Jalur 2:   │ │ Jalur 3: In-App     │
│  Download & Klik    │ │  Deep Link  │ │ Community Store     │
│  (File Association) │ │  sboard://  │ │ (GitHub Repository) │
└─────────────────────┘ └─────────────┘ └─────────────────────┘
```

### A. Mode Tema Terang, Gelap & Ikut Sistem (Day/Night Sync)
* **Mode Pilihan:** `Light` (Selalu Terang), `Dark` (Selalu Gelap), dan `Follow System` (`isSystemInDarkTheme()`).
* **Dukungan Dual-Palette di `theme.json`:** Satu paket `.sboard-theme` dapat mendefinisikan palet `colorsLight` dan `colorsDark` sekaligus agar otomatis beradaptasi saat HP berganti mode malam.

### B. 3 Alur Distribusi Web
1. **File Association (Download Browser Biasa):**
   * Di `AndroidManifest.xml`, Sboard mendaftarkan intent filter untuk ekstensi `.sboard-theme`.
   * Saat user mendownload tema dari web/browser mana pun dan mengetuk file hasil download, Android otomatis memunculkan pilihan: *"Buka dengan Sboard"*. Tema langsung diekstrak dan dipasang.
2. **Web Deep Link (One-Click Install):**
   * Website komunitas dapat memasang tautan protokol khusus:
     `href="sboard://theme?download=https://example.com/themes/nord.sboard-theme"`
   * Mengklik tautan ini di browser HP akan langsung membuka Sboard dan mengunduh serta memasang tema secara instan tanpa user repot membuka folder download.
3. **In-App Community Catalog (Tanpa Server Berbayar):**
   * Memanfaatkan **GitHub Repository** publik sebagai database tema gratis.
   * File `index.json` di repo memuat katalog nama tema, pratinjau kartu gambar, dan link file `.sboard-theme`.
   * Tab *"Explore Online Themes"* di dalam aplikasi membaca file indeks ini dan menampilkan katalog estetik dengan tombol *1-Click Download*.

---

## 5. Standar Aset Visual: Curated Vector Asset System (Lucide Design)

12 icon vektor esensial yang diadopsi dari standar modern **[Lucide Icons](https://lucide.dev)** (stroke 2dp, sudut melengkung), disimpan langsung di folder `res/drawable/` tanpa dependensi eksternal (ukuran total < 8 KB):
`ic_backspace`, `ic_shift`, `ic_caps_lock`, `ic_enter`, `ic_search`, `ic_send`, `ic_next`, `ic_globe`, `ic_clipboard`, `ic_pin`, `ic_arrow_left`, `ic_arrow_right`, `ic_settings`.

* Mendukung pewarnaan dinamis (`tint`) 100% mengikuti tema aktif.

---

## 6. Spesifikasi Developer Keys (`Ctrl` & `F1`–`F12` via `Fn`)

```
[Normal Mode: Baris Angka Teratas]
[ 1 ] [ 2 ] [ 3 ] [ 4 ] [ 5 ] [ 6 ] [ 7 ] [ 8 ] [ 9 ] [ 0 ]

                ▲
                │  Tekan Tombol [ Fn ]
                ▼

[Laptop Mode: Baris Function Keys F1 - F12]
[ F1 ] [ F2 ] [ F3 ] [ F4 ] [ F5 ] [ F6 ] [ F7 ] [ F8 ] [ F9 ] [ F10 ] [ F11 ] [ F12 ]
```

* **Mekanisme `Fn`:** Menekan tombol `Fn` mengubah baris angka atas menjadi `F1` sampai `F12` secara instan.
* **Keycode Resmi Android:** Pemetaan ke `KeyEvent.KEYCODE_F1` (131) s/d `KeyEvent.KEYCODE_F12` (142) untuk mendukung terminal Linux/Termux, SSH, Neovim, dan `htop`.
* **Shortcut Native `Ctrl`:** Mengoperasikan pintasan standar PC (`Ctrl+A`, `Ctrl+C`, `Ctrl+V`, `Ctrl+X`, `Ctrl+Z`) langsung dari keyboard fisik tanpa tombol toolbar ganda.

---

## 7. Spesifikasi Background Gambar & Ergonomi

* **Custom Background Anti-OOM:** Foto galeri otomatis di-*downscale* dan di-*crop* tepat ke dimensi keyboard (WebP) dengan slider lapisan transparan (dimming 0% - 80%) agar teks tombol selalu kontras.
* **Tekan Lama Simbol (Opsional):** Master Toggle On/Off agar tidak menghambat pengetikan cepat, disertai slider durasi (150 ms - 500 ms).
* **Kompatibilitas Sistem Dinamis:** Evaluasi reaktif `EditorInfo.imeOptions` (Enter/Search/Send/Go) dan pemutusan otomatis cache/autocorrect pada mode password.

---

## 8. Spesifikasi Dynamic Resize & Landscape Adaptation

* **Proportional Weight-Based:** Ukuran tombol dihitung dengan rasio `weight` Compose, bebas dari ukuran `dp` statis agar resize tidak merusak layout.
* **Landscape Adaptation:** 
  * `onEvaluateFullscreenMode = false` (anti-extract mode layar putih).
  * Batas tinggi landscape maksimal 40% - 45%.
  * Opsi Side Padding & Split Mode agar jempol tidak pegal.

---

## 9. Spesifikasi Modul Clipboard (Sistem Tumpukan / FIFO)

* **Sistem Tumpukan (FIFO ala FlorisBoard):** Batas 30–50 klip unpinned. Klip tertua otomatis terdepak saat kapasitas penuh.
* **Pinned Clips:** Kapasitas besar di seksi atas, kebal dari sistem tumpukan FIFO.
* **Anti-Toast Spam & Proteksi Password:** Membaca metadata tanpa polling kotor, dan otomatis menolak menyimpan data bertanda `EXTRA_IS_SENSITIVE` (hanya di memori volatil selama 60 detik).

---

## 10. Arsitektur Hybrid: Kotlin + Rust (`sboard-core`)

```
┌─────────────────────────────────────────────────────────────┐
│                    InputMethodService                       │
│      (Kotlin: Android Lifecycle & Inline Autofill API)      │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼─────────────┐   ┌─────────────▼──────────────┐
│        InputEngine         │   │      Compose Keyboard      │
│     (Kotlin: Text Input)   │   │  (Kotlin: Pointer Events)  │
└──────────────┬─────────────┘   └─────────────▲──────────────┘
               │                               │
               └───────────────┬───────────────┘
                               │ JNI / UniFFI Bridge
┌──────────────────────────────▼──────────────────────────────┐
│                      sboard-core (Rust)                     │
│  ├─ Spatial Hitbox Model (Euclidean Distance)               │
│  ├─ Bigram & Probability Engine (Anti-Typo)                 │
│  ├─ Layout & Key Mapping Data Structures                    │
│  └─ Modifier State Machine (Shift, Ctrl, Alt, Fn)           │
└─────────────────────────────────────────────────────────────┘
```

---

## 11. Roadmap Pengembangan

1. **Fase 1: Rust Core Setup (`sboard-core`)**
   * Struct tombol, layout data QWERTY, state machine (`Shift`, `Ctrl`, `Fn`).
   * Algoritma jarak titik sentuh (Euclidean distance).
2. **Fase 2: Android Studio & JNI Binding**
   * Jembatan Kotlin-Rust via NDK/UniFFI.
3. **Fase 3: Compose IME View, Multi-Touch & Gestur Cerdas**
   * Layout visual QWERTY proporsional + resize handle.
   * Gestur Spacebar (Cursor Glide & Hold IME Picker).
   * Tombol Backspace Anti-Nyangkut (Coroutine Cancellation Guard).
   * Aset 12 Vector Drawables murni (Lucide standard).
   * Mode Landscape anti-extract UI.
   * Evaluasi dinamis `ImeOptions` (Enter/Search/Send).
   * Sentuhan instan di `ACTION_DOWN`.
4. **Fase 4: Clipboard Module & Developer Power-Keys**
   * Sistem tumpukan FIFO clipboard + Pinned clips.
   * Tombol `Ctrl` + fungsi `F1`–`F12` via tombol `Fn`.
5. **Fase 5: Sistem Tema & Personalisasi**
   * Mode tema Gelap, Terang, dan Ikut Sistem (`isSystemInDarkTheme`).
   * Format paket `.sboard-theme` (ZIP) + Theme Creator UI di Compose.
   * Dukungan Deep Link (`sboard://`) dan Intent Filter web download.
   * Background gambar kustom + dimming slider.
6. **Fase 6: Polish & Kompatibilitas Lanjutan**
   * Toggle & delay slider simbol tekan lama.
   * Inline Autofill saran Gmail/password & Emoji picker (`androidx.emoji2`).
