# HadesLoader - 8 Ball Pool Direct Injector

An Android application that provides accurate ball trajectory prediction for 8 Ball Pool games through direct APK modification. The injector modifies the game APK itself to include trajectory prediction functionality, eliminating the need for overlay permissions.

## Features

- **Direct APK Injection**: Modifies the game APK to include trajectory prediction
- **No Overlay Permissions**: Trajectory is built into the game, no overlay needed
- **Game-Accurate Physics**: Uses real 8 Ball Pool physics constants extracted from actual game source code
- **Precise Trajectory Prediction**: Physics-based ball trajectory calculation with realistic collision detection
- **Resolution Adaptive**: Automatically adapts to any phone screen resolution
- **Customizable Power**: Adjustable shot power via slider control
- **Pocket Detection**: Visual indicators for table pockets
- **Cushion Physics**: Realistic ball bounce and cushion collision simulation
- **Automatic Game Detection**: Automatically detects and verifies 8 Ball Pool installation
- **Version Compatibility**: Checks for compatible game versions before injection

## Technical Architecture

### Core Components

1. **ApkModifier** (`injector/ApkModifier.kt`)
   - APK extraction and modification engine
   - Working directory management
   - Trajectory code injection into game APK
   - Modded APK preparation for installation

2. **ModInstaller** (`injector/ModInstaller.kt`)
   - Game installation detection
   - Version compatibility checking
   - APK path extraction
   - Injection environment preparation

3. **MainActivity** (`MainActivity.kt`)
   - Injection process UI and control
   - Game installation verification
   - Progress tracking and user feedback
   - Modded APK installation handling

4. **TrajectoryCalculator** (`physics/TrajectoryCalculator.kt`)
   - Physics engine for ball movement and collision detection
   - Cushion and ball collision simulation
   - Trajectory prediction with configurable time steps
   - Designed for integration into game's physics system

5. **ResolutionManager** (`utils/ResolutionManager.kt`)
   - Screen resolution detection and scaling
   - Automatic table dimension calculation based on screen size
   - Coordinate transformation between different resolutions
   - Pocket position calculation

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

1. **Install 8 Ball Pool**
   - Download and install the official 8 Ball Pool game from Google Play Store
   - Ensure you have version 5.0.0 or higher installed

2. **Enable Unknown Sources**
   - Go to Settings > Security
   - Enable "Install from Unknown Sources"

3. **Install HadesLoader APK**
   - Transfer the HadesLoader APK to your device
   - Open the APK file
   - Follow the installation prompts

4. **Injection Process**
   - Open the HadesLoader app
   - The app will automatically detect if 8 Ball Pool is installed
   - Tap "Inject Mod" to modify the game APK
   - Wait for the injection process to complete
   - Tap "Install Modded APK" to install the modified game
   - Install the modded APK when prompted by the system

## Usage

1. **Verify Game Installation**
   - Open HadesLoader
   - Check the "Game Status" indicator
   - Green = 8 Ball Pool detected and ready for injection
   - Red = 8 Ball Pool not found
   - Orange = Incompatible game version

2. **Inject Trajectory Mod**
   - Tap "Inject Mod" button
   - Wait for the injection process to complete
   - Progress bar will show injection status
   - When complete, "Install Modded APK" button will be enabled

3. **Install Modded Game**
   - Tap "Install Modded APK" button
   - System will prompt for APK installation
   - Install the modded version of 8 Ball Pool
   - The modded game will replace the original

4. **Play with Trajectory**
   - Open the modded 8 Ball Pool game
   - Trajectory prediction will be built into the game
   - No overlay needed - trajectory appears as part of game
   - Play normally with trajectory assistance

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
│   │   │   ├── Constants.kt
│   │   │   ├── physics/
│   │   │   │   └── TrajectoryCalculator.kt
│   │   │   ├── service/
│   │   │   │   ├── TrajectoryOverlayService.kt
│   │   │   │   └── TrajectoryOverlayView.kt
│   │   │   ├── injector/
│   │   │   │   ├── GameAccessibilityService.kt
│   │   │   │   ├── GameMemoryReader.kt
│   │   │   │   └── ModInstaller.kt
│   │   │   ├── cue/
│   │   │   │   └── CueDetector.kt
│   │   │   └── utils/
│   │   │       └── ResolutionManager.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── accessibility_service_config.xml
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

## Architecture Documentation

For detailed technical information about the architecture, component interactions, and design decisions, see [ARCHITECTURE.md](ARCHITECTURE.md).