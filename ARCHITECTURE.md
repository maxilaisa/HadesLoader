# HadesLoader Architecture Documentation

## Overview

HadesLoader is a direct 8 Ball Pool trajectory injector that modifies the game APK itself to include trajectory prediction functionality. This approach eliminates the need for overlay permissions and provides a more integrated experience where the trajectory system is built directly into the game.

## Key Architectural Changes

### 1. Removal of Overlay System
- **Previous Approach**: Used Android overlay permissions to display trajectory over the game screen
- **Current Approach**: Direct APK injection - trajectory code is built into the game itself
- **Benefits**: No overlay permissions needed, no accessibility services, cleaner integration

### 2. Direct APK Modification
- **Injection Process**: Modifies the actual 8 Ball Pool APK to include trajectory code
- **Installation Flow**: Creates a modded APK that users install instead of the original
- **Runtime Integration**: Trajectory calculation happens within the game's own rendering pipeline

## Core Components

### 1. MainActivity.kt
**Purpose**: Main UI controller for injection process

**Key Features**:
- Game installation verification via ModInstaller
- Injection process management
- Progress tracking and user feedback
- Modded APK installation handling
- No permission management (removed overlay/accessibility requirements)

**Workflow**:
1. Checks game installation on startup
2. Updates UI with game status (green/red/orange indicators)
3. Enables inject button based on game availability
4. Manages injection process with progress updates
5. Handles modded APK installation

### 2. ApkModifier.kt
**Purpose**: Core APK modification and injection engine

**Key Features**:
- Original APK extraction from installed game
- Working directory management
- Trajectory code injection into APK
- Modded APK preparation for installation
- Cleanup functionality

**Injection Process**:
```
Original Game APK → Extract to Work Directory → Inject Trajectory Code → Create Modded APK → Install
```

**Technical Implementation**:
- In production: Would use APKTool for decompilation/recompilation
- Would inject Smali code into game classes
- Would modify game's rendering pipeline
- Would integrate physics calculations
- Would sign the modded APK

### 3. ModInstaller.kt
**Purpose**: Game detection and version compatibility

**Key Features**:
- Package manager integration for game detection
- Version compatibility checking
- Sealed class status reporting
- APK path extraction

**Status States**:
- `Available(version)`: Game installed and compatible
- `NotInstalled`: Game not found on device
- `IncompatibleVersion(current, required)`: Game version too old

### 4. TrajectoryCalculator.kt
**Purpose**: Physics engine for trajectory calculation (to be injected)

**Key Features**:
- Realistic ball physics (friction, restitution)
- Cushion collision simulation
- Time-step based simulation (60 FPS)
- Trajectory point generation
- Designed for integration into game's physics system

**Integration Points**:
- Called from game's shot handling code
- Uses game's table dimensions
- Integrates with game's ball physics
- Renders trajectory within game's graphics pipeline

## Installation Flow

```
User installs HadesLoader APK
    ↓
MainActivity checks game installation via ModInstaller
    ↓
Status displayed in UI (Game Status indicator)
    ↓
If game available: Enable inject button
If not available: Show installation prompt
    ↓
User taps "Inject Mod"
    ↓
ApkModifier extracts original game APK
    ↓
Injection process begins (progress bar shown)
    ↓
Trajectory code injected into APK
    ↓
Modded APK created in cache directory
    ↓
User taps "Install Modded APK"
    ↓
System prompts for APK installation
    ↓
User installs modded APK
    ↓
Original game uninstalled/replaced with modded version
    ↓
Trajectory prediction built into game
```

## Injection Mechanism

### Direct APK Injection Approach

The implementation uses direct APK modification:

1. **APK Extraction**: Extract original game APK from device
2. **Decompilation**: Use APKTool to decompile to Smali code
3. **Code Injection**: Inject trajectory Smali code into game classes
4. **Rendering Integration**: Modify game's rendering pipeline to show trajectory
5. **Physics Integration**: Integrate trajectory calculation with game physics
6. **Recompilation**: Recompile modified APK with APKTool
7. **Signing**: Sign the modded APK for installation
8. **Installation**: Install modded APK to replace original game

### Technical Details

**Smali Injection Points**:
- Game activity initialization
- Shot handling methods
- Rendering pipeline
- Physics engine integration
- Touch event handling

**Physics Integration**:
- Uses game's existing physics constants
- Integrates with game's collision detection
- Maintains game's original behavior
- Adds trajectory rendering on top

**Rendering Integration**:
- Hooks into game's OpenGL rendering
- Draws trajectory lines in game's coordinate system
- Uses game's table dimensions and scaling
- Respects game's visual settings

## Configuration

