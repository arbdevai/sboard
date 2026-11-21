# 🖥️ Sboard PC Keyboard Architecture & Implementation Roadmap

Dokumen ini adalah cetak biru teknis (technical blueprint) untuk mentransformasikan **Sboard (Sahal Board)** menjadi papan ketik virtual berstandar **Real PC Keyboard** seutuhnya, dirancang khusus untuk alur kerja profesional Android (Termux, DevBox, Web IDE, Text Editor, dan Android Native EditText).

---

## 1. Analisis Arsitektur Sboard Saat Ini (Baseline v1.0.24)

### A. Komponen Eksisting
1. **Input Lifecycle (`SboardIME.kt`)**:
   - Berbasis Android `InputMethodService`.
   - Mengelola `modifierState` (`isShifted`, `isCapsLock`, `isCtrlActive`, `isAltActive`, `isFnActive`, `isSymActive`, `isSymPage2Active`).
   - Deteksi konteks terminal (`isTerminalField`) untuk Termux, DevBox, dan emulator terminal.
   - Penanganan gesture: Spacebar cursor glide (`KEYCODE_DPAD_LEFT`/`RIGHT`) dan Backspace repeat.
2. **Layout Provider (`KeyLayoutProvider.kt`)**:
   - Menyediakan layer QWERTY (4 baris), Simbol Halaman 1 & 2, Number Pad (4x4), dan Number Row opsional (1-0).
   - Tombol Shift saat ini berada di baris ke-3 QWERTY, namun logikanya masih sebatas pemilih huruf kapital (lowercase/uppercase) dan Caps Lock.
3. **Deck Terminal & Navigasi (`FnRowView.kt`)**:
   - 2 baris keyboard developer: Baris navigasi (`ESC, TAB, ALT, panah ←↓↑→, HOME, END, PGUP, PGDN, DEL`) dan Baris F-Keys (`F1-F12`).
   - Translasi ANSI escape sequence VT100 / xterm untuk terminal mode.
4. **Bilah Aksi Cepat (`SuggestionsBar.kt`)**:
   - Sisi kiri: Tombol `[CTRL]` dan `[FN]`.
   - Sisi tengah: Saran kata adaptif (Trie O(L) + Bigram Rust).
   - Sisi kanan: `[Clipboard]`, `[Resize]`, `[Settings]`.

### B. Keterbatasan Saat Ini Terhadap Kebutuhan Real PC Keyboard
| Aspek | Kondisi Saat Ini | Target Real PC Keyboard |
| :--- | :--- | :--- |
| **Kombinasi CTRL** | Terbatas pada `A, C, V, X, Z, Y` (via Context Menu) dan ASCII byte di terminal. Shortcut lain (`Ctrl+Panah`, `Ctrl+Del`, `Ctrl+Backspace`, `Ctrl+F`, `Ctrl+S`, `Ctrl+W`, dll.) belum optimal. | Dukungan penuh seluruh alfabet (`Ctrl+A..Z`), kontrol navigasi kata (`Ctrl+←/→`), hapus kata (`Ctrl+Backspace/Delete`), lompat dokumen (`Ctrl+Home/End`), tab & window (`Ctrl+T/W/N`), dan multi-chording (`Ctrl+Shift+Z/←/→`). |
| **Tombol SHIFT** | Hanya bekerja pada karakter alfabet `a-z` -> `A-Z`. | Bekerja universal sebagai modifier hardware PC: Shift+Angka menghasilkan simbol PC (`!@#$%^&*()`), Shift+Panah melakukan seleksi teks, Shift+Tab back-tab, dan chording bersama Ctrl/Alt. |
| **Seleksi Teks (Selection)** | Tidak ada seleksi teks interaktif dari keyboard. Pengguna harus mengandalkan handle kursor sentuh bawaan Android OS yang sulit dan lambat. | Seleksi teks presisi via keyboard: Shift+Panah (per karakter/baris), Ctrl+Shift+Panah (per kata), Shift+Home/End (per baris), dan **Selection Glide pada Spacebar** (geser seleksi langsung dengan jempol). |
| **Modifier State Machine** | Toggle biner biasa (on/off). | **Tri-State Latch/Lock Engine**: `Off` -> `Latched` (aktif 1x untuk ketukan berikutnya lalu auto-release) -> `Locked` (double tap untuk mengunci permanen). Mendukung kombinasi simultan (`Ctrl + Shift`). |

