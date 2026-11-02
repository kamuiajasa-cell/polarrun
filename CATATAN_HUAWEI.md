# Modul :huawei di PolarRun (langkah 1)

Sumber: JoyoFit (AGPL-3.0-or-later, turunan Gadgetbridge). `LICENSE` dan `NOTICE` ada di `huawei/`.
Karena itu PolarRun yang memuat modul ini juga wajib AGPL (sumber terbuka untuk penerima APK).

## Yang diubah dari JoyoFit
- Dibuang: semua `*ModuleApi.kt`, `HuaweiWearModule.kt` (jembatan Expo/JS), `ExpoSqliteConnection.java`,
  `HealthConnectPermissionContract.kt`, `AlarmRecords.kt`, `BleProbeReceiver.kt`, manifest debug/play, folder iOS dan tes.
- Baru: `SqlConnection.kt` (`AndroidSqlConnection`) menggantikan `ExpoSqlConnection.kt`; database memakai SQLite bawaan Android.
  File DB: `filesDir/SQLite/joyofit.db`; migrasi dijalankan sisi native dari `assets/joyofit-db-migrations.json`.
- `WearTypes.kt`: `WearException` kini `RuntimeException` biasa.
- `minSdk` aplikasi 26 -> 29. Izin SMS, log panggilan, dan kontak dibuang lewat `tools:node="remove"` di manifest aplikasi.

## STATUS: BELUM DIKOMPILASI
Lingkungan tempat ini dibuat tidak punya Android SDK/Gradle, jadi build belum pernah dijalankan.
Jalankan GitHub Actions "Build APK"; kalau ada error kompilasi, kirim log-nya, biasanya tinggal beberapa baris.
Titik yang paling mungkin bermasalah: `SqlConnection.kt` (CursorFactory dan SQLiteCursor), dan impor yang tertinggal.

## Langkah 2 (sudah ditulis, belum dikompilasi)
- `huawei/.../HuaweiWatch.kt`: satu-satunya kelas `public` modul (scan, bonded, connect, sync, disconnect, state/log sebagai StateFlow).
- `app/.../ui/WatchScreen.kt` dan tab "Jam" di `MainActivity.kt`: izin Bluetooth, daftar jam, scan, konfirmasi ikatan pertama,
  tombol Sinkron/Putuskan, dan log yang bisa disalin.
- Sambungan pertama ke jam yang belum diikat menampilkan peringatan: ikatan baru menggantikan pasangan HP lama jam.

## Belum ada
- Pembaca database jam -> `Wellness`/`WeekStat` (langkah 3).
- Mode latar belakang (service sinkron otomatis).
