# Contributing Guidelines

Welcome to the App Development Front-End project! This document provides comprehensive guidelines for contributing to our Android learning application.

## 🌿 Development Workflow

### Branch Strategy
We follow a simplified Git flow suitable for educational projects:

```
main (production-ready)
├── dev (integration branch)
├── feat-jwt-expiration-fix
├── feat-bell-pepper-ui-updates
├── feat-course-navigation-consistency
└── bugfix/profile-picture-sync
```

**Branch Types:**
- `main` - Stable, production-ready code
- `dev` - Integration branch for features
- `feat-*` - New features and enhancements
- `bug-*` - Bug fixes and issue resolutions

### Contribution Process
1. **Create a feature branch**:
   ```bash
   git checkout dev
   git pull upstream dev
   git checkout -b feat-your-amazing-feature
   ```

2. **Make your changes** following our coding standards
3. **Test thoroughly** (unit tests, integration tests, manual testing or whatever tickles your fancy. Make sure it works)
4. **Commit with descriptive messages**:
   ```bash
   git commit -m "Add JWT expiration handling to ProfileRepository
    ,Implement automatic detection of expired tokens
    ,Add graceful session cleanup on 401 responses
    ,Include comprehensive logging for debugging
       "
   ```

5. **Push and create pull request**:
   ```bash
   git push origin feat-your-amazing-feature
   ```

## 📝 Commit Message Standards

### Examples
```bash
# Good commit messages
feat(auth): implement JWT expiration handling in repositories
fix(ui): resolve bell pepper purchase visual feedback delay
refactor(home): clean up unnecessary comments and improve documentation
test(profile): add comprehensive unit tests for ProfileViewModel

# Bad commit messages
fix stuff
update code
changes
working version
```

## 🏗 Architecture Guidelines

### Current Architecture (MVVM + Clean Architecture)
Our application follows **MVVM with Clean Architecture** principles:

```kotlin
// ✅ Good: Repository implementation with proper error handling
@Singleton
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
}
```

### State Management
Use **StateFlow** for reactive UI updates:

```kotlin
// ✅ Good: StateFlow with proper error handling
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
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

### Dependency Injection with Hilt
All dependencies must be provided through Hilt:

```kotlin
// ✅ Good: Proper repository binding
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository
}
```

## 🎨 Code Quality Standards

### SOLID & DRY Principles
We strictly follow **SOLID** and **DRY** principles:

```kotlin
// ✅ Good: Single Responsibility Principle
class JWTExpirationHandler @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    fun isJWTExpired(response: Response<*>): Boolean {
        return response.code() == 401 && 
               response.errorBody()?.string()?.contains("JWT expired") == true
    }
    
    suspend fun handleJWTExpiration() {
        authRepository.clearSession()
        // Trigger app-wide logout
    }
}

// ❌ Bad: Multiple responsibilities in one class
class ProfileHandler {
    fun loadProfile() { /* profile logic */ }
    fun handleJWT() { /* JWT logic */ }
    fun updateUI() { /* UI logic */ }
    fun validateData() { /* validation logic */ }
}
```

### Error Handling Standards
All error scenarios must be properly handled:

```kotlin
// ✅ Good: Comprehensive error handling
private suspend fun loadUserData() {
    try {
        _state.value = UiState.Loading
        val user = userRepository.getCurrentUser()
        _state.value = UiState.Success(user)
    } catch (e: JWTExpiredException) {
        Log.w("ViewModel", "Session expired, redirecting to login")
        _state.value = UiState.Error(e)
        // Handle specific JWT expiration
    } catch (e: NetworkException) {
        Log.e("ViewModel", "Network error loading user data", e)
        _state.value = UiState.Error(e)
    } catch (e: Exception) {
        Log.e("ViewModel", "Unexpected error loading user data", e)
        _state.value = UiState.Error(e)
    }
}
```

### Documentation Standards
We follow **Proffesional programming level** documentation:

```kotlin
// ✅ Good: Function-level comments explaining purpose and context
// Load and display courses that the user is currently progressing through
private fun setupContinueLearning(userData: User) {
    lifecycleScope.launch {
        try {
            val courses = withContext(Dispatchers.IO) {
                courseRepositoryImpl.getCourses(userData)
            }
            // Filter for active courses (progress > 0 and < totalTasks)
            val activeCourses = courses.filter { course ->
                course.progress > 0 && course.progress < course.totalTasks
            }
            updateUI(activeCourses)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up continue learning", e)
        }
    }
}

// ❌ Bad: Obvious or redundant comments
private fun setupContinueLearning(userData: User) {
    // Launch coroutine
    lifecycleScope.launch {
        // Try to get courses
        try {
            // Get courses from repository
            val courses = courseRepositoryImpl.getCourses(userData)
            // Filter courses
            val activeCourses = courses.filter { /* filter logic */ }
        } catch (e: Exception) {
            // Lol throw error (╯°□°）╯︵ ┻━┻
            Log.e("TAG", "Error", e)
        }
    }
}
```

## 🔧 Build and Validation

### Pre-commit Checklist
Before submitting any changes, ensure:

- [ ] **Code compiles** without warnings
- [ ] **All tests pass** (unit, integration, UI)
- [ ] **Lint checks pass** (ktlint, Android lint)
- [ ] **No unused imports** or variables
- [ ] **Documentation updated** if adding new features
- [ ] **Architecture compliance** verified
- [ ] **Performance impact** considered

### Validation Commands
```bash
# Build and test everything
./gradlew clean build test

# Run lint checks
./gradlew ktlintCheck
./gradlew lint

# Check for dependency updates
./gradlew dependencyUpdates

# Generate coverage reports
./gradlew jacocoTestReport
```

## 🤝 Code Review Guidelines

### What Reviewers Look For
- **Architecture compliance** with MVVM + Clean Architecture
- **Proper error handling** and edge case coverage
- **Performance implications** of changes
- **UI/UX consistency** with existing patterns
- **Security considerations** (especially authentication)

### Review Checklist
- [ ] Code follows established patterns
- [ ] All edge cases handled appropriately
- [ ] Documentation updated
- [ ] No breaking changes without discussion
- [ ] Performance impact assessed
- [ ] Security implications considered