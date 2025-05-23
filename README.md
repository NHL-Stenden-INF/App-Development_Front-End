# AppDev NHL - Frontend (Android)

This is the Android frontend application for the AppDev NHL project. It allows users to register, log in, and interact with the application's features through a comprehensive interface with multiple sections and functionalities.

## Project Structure

-   `/app/src/main/java/com/nhlstenden/appdev/`: Contains the Kotlin source code for the Android activities, fragments, and UI components.
    -   `LoginActivity.kt`: Handles user login.
    -   `RegisterActivity.kt`: Handles new user registration.
    -   `MainActivity.kt`: The main container activity housing navigation between fragments.
    -   `SupabaseClient.kt`: Configures and manages communication with Supabase backend services.
    -   `AnimatedButton.kt`: Custom UI component for interactive buttons.
    -   `HomeFragment.kt`: Home screen display.
    -   `ProfileFragment.kt`: User profile management.
    -   `FriendsFragment.kt`: Friends list and social features.
    -   `TasksFragment.kt`: Task management interface.
    -   `CoursesFragment.kt`: Courses overview.
    -   `CourseTopicsFragment.kt`: Individual course topic details.
    -   `ProgressFragment.kt`: User progress tracking and visualization.
    -   `RewardsFragment.kt`: User rewards and achievements.
    -   `QRScannerActivity.kt`: QR code scanning functionality.
    -   `ImageCropActivity.kt`: Image editing for profile pictures or uploads.
    -   `models/`: Data models and managers.
        -   `CourseModels.kt`: Data classes for course-related information.
        -   `RewardsManager.kt`: Logic for managing user rewards.
-   `/app/src/main/res/`: Contains Android resources.
    -   `layout/`: XML layout files for activities and fragments.
    -   `drawable/`: Image assets and custom drawables.
    -   `values/`: XML files for strings, colors, styles, and themes.
    -   `navigation/`: Navigation graphs for the application.
    -   `animator/`, `anim/`: Animation resources.
-   `/app/build.gradle.kts`: Gradle build script for the application module.

## Prerequisites

-   Android Studio (latest stable version recommended)
-   Android SDK (target SDK is 33, min SDK is 33 - ensure these are installed via Android Studio's SDK Manager)
-   An Android Emulator (configured in Android Studio) or a physical Android device (with USB debugging enabled)
-   The backend services (Supabase) must be properly configured and accessible.

## Setup and Running the Application

1.  **Clone the Repository:**
    ```bash
    # If you haven't already, clone the main project repository
    git clone https://github.com/NHL-Stenden-INF/App-Development_Front-End
    cd App-Development_Front-End
    ```

2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open" or "Open an Existing Project".
    *   Navigate to the `App-Development_Front-End` directory and open it.
    *   Android Studio will sync the Gradle project. This might take a few minutes.

3.  **Configure Backend Connection:**
    *   The application uses Supabase as a backend service. Ensure that the `SupabaseClient.kt` file has the correct configuration for connecting to your Supabase instance.

4.  **Build the Application:**
    *   In Android Studio, select "Build" > "Make Project" or click the "Make Project" button (often a hammer icon).

5.  **Run the Application:**
    *   Select a target device (emulator or physical device) from the dropdown menu in the toolbar.
    *   Click the "Run 'app'" button (often a green play icon) or select "Run" > "Run 'app'".
    *   The application will be installed and launched on the selected device/emulator.

## Features

-   User registration and authentication
-   Home dashboard with activity overview
-   Course browsing and topic exploration
-   Task management
-   Progress tracking with visual charts
-   Social features including friends management
-   Rewards and achievements system
-   QR code scanning functionality
-   Profile management and customization

## Libraries Used

The application uses several key libraries and frameworks:

-   **Navigation Component**: For fragment-based navigation
-   **Retrofit & Gson**: For API communication
-   **Glide**: For image loading and processing
-   **Lottie**: For advanced animations
-   **MPAndroidChart**: For data visualization
-   **QRCode-Kotlin & ZXing**: For QR code generation and scanning
-   **Supabase SDK**: For backend communication

## Development Guidelines

When contributing to this project, please adhere to the existing code style and patterns. The application follows a fragment-based architecture with a single main activity serving as the container for most UI components.

---
