# Debug LED PWM App (Android App Migration Changelog & Documentation)

[English](README.md) | [中文](README-ZH.md)

---

## 1. Project Overview
This project is an Android sample application (`debgu_led_pwm_app`, package: `com.usbsdk.sample2`) designed for PWM RGB LED light strip control and real-time audio visualization.
The application allows testing various dynamic lighting effects (static white/colors, breathing light, flicker, gradient cycle, chase/running lamp, rainbow flow, random colors, and music visualizer), interacting directly with the underlying hardware device node `/dev/ledstrip` via native JNI.

---

## 2. Migration Background & Objectives

- **Legacy Context**: The original project was built on legacy Android Studio 3.6.2 (AGP 3.6.2) using outdated Groovy DSL build scripts.
- **Migration Goals**:
  - Upgrade and modernize the build infrastructure to **Gradle 9.4.1 + Android Gradle Plugin (AGP) 9.2.1**.
  - Migrate all build scripts to **Kotlin DSL (`.gradle.kts`)** and introduce **Version Catalog (`libs.versions.toml`)**.
  - Ensure full compatibility with **AndroidX**, **Java 17**, and **Android 12+ (API 31+)** runtime requirements.
  - **Native Library Optimization**: Avoid packaging any `.so` libraries inside the APK, instead leveraging the device firmware's public vendor library (`/vendor/lib64/libledstrip.so`).
  - Maintain all existing business logic, lighting modes, and hardware communication behavior with zero regressions, validated via ADB flashing on real hardware.

---

## 3. Environment Requirements

- **JDK Version**: OpenJDK 17 (OpenJDK 17.0.4.1 / Eclipse Temurin 17 or higher recommended)
- **Gradle Version**: 9.4.1 (driven via project wrapper `./gradlew`)
- **Android Gradle Plugin (AGP)**: 9.2.1
- **SDK Configuration**:
  - `minSdk = 29` (Android 10.0+)
  - `targetSdk = 34` (Android 14.0)
  - `compileSdk = release(36) { minorApiLevel = 1 }`
- **Signing Configuration**: Retained original `signature/facesdk-library.keystore` applied to both Debug and Release build types.

---

## 4. Changelog & Key Modifications

### 4.1 Build System Modernization
- **Gradle & AGP Upgrade**:
  - Gradle upgraded to `9.4.1`.
  - Android Gradle Plugin (AGP) upgraded to `9.2.1`.
- **Kotlin DSL Migration**:
  - `settings.gradle` -> `settings.gradle.kts`
  - `build.gradle` (Root) -> `build.gradle.kts`
  - `app/build.gradle` -> `app/build.gradle.kts`
- **Version Catalog Adoption**:
  - Created `gradle/libs.versions.toml` to centrally manage dependencies, including `androidx-appcompat` (`1.6.1`) and `audiovisualizer` (`2.2.5`).
- **Gradle Properties**:
  - Configured `android.useAndroidX=true`, `org.gradle.configuration-cache=true`, and JVM arguments `-Xmx2048m -Dfile.encoding=UTF-8` in `gradle.properties`.

### 4.2 Code & Manifest Adaptation
- **Non-final Resource IDs**:
  - Under AGP 9.x, resource IDs are non-final by default. Refactored `switch (id)` in `LedStripTestActivity.java` into an `if-else` block.
- **Android 12+ Manifest Compliance**:
  - Added `android:exported="true"` explicitly to the launcher Activity in `AndroidManifest.xml`.
- **Default Resources**:
  - Added fallback `res/values/baudrates.xml` to prevent build warnings across locales.

---

## 5. Native Library Architecture (`libledstrip.so`)

### 5.1 Zero-SO Packaging Architecture
- **Rationale**:
  `libledstrip.so` is a board-level hardware communication wrapper tightly bound to the device firmware and kernel driver. It is pre-installed on the device under `/vendor/lib64/libledstrip.so` (or `/vendor/lib/libledstrip.so`).
  To prevent APK bloat and potential binary version mismatches across firmware revisions, **this project does NOT embed any `.so` files in `app/libs/` or the resulting APK (APK `lib/` directory is empty)**.

### 5.2 Vendor Public Library Exposure & Android 12+ Linker Namespace
- The device firmware exports the library in `/vendor/etc/public.libraries.txt`:
  ```text
  libOpenCL.so
  libhdxutil.so
  libledstrip.so
  libhdxserial_port.so
  ```
- **Linker Namespace Isolation**:
  On Android 12 (API 31) and above, Android enforces strict NDK Linker Namespace isolation for non-system apps. To grant the application ClassLoader permission to load the vendor library, `AndroidManifest.xml` must declare:
  ```xml
  <uses-native-library
      android:name="libledstrip.so"
      android:required="false" />
  ```
  This instructs `nativeloader` to link the vendor public libraries into the application's native linker namespace.

