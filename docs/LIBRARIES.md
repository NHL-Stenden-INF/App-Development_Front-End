# Libraries and Dependencies

This document lists all external libraries and dependencies used in the project.

## Core Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.core:core-ktx | 1.12.0 | Kotlin extensions for Android core libraries |
| androidx.appcompat:appcompat | 1.6.1 | Backward compatibility for modern Android features |
| com.google.android.material:material | 1.11.0 | Material Design components |
| androidx.constraintlayout:constraintlayout | 2.1.4 | Advanced UI layouts |
| androidx.navigation:navigation-fragment-ktx | 2.7.7 | Navigation component for fragments |
| androidx.navigation:navigation-ui-ktx | 2.7.7 | Navigation component for UI |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.7.0 | ViewModel and LiveData |
| androidx.lifecycle:lifecycle-livedata-ktx | 2.7.0 | LiveData components |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.7.0 | Lifecycle components |

## Dependency Injection

| Library | Version | Purpose |
|---------|---------|---------|
| com.google.dagger:hilt-android | 2.50 | Dependency injection framework |
| com.google.dagger:hilt-android-compiler | 2.50 | Hilt annotation processor |

## Database

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.room:room-runtime | 2.6.1 | Local database ORM |
| androidx.room:room-ktx | 2.6.1 | Kotlin extensions for Room |
| androidx.room:room-compiler | 2.6.1 | Room annotation processor |

## Networking

| Library | Version | Purpose |
|---------|---------|---------|
| com.squareup.retrofit2:retrofit | 2.9.0 | HTTP client |
| com.squareup.retrofit2:converter-gson | 2.9.0 | JSON parsing |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client implementation |
| com.squareup.okhttp3:logging-interceptor | 4.12.0 | HTTP logging |

## Image Loading

| Library | Version | Purpose |
|---------|---------|---------|
| com.github.bumptech.glide:glide | 4.16.0 | Image loading and caching |
| com.github.bumptech.glide:compiler | 4.16.0 | Glide annotation processor |

## Testing

| Library | Version | Purpose |
|---------|---------|---------|
| junit:junit | 4.13.2 | Unit testing |
| androidx.test.ext:junit | 1.1.5 | Android instrumentation testing |
| androidx.test.espresso:espresso-core | 3.5.1 | UI testing |
| org.mockito:mockito-core | 5.8.0 | Mocking framework |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.7.3 | Coroutines testing |

## Build Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Android Gradle Plugin | 8.2.2 | Android build system |
| Kotlin Gradle Plugin | 1.9.22 | Kotlin language support |
| Gradle | 8.2 | Build automation |

## Version Management

All versions are managed in the project's `build.gradle` files:

```groovy
buildscript {
    ext {
        kotlin_version = '1.9.22'
        hilt_version = '2.50'
        room_version = '2.6.1'
        lifecycle_version = '2.7.0'
        navigation_version = '2.7.7'
    }
}
```

## Adding New Dependencies

1. Check for the latest stable version
2. Add to appropriate `build.gradle` file
3. Update version catalog if used
4. Sync project
5. Update this document

## Version Updates

Regularly check for updates using:
```bash
./gradlew dependencyUpdates
```

## Security Considerations

- Keep dependencies updated
- Use only trusted sources
- Review dependency licenses
- Monitor security advisories 