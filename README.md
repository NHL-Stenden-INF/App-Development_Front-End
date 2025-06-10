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

## 🏗 Recent Major Updates

### Authentication & Security Enhancements (Latest)
- **JWT Expiration Handling**: Automatic detection and graceful redirect to login
- **SplashActivity Improvements**: Pre-validation of authentication status
- **Enhanced Security**: Proper token validation across all repositories
- **Session Management**: Automatic cleanup of expired sessions

### UI/UX Improvements
- **Course Navigation Fix**: Consistent progress display across all entry points
- **Bell Pepper Visual Updates**: Real-time UI refresh after purchases
- **Profile Updates**: Immediate visual feedback for all profile changes
- **Loading States**: Improved loading indicators and error handling

### Code Quality
- **Comment Standardization**: Professional-level documentation for 2nd year students
- **Architecture Refinement**: Enhanced SOLID and DRY principles implementation
- **Error Handling**: Comprehensive error management across all layers

## 📚 Documentation

### Development Documentation
- [📋 Architecture Overview](docs/ARCHITECTURE.md) - System design and patterns
- [📦 Libraries & Dependencies](docs/LIBRARIES.md) - Complete dependency list
- [🎨 Code Conventions](docs/CODE_CONVENTIONS.md) - Coding standards and naming
- [🤝 Contributing Guide](docs/CONTRIBUTING.md) - Development workflow

### Feature Documentation
- **Authentication Flow**: JWT-based login with automatic session management
- **Course System**: Hierarchical course structure with progress tracking
- **Social Features**: Friend management and progress sharing
- **Rewards System**: Gamification elements and achievement tracking

## 🧪 Testing

The application includes comprehensive testing coverage:

### Test Types
- **Unit Tests**: ViewModel logic, repositories, use cases
- **Integration Tests**: Data flow and repository interactions
- **UI Tests**: User interface and navigation flows
- **Authentication Tests**: Login/logout and session management

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test suite
./gradlew testDebugUnitTest

# UI tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## 🚦 Current Status

### ✅ Implemented Features
- Complete authentication system with JWT handling
- Full course management with progress tracking
- Social features (friends, profiles, sharing)
- Rewards and gamification system
- Real-time UI updates for all user actions
- Comprehensive error handling and validation

### 🔄 Recent Fixes
- Course navigation consistency across all screens
- Bell pepper purchase visual feedback
- JWT expiration graceful handling
- Profile picture update synchronization
- Comment cleanup and documentation standardization

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Android Jetpack**: Modern Android development components
- **Material Design**: Google's design system for beautiful UIs
- **Kotlin Community**: Excellent language support and ecosystem
- **Open Source Libraries**: All the amazing libraries that make this possible
- **NHL Stenden**: Educational institution supporting this project


