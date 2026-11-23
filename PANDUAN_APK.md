# Panduan Menjalankan & Membangun APK — ABSENSI PJOK (UPTD SDN LOKOJULI)

## Identitas Proyek Android
- **Nama Aplikasi**: Absensi PJOK
- **Package ID / Application ID**: `id.sch.lokojuli.absensipjok`
- **Sekolah**: UPTD SDN LOKOJULI
- **Guru PJOK**: Andi Mozes Leavingstone Bire, S.Pd.
- **Orientasi**: Portrait
- **Database Offline**: SQLite lokal via Android Room (`absensi_pjok_lokojuli.db`)
- **Teknologi**: Native Android (Kotlin, Jetpack Compose, Material 3, Room SQLite)

---

## Cara Membangun APK Otomatis di GitHub Actions (Tanpa Android Studio)

Proyek ini sudah dilengkapi Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) dan workflow GitHub Actions di `.github/workflows/build-apk.yml`.

1. **Sinkronkan ke GitHub**:
   - Push seluruh isi proyek ini ke repository GitHub Anda (branch `main` atau `master`), atau gunakan fitur Push to GitHub di menu AI Studio.
2. **Jalankan Workflow Build APK**:
   - Setiap `push` ke branch `main`/`master` otomatis menjalankan workflow **Build Android APK**.
   - Atau buka tab **Actions** di GitHub > pilih **Build Android APK** > klik **Run workflow** (`workflow_dispatch`).
3. **Unduh APK Siap Instal**:
   - Setelah workflow selesai (hijau), buka hasil run tersebut.
   - Pada bagian **Artifacts**, unduh **`absensi-pjok-lokojuli-debug-apk`**.
   - Ekstrak file ZIP tersebut untuk mendapatkan `app-debug.apk`, salin ke HP Android, lalu instal.

---

## Cara Membangun APK Secara Lokal (Terminal / Android Studio)
### Terminal (dengan Gradle Wrapper atau Gradle sistem)
```bash
# Menggunakan Gradle Wrapper (Linux/macOS/GitHub Actions)
chmod +x gradlew
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug

# Atau di Windows
gradlew.bat :app:assembleDebug
```
Hasil APK berada di:
`app/build/outputs/apk/debug/app-debug.apk`
