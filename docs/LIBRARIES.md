# Libraries and Dependencies

This document lists all external libraries and dependencies used in the Android learning application.

## 🔧 Core Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.core:core-ktx | 1.12.0 | Kotlin extensions for Android core libraries |
| androidx.appcompat:appcompat | 1.6.1 | Backward compatibility for modern Android features |
| androidx.activity:activity-ktx | 1.7.2 | Activity Kotlin extensions and modern APIs |
| com.google.android.material:material | 1.9.0 | Material Design 3 components and theming |
| androidx.constraintlayout:constraintlayout | 2.1.4 | Advanced flexible UI layouts |

## 🏗 Architecture & Framework

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.6.2 | ViewModel with Kotlin coroutines support |
| androidx.lifecycle:lifecycle-livedata-ktx | 2.6.2 | LiveData with Kotlin extensions |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.6.2 | Lifecycle-aware components and coroutines |
| androidx.navigation:navigation-fragment-ktx | 2.6.0 | Navigation component for fragments |
| androidx.navigation:navigation-ui-ktx | 2.6.0 | Navigation component UI integration |

## 🔌 Dependency Injection

| Library | Version | Purpose |
|---------|---------|---------|
| com.google.dagger:hilt-android | 2.48 | Dependency injection framework for Android |
| com.google.dagger:hilt-android-compiler | 2.48 | Hilt annotation processor for code generation |

## 🔒 Security & Storage

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.security:security-crypto | 1.1.0-alpha06 | Encrypted SharedPreferences for secure data storage |

## 🌐 Networking & Coroutines

| Library | Version | Purpose |
|---------|---------|---------|
| com.squareup.okhttp3:okhttp | 4.11.0 | HTTP client for API communication |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.7.3 | Kotlin coroutines for Android |

## 🎨 UI & Visual Components

| Library | Version | Purpose |
|---------|---------|---------|
| com.github.PhilJay:MPAndroidChart | v3.1.0 | Charts and data visualization (pie charts, progress) |
| com.mikhaellopez:circularprogressbar | 3.1.0 | Circular progress indicators |
| com.daimajia.numberprogressbar:library | 1.4 | Horizontal progress bars with numbers |
| com.airbnb.android:lottie | 6.1.0 | Smooth animations and micro-interactions |
| androidx.swiperefreshlayout:swiperefreshlayout | 1.1.0 | Pull-to-refresh functionality |

## 📸 Image Processing & Camera

| Library | Version | Purpose |
|---------|---------|---------|
| com.github.bumptech.glide:glide | 4.15.1 | Image loading, caching, and processing |
| androidx.camera:camera-core | 1.3.1 | Core CameraX functionality |
| androidx.camera:camera-camera2 | 1.3.1 | Camera2 implementation for CameraX |
| androidx.camera:camera-lifecycle | 1.3.1 | Lifecycle-aware camera operations |
| androidx.camera:camera-view | 1.3.1 | Camera preview and UI components |
| androidx.camera:camera-extensions | 1.3.1 | Camera extensions and effects |
| com.github.yalantis:ucrop | 2.2.8 | Image cropping for profile pictures |

## 📱 QR Code & Scanning

| Library | Version | Purpose |
|---------|---------|---------|
| com.google.mlkit:barcode-scanning | 17.1.0 | ML Kit barcode and QR code scanning |
| com.github.yuriy-budiyev:code-scanner | 2.3.0 | QR code scanner with camera integration |
| com.google.zxing:core | 3.5.2 | Core ZXing library for QR code generation |
| com.journeyapps:zxing-android-embedded | 4.3.0 | Android-embedded ZXing for QR codes |

## 🧪 Testing

| Library | Version | Purpose |
|---------|---------|---------|
| junit:junit | 4.13.2 | Unit testing framework |
| androidx.test.ext:junit | 1.1.5 | Android instrumentation testing extensions |
| androidx.test.espresso:espresso-core | 3.5.1 | UI testing and automation |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.7.3 | Coroutines testing utilities |
| org.junit.jupiter:junit-jupiter-api | 5.10.0 | JUnit 5 testing API |
| org.junit.jupiter:junit-jupiter-engine | 5.10.0 | JUnit 5 runtime engine |

## 🛠 Build Tools & Plugins

| Tool | Version | Purpose |
|------|---------|---------|
| Android Gradle Plugin | 8.2.2 | Android build system and tooling |
| Kotlin Gradle Plugin | 1.9.22 | Kotlin language support and compilation |
| Gradle Wrapper | 8.2+ | Build automation and dependency management |
| Hilt Plugin | 2.48 | Hilt dependency injection plugin |

## 📋 Version Management

All versions are centrally managed in the app's `build.gradle` file:

```groovy
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.nhlstenden.appdev"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
}

dependencies {
    def hilt_version = "2.48"
    def lifecycle_version = "2.6.2"
    def nav_version = "2.6.0"
    def kotlin_version = "1.9.22"
    def camerax_version = "1.3.1"
    
    // Dependencies using version variables
    implementation "com.google.dagger:hilt-android:$hilt_version"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
    // ... other dependencies
}
```

## 🔄 Dependency Management Best Practices

### Version Consistency
- **Centralized version management** using variables in build.gradle
- **Compatible version ranges** for related libraries (e.g., CameraX suite)
- **Regular updates** following semantic versioning principles

### Security Considerations
- **Encrypted storage** for sensitive data (EncryptedSharedPreferences)
- **Secure networking** with OkHttp and proper certificate validation
- **Regular security audits** of dependencies for vulnerabilities

### Performance Optimizations
- **Image loading efficiency** with Glide caching strategies
- **Coroutines** for non-blocking asynchronous operations
- **Lightweight alternatives** chosen where possible (NumberProgressBar vs heavy chart libraries)

## 📱 Platform-Specific Features

### Android Jetpack Integration
- **Lifecycle components** for proper lifecycle management
- **Navigation component** for type-safe navigation
- **ViewBinding** enabled for type-safe view references
- **DataBinding** enabled for reactive UI updates

### Material Design Implementation
- **Material Design 3** components throughout the app
- **Dynamic theming** support for user preferences
- **Accessibility** considerations with proper content descriptions

## 🆕 Recent Additions & Updates

### Latest Dependencies (Recent Updates)
- **Enhanced security** with EncryptedSharedPreferences
- **Improved camera functionality** with CameraX 1.3.1
- **Better testing support** with JUnit 5 and coroutines testing
- **Modern UI components** with latest Material Design versions

### Deprecated Libraries Removed
- **Legacy camera APIs** replaced with CameraX
- **Old HTTP clients** replaced with modern OkHttp
- **Outdated testing frameworks** upgraded to latest versions

## 🚀 Adding New Dependencies

### Process for Adding Dependencies
1. **Research compatibility** with existing versions
2. **Check for conflicts** with current dependencies
3. **Update build.gradle** with appropriate version
4. **Sync project** and verify successful integration
5. **Update this documentation** with new additions
6. **Test thoroughly** to ensure no breaking changes

### Dependency Validation Commands
```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Verify dependency tree
./gradlew dependencies

# Check for security vulnerabilities
./gradlew dependencyCheckAnalyze
```

---

This dependency list reflects the current state of a modern Android application following best practices for performance, security, and maintainability. 