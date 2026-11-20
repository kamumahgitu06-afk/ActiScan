# ActiScan — Exported Activity Auditor for Android Developers

**Developer:** Yaudah iya

**ActiScan** adalah aplikasi developer tool native Android (Kotlin & Jetpack Compose) untuk mengaudit komponen `Activity` yang berstatus `exported="true"` pada perangkat milik pengguna/developer sendiri secara lokal.

## Tujuan & Ruang Lingkup

- Ditujukan untuk **Android developers dan peneliti keamanan** guna memverifikasi konfigurasi `android:exported` dan perlindungan permission pada aplikasi mereka sendiri.
- **Tidak mengumpulkan atau mengirim data apa pun ke server.** Seluruh proses pemindaian berjalan 100% lokal di perangkat.
- **Tidak mengakses data privat, database, atau file internal milik aplikasi lain.** Aplikasi hanya membaca metadata deklarasi manifest yang bersifat publik (`PackageManager.GET_ACTIVITIES` untuk `activityInfo.exported == true`).
- **Mematuhi penuh pembatasan izin Android, lockscreen, dan tanpa eksekusi root.** Fitur uji peluncuran (`TEST`) hanya memanggil API publik resmi Android (`startActivity` dengan `Intent` + `ComponentName` + `FLAG_ACTIVITY_NEW_TASK`) dan mencatat apabila sistem menolak akses (`SecurityException`).

## Fitur Utama

1. **Pemindaian Lokal via `PackageManager`**: Memindai aplikasi terinstall menggunakan `PackageManager.getInstalledPackages(GET_ACTIVITIES)` dan menampilkan Activity dengan `exported == true`.
2. **Metadata Komponen Lengkap**: Menampilkan nama aplikasi, package name, activity name, status permission (atau `TANPA IZIN` bila tidak ada deklarasi permission), tipe aplikasi (`USER` / `SYSTEM`), dan status aktif (`ENABLED` / `DISABLED`).
3. **Pengujian Manual Satu-Satu**: Tidak ada auto-launch massal. Pengembang menguji masing-masing Activity secara manual dengan hasil netral:
   - `LAUNCHED` — Berhasil Diluncurkan
   - `DENIED` — Ditolak Sistem (`SecurityException`)
   - `REQUIRES_PARAMETERS` — Membutuhkan Parameter Tambahan
   - `NOT_TESTED` — Belum Diuji
4. **Deteksi Status Root (Read-Only)**: Indikator pasif status perangkat (`Rooted` / `Non-Root`) melalui pengecekan file biner `su` dan `PackageManager` (tanpa menandai UBL/`test-keys` sebagai root, serta tanpa menjalankan perintah `su` atau meminta akses root). Dapat disembunyikan melalui Pengaturan.
5. **Export Laporan `.txt`**: Menyimpan laporan audit lokal ke file `.txt` menggunakan Storage Access Framework (`ACTION_CREATE_DOCUMENT`) tanpa memerlukan izin penyimpanan luas.

## Penggunaan Izin `QUERY_ALL_PACKAGES`

Izin `QUERY_ALL_PACKAGES` digunakan secara ketat untuk fungsi inti aplikasi developer audit ini agar dapat membaca daftar paket terinstall dan memeriksa komponen `exported` Activity pada Android 11 (API 30) ke atas secara lokal. Tidak ada data daftar aplikasi yang disimpan secara eksternal atau dikirim ke luar perangkat.
