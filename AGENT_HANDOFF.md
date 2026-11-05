# 🤝 AGENT HANDOFF: Perbaikan Error ViewTreeLifecycleOwner pada SboardIME

Dokumen ini ditulis untuk menginformasikan agent sesi sebelah mengenai akar masalah dan patch perbaikan yang telah diterapkan pada `SboardIME.kt`.

---

## 1. Akar Masalah (Root Cause)

Error yang terjadi pada crash log:
```text
java.lang.IllegalStateException: ViewTreeLifecycleOwner not found from android.widget.LinearLayout{... #1020437 android:id/parentPanel}
    at android.inputmethodservice.InputMethodService.setInputView(InputMethodService.java:2517)
```

### Mengapa Ini Terjadi?
1. Pada `InputMethodService` Android, jendela IME bukan `Activity`, melainkan dialog sistem khusus (`mWindow`).
2. Framework Android di `InputMethodService.java` meng-inflate container internal `mRootView` dan menetapkan ID internal:
   `mRootView.setId(com.android.internal.R.id.parentPanel);`
3. Saat `onCreateInputView()` mengembalikan `ComposeView`, method `setInputView()` menempelkan `ComposeView` ke dalam `mInputFrame` yang berada di dalam `parentPanel`.
4. `ComposeView` yang terpasang memanggil `onAttachedToWindow()`, yang kemudian mencari `WindowRecomposer`. Karena Recomposer default bersifat `LifecycleAware`, ia merayap ke view teratas (`rootView` = `parentPanel`) untuk mengambil `ViewTreeLifecycleOwner.get(parentPanel)`.
5. Karena sebelumnya `setViewTreeLifecycleOwner` hanya dipasang pada `decorView` sebelum `parentPanel` selesai di-inflate/dikaitkan, `parentPanel` tidak memiliki tag `LifecycleOwner`, sehingga melempar crash fatal `IllegalStateException`.

---

## 2. Patch yang Sudah Diterapkan (Commit `f634309`)

Perubahan pada file `app/src/main/java/com/sahalos/sboard/service/SboardIME.kt`:

1. **Robust Multi-Level `installViewTreeOwners()`:**
   - Memasang `LifecycleOwner`, `ViewModelStoreOwner`, dan `SavedStateRegistryOwner` pada:
     - `win.decorView`
     - `content` (`android.R.id.content`)
     - `parentPanel` secara spesifik (`resources.getIdentifier("parentPanel", "id", "android")`)
     - Seluruh child view yang ada di dalam `decorView`.
2. **Kunci ViewCompositionStrategy pada `ComposeView`:**
   ```kotlin
   composeView.setViewCompositionStrategy(
       ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@SboardIME)
   )
   ```
   Ini memastikan ComposeView menggunakan lifecycle resmi `SboardIME` dan tidak bergantung pada pelepasan window liar.
3. **Siklus Lifecycle yang Lengkap:**
   - Menambahkan event `ON_START` pada `onCreate()`.
   - Memanggil `installViewTreeOwners()` kembali pada `onConfigureWindow()` dan `onWindowShown()`.
   - Mengirim event `ON_RESUME` pada `onWindowShown()` dan `ON_PAUSE` pada `onWindowHidden()`.
4. **Verifikasi Linter:**
   - Telah diverifikasi dengan `verify_kotlin_syntax.py` dan lolos 100% tanpa warning/error (Exit code 0).

---

## 3. Langkah untuk Agent di Sesi Sebelah
Tidak perlu menulis ulang kode lifecycle ini dari awal. Cukup jalankan:
```bash
git log -n 1
```
Pastikan commit `f634309` sudah ada, lalu lanjutkan proses build APK atau pengujian keyboard di HP!
