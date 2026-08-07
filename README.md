# FabScreen

Android touchscreen control application for Snapmaker 3-in-1 3D printers, including Artisan (A400), J1, and A350 series devices.

FabScreen provides a native Android interface to control 3D printing, laser engraving, and CNC machining workflows directly from the device touchscreen.

## Supported Devices

- **Snapmaker Artisan** (A400) — 3D printing, laser engraving, CNC machining
- **Snapmaker J1** — Dual-extruder IDEX 3D printing
- **Snapmaker A350** — 3D printing, laser engraving, CNC machining

## Features

- 3D printing, laser engraving, and CNC machining control
- Wi-Fi and USB file transfer
- Real-time machine status monitoring
- Firmware over-the-air (OTA) updates
- Laser camera and auto-focus support (Artisan series)
- Bluetooth-based laser camera capture
- Modular tool head detection and management
- Multi-language UI support

## Project Structure

```
apps/
  a400/          Artisan (A400) main application
  a350/          A350 main application
  j1/            J1 main application
  updater/       OTA updater app
features/
  print/         3D print workflow
  settings/      Device settings and firmware update
  machine-tools/ Machine tool control (laser, CNC)
  file-manager/  File browsing and transfer
  remote/        Remote control features
  home/          Home screen
  guide/         Onboarding and setup guide
  welcome/       First-time setup wizard
  add-ons/       Accessory management
platform/
  base/          Core platform layer (services, machine models, connection)
  core/          UI framework and shared components
  lib/           Utility libraries (serial port, checksum, etc.)
buildSrc/        Gradle dependency version catalog
scripts/         Internal build and deployment scripts
ijkplayer_java/  Video player integration
```

## Dependencies

Essential third-party libraries:

- [ButterKnife](https://github.com/JakeWharton/butterknife) — view binding
- [RxJava](https://github.com/ReactiveX/RxJava) — reactive state management
- [RxAndroid](https://github.com/ReactiveX/RxAndroid) — Android main thread scheduler
- [Okio](https://github.com/square/okio) — byte array and I/O processing
- [AndServer](https://github.com/yanzhenjie/AndServer) — embedded HTTP server for remote commands
- [Retrofit 2](https://github.com/square/retrofit) — HTTP API client
- [ARouter](https://github.com/alibaba/ARouter) — in-app routing
- [Firebase Crashlytics](https://firebase.google.com/products/crashlytics) — crash reporting
- [ijkplayer](https://github.com/bilibili/ijkplayer) — video playback

## Prerequisites

- **Android Studio** 3.5+
- **JDK** 1.8
- **Android NDK** 22.1.7171670
- **Gradle** 5.4.1+ (wrapper included)

## Getting Started

### 1. Clone the repository

```bash
git https://github.com/Snapmaker/Snapmaker-Artisan-Screen-App.git
cd Snapmaker-Artisan-Screen-App
```

### 2. Configure Firebase (optional)

Firebase Crashlytics and Analytics are used by default. If you want to use Firebase services, copy the example templates and fill in your own Firebase project credentials:

```bash
cp apps/a400/google-services.json.example apps/a400/google-services.json
cp apps/a350/google-services.json.example apps/a350/google-services.json
cp apps/j1/google-services.json.example apps/j1/google-services.json
```

If you do not need Firebase, remove the `com.google.gms.google-services` and `com.google.firebase.crashlytics` plugins from each app's `build.gradle`, and remove the Firebase dependency entries.

### 3. Configure signing (optional)

Debug builds do not require signing configuration. For release builds, create a keystore and configure signing credentials via environment variables or `gradle.properties`:

```bash
export KEYSTORE_PATH=/path/to/your.keystore
export KEYSTORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

Alternatively, add these to your `~/.gradle/gradle.properties` or project-level `local.properties`:

```properties
KEYSTORE_PATH=/path/to/your.keystore
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 4. Build

```bash
# Build all debug APKs
./gradlew assembleDebug

# Build a specific variant
./gradlew :apps:a400:assembleDebug
./gradlew :apps:j1:assembleDebug
```

## Style Guide

```java
class FooFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_foo;
    }

    private void initView() {

    }

    private void initData() {

    }

    private void handleBar() {

    }

    @OnClick(R.id.btn_baz)
    void onClickBaz() {

    }
}
```

## Author

Snapmaker Software Team

## License

TBD