---

## 2. Spesifikasi Detail Fitur Real PC Keyboard

### A. Mesin Kombinasi CTRL Lengkap (PC Standard)

Sistem akan membedakan eksekusi shortcut berdasarkan dua lingkungan: **Terminal Context** (Termux, DevBox, SSH) dan **Android Standard/WebView Context** (Chrome, Docs, VSCode Web, Obsidian, Chat).

#### 1. Navigasi & Manipulasi Kata (Word-Level Editing)
- **`Ctrl + ←` (DPAD Left)**:
  - *Android Standard:* `ic.sendKeyEvent` dengan `KEYCODE_DPAD_LEFT` + `META_CTRL_ON`. Di seluruh `EditText` dan browser modern, ini otomatis melompati 1 kata ke kiri.
  - *Terminal:* Mengirimkan sequence ANSI `\u001B[1;5D` (xterm standard).
- **`Ctrl + →` (DPAD Right)**:
  - *Android Standard:* `ic.sendKeyEvent` dengan `KEYCODE_DPAD_RIGHT` + `META_CTRL_ON` (lompat 1 kata ke kanan).
  - *Terminal:* Mengirimkan sequence ANSI `\u001B[1;5C`.
- **`Ctrl + Backspace`**:
  - *Android Standard:* Hapus kata sebelumnya secara utuh. Jika input connection tidak merespons `KEYCODE_DEL` + `META_CTRL_ON`, Sboard membaca batas kata via `ic.getTextBeforeCursor(64, 0)` dan menghapus dengan `ic.deleteSurroundingText(wordLength, 0)`.
  - *Terminal:* Mengirimkan kode `\u0017` (ASCII `0x17` / Ctrl+W - Unix word erase).
- **`Ctrl + Delete` (Forward Delete Word)**:
  - *Android Standard:* Hapus kata di depan kursor via `KEYCODE_FORWARD_DEL` + `META_CTRL_ON` atau `deleteSurroundingText(0, wordLength)`.
  - *Terminal:* Mengirimkan `\u001B[3;5~` (Delete with Ctrl modifier).
- **`Ctrl + Home` / `Ctrl + End`**:
  - *Android Standard:* Melompat langsung ke awal / akhir seluruh dokumen (`KEYCODE_MOVE_HOME` / `MOVE_END` + `META_CTRL_ON`).
  - *Terminal:* Mengirimkan `\u001B[1;5H` / `\u001B[1;5F`.

#### 2. Matriks Pintasan Alfabet PC Lengkap (`Ctrl + [A-Z]`)
| Shortcut | Mode Android / Web / Dokumen | Mode Terminal (DevBox / Termux) |
| :--- | :--- | :--- |
| **Ctrl + A** | Select All (`performContextMenuAction(selectAll)`) | `\u0001` (Awal baris / Readline HOME) |
| **Ctrl + B** | Bold Text / Bookmark | `\u0002` (Mundur 1 karakter) |
| **Ctrl + C** | Copy (`performContextMenuAction(copy)`) | `\u0003` (`SIGINT` interrupt proses) |
| **Ctrl + D** | Bookmark / Hapus karakter depan | `\u0004` (`EOF` / Keluar shell) |
| **Ctrl + E** | Align Center / Cari URL bar | `\u0005` (Akhir baris / Readline END) |
| **Ctrl + F** | Find in Page / Dokumen | `\u0006` (Maju 1 karakter) |
| **Ctrl + G** | Find Next | `\u0007` (`BEL` / Bell audio) |
| **Ctrl + H** | History browser / Replace | `\u0008` (Backspace karakter) |
| **Ctrl + I** | Italic Text | `\u0009` (`TAB` / Auto-completion) |
| **Ctrl + J** | Downloads list | `\u000A` (`LF` / Line feed) |
| **Ctrl + K** | Insert Hyperlink / Clear to end | `\u000B` (Kill line forward) |
| **Ctrl + L** | Focus Address Bar | `\u000C` (Clear screen terminal) |
| **Ctrl + M** | Indent baru | `\u000D` (`CR` / Carriage return) |
| **Ctrl + N** | New Window / Document | `\u000E` (Next command history) |
| **Ctrl + O** | Open File | `\u000F` (Flush output) |
| **Ctrl + P** | Print / Quick File Search (VSCode) | `\u0010` (Previous command history) |
| **Ctrl + Q** | Quick Quit | `\u0011` (`XON` resume flow) |
| **Ctrl + R** | Reload / Refresh halaman | `\u0012` (Reverse search history `fzf`) |
| **Ctrl + S** | Save dokumen | `\u0013` (`XOFF` pause flow) |
| **Ctrl + T** | New Tab (Browser / IDE) | `\u0014` (Transpose character) |
| **Ctrl + U** | Underline / View Source | `\u0015` (Kill line backward) |
| **Ctrl + V** | Paste (`performContextMenuAction(paste)`) | `\u0016` (Verbatim input escape) |
| **Ctrl + W** | Close Tab / Document | `\u0017` (Kill word backward) |
| **Ctrl + X** | Cut (`performContextMenuAction(cut)`) | `\u0018` (`CAN` Cancel / Nano exit) |
| **Ctrl + Y** | Redo Action | `\u0019` (`EM` End of Medium / Nano paste) |
| **Ctrl + Z** | Undo Action | `\u001A` (`SIGTSTP` Background job) |

