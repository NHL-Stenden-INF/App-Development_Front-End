# AppDev NHL - Frontend (Android)

This is the Android frontend application for the AppDev NHL project. It allows users to register, log in, and interact with the application's features (once logged in).

## Project Structure

-   `/app/src/main/java/com/nhlstenden/appdev/`: Contains the Kotlin source code for the Android activities, services, and data models.
    -   `LoginActivity.kt`: Handles user login.
    -   `RegisterActivity.kt`: Handles new user registration.
    -   `MainActivity.kt`: The main screen after a user logs in. (Placeholder for further features)
    -   `ApiService.kt`: Defines the Retrofit interface for communicating with the backend API.
    -   `RetrofitClient.kt`: Configures the Retrofit instance, including the backend's base URL.
    -   `NetworkModels.kt`: Contains data classes for network requests and responses.
-   `/app/src/main/res/`: Contains Android resources.
    -   `layout/`: XML layout files for activities.
    -   `drawable/`: Image assets and custom drawables.
    -   `values/`: XML files for strings, colors, styles, and themes.
    -   `animator/`, `anim/`: Animation resources.
-   `/app/build.gradle.kts`: Gradle build script for the application module.

## Prerequisites

-   Android Studio (latest stable version recommended)
-   Android SDK (target SDK is 33, min SDK is 33 - ensure these are installed via Android Studio's SDK Manager)
-   An Android Emulator (configured in Android Studio) or a physical Android device (with USB debugging enabled)
-   The [AppDev NHL Backend](https://github.com/NHL-Stenden-INF/App-Development_Back-End) must be running and accessible from the Android device/emulator.

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

3.  **Configure Backend IP Address (Important!):**
    *   Open the file `app/src/main/java/com/nhlstenden/appdev/RetrofitClient.kt`.
    *   Locate the `BASE_URL` constant:
        ```kotlin
        private const val BASE_URL = "http://10.0.2.2:3000/" // Emulator default
        ```
    *   **If using an Android Emulator:** `http://10.0.2.2:3000/` is usually correct if your backend is running on the same machine (localhost) as the emulator. `10.0.2.2` is a special alias to the host loopback interface.
    *   **If using a Physical Device:**
        *   Ensure your physical device and the machine running the backend are on the **same Wi-Fi network**.
        *   Find the local IP address of the machine running the backend (e.g., by using `ipconfig` on Windows or `ifconfig`/`ip addr` on Linux/macOS).
        *   Change `BASE_URL` to this IP address, e.g., `http://192.168.1.100:3000/` (replace `192.168.1.100` with your backend machine's actual local IP).
    *   **If your backend runs on a different port**, update the port number accordingly.

4.  **Build the Application:**
    *   In Android Studio, select "Build" > "Make Project" or click the "Make Project" button (often a hammer icon).

5.  **Run the Application:**
    *   Select a target device (emulator or physical device) from the dropdown menu in the toolbar.
    *   Click the "Run 'app'" button (often a green play icon) or select "Run" > "Run 'app'".
    *   The application will be installed and launched on the selected device/emulator.

## Features

-   User registration with username, email, and password.
-   User login with email and password using Basic Authentication.
-   Swipe gestures to navigate between Login and Register screens.
-   (Future features will be added to `MainActivity` and other parts of the app.)

## Interacting with the Backend

-   The application uses [Retrofit](https://square.github.io/retrofit/) for making HTTP requests to the backend API.
-   The `ApiService.kt` interface defines the API endpoints.
-   Authentication is handled by sending a Basic Auth header with user credentials.

---
