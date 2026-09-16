# Debug LED PWM App (Android App 工程迁移更新日志与文档)

[English](README.md) | [中文](README-ZH.md)

---

## 1. 项目概述
本项目为 Android 平台下的 LED 灯带 PWM 控制与音频可视化测试工程（`debgu_led_pwm_app`），包名为 `com.usbsdk.sample2`。
项目主要用于测试 RGB LED 灯带的不同灯效模式（包括单色常亮、呼吸灯、闪烁、渐变、跑马灯、彩虹流动、随机颜色、音乐律动可视化等），并通过 JNI 原生层与系统底层设备节点 `/dev/ledstrip` 进行数据通信与硬件控制。

---

## 2. 迁移背景与目标

- **历史背景**：原工程基于旧版 Android Studio 3.6.2 (AGP 3.6.2) 及早期 Groovy DSL 构建，依赖旧式配置。
- **迁移目标**：
  - 升级并重构至现代化的 **Gradle 9.4.1 + AGP 9.2.1** 构建体系。
  - 构建脚本全量迁移至 **Kotlin DSL (`.gradle.kts`)** 并引入 **Version Catalog (`libs.versions.toml`)**。
  - 完全适配 **AndroidX**、**Java 17** 及 **Android 12+ (API 31+)** 运行规范。
  - **SO 原生库规范化**：不向工程中内置打包任何 SO 文件，完全通过系统 Vendor 共有库（Public Native Library）机制调用 `/vendor/lib64/libledstrip.so`。
  - 保证现有所有业务逻辑、灯效模式及硬件通信功能完全不变并通过真机 ADB 验证。

---

## 3. 环境要求

- **JDK 版本**：OpenJDK 17 (推荐 OpenJDK 17.0.4.1 / Eclipse Temurin 17 或以上)
- **Gradle 版本**：9.4.1 (通过工程内置 Wrapper `./gradlew` 驱动)
- **Android Gradle Plugin (AGP)**：9.2.1
- **SDK 配置**：
  - `minSdk = 29` (Android 10.0+)
  - `targetSdk = 34` (Android 14.0)
  - `compileSdk = release(36) { minorApiLevel = 1 }`
- **签名配置**：保留并配置原有 `signature/facesdk-library.keystore`（Debug / Release 均生效）。

---

## 4. 核心变更记录 (Changelog)

### 4.1 构建体系重构 (Build System Modernization)
- **Gradle & AGP 升级**：Gradle 升级至 `9.4.1`，AGP 升级至 `9.2.1`。
- **Kotlin DSL 迁移**：
  - `settings.gradle` -> `settings.gradle.kts`
  - `build.gradle` (Root) -> `build.gradle.kts`
  - `app/build.gradle` -> `app/build.gradle.kts`
- **引入 Version Catalog**：
  - 新增 `gradle/libs.versions.toml` 统一管理版本，包含 `androidx-appcompat` (`1.6.1`) 与 `audiovisualizer` (`2.2.5`)。
- **Gradle 属性配置**：
  - 在 `gradle.properties` 中开启 `android.useAndroidX=true`、`org.gradle.configuration-cache=true` 及 JVM 参数 `-Xmx2048m -Dfile.encoding=UTF-8`。

### 4.2 代码与清单适配 (Code & Manifest Adaptation)
- **Non-final Resource IDs 适配**：
  - 在 AGP 9.x 环境下，资源 ID 默认非 final 常量。在 `LedStripTestActivity.java` 中将 `switch (id)` 根据 `R.id` 的分支结构重构成 `if-else` 结构。
- **Android 12+ 清单声明**：
  - `AndroidManifest.xml` 中的主入口 Activity 显式声明 `android:exported="true"`。
- **多语言默认资源补全**：
  - 在 `res/values/` 下补全 `baudrates.xml` 默认资源，避免构建警告。

---

## 5. 原生库特别说明 (`libledstrip.so`)

### 5.1 不打包 SO 到 APK 的设计
- **设计原因**：
  LED 灯带控制库 `libledstrip.so` 属于板级/硬件强相关的驱动层封装库，设备固件中已将其内置在 `/vendor/lib64/libledstrip.so`（或 `/vendor/lib/libledstrip.so`）路径下。
  为避免 APK 体积冗余以及不同硬件版本固件的 SO 二进制冲突，**本项目不在 `app/libs/` 中放置任何 `.so` 文件，生成的 APK 内 `lib/` 目录为空**。

### 5.2 系统共有库声明机制 (Public Libraries)
- 设备固件在 `/vendor/etc/public.libraries.txt` 中已对外公开了该库：
  ```text
  libOpenCL.so
  libhdxutil.so
  libledstrip.so
  libhdxserial_port.so
  ```
- **Android 12+ Linker 命名空间隔离与适配**：
  在 Android 12 (API 31) 及更高版本中，由于 NDK Linker Namespace 安全隔离机制，普通的第三方应用命名空间默认无法直接跨命名空间加载 Vendor 库。
  必须在 `AndroidManifest.xml` 的 `<application>` 节点下显式添加以下声明：
  ```xml
  <uses-native-library
      android:name="libledstrip.so"
      android:required="false" />
  ```
  该声明会指示 Android 运行时（`nativeloader`）在为应用创建 ClassLoader Linker Namespace 时，将 Vendor 共有库暴露给应用。