---

### B. Arsitektur Tombol SHIFT Mandiri (PC-Grade Hardware Shift)

Pada keyboard PC standar, tombol Shift adalah modifier umum yang mengubah perilaku hampir seluruh tombol di papan ketik.

#### 1. Perilaku Shift pada Baris Angka (Number Row PC Symbols)
Ketika Shift aktif (Latched atau Locked), Baris Angka (`1-0`) otomatis bertransformasi menampilkan simbol PC standar:
- `1` $\rightarrow$ `!`
- `2` $\rightarrow$ `@`
- `3` $\rightarrow$ `#`
- `4` $\rightarrow$ `$`
- `5` $\rightarrow$ `%`
- `6` $\rightarrow$ `^`
- `7` $\rightarrow$ `&`
- `8` $\rightarrow$ `*`
- `9` $\rightarrow$ `(`
- `0` $\rightarrow$ `)`
- `-` $\rightarrow$ `_`
- `=` $\rightarrow$ `+`

#### 2. Perilaku Shift pada Tanda Baca & Simbol
- `,` $\rightarrow$ `<`
- `.` $\rightarrow$ `>`
- `/` $\rightarrow$ `?`
- `;` $\rightarrow$ `:`
- `'` $\rightarrow$ `"`
- `[` $\rightarrow$ `{`
- `]` $\rightarrow$ `}`
- `\` $\rightarrow$ `|`
- `` ` `` $\rightarrow$ `~`

#### 3. State Machine Tri-State Shift (Latched, Locked, Released)
- **Kondisi 1: Released (Off)** $\rightarrow$ Pengetikan huruf kecil standar.
- **Kondisi 2: Latched (Shift 1x)** $\rightarrow$ Mengetuk Shift 1 kali mengaktifkan mode kapital/simbol untuk **1 tombol berikutnya**. Setelah 1 tombol diketik, Shift otomatis kembali ke status Off (Released).
- **Kondisi 3: Locked (Caps Lock)** $\rightarrow$ Mengetuk Shift dua kali cepat (*double tap*) atau menahan Shift 300ms mengunci Shift secara permanen (*Caps Lock*). Seluruh huruf menjadi kapital sampai tombol Shift ditekan kembali.
- **Visual Feedback:**
  - *Off:* Ikon panah outline biasa (`textColor.copy(alpha = 0.8f)`).
  - *Latched:* Ikon terisi solid warna aksen (`accentColor`).
  - *Locked:* Ikon solid dengan garis bawah horizontal tebal khas Caps Lock PC.

---

### C. Fitur Seleksi Teks Presisi (Real PC Text Selection Engine)

Di layar sentuh smartphone, menyeleksi teks dengan jari adalah salah satu pengalaman paling menyiksa pengguna. Dengan mengubah Sboard menjadi Real PC Keyboard, pengguna dapat menyeleksi teks dengan presisi tingkat piksel menggunakan keyboard.

#### 1. Seleksi Teks via Keyboard Panah (Shift + Navigation)
Saat `Shift` aktif:
- **`Shift + ←` / `Shift + →`**:
  - Mengirim `sendKeyEvent` dengan `KEYCODE_DPAD_LEFT` / `RIGHT` disertai `META_SHIFT_ON`.
  - Android EditText secara otomatis membuat seleksi karakter demi karakter ke arah kiri atau kanan.
  - Terminal mengirimkan `\u001B[1;2D` (Shift+Left) dan `\u001B[1;2C` (Shift+Right).
