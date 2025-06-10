# Architecture Overview

This document provides a comprehensive overview of the application's architecture, design patterns, and component interactions in the Android learning application.

## 🏗 Architecture Pattern

The application follows **MVVM (Model-View-ViewModel)** architecture pattern with **Clean Architecture** principles, implementing a layered approach for maintainability, testability, and scalability.

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                        │
├─────────────────────────────────────────────────────────────┤
│  Activities & Fragments  │  ViewModels  │  UI State Classes │
│  ─────────────────────────────────────────────────────────  │
│  • HomeFragment          │  • ProfileVM │  • UiState<T>     │
│  • CoursesFragment       │  • CourseVM  │  • ProfileState   │
│  • ProgressFragment      │  • AuthVM    │  • LoadingState   │
│  • ProfileFragment       │  • FriendsVM │  • ErrorState     │
│  • MainActivity          │  • RewardsVM │                   │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                             │
├─────────────────────────────────────────────────────────────┤
│    Use Cases    │    Repository Interfaces   │  Domain Models│
│  ────────────────────────────────────────────────────────   │
│  • AuthUseCases │  • AuthRepository          │  • User       │
│  • CourseUseCase│  • CourseRepository        │  • Course     │
│  • ProfileCase  │  • ProfileRepository       │  • Progress   │
│                 │  • FriendsRepository       │  • Friend     │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                     DATA LAYER                              │
├─────────────────────────────────────────────────────────────┤
│  Repository Implementations │  Data Sources │  Data Models  │
│  ─────────────────────────────────────────────────────────  │
│  • AuthRepositoryImpl       │  • Local DS   │  • UserDto    │
│  • CourseRepositoryImpl     │  • Remote DS  │  • CourseDto  │
│  • ProfileRepositoryImpl    │  • Cache DS   │  • ApiModels  │
│  • FriendsRepositoryImpl    │               │               │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 Key Architectural Components

### 1. Presentation Layer

**Activities & Fragments**
- **MainActivity**: Main container with navigation and profile header management
- **SplashActivity**: Authentication pre-validation and app initialization
- **Feature Fragments**: Modular UI components for each main feature
- **Custom Views**: Reusable UI components with specific functionality

**ViewModels (MVVM Pattern)**
```kotlin
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
    
    fun loadProfile() {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                val profile = profileRepository.getUserProfile()
                _profileState.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

**State Management**
```kotlin
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: Profile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
```

### 2. Domain Layer

**Repository Interfaces**
```kotlin
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun getCurrentUser(): Flow<User?>
    suspend fun logout()
    fun getCurrentUserSync(): User?
}
```

**Domain Models**
```kotlin
data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val authToken: String,
    val profilePictureUrl: String? = null
)

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val imageResId: Int,
    var progress: Int,
    var totalTasks: Int
)
```

### 3. Data Layer

**Repository Implementations**
```kotlin
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val encryptedPrefs: EncryptedSharedPreferences
) : AuthRepository {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = supabaseClient.login(email, password)
            if (response.isSuccessful) {
                val user = response.body()!!
                saveUserToPrefs(user)
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 🔐 Authentication & Security Architecture

### JWT Authentication Flow
```
┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ SplashActivity│───▶│  JWT Validation │───▶│ MainActivity or │
│               │    │                 │    │ LoginActivity   │
└─────────────┘    └─────────────────┘    └─────────────────┘
                            │
                    ┌─────────────────┐
                    │ Repository Layer│
                    │ JWT Expiration  │
                    │ Detection       │
                    └─────────────────┘
```

**Security Features:**
- **Encrypted Storage**: User tokens stored in EncryptedSharedPreferences
- **Automatic Token Validation**: Pre-validation in SplashActivity
- **JWT Expiration Handling**: Automatic detection and graceful session cleanup
- **Repository-Level Security**: JWT validation across all API calls

### Authentication Implementation
```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            try {
                // Validate JWT by making a real API call
                val user = userRepository.getUserAttributes()
                if (user != null) {
                    // JWT valid, proceed to main app
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    // JWT invalid, redirect to login
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            } catch (e: Exception) {
                // Handle JWT expiration
                authRepository.clearSession()
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
```

## 🎯 Dependency Injection with Hilt

### Application Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClient()
    
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
```

## 🚀 Navigation Architecture

### NavigationManager Utility
```kotlin
object NavigationManager {
    fun navigateToCourseTasks(activity: Activity, courseId: String) {
        val fragment = CourseFragment()
        val args = Bundle().apply {
            putString("COURSE_ID", courseId)
        }
        fragment.arguments = args
        
        val fragmentManager = (activity as FragmentActivity).supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
            
        // Hide main navigation during course view
        activity.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.GONE
        activity.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.VISIBLE
    }
}
```

## 📊 State Management Strategy

### UI State Patterns
```kotlin
// Generic UI State
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

// Feature-Specific State
sealed class CourseState {
    object Loading : CourseState()
    data class Success(
        val courses: List<Course>,
        val filteredCourses: List<Course>,
        val searchQuery: String = "",
        val selectedDifficulty: String? = null
    ) : CourseState()
    data class Error(val message: String) : CourseState()
}
```

## 🔄 Error Handling Architecture

### Repository-Level Error Handling
```kotlin
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ProfileRepository {
    
    override suspend fun getUserProfile(): Profile {
        return try {
            val response = supabaseClient.getUserProfile()
            if (response.isSuccessful) {
                response.body()!!.toDomain()
            } else {
                // Check for JWT expiration
                if (isJWTExpired(response)) {
                    handleJWTExpiration()
                    throw JWTExpiredException("Session expired")
                }
                throw ApiException("Failed to load profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error loading profile", e)
            throw e
        }
    }
    
    private fun isJWTExpired(response: Response<*>): Boolean {
        return response.code() == 401 && 
               response.errorBody()?.string()?.contains("JWT expired") == true
    }
    
    private suspend fun handleJWTExpiration() {
        authRepository.clearSession()
        // Trigger app-wide logout
    }
}
```

## 🧪 Testing Architecture

### Test Strategy
```kotlin
// ViewModel Testing
@Test
fun `loadProfile should emit success state when repository returns profile`() = runTest {
    // Given
    val expectedProfile = Profile(name = "Test User")
    coEvery { profileRepository.getUserProfile() } returns expectedProfile
    
    // When
    viewModel.loadProfile()
    
    // Then
    assertEquals(ProfileState.Success(expectedProfile), viewModel.profileState.value)
}
```

## 🚀 Performance Optimizations

### Memory Management
- **ViewBinding nullification** in fragment onDestroyView
- **StateFlow scope management** with appropriate lifecycles
- **Image loading optimization** with Glide caching
- **Database query optimization** with proper indexing

### UI Performance
- **RecyclerView optimization** with DiffUtil
- **Layout inflation caching** for frequently used views
- **Background processing** for heavy operations
- **Progressive loading** for large datasets

## 📱 Recent Architectural Improvements

### JWT Expiration Handling (Latest)
- **Proactive validation** in SplashActivity
- **Repository-wide detection** of expired tokens
- **Graceful session cleanup** across all layers
- **Automatic redirect** to login without UI flash

### Real-time UI Updates
- **Immediate feedback** for user actions (bell pepper purchases)
- **StateFlow reactive streams** for consistent state
- **Profile update synchronization** across fragments
- **Course navigation consistency** across all entry points

### Code Quality Enhancements
- **SOLID principles** implementation
- **DRY code structure** with shared utilities
- **Professional commenting** for maintainability
- **Clean Architecture adherence** throughout
