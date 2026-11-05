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

## Ringkasan untuk Agent Eksekutor:
1. Update `KeyLayoutProvider.kt`: Berikan spacer `0.5f` di kiri dan kanan baris 2 (`ASDFGHJKL`).
2. Update `SboardIME.kt`: Ubah `moveCursor` menjadi pengiriman `sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT / RIGHT)`.
3. Update `SboardKeyboardView.kt`: Perbaiki gesture seret spasi agar tidak ditelan child `KeyView`.
4. Update `ResizeOverlayView.kt`: Stabilkan gesture drag dan tambahkan tombol preset ukuran (85%, 100%, 115%, 125%).