- **`Shift + ↑` / `Shift + ↓`**:
  - Mengirim `KEYCODE_DPAD_UP` / `DOWN` dengan `META_SHIFT_ON` untuk menyeleksi baris teks ke atas atau ke bawah.
- **`Ctrl + Shift + ←` / `Ctrl + Shift + →` (Word Selection)**:
  - Mengirim `KEYCODE_DPAD_LEFT` / `RIGHT` dengan modifier gabungan `META_CTRL_ON or META_SHIFT_ON`.
  - Menyeleksi blok teks kata demi kata secara instan.
- **`Shift + Home` / `Shift + End` (Line Selection)**:
  - Menyeleksi dari posisi kursor saat ini hingga ke ujung paling awal atau paling akhir baris.
- **`Ctrl + Shift + Home` / `Ctrl + Shift + End`**:
  - Menyeleksi dari posisi kursor saat ini hingga ke awal/akhir dokumen teks.

#### 2. Fitur "Selection Glide" pada Spacebar (Gestur Geser Seleksi)
- **Mekanisme Kerja:**
  1. Jika pengguna menggeser (*drag*) Spacebar dalam kondisi normal $\rightarrow$ kursor bergeser (fungsi spacebar cursor glide eksisting).
  2. Jika pengguna mengaktifkan **Shift** lalu menggeser Spacebar $\rightarrow$ Sboard masuk ke mode **Selection Glide**!
  3. Setiap akumulasi pergeseran jari melebihi `glideThresholdPx` (12dp), sistem memicu seleksi teks:
     - Geser ke kiri: Memperluas seleksi ke kiri (`DPAD_LEFT` + `META_SHIFT_ON`).
     - Geser ke kanan: Memperluas seleksi ke kanan (`DPAD_RIGHT` + `META_SHIFT_ON`).
  4. Pengguna dapat memilih 1 huruf, 3 kata, atau 1 kalimat utuh hanya dengan menyeret jempol di atas tombol spasi tanpa pernah menyentuh layar atas!

#### 3. Selection Action HUD (Toolbar Kontekstual Saat Teks Terseleksi)
Ketika teks terdeteksi aktif terseleksi (via `ic.getSelectedText(0).isNotEmpty()` atau mode selection aktif), bilah saran kata di tengah otomatis bertransformasi menjadi **Selection Action HUD**:
```
[CTRL] [FN] | [✂️ CUT] [📋 COPY] [📥 PASTE] [🔍 ALL] | [RESIZE] [SETTINGS]
```
- Mengetuk tombol Cut/Copy langsung mengeksekusi aksi clipboard dan membersihkan status seleksi.

---

### D. Multi-Modifier Chording Engine (`Ctrl`, `Alt`, `Shift`, `Fn`)

Arsitektur modifier baru harus mendukung kombinasi beberapa modifier aktif sekaligus (chording):
```kotlin
data class ModifierState(
    val shiftMode: ShiftMode = ShiftMode.OFF, // OFF, LATCHED, LOCKED
    val ctrlMode: ModifierMode = ModifierMode.OFF, // OFF, LATCHED, LOCKED
    val altMode: ModifierMode = ModifierMode.OFF, // OFF, LATCHED, LOCKED
    val isFnActive: Boolean = false,
    val isSymActive: Boolean = false,
    val isSymPage2Active: Boolean = false
)

enum class ShiftMode { OFF, LATCHED, LOCKED }
enum class ModifierMode { OFF, LATCHED, LOCKED }
```

#### Aturan Resolusi Meta Mask:
```kotlin
fun buildMetaState(state: ModifierState): Int {
    var meta = 0
    if (state.shiftMode != ShiftMode.OFF) {
        meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    }
    if (state.ctrlMode != ModifierMode.OFF) {
        meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    }
    if (state.altMode != ModifierMode.OFF) {
        meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    }
    if (state.shiftMode == ShiftMode.LOCKED) {
        meta = meta or KeyEvent.META_CAPS_LOCK_ON
    }
    return meta
}
```
Setiap kali tombol karakter atau navigasi dieksekusi dengan modifier latched:
- Modifier yang berstatus `LATCHED` langsung otomatis di-reset ke `OFF`.
- Modifier yang berstatus `LOCKED` tetap aktif.