### 5.3 JNI Loading Mechanism
- In Java layer [`LedStripJni.java`](file:///h:/debug_app/111_user_sdk/debgu_led_pwm_app/app/src/main/java/com/goodchip/ledstrip/LedStripJni.java), the standard loader is used:
  ```java
  static {
      try {
          System.loadLibrary("ledstrip");
          Log.i("LedStripJni", "libledstrip.so loaded successfully");
      } catch (Throwable t) {
          Log.e("LedStripJni", "Failed to load libledstrip.so", t);
      }
  }
  ```
- Verified on real hardware: the system resolves and loads symbols directly from `/vendor/lib64/libledstrip.so`, successfully controlling `/dev/ledstrip` without crash or failure.

---

## 6. Build & Verification Guide

### 6.1 Build APK
Execute the following command in the project root to assemble the Debug APK:

```bash
# Windows PowerShell / CMD
.\gradlew.bat clean assembleDebug

# Linux / macOS
./gradlew clean assembleDebug
```

Output APK location:
`app/build/outputs/apk/debug/app-debug.apk`

### 6.2 ADB Deployment & Execution Verification
Install the APK onto a connected target device and verify via ADB:

```bash
# 1. Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Grant audio record permission (required for music visualizer)
adb shell pm grant com.usbsdk.sample2 android.permission.RECORD_AUDIO

# 3. Launch main Activity
adb shell am start -n com.usbsdk.sample2/com.usbsdk.sample.LedStripTestActivity

# 4. Check logcat output to verify SO loading and hardware status
adb logcat -d -s LedStripJni:V LedStripTest:V
```

---

## 7. Frequently Asked Questions (FAQ)

### Q1: Why is `libledstrip.so` not packaged inside the project's `libs` directory or the output APK?
**A**:
1. **Pre-installed System Vendor Library**: `libledstrip.so` is a board-level hardware driver abstraction library tightly coupled with the hardware and kernel device node (`/dev/ledstrip`). It is already pre-installed in the device system firmware at `/vendor/lib64/libledstrip.so` (or `/vendor/lib/libledstrip.so`).
2. **Avoid Version & ABI Conflicts**: Driver `.so` libraries depend heavily on the underlying Bionic C runtime. Packaging a fixed `.so` inside an APK causes unnecessary APK size bloat and risks binary incompatibility or crashes after firmware OTA updates.
3. **Seamless Driver Updates**: By leveraging the system vendor library, future firmware driver optimizations take effect automatically without requiring customers to recompile or republish their applications.

---

### Q2: Why is `<uses-native-library>` mandatory in `AndroidManifest.xml`?
**A**:
1. **Android 12+ (API 31+) Linker Namespace Isolation**:
   Starting with Android 12, Google enforced strict NDK Linker Namespace isolation. Standard third-party apps run in isolated application namespaces and cannot access vendor partition libraries directly by default. Invoking `System.loadLibrary("ledstrip")` directly would fail with `dlopen failed: library "libledstrip.so" not found`.
2. **Public Native Library Mechanism**:
   The device firmware declares `libledstrip.so` as a public vendor library in `/vendor/etc/public.libraries.txt`.
3. **Dynamic Linker Namespace Mapping**:
   Declaring the library in `AndroidManifest.xml`:
   ```xml
   <uses-native-library
       android:name="libledstrip.so"
       android:required="false" />
   ```
   instructs Android's `nativeloader` to link and expose the pre-installed `libledstrip.so` from the vendor namespace into the app's ClassLoader namespace at process startup, allowing standard `System.loadLibrary("ledstrip")` calls to succeed.

---

### Q3: Why is `android:required` set to `"false"` instead of `"true"`?
**A**:
- **Graceful Fallback & Compatibility**:
  - If set to `android:required="true"`, installing the APK on devices without this specific vendor hardware library (such as generic Android tablets or emulators) will immediately fail with `INSTALL_FAILED_MISSING_SHARED_LIBRARY`.
  - Setting it to `"false"` allows the APK to install cleanly on any Android device. In the application code (`LedStripJni.java`), `System.loadLibrary("ledstrip")` is protected inside a `try-catch` block, ensuring the app handles missing hardware gracefully without crashing.

---

### Q4: How should customers integrate the LED strip control into their own application?
**A**:
1. **Step 1**: Do **NOT** copy any `.so` files into your project's `jniLibs` directory.
2. **Step 2**: Add the native library declaration within the `<application>` tag of your `AndroidManifest.xml`:
   ```xml
   <application ...>
       <!-- Declare access to device vendor pre-installed LED native library -->
       <uses-native-library
           android:name="libledstrip.so"
           android:required="false" />
       ...
   </application>
   ```
3. **Step 3**: Copy or reference the `com.goodchip.ledstrip.LedStripJni` class into your source tree, then invoke `LedStripJni.getLedstrip().Init()` and `sendData(...)` directly in your business logic.