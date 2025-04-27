# Running Tracker

A modern Android application for tracking your workouts, including running, walking, cycling, swimming, and other activities. Built with Jetpack Compose and following modern Android development practices.

## Features

- 🏃‍♂️ Track multiple types of workouts (Running, Walking, Cycling, Swimming, Other)
- 📍 Real-time location tracking with route mapping
- 📊 Automatic calculation of workout metrics:
  - Distance traveled
  - Calories burned
  - Estimated heart rate
  - Workout duration
- 🗺️ Interactive map showing your workout route
- 📱 Modern Material 3 UI with Jetpack Compose
- 🔐 User authentication (login/register)
- 💾 Workout history and statistics

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: Kotlin Flow
- **Location Services**: Google Location Services
- **Maps**: Google Maps for Android
- **API Communication**: Retrofit
- **Dependency Injection**: Manual DI (can be migrated to Hilt)

## Prerequisites

- Android Studio Hedgehog | 2023.1.1 or newer
- Android SDK 34 (Android 14)
- Kotlin 1.9.0 or newer
- Google Maps API key

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/RunningTracker.git
   ```

2. Open the project in Android Studio

3. Add your Google Maps API key:
   - Create a new file `local.properties` in the root directory
   - Add your API key:
     ```
     MAPS_API_KEY=your_api_key_here
     ```

4. Build and run the app

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/kbtc/runningtracker/
│   │   │       ├── app/
│   │   │       │   ├── api/           # API interfaces and data classes
│   │   │       │   ├── data/          # Data models
│   │   │       │   ├── di/            # Dependency injection
│   │   │       │   ├── location/      # Location services
│   │   │       │   ├── repository/    # Data repositories
│   │   │       │   ├── ui/            # UI components and screens
│   │   │       │   └── theme/         # App theming
│   │   │       └── MainActivity.kt    # App entry point
│   │   └── res/                       # Resources
│   └── test/                          # Test files
```

## Usage

1. **Authentication**
   - Register a new account or login with existing credentials
   - User data is securely stored and managed

2. **Starting a Workout**
   - Tap "Start New Workout"
   - Select workout type (Running, Walking, Cycling, etc.)
   - The app will start tracking your location and workout metrics

3. **During Workout**
   - View your current route on the map
   - See real-time workout statistics
   - The app continues tracking in the background

4. **Ending a Workout**
   - Tap "End Workout" when finished
   - View your workout summary
   - Workout data is automatically saved

5. **Viewing History**
   - Access your workout history from the main screen
   - View detailed statistics for each workout
   - See your route on the map

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Google Maps API for location tracking and mapping
- Android Jetpack for modern Android development tools
- Material Design 3 for the UI components 