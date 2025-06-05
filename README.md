# AppDev NHL - Frontend (Android)

This is the Android frontend application for the AppDev NHL project. It allows users to register, log in, and interact with the application's features through a comprehensive interface with multiple sections and functionalities.

## Project Structure

```
appdev/
│
├── core/                   # Reusable/shared code across features
│   ├── components/         # Shared UI components (e.g., ImageCropActivity)
│   ├── di/                 # Dependency injection setup (e.g., Hilt modules)
│   ├── network/            # Retrofit setup, API service
│   ├── database/           # Room setup
│   ├── utils/              # Shared helpers, extensions
│   ├── repositories/       # Shared repositories/interfaces
│   ├── models/             # Shared domain models
│   ├── ui/
│   │   └── base/           # BaseFragment, BaseViewModel, etc.
│   └── AppConstants.kt     # Global constants
│
├── features/               # Each feature in its own folder
│   ├── friends/            # Friends management (includes FriendsFragment, utils, viewmodels, etc.)
│   ├── profile/            # Profile management (ProfileFragment, viewmodels, adapters, etc.)
│   ├── rewards/            # Rewards and achievements system
│   ├── courses/            # Course browsing and topic exploration
│   ├── progress/           # Progress tracking
│   ├── task/               # Task management
│   ├── login/              # Login and registration
│   └── home/               # Home dashboard
│
├── MainApplication.kt      # Application class
├── MainActivity.kt         # Main activity (fragment container)
└── supabase/               # Supabase client and related code
```

- All feature-specific code (fragments, viewmodels, adapters, repositories, etc.) is inside its respective feature folder.
- Shared code (UI components, base classes, models, utils, constants) is in `core/`.
- `AppConstants.kt` is now in `core/`.
- `FriendsFragment.kt` is now in `features/friends/`.

## SOLID Principles

| Principle                     | How it's simplified but respected                               |
| ----------------------------- | --------------------------------------------------------------- |
| **S** – Single Responsibility | Each file does one thing: e.g., ViewModel handles UI logic only |
| **O** – Open/Closed           | You can add new features without modifying others               |
| **L** – Liskov                | Interfaces (e.g., repositories) let you swap implementations    |
| **I** – Interface Segregation | Simple interfaces per feature, small and focused                |
| **D** – Dependency Inversion  | Core depends on interfaces; Hilt injects implementations        |

## Notes
- Each feature folder contains all files for that feature (UI, ViewModel, Repository, etc.).
- Shared code (network, DI, database, utils, shared models, base UI) lives in `core/`.
- Global constants go in `core/AppConstants.kt`.
- This structure is scalable, easy to navigate, and respects SOLID.

## Drawable Resource Naming Convention

To keep the `drawable` folder organized and maintainable, we follow strict naming conventions:

- **General Rule:**
  - The feature, page, or domain should always be the prefix for all drawable file names. This makes it easy to locate and group related resources.
  - Use lowercase letters and underscores.
  - Be descriptive and consistent.
  - For different file types (XML, PNG, GIF), keep the naming consistent across types.

- **Course Images:**
  - Prefix with `course_` followed by the course name or type.
  - Example: `course_html.xml`, `course_css.xml`, `course_sql.xml`

- **Profile & Lives:**
  - Prefix with `profile_` for all profile-related assets.
  - Example: `profile_lives_anim_one.png`, `profile_lives_zero_lifes.png`, `profile_placeholder.png`

- **Rewards & Achievements:**
  - Prefix with `reward_` or `achievement_` followed by the reward or achievement name.
  - Example: `reward_point_multiplier.xml`, `achievement_nightowl.xml`, `achievement_perfect.xml`, `achievement_css.xml`

- **Icons:**
  - Prefix with the feature or page, then `_icon_`, then the icon's purpose and optionally a size or variant.
  - Example: `home_icon_arrow_left.xml`, `profile_icon_placeholder.xml`, `rewards_icon_medal_gold.xml`, `qr_icon_code_scanner.xml`

- **Other Assets:**
  - Use the feature or page as a prefix, followed by a descriptive name.
  - Example: `mascot_static.xml`, `mascot.gif`, `app_logo.png`, `scanner_overlay.xml`, `input_background.xml`

This convention makes it easy to locate, reference, and maintain drawable resources, especially as the project grows. While the project may not yet be fully consistent with this convention, this is the intended standard moving forward.

# Courses/ Tasks/ Questions naming conventions

- **Courses file:**
  - All are stored in `coures.xml`
  - The title of a course is used as an ID to find the related `tasks` file
  - Title can contain spaces and capital letters

- **Task files**
  - Suffix with `_tasks`
  - Prefix with the title of the related course in lowercase and with ` ` replaced with `_`
  - The title of a topic is used as an ID to fin the related `questions` file

- **Question files**
  - Suffix with `_questions`
  - Prefix with the title of the related topic in lowercase and with ` ` replaced with `_`

## Prerequisites

- Android Studio (latest stable version recommended)
- Android SDK (target SDK is 33, min SDK is 33 - ensure these are installed via Android Studio's SDK Manager)
- An Android Emulator (configured in Android Studio) or a physical Android device (with USB debugging enabled)
- The backend services (Supabase) must be properly configured and accessible.

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
