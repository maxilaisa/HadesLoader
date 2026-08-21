# HadesLoader - 8 Ball Pool Trajectory Injector

An Android application that provides accurate ball trajectory prediction for 8 Ball Pool games. The injector automatically adapts to different phone screen resolutions to ensure precise trajectory calculations and table positioning.

## Features

- **Game-Accurate Physics**: Uses real 8 Ball Pool physics constants extracted from actual game source code
- **Precise Trajectory Prediction**: Physics-based ball trajectory calculation with realistic collision detection
- **Resolution Adaptive**: Automatically scales to any phone screen resolution for accurate positioning
- **Real-time Overlay**: Displays trajectory prediction directly over the game screen
- **Customizable Power**: Adjustable shot power via slider control
- **Pocket Detection**: Visual indicators for table pockets
- **Cushion Physics**: Realistic ball bounce and cushion collision simulation
- **Background Service**: Runs as a foreground service without interfering with gameplay

## Technical Architecture

### Core Components

1. **TrajectoryCalculator** (`physics/TrajectoryCalculator.kt`)
   - Physics engine for ball movement and collision detection
   - Cushion and ball collision simulation
   - Trajectory prediction with configurable time steps
   - Pocket path prediction

2. **ResolutionManager** (`utils/ResolutionManager.kt`)
   - Screen resolution detection and scaling
   - Automatic table dimension calculation based on screen size
   - Coordinate transformation between different resolutions
   - Pocket position calculation

3. **TrajectoryOverlayService** (`service/TrajectoryOverlayService.kt`)
   - Android foreground service for screen overlay
   - Real-time trajectory rendering
   - System permission handling

4. **MainActivity** (`MainActivity.kt`)
   - User interface controls
   - Service management
   - Permission request handling

## Resolution Adaptation

The injector uses a reference resolution system to ensure accuracy across different devices:

- **Base Reference**: 1080x1920 (portrait)
- **Automatic Scaling**: Calculates scale factors based on actual screen dimensions
- **Uniform Scaling**: Maintains aspect ratio for accurate table representation
- **Dynamic Adjustment**: Real-time adaptation to screen orientation changes

### Supported Resolutions

- HD (720x1280)
- Full HD (1080x1920)
- Quad HD (1440x2560)
- 4K (2160x3840)
- Custom resolutions with automatic scaling

## Build Instructions

### Prerequisites

- Android Studio Hedgehog or later
- JDK 8 or higher
- Android SDK API 34
- Gradle 8.0

### Building the APK

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd HadesLoader
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the HadesLoader directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

4. **Build the APK**
   - Click Build > Build Bundle(s) / APK(s) > Build APK(s)
   - Or use the command line:
   ```bash
   ./gradlew assembleDebug
   ```

5. **Locate the APK**
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

### Building Release APK

1. **Configure signing**
   - Add your signing configuration in `app/build.gradle`
   - Or use the default debug signing for testing

2. **Build release version**
   ```bash
   ./gradlew assembleRelease
   ```

## Installation

1. **Enable Unknown Sources**
   - Go to Settings > Security
   - Enable "Install from Unknown Sources"

2. **Install the APK**
   - Transfer the APK to your device
   - Open the APK file
   - Follow the installation prompts

3. **Grant Permissions**
   - Open the HadesLoader app
   - Grant overlay permission when prompted
   - The app will guide you through the permission setup

## Usage

1. **Start the Service**
   - Open HadesLoader
   - Adjust the power slider as needed
   - Tap "Start Service"
   - Grant overlay permission if requested

2. **Enter the Game**
   - Open your 8 Ball Pool game
   - The trajectory overlay will appear on screen

3. **Aim and Shoot**
   - The red line shows the predicted ball path
   - The green dashed line shows aim direction
   - Blue circles indicate pocket positions
   - White circle shows cue ball position

4. **Stop the Service**
   - Return to HadesLoader
   - Tap "Stop Service" to disable the overlay

## Physics Model

The trajectory calculation uses realistic physics parameters:

- **Friction**: 0.985 (ball deceleration)
- **Cushion Restitution**: 0.9 (energy loss on cushion collision)
- **Ball Restitution**: 0.95 (energy loss on ball collision)
- **Time Step**: 0.016s (60 FPS simulation)
- **Minimum Velocity**: 0.1 (stop threshold)

## Permissions

The app requires the following permissions:

- `SYSTEM_ALERT_WINDOW`: Display overlay over other apps
- `FOREGROUND_SERVICE`: Run trajectory service in background
- `POST_NOTIFICATIONS`: Show service notifications

## Compatibility

- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)
- **Recommended RAM**: 2GB or higher
- **Screen Orientation**: Portrait and Landscape supported

## Troubleshooting

### Overlay Not Showing
- Ensure overlay permission is granted
- Check if the service is running in notifications
- Restart the service from the main screen

### Inaccurate Trajectory
- The system automatically adapts to your resolution
- Ensure you're using the latest version
- Check screen orientation is detected correctly

### Service Crashes
- Check Android version compatibility
- Ensure sufficient memory is available
- Review logcat for error messages

## Development

### Project Structure
```
HadesLoader/
├── app/
│   ├── src/main/
│   │   ├── java/com/hadesloader/poolinjector/
│   │   │   ├── MainActivity.kt
│   │   │   ├── physics/
│   │   │   │   └── TrajectoryCalculator.kt
│   │   │   ├── service/
│   │   │   │   └── TrajectoryOverlayService.kt
│   │   │   └── utils/
│   │   │       └── ResolutionManager.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   └── values/
│   │   │       ├── colors.xml
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

### Modifying Physics Parameters

Edit `TrajectoryCalculator.kt` to adjust physics:

```kotlin
private val friction = 0.985f          // Ball deceleration
private val cushionRestitution = 0.9f  // Cushion bounce
private val ballRestitution = 0.95f    // Ball collision
```

### Adjusting Base Resolution

Edit `ResolutionManager.kt` to change reference resolution:

```kotlin
private val baseWidth = 1080f
private val baseHeight = 1920f
```

## License

This project is for educational purposes only. Use responsibly and in accordance with game terms of service.

## Disclaimer

This software is intended for educational and research purposes. The developers are not responsible for any misuse of this software. Always respect game terms of service and fair play policies.