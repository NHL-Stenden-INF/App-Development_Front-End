# Code Conventions

This document outlines the coding standards, naming conventions, and best practices used in our Android learning application. All code should follow SOLID and DRY principles.

## 📱 Project Structure

### Package Organization
```
com.nhlstenden.appdev/
├── core/                      # Shared core functionality
│   ├── models/               # Domain models (User, Course, etc.)
│   ├── repositories/         # Repository interfaces
│   └── utils/                # Utility classes
├── features/                 # Feature modules
│   ├── auth/                 # Authentication & login
│   ├── home/                 # Home screen with continue learning
│   ├── courses/              # Course browsing and details
│   ├── progress/             # Progress tracking and charts
│   ├── profile/              # User profile management
│   ├── friends/              # Social features
│   ├── rewards/              # Gamification and achievements
│   └── task/                 # Task execution and bell pepper system
├── shared/                   # Shared UI components
│   ├── components/           # Reusable custom views
│   └── ui/                   # Base fragments and utilities
└── supabase/                 # Backend communication
```

## 🏷 Naming Conventions

### Files and Classes

#### Core Components
```kotlin
// ✅ Activities
MainActivity.kt
SplashActivity.kt
LoginActivity.kt

// ✅ Fragments  
HomeFragment.kt
ProfileFragment.kt
CoursesFragment.kt

// ✅ ViewModels
ProfileViewModel.kt
CourseViewModel.kt
AuthViewModel.kt

// ✅ Repository Implementations
ProfileRepositoryImpl.kt
CourseRepositoryImpl.kt
AuthRepositoryImpl.kt

// ✅ Repository Interfaces
interface ProfileRepository
interface CourseRepository
interface AuthRepository
```

#### Data Classes and Models
```kotlin
// ✅ Domain Models
data class User(...)
data class Course(...)
data class Profile(...)

// ✅ UI State Classes
sealed class ProfileState { ... }
sealed class UiState<out T> { ... }
data class HomeCourse(...)

// ✅ Data Transfer Objects
data class UserDto(...)
data class CourseDto(...)
```

#### Adapters and Custom Views
```kotlin
// ✅ RecyclerView Adapters
HomeCourseAdapter.kt
CourseProgressAdapter.kt
FriendAdapter.kt

// ✅ Dialog Fragments
BuyBellPepperDialogFragment.kt
DisplayNameDialogFragment.kt

// ✅ Utility Classes
NavigationManager.kt
LevelCalculator.kt
StreakManager.kt
```

### Layout Files

#### Screen Layouts
```xml
<!-- ✅ Activities -->
activity_main.xml
activity_splash.xml
activity_login.xml

<!-- ✅ Fragments -->
fragment_home.xml
fragment_profile.xml
fragment_courses.xml
fragment_progress.xml

<!-- ✅ Custom Views -->
view_profile_header.xml
view_streak_counter.xml
```

#### Item Layouts
```xml
<!-- ✅ RecyclerView Items -->
item_course.xml
item_friend.xml
item_progress.xml

<!-- ✅ Dialogs -->
dialog_buy_bell_pepper.xml
dialog_display_name.xml

<!-- ✅ Includes -->
include_loading_state.xml
include_error_state.xml
```

### Resource Naming

#### Drawables and Icons
```xml
<!-- ✅ Icons -->
ic_home.xml
ic_profile.xml
ic_bell_pepper.xml
ic_fire.xml

<!-- ✅ Backgrounds -->
bg_button_primary.xml
bg_card_elevated.xml
day_circle_active.xml
day_circle_inactive.xml

<!-- ✅ Shapes -->
shape_rounded_corners.xml
shape_circle.xml
```

#### Colors and Dimensions
```xml
<!-- ✅ Colors -->
<color name="colorPrimary">#...</color>
<color name="colorAccent">#...</color>
<color name="textColorPrimary">#...</color>

<!-- ✅ Dimensions -->
<dimen name="margin_standard">16dp</dimen>
<dimen name="text_size_large">18sp</dimen>
<dimen name="elevation_card">4dp</dimen>
```

#### Strings
```xml
<!-- ✅ Screen Titles -->
<string name="title_home">Home</string>
<string name="title_courses">Courses</string>

<!-- ✅ Action Buttons -->
<string name="action_save">Save</string>
<string name="action_cancel">Cancel</string>

<!-- ✅ Messages -->
<string name="message_loading">Loading...</string>
<string name="error_network">Network error occurred</string>
```

## 🎨 Code Style Guidelines

### Kotlin Conventions

