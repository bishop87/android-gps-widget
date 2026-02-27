# GpsTracker

GpsTracker is a native Android application designed to accurately capture and send the device's GPS position to a remote backend HTTP API. It is built with a focus on reliability and flexibility, supporting Android 8.1 (API 27) and above.

## Features

- **Multiple Tracking Triggers:**
  - **Manual:** Send location instantly via the app's home screen.
  - **Scheduled Background Tracking:** Configurable time interval using a reliable Foreground Service and WorkManager fallback.
  - **Home Screen Widget:** A quick-action home screen widget to trigger a location update.
  - **Overlay Button:** An always-on-top floating button to trigger a location update without leaving your current app.
- **High Accuracy & Reliability:** Utilizes Google's `FusedLocationProviderClient` to prioritize location accuracy while managing battery consumption.
- **Robust Background Execution:** Survives system optimizations and Doze mode via Foreground Services and persistent notifications.
- **Secure Configuration:** Safely stores backend API URLs, credentials, and user settings using Android's `EncryptedSharedPreferences`.
- **Resilient Networking:** Features automatic request retries, timeout management, and graceful error handling.

## Tech Stack

- **Platform:** Native Android (Min SDK 27)
- **Language:** Kotlin
- **Architecture:** MVVM (recommended)
- **Key Libraries:** FusedLocationProviderClient, WorkManager, Retrofit/OkHttp, EncryptedSharedPreferences

## Getting Started

1. Clone the repository and open the project in **Android Studio**.
2. Sync dependencies and build the project.
3. Run the app on a physical Android device or an emulator equipped with Google Play Services.
4. Upon the first launch, follow the prompts to grant the necessary dynamic permissions (Location, Notifications, Display over other apps).
5. Open the **Settings** screen to configure your custom backend API URL, credentials, and background tracking intervals.

## Privacy & Permissions
This application requires runtime permissions for precise location access, background location (if scheduled tracking is enabled), notifications, and system alert windows for the overlay functionality. All data transmission uses HTTPS to ensure privacy.