### 5.3 JNI 加载调用
- 在 Java 层 [`LedStripJni.java`](file:///h:/debug_app/111_user_sdk/debgu_led_pwm_app/app/src/main/java/com/goodchip/ledstrip/LedStripJni.java) 中，直接调用标准系统加载方法即可：
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
- 经真机验证，系统会正确从 `/vendor/lib64/libledstrip.so` 解析并加载符号，正常打开与操作 `/dev/ledstrip`。

---

## 6. 构建与验证指南

### 6.1 编译 APK
在项目根目录下执行以下命令完成 Debug 编译：

```bash
# Windows PowerShell / CMD
.\gradlew.bat clean assembleDebug

# Linux / macOS
./gradlew clean assembleDebug
```

编译输出路径：
`app/build/outputs/apk/debug/app-debug.apk`

### 6.2 ADB 安装与真机运行验证
通过 ADB 命令行将 APK 安装至已连接设备并启动测试：

```bash
# 1. 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. 授予音频录制权限（用于音乐律动效果）
adb shell pm grant com.usbsdk.sample2 android.permission.RECORD_AUDIO

# 3. 启动主界面 Activity
adb shell am start -n com.usbsdk.sample2/com.usbsdk.sample.LedStripTestActivity

# 4. 查看运行日志确认 SO 库与设备状态
adb logcat -d -s LedStripJni:V LedStripTest:V
```

---

## 7. 常见问题 (FAQ)

### Q1: 为什么 Demo 工程的 `libs` 目录没有打包 `libledstrip.so`，编译出的 APK 内 `lib/` 也是空的？
**答**：
1. **固件底层内置库**：`libledstrip.so` 是板级硬件驱动封装库，与主板硬件及内核驱动（`/dev/ledstrip`）强绑定。该库已经直接预置在设备的系统固件中（路径为 `/vendor/lib64/libledstrip.so` 或 `/vendor/lib/libledstrip.so`）。
2. **避免软硬件版本冲突**：驱动 SO 库与系统底层 Bionic 运行库强相关，随 APK 打包固定版本会导致体积冗余，且设备固件 OTA 升级后容易引发二进制兼容性问题或冲突。
3. **支持驱动无感升级**：依赖设备系统内置库，后续固件优化底层驱动时，客户应用无需重新编译打包即可自动生效。

---

### Q2: 为什么必须在 `AndroidManifest.xml` 中配置 `<uses-native-library>`？
**答**：
1. **Android 12+ (API 31+) Linker 命名空间安全隔离**：
   从 Android 12 开始，Google 强制启用了 NDK Linker Namespace 隔离。普通应用处于受限的 App ClassLoader 命名空间中，默认无法跨界加载 `/vendor/` 分区下的原生共享库。如果直接调用 `System.loadLibrary("ledstrip")`，系统会报错拒绝加载：`dlopen failed: library "libledstrip.so" not found`。
2. **系统公共库暴露机制 (Public Native Library)**：
   设备系统已在 `/vendor/etc/public.libraries.txt` 中将 `libledstrip.so` 声明为开放共享库。
3. **运行时动态链接注入**：
   在 `AndroidManifest.xml` 的 `<application>` 下配置：
   ```xml
   <uses-native-library
       android:name="libledstrip.so"
       android:required="false" />
   ```
   该声明会指示 Android 运行时（`nativeloader`）在为应用创建进程命名空间时，将固件中的 `libledstrip.so` 挂载链接并暴露给应用，使 Java 层能够合法且顺利地加载和调用。

---

### Q3: 为什么配置中的 `android:required` 设置为 `"false"` 而不是 `"true"`？
**答**：
- **软依赖与容错设计（Graceful Fallback）**：
  - 若配置为 `android:required="true"`，当该 APK 安装在未内置该硬件驱动库的普通 Android 设备、平板或模拟器上时，系统安装管理器（`PackageManager`）会直接拒绝安装并报错 `INSTALL_FAILED_MISSING_SHARED_LIBRARY`。
  - 配置为 `false` 后，应用可以在任何 Android 设备上正常安装与运行基础界面；在代码实现（`LedStripJni.java`）中，`System.loadLibrary("ledstrip")` 被包裹在 `try-catch` 保护块中，即便在无该硬件的测试设备上也不会发生崩溃，极大地提高了应用的鲁棒性与设备兼容性。

---

### Q4: 客户将灯带控制功能集成到自己的主应用（App）时应如何操作？
**答**：
1. **第一步**：无需向本地工程的 `jniLibs` 目录拷贝任何 `.so` 文件。
2. **第二步**：在客户主 App 的 `AndroidManifest.xml` 中的 `<application>` 标签下添加声明：
   ```xml
   <application ...>
       <!-- 声明使用系统 Vendor 预置的 LED 灯带原生库 -->
       <uses-native-library
           android:name="libledstrip.so"
           android:required="false" />
       ...
   </application>
   ```
3. **第三步**：直接复制或引用 `com.goodchip.ledstrip.LedStripJni` Java 封装类，在业务代码中正常调用 `LedStripJni.getLedstrip().Init()` 以及 `sendData(...)` 进行控制即可。