#### Basic Formatting
```kotlin
// ✅ Good: Proper indentation and spacing
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

#### Variable and Function Naming
```kotlin
// ✅ Good: Descriptive names
val activeCourses = courses.filter { course ->
    course.progress > 0 && course.progress < course.totalTasks
}

// Create and display the weekly streak counter with visual indicators
private fun setupStreakCounter(view: View) { ... }

// Check if the app is currently in dark mode
private fun isNightMode(): Boolean { ... }

// ❌ Bad: Unclear names
val ac = courses.filter { c -> c.p > 0 && c.p < c.t }
private fun setup(v: View) { ... }
private fun check(): Boolean { ... }
```

#### StateFlow and Coroutines
```kotlin
// ✅ Good: Proper StateFlow usage
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {
    
    private val _courseState = MutableStateFlow<CourseState>(CourseState.Loading)
    val courseState: StateFlow<CourseState> = _courseState.asStateFlow()
    
    fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = courseRepository.getCourses()
                _courseState.value = CourseState.Success(courses)
            } catch (e: Exception) {
                _courseState.value = CourseState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// ✅ Good: Proper coroutine scope usage in fragments
// Fetch course data from the repository in background thread
private fun loadData() {
    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                courses = courseRepositoryImpl.getCourses(currentUser)
                withContext(Dispatchers.Main) {
                    setupPieChart()
                    setupCourseList()
                }
            }
        } catch (e: RuntimeException) {
            Log.e("ProgressFragment", "Error loading data", e)
        }
    }
}
```

### Error Handling Standards

#### Repository Level
```kotlin
// ✅ Good: Comprehensive error handling with JWT expiration detection
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
```

#### Fragment Level
```kotlin
// ✅ Good: UI error handling with user feedback
private fun observeViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
        profileViewModel.profileState.collect { state ->
            when (state) {
                is ProfileState.Loading -> {
                    showLoadingState()
                }
                is ProfileState.Success -> {
                    hideLoadingState()
                    updateProfileUI(state.profile)
                }
                is ProfileState.Error -> {
                    hideLoadingState()
                    showErrorMessage(state.message)
                    Log.e("ProfileFragment", "Profile loading failed: ${state.message}")
                }
            }
        }
    }
}
```

## 📝 Documentation Standards

### Function-Level Comments (2nd Year Student Level)
```kotlin
// ✅ Good: Meaningful comments explaining purpose and business logic
// Initialize UI components and load user profile data
fun setupUI(view: View) { ... }

// Monitor profile state changes and handle invalid display names
private fun observeViewModel() { ... }

// Create and configure the pie chart showing overall task completion
private fun setupPieChart() { ... }

// Load and display courses that the user is currently progressing through
private fun setupContinueLearning(userData: User) { ... }

// ❌ Bad: Obvious or redundant comments
// Set up UI
fun setupUI(view: View) { ... }

// Observe view model
private fun observeViewModel() { ... }

// Set up pie chart
private fun setupPieChart() { ... }
```

### Class-Level Documentation
```kotlin
/**
 * Fragment responsible for displaying user progress across all courses.
 * Features:
 * - Overall progress pie chart
 * - Individual course progress list
 * - Real-time progress updates
 * - Navigation to course details
 */
class ProgressFragment : Fragment() { ... }

