# 🤝 AGENT HANDOFF: Analisis & Solusi Bug Sboard v1.0.5

Dokumen ini adalah panduan arsitektur dan investigasi teknis untuk **Agent Sesi Eksekusi** agar perbaikan bug presisi 100% dan tidak merusak cetak biru `SPEC.md`.

---

## 1. Crash Log Fatal: `BadTokenException`
```text
android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?
    at android.view.ViewRootImpl.setView(ViewRootImpl.java:1551)
    at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:158)
    at android.app.Dialog.show(Dialog.java:352)
    at androidx.compose.ui.window.AndroidDialog_androidKt$Dialog$1$1.invokeSuspend(AndroidDialog.android.kt:217)
```

### Penyebab (Root Cause):
* Pada `ClipboardScreen.kt`, terdapat pemanggilan `AlertDialog` saat kartu klip ditekan lama (long-press).
* **Aturan Mutlak Android IME:** `SboardIME` adalah turunan `InputMethodService` (**Service**), BUKAN `Activity`. Komponen Compose `AlertDialog` / `Dialog` di balik layar selalu mencoba membuat Window OS baru yang mewajibkan Token Activity (`token null is not valid`). Akibatnya, aplikasi crash seketika!
* **Solusi Wajib:**
  Dilarang keras memakai `AlertDialog` atau `Dialog` di dalam IME! Ganti dengan **Inline Modal Overlay** (menggunakan `Box` lokal dengan latar semi-transparan yang membungkus kartu aksi Pin & Hapus langsung di dalam hierarki ComposeView yang sama).

---

## 2. Bug Tombol Hapus (Backspace Tidak Berfungsi)

### Penyebab (Root Cause):
1. **Konflik Touch Event di Compose:**
   Di `SboardKeyboardView.kt`, tombol Backspace dibungkus dengan `Box.pointerInput { detectTapGestures(onPress = { ... }) }`, tetapi child di dalamnya ([`KeyView`](file:///home/coder/projects/sahalOS/sboard/app/src/main/java/com/sahalos/sboard/ui/keyboard/KeyView.kt)) memiliki modifier `.clickable(onClick = { /* handled by pointerInput */ })`. Di Compose, `.clickable` pada child **mengonsumsi event sentuhan lebih dulu**, sehingga event `onPress` pada pembungkus luar tidak pernah terpicu!
2. **Keterbatasan `deleteSurroundingText`:**
   Pada beberapa aplikasi/kolom input (seperti WebView, terminal, atau kolom pencarian), `ic.deleteSurroundingText(1, 0)` bisa mengembalikan nilai false / tidak bereaksi jika tidak didampingi pengiriman KeyEvent resmi.

### Solusi Wajib:
1. Pastikan `KeyView` untuk Backspace **tidak memiliki `.clickable` ganda**. Cukup gunakan satu penanganan sentuhan.
2. Di `performBackspace()`, tambahkan fallback resmi Android IME:
   ```kotlin
   val ic = currentInputConnection ?: return
   val selectedText = ic.getSelectedText(0)
   if (!selectedText.isNullOrEmpty()) {
       ic.commitText("", 1)
   } else {
       val deleted = ic.deleteSurroundingText(1, 0)
       if (!deleted) {
           sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
       }
   }
   ```

---

## 3. Halaman Clipboard Memanjang 1 Layar Penuh & Teks Panjang Berantakan

### Masalah:
* Saat beralih ke `KeyboardMode.CLIPBOARD`, tampilan keyboard memanjang menutupi seluruh layar HP dan kartu klip menampilkan teks panjang tanpa batasan yang rapi.

### Penyebab & Solusi Wajib:
1. **Ketinggian Tetap Sama dengan Keyboard:**
   `ClipboardScreen` TIDAK BOLEH memakai `Modifier.fillMaxSize()` tanpa batasan tinggi. Ketinggiannya **wajib di-lock sama persis dengan tinggi keyboard saat aktif**:
   ```kotlin
   val keyboardHeightDp = ((theme.keyHeightDp * 4) + 40 + bottomPaddingDp) * keyboardHeightRatio
   Modifier.fillMaxWidth().height(keyboardHeightDp.dp)
   ```
2. **Tampilan 2 Kolom (Grid) ala Gboard:**
   Ganti list 1 kolom vertikal menjadi `LazyVerticalGrid(columns = GridCells.Fixed(2))` dengan padding kompak.
3. **Truncate Teks Panjang:**
   Setiap kartu klip wajib membatasi teks:
   ```kotlin
   maxLines = 2,
   overflow = TextOverflow.Ellipsis
   ```
4. **Menu Aksi Pin / Hapus Mandiri:**
   Saat kartu di-long-press, tampilkan dialog aksi **inline overlay** di dalam area keyboard (bukan `AlertDialog`), dengan tombol:
   * 📌 Sematkan / Lepas Sematan
   * 🗑️ Hapus Klip Ini

---

## 4. Fitur Resize Melayang / Lari ke Atas Layar

### Penyebab & Solusi Wajib:
1. `ResizeOverlayView` menggunakan `Modifier.fillMaxSize()`. Pada jendela IME Android, `fillMaxSize()` memaksa container view meregang keluar batas dagu layar hingga ke bagian atas layar (*floating over app*).
2. **Solusi:**
   Tinggi `ResizeOverlayView` dan keyboard pembungkusnya harus tetap diikat pada dasar keyboard (`Modifier.fillMaxWidth().height(calculatedHeight)`).
   Menggeser handle atas hanya mengubah `previewHeightRatio` (antara `0.80f` s/d `1.25f`), dan handle bawah mengubah `previewBottomPaddingDp` (`0` s/d `36.dp`).

---

## 5. Ringkasan Checklist Rilis v1.0.6:
- [x] Hapus import `androidx.compose.material3.AlertDialog` dari `ClipboardScreen.kt`. Ganti dengan inline modal overlay di hierarki Compose yang sama untuk mencegah `BadTokenException`.
- [x] Perbaiki Backspace: `KeyView` dibuat mendukung `onClick = null` agar gesture `detectTapGestures(onPress = ...)` pada wrapper luar tidak dikonsumsi, serta menyertakan fallback resmi `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`.
- [x] Kunci tinggi `ClipboardScreen` dan `ResizeOverlayView` agar sama persis dengan tinggi keyboard (`totalKeyboardHeightDp`) tanpa `Modifier.fillMaxSize()`.
- [x] Ubah kartu clipboard menjadi 2 kolom grid (`LazyVerticalGrid(GridCells.Fixed(2))`) dengan teks terpotong rapi (`maxLines = 2`, `TextOverflow.Ellipsis`).
- [x] Tambahkan menu aksi mandiri (Pin/Lepas Sematan & Hapus Klip) per kartu via inline overlay.
- [x] Jalankan verifikasi `verify_kotlin_syntax.py` dengan 0 kesalahan.
- [x] Build sukses di GitHub Actions CI (Run ID: 41972465852) dan APK v1.0.6 terbit.
