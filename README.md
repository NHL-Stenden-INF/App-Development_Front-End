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

## 🏗️ SOLID Principles & Loose Coupling Examples

This project demonstrates excellent adherence to SOLID principles and loose coupling patterns. Below are real examples from our codebase:

### 🔧 Dependency Injection (Loose Coupling)

**Hilt Dependency Injection with Interface Binding:**
```kotlin
// core/di/SupabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    
    @Binds
    abstract fun bindCoursesRepository(impl: CoursesRepositoryImpl): CoursesRepository
}
```

**ViewModel with Injected Dependencies:**
```kotlin
// features/profile/viewmodels/ProfileViewModel.kt
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    // ViewModel depends on abstractions, not concrete implementations
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
}
```

### Single Responsibility Principle (SRP)

**Repository handling only data operations:**
```kotlin
// features/profile/repositories/ProfileRepositoryImpl.kt
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ProfileRepository {
    
    override suspend fun getProfile(): Result<Profile> {
        // Only responsible for profile data management
        // JWT handling delegated to authRepository
        // API calls delegated to supabaseClient
    }
}
```

### Open/Closed Principle (OCP)

**Base classes enabling extension without modification:**
```kotlin
// core/ui/base/BaseViewModel.kt
open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected fun launchWithLoading(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch {
            _isLoading.value = true
            try {
                block()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Extended without modifying base class
class ProfileViewModel @Inject constructor(...) : ViewModel() {
    // Extends base functionality while adding profile-specific logic
}
```

### Interface Segregation Principle (ISP)

**Focused, specific repository interfaces:**
```kotlin
// core/repositories/ProfileRepository.kt
interface ProfileRepository {
    suspend fun getProfile(): Result<Profile>
    suspend fun updateProfile(displayName: String, bio: String?, profilePicture: String?): Result<Profile>
    suspend fun updateProfilePicture(imagePath: String): Result<Profile>
    suspend fun logout(): Result<Unit>
}

// core/repositories/AuthRepository.kt  
interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun handleJWTExpiration()
}
```

### Dependency Inversion Principle (DIP)

**High-level modules depending on abstractions:**
```kotlin
// features/courses/viewmodels/CoursesViewModel.kt
@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val coursesRepository: CoursesRepository,  // Interface, not implementation
    private val authRepository: AuthRepository         // Interface, not implementation
) : ViewModel() {
    
    fun loadCoursesWithProgress() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    _courses.value = coursesRepository.getCourses(currentUser) ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load courses"
            }
        }
    }
}
```

### Reactive State Management (Loose Coupling)

**StateFlow for decoupled UI updates:**
```kotlin
// features/courses/viewmodels/CoursesViewModel.kt
class CoursesViewModel @Inject constructor(...) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        viewModelScope.launch {
            // Reactive combination of multiple state streams
            kotlinx.coroutines.flow.combine(
                _courses,
                _searchQuery,
                _selectedStars
            ) { courses, query, stars ->
                courses.filter { course ->
                    val matchesSearch = query.isEmpty() || 
                        course.title.contains(query, ignoreCase = true)
                    val matchesStars = stars == null || course.difficulty == stars
                    matchesSearch && matchesStars
                }
            }.collect { filtered ->
                _filteredCourses.value = filtered
            }
        }
    }
}
```

### Error Handling with Result Pattern

**Consistent error handling promoting loose coupling:**
```kotlin
// core/utils/ErrorHandler.kt
object ErrorHandler {
    fun <T> handleResult(
        result: Result<T>,
        tag: String,
        operation: String,
        onSuccess: (T) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        result.fold(
            onSuccess = { data ->
                Log.d(tag, "$operation completed successfully")
                onSuccess(data)
            },
            onFailure = { exception ->
                val errorMessage = exception.message ?: "Unknown error occurred"
                Log.e(tag, "$operation failed: $errorMessage", exception)
                onFailure(errorMessage)
            }
        )
    }
}
```

These examples demonstrate how our architecture promotes **maintainability**, **testability**, and **flexibility** through proper application of SOLID principles and loose coupling patterns.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.