---

## 3. Desain Tata Letak (Layout Evolution)

### A. Opsi Layout QWERTY dengan Dedicated PC Shift
1. **Sisi Kiri Baris 3:**
   - Tombol `[SHIFT]` mandiri dengan lebar `1.3f`.
   - Menampilkan indikator status dinamis: `⇧` (Off), `⬆` (Latched), `⇪` (Caps Lock).
2. **Dedicated Arrow Deck di Fn Row (`FnRowView.kt`):**
   - Tetap mempertahankan baris navigasi terminal: `ESC`, `TAB`, `ALT`, `←`, `↓`, `↑`, `→`, `HOME`, `END`, `PGUP`, `PGDN`, `DEL`.
   - Mengintegrasikan feedback visual saat `Shift` atau `Ctrl` aktif sehingga tombol panah di Fn Row langsung menyala dengan label indikator seleksi (`SEL ←`, `SEL →`).
3. **Baris Angka dengan Shift Preview:**
   - Ketika `enableNumberRow = true` dan `Shift` aktif, label primer baris angka langsung berubah menampilkan `! @ # $ % ^ & * ( )`.

---

## 4. Rencana Implementasi Bertahap (Implementation Roadmap)

Sesuai dengan protokol **Android Compose Builder Skill**, seluruh perubahan kode berikutnya wajib memenuhi kriteria:
- Zero local build di chroot mobile.
- Verifikasi sintaks via `python3 scripts/verify_kotlin_syntax.py .` (0 hard errors).
- Implementasi vertikal utuh hulur ke hilir (Zero TODOs, zero dummy data).
- Otomasi kompilasi dan rilis via GitHub Actions CI.

### Fase 1: Modifier State Machine & Tri-State Latching
- Perbarui `ModifierState.kt` dengan enum `ShiftMode` dan `ModifierMode` (`OFF`, `LATCHED`, `LOCKED`).
- Buat logic double-tap detection pada tombol Shift dan Ctrl untuk beralih antara Latched dan Locked.
- Integrasikan `buildMetaState()` di `SboardIME.kt`.

### Fase 2: Ekstensi Kombinasi CTRL Universal (Desktop & Terminal)
- Implementasikan routing lengkap `Ctrl+A..Z`, `Ctrl+←/→`, `Ctrl+Backspace`, `Ctrl+Del`, `Ctrl+Home/End`.
- Tambahkan fallback cerdas: coba `performContextMenuAction`, lalu `sendKeyEvent` dengan `META_CTRL_ON`, dan fallback manipulasi `InputConnection` text boundary.
- Perkaya tabel escape sequence ANSI VT100 untuk mode terminal.

### Fase 3: Hardware-Grade Shift & Simbol Angka
- Sinkronisasikan `KeyLayoutProvider.kt` agar baris angka dan baris simbol mengenali status `ShiftMode`.
- Saat Shift aktif, tombol alfabet merender huruf besar dan baris angka merender simbol PC.
- Pastikan auto-unlatch bekerja presisi setelah 1 karakter diketik jika statusnya `LATCHED`.

### Fase 4: Precision Text Selection Engine & Selection Glide
- Implementasikan logic `Shift + Panah` dengan `META_SHIFT_ON` di `SboardIME.kt`.
- Implementasikan **Selection Glide** pada gesture Spacebar di `SboardKeyboardView.kt`:
  - Jika `shiftMode != OFF`, ubah delta geser menjadi event seleksi teks (`KEYCODE_DPAD_LEFT/RIGHT` + `META_SHIFT_ON`).
- Integrasikan Contextual Selection HUD pada `SuggestionsBar.kt` saat ada teks yang sedang diblok.

### Fase 5: Pengujian Kestabilan, Linting, & Autopilot Release CI
- Uji kompatibilitas di Google Chrome, DevBox, Termux, WhatsApp, dan VSCode Web.
- Verifikasi nol kebocoran memori pada coroutine gesture.
- Validasi sintaksis `verify_kotlin_syntax.py`.
- Bump versi ke `v1.1.0` (Major Feature Release: Real PC Keyboard Edition).

---

> Dokumen ini siap dijadikan acuan tunggal saat eksekusi kode dimulai pada sesi pengerjaan berikutnya.