### Constants.kt
Centralized configuration for game targeting:
```kotlin
object Constants {
    const val GAME_PACKAGE_NAME = "com.miniclip.eightballpool"
    const val MIN_GAME_VERSION = "5.0.0"
}
```

### Physics Parameters
Configurable in TrajectoryCalculator.kt:
```kotlin
private val friction = 0.985f          // Ball deceleration
private val cushionRestitution = 0.9f  // Cushion bounce
private val ballRestitution = 0.95f    // Ball collision
private val timeStep = 0.016f          // 60 FPS simulation
```

## User Experience

### Simplified Workflow
1. Install 8 Ball Pool from Play Store
2. Install HadesLoader APK
3. Open HadesLoader
4. See game status (green if ready)
5. Tap "Inject Mod"
6. Wait for injection to complete
7. Tap "Install Modded APK"
8. Install modded game when prompted
9. Play modded game with built-in trajectory

### Status Indicators
- **Green**: "8 Ball Pool X.X.X - Ready for injection" - Game detected and compatible
- **Red**: "8 Ball Pool not found" - Game not installed
- **Orange**: "Incompatible version (X.X.X)" - Game version too old

### Progress Tracking
- Progress bar during injection
- Status text updates (extracting, injecting, completing)
- Clear success/failure messages
- Cancel option during injection

## Security Considerations

### No Overlay System Benefits
- No sensitive permissions required
- No accessibility service monitoring
- No screen reading capabilities
- No foreground service overhead
- Clean installation process

### Current Security Model
- Local APK modification only
- No internet connectivity required
- Standard Android permissions (storage, install packages)
- No sensitive data collection
- No root access required

### APK Signing
- Modded APK must be properly signed
- Uses debug signing for development
- Production would need proper signing keys
- Maintains APK integrity

## Technical Benefits

### Simplified Architecture
- Reduced code complexity (no overlay services)
- No permission management
- No accessibility API integration
- Cleaner separation of concerns
- More reliable operation

### Improved User Experience
- No permission hassles
- Clear installation process
- Built-in trajectory (no overlay issues)
- Better performance (no overlay overhead)
- More integrated experience

### Game Integration
- Trajectory appears as part of game
- Native game performance
- Consistent visual style
- Works with game's settings
- No overlay interference

## File Structure

```
HadesLoader/
├── app/
│   ├── src/main/
│   │   ├── java/com/hadesloader/poolinjector/
│   │   │   ├── MainActivity.kt              # UI and injection control
│   │   │   ├── Constants.kt                 # Configuration constants
│   │   │   ├── physics/
│   │   │   │   └── TrajectoryCalculator.kt  # Physics engine (to be injected)
│   │   │   ├── injector/
│   │   │   │   ├── ApkModifier.kt           # APK modification engine
│   │   │   │   └── ModInstaller.kt          # Game detection
│   │   │   └── utils/
│   │   │       └── ResolutionManager.kt     # Screen adaptation
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml        # Injection UI
│   │   │   └── values/
│   │   │       ├── colors.xml
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml              # No overlay/accessibility permissions
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Removed Components

The following components were removed as they are no longer needed for direct injection:

- **TrajectoryOverlayService.kt**: Overlay rendering service
- **TrajectoryOverlayView.kt**: Overlay view component
- **GameAccessibilityService.kt**: Game state monitoring via accessibility
- **GameMemoryReader.kt**: Memory reading component
- **CueDetector.kt**: Equipment detection
- **accessibility_service_config.xml**: Accessibility service configuration

## Future Enhancements

### Potential Improvements
1. **Advanced Injection**: More sophisticated Smali injection techniques
2. **Multi-Version Support**: Support for multiple game versions
3. **Custom Trajectory Styles**: User-configurable trajectory appearance
4. **Physics Customization**: User-adjustable physics parameters
5. **Backup/Restore**: Ability to backup original APK

### Performance Optimizations
1. **Faster Injection**: Optimize APK modification process
2. **Incremental Updates**: Only inject changed code
3. **Caching**: Cache injection results for same game version
4. **Background Processing**: Allow injection to continue in background

## Implementation Notes

### Current Implementation Status
The current implementation provides the framework for APK injection:
- APK extraction and working directory management
- Game detection and version checking
- UI for injection process
- Physics engine for trajectory calculation
- Installation handling for modded APK

### Production Requirements
For production use, the following would be needed:
- APKTool integration for actual Smali injection
- Proper APK signing infrastructure
- Extensive testing across game versions
- Error handling for various game modifications
- Update mechanism for game version changes

## Conclusion

The direct injection architecture provides a cleaner, more integrated approach to trajectory prediction by modifying the game APK itself rather than using overlay permissions. This eliminates complex permission management and provides a more seamless user experience where the trajectory system is built directly into the game.