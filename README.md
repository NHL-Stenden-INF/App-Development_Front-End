# App Development Front-End

A modern Android learning application built with Kotlin, featuring course management, progress tracking, social features, and gamification elements. The app follows MVVM architecture with Clean Architecture principles and includes robust authentication, real-time progress updates, and an intuitive Material Design interface.

## 🚀 Features

### Core Learning Features
- **Course Management**: Browse and access programming courses with progress tracking
- **Task System**: Interactive programming tasks with immediate feedback
- **Progress Tracking**: Visual progress indicators with pie charts and completion statistics
- **Streak Counter**: Daily learning streak tracking with visual calendar
- **Bell Pepper Rewards**: Gamification currency system for achievements

### Social & Profile Features
- **User Profiles**: Customizable profiles with display names and avatars
- **Friends System**: Add friends, view their progress, and compare achievements
- **Rewards System**: Achievement badges and daily rewards
- **Profile Customization**: Camera integration for profile pictures with cropping support

### Technical Features
- **Secure Authentication**: JWT-based authentication with automatic token refresh
- **Offline Support**: Local data caching for seamless offline experience
- **Real-time Updates**: Immediate UI updates for all user actions
- **QR Code Integration**: Course sharing and quick access via QR codes
- **Dark Mode Support**: Automatic theme switching based on system preferences

## 🛠 Technical Stack

### Core Technologies
- **Language**: Kotlin 1.9.22
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt (Dagger)
- **UI Framework**: Android Jetpack with Material Design 3
- **Backend**: Supabase (PostgreSQL database)
- **Authentication**: JWT tokens with secure storage

### Key Libraries
- **UI**: Material Design Components, ViewBinding, Navigation Component
- **Data**: Encrypted SharedPreferences, Coroutines, LiveData/StateFlow
- **Networking**: OkHttp for API communication
- **Image Processing**: Glide, CameraX, uCrop
- **Charts**: MPAndroidChart for progress visualization
- **QR Codes**: ZXing for generation and MLKit for scanning
- **Animations**: Lottie for smooth animations

## 📱 App Structure

### Main Screens
- **Home**: Continue learning section, daily streak counter, quick course access
- **Courses**: Full course catalog with search and filtering capabilities
- **Progress**: Personal progress overview with detailed statistics
- **Friends**: Social features for connecting with other learners
- **Rewards**: Achievement system and daily reward collection
- **Profile**: User settings, statistics, and profile customization

### Architecture Layers
```
├── Presentation Layer (UI)
│   ├── Activities & Fragments
│   ├── ViewModels (MVVM)
│   └── Custom Views
├── Domain Layer (Business Logic)
│   ├── Use Cases
│   ├── Repository Interfaces
│   └── Domain Models
└── Data Layer
    ├── Repository Implementations
    ├── Local Data Sources
    └── Remote Data Sources
```

## 🔧 Setup and Installation

### Prerequisites
- **Android Studio**: Hedgehog (2023.3.1) or newer
- **Android SDK**: API 24+ (Android 7.0) minimum, API 34+ target
- **JDK**: Java 17 or newer
- **Gradle**: 8.2+

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/NHL-Stenden-INF/App-Development_Front-End.git
   cd App-Development_Front-End
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open" or "Open an Existing Project"
   - Navigate to the project directory and open it
   - Allow Gradle sync to complete (may take several minutes on first run)

3. **Configure Backend Connection**
   - Ensure `SupabaseClient.kt` has the correct Supabase configuration
   - Verify database connection settings are properly configured

4. **Build the Project**
   ```bash
   # Command line build (optional)
   ./gradlew build
   
   # Or use Android Studio: Build > Make Project
   ```

5. **Run the Application**
   - Select a target device (emulator or physical device)
   - Click the "Run" button in Android Studio
   - The app will install and launch automatically

### Development Build Commands
```bash
# Clean build
./gradlew clean build

# Install debug build
./gradlew installDebug

# Run tests
./gradlew test

# Generate documentation
./gradlew dokkaHtml
```

## 📚 Documentation

### Development Documentation
- [🎨 Code Conventions](docs/CODE_CONVENTIONS.md) - Coding standards and naming
- [🤝 Contributing Guide](docs/CONTRIBUTING.md) - Development workflow

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.