/**
 * Repository implementation for user profile operations.
 * Handles:
 * - Profile data fetching from Supabase
 * - JWT expiration detection and handling
 * - Local profile caching
 * - Profile picture management
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(...) : ProfileRepository { ... }
```

## 🎯 Architecture Compliance

### MVVM Pattern Implementation
```kotlin
// ✅ Good: Clear separation of concerns
// Fragment (View) - Only UI logic
class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.loadProfile()
    }
    
    private fun observeViewModel() {
        // UI state observation only
    }
}

// ViewModel - Business logic and state management
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    // State management and business logic only
}

// Repository - Data operations
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {
    // Data fetching and caching logic only
}
```

### Dependency Injection
```kotlin
// ✅ Good: Proper Hilt usage
@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel()

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ProfileRepository
```

## 🧪 Testing Conventions

### Test File Naming
```kotlin
// ✅ Unit Tests
ProfileViewModelTest.kt
CourseRepositoryTest.kt
AuthRepositoryImplTest.kt

// ✅ Integration Tests
ProfileIntegrationTest.kt
CourseFlowTest.kt

// ✅ UI Tests
HomeFragmentUITest.kt
LoginFlowUITest.kt
```

### Test Method Naming
```kotlin
// ✅ Good: Descriptive test names
class ProfileViewModelTest {
    
    @Test
    fun `loadProfile should emit success state when repository returns profile`() = runTest {
        // Given, When, Then
    }
    
    @Test
    fun `loadProfile should emit error state when JWT expires`() = runTest {
        // Given, When, Then
    }
    
    @Test
    fun `updateDisplayName should save new name and refresh profile`() = runTest {
        // Given, When, Then
    }
}
```

## 🔧 Build and Git Conventions

### Commit Messages
```bash
# ✅ Good: Clear, descriptive commits
feat(auth): implement JWT expiration handling in repositories
fix(ui): resolve bell pepper purchase visual feedback delay
refactor(home): clean up unnecessary comments and improve documentation
test(profile): add comprehensive unit tests for ProfileViewModel

# ❌ Bad: Unclear commits
fix stuff
update
changes
working now
```

### Branch Naming
```bash
# ✅ Good: Descriptive branch names
feature/jwt-expiration-handling
feature/bell-pepper-visual-updates
bugfix/profile-picture-sync-issue
refactor/home-fragment-comments
```

## 🚀 Performance Best Practices

### Memory Management
```kotlin
// ✅ Good: Proper ViewBinding nullification
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}

// ✅ Good: Proper StateFlow scope management
val filteredCourses = combine(
    courseState,
    searchQuery,
    selectedDifficulty
) { state, query, difficulty ->
    // Combine logic
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
)
```

### UI Performance
```kotlin
// ✅ Good: RecyclerView optimization with proper ViewHolder
class CourseAdapter : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val progressBar: NumberProgressBar = view.findViewById(R.id.progressBar)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseTitle.text = course.title
        holder.progressBar.progress = course.progressPercent
        Glide.with(holder.courseImage.context)
            .load(course.imageResId)
            .into(holder.courseImage)
    }
}
```

## 🔐 Security Standards

### JWT Handling
```kotlin
// ✅ Good: Secure JWT validation and expiration handling
class AuthRepository @Inject constructor(
    private val encryptedPrefs: EncryptedSharedPreferences
) {
    
    fun isTokenValid(): Boolean {
        val token = encryptedPrefs.getString(JWT_TOKEN_KEY, null)
        return token != null && !isTokenExpired(token)
    }
    
    private fun isTokenExpired(token: String): Boolean {
        try {
            val jwt = JWT.decode(token)
            return jwt.expiresAt.before(Date())
        } catch (e: Exception) {
            Log.w("AuthRepository", "Invalid JWT token", e)
            return true
        }
    }
    
    suspend fun clearSession() {
        encryptedPrefs.edit()
            .remove(JWT_TOKEN_KEY)
            .remove(USER_ID_KEY)
            .apply()
    }
}
```

### API Security
```kotlin
// ✅ Good: Secure API communication with proper error handling
class ApiClient @Inject constructor(
    private val httpClient: OkHttpClient
) {
    
    private fun addAuthHeaders(builder: Request.Builder) {
        val token = authRepository.getValidToken()
        if (token != null) {
            builder.addHeader("Authorization", "Bearer $token")
        }
    }
    
    suspend fun makeAuthenticatedRequest(request: Request): Response {
        val response = httpClient.newCall(request).execute()
        
        // Check for JWT expiration
        if (response.code == 401) {
            authRepository.handleTokenExpiration()
            throw UnauthorizedException("Session expired")
        }
        
        return response
    }
}
```

## 📋 Code Review Checklist

Before submitting code for review, ensure:

### Architecture & Design
- [ ] Follows MVVM + Clean Architecture patterns
- [ ] Proper separation of concerns
- [ ] Dependencies injected via Hilt
- [ ] StateFlow used for reactive programming

### Code Quality
- [ ] SOLID and DRY principles followed
- [ ] Meaningful variable and function names
- [ ] Appropriate error handling
- [ ] No code duplication

### Security
- [ ] JWT tokens handled securely
- [ ] Sensitive data encrypted
- [ ] API endpoints protected
- [ ] Input validation implemented

### Testing
- [ ] Unit tests for ViewModels and Repositories
- [ ] Test coverage for error scenarios
- [ ] Integration tests for data flow
- [ ] UI tests for critical user journeys

### Documentation
- [ ] Function-level comments for complex logic
- [ ] Clear class-level documentation
- [ ] README updates for new features
- [ ] Architecture documentation updates

### Performance
- [ ] Memory leaks prevented
- [ ] UI performance optimized
- [ ] Background processing appropriate
- [ ] Resource management proper

### Current Application Features
- [ ] JWT expiration handling implemented
- [ ] Course navigation consistency maintained
- [ ] Bell pepper visual updates working
- [ ] Profile picture synchronization functional
- [ ] Streak counter accuracy verified
