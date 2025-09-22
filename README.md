# Kiosk Sensors Plugin

A Flutter plugin for integrating with POS kiosk sensors (SU/SI devices) via native Android SDKs.

## 🚨 **REQUIRED SETUP FOR ANY PROJECT USING THIS PLUGIN**

### **Step 1: Copy AAR Files**
Copy the following AAR files to your project's `android/app/libs/` directory:

```bash
# Create libs directory in your Flutter project
mkdir android/app/libs

# Copy these AAR files from the plugin to your project:
# - PosLibUsb-debug_1.0.16.aar
# - PosLibUsb-release_1.0.16.aar  
# - SuSDK-debug_2.1.7.aar
# - SuSDK-release_2.1.7.aar
# - SiSDK-debug_2.0.11.aar
# - SiSDK-release_2.0.11.aar
```

### **Step 2: Configure Android Build**
Add to your `android/app/build.gradle` (or `build.gradle.kts`):

```kotlin
android {
    // ... your existing configuration
    
    packagingOptions {
        pickFirst("**/libPosLibUsb.so")
        pickFirst("lib/arm64-v8a/libPosLibUsb.so")
        pickFirst("lib/armeabi-v7a/libPosLibUsb.so")
        pickFirst("lib/x86/libPosLibUsb.so")
        pickFirst("lib/x86_64/libPosLibUsb.so")
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Required AAR dependencies
    debugImplementation(files("libs/PosLibUsb-debug_1.0.16.aar"))
    releaseImplementation(files("libs/PosLibUsb-release_1.0.16.aar"))
    
    debugImplementation(files("libs/SuSDK-debug_2.1.7.aar"))
    releaseImplementation(files("libs/SuSDK-release_2.1.7.aar"))
    
    debugImplementation(files("libs/SiSDK-debug_2.0.11.aar"))
    releaseImplementation(files("libs/SiSDK-release_2.0.11.aar"))
}
```

⚠️ **Without these steps, your build will fail with dependency resolution errors!**

---

## Features

- ✅ **SU (Motion) sensor integration** - Distance measurement and proximity detection
- ✅ **SI (Status Indicator) sensor integration** - LED control and status monitoring  
- ✅ **Real-time sensor data streaming** - Reactive programming with Dart streams
- ✅ **USB device management** - Automatic device detection and permission handling
- ✅ **LED control for SI devices** - Full RGB color control with patterns
- ✅ **Permission handling** - Seamless USB permission management

## Supported Hardware

This plugin integrates with the following native Android SDKs:
- **PosLibUsb** v1.0.16 - USB communication library
- **SuSDK** v2.1.7 - Motion sensor SDK  
- **SiSDK** v2.0.11 - Status Indicator SDK

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  kiosk_sensors_plugin:
    git:
      url: https://github.com/yourcompany/kiosk_sensors_plugin.git
```

## Usage

### Basic Setup

```dart
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

// Initialize the plugin
await KioskSensorsPlugin.initialize();

// Get available ports
final suPorts = await KioskSensorsPlugin.getPorts(DeviceType.SU);
final siPorts = await KioskSensorsPlugin.getPorts(DeviceType.SI);
```

### Sensor Data Streaming

```dart
// Listen to SU sensor updates
KioskSensorsPlugin.suSensorUpdates.listen((data) {
  print('Distance: \${data.value}mm - Status: \${data.status.name}');
});

// Listen to SI sensor updates  
KioskSensorsPlugin.siSensorUpdates.listen((data) {
  print('SI Sensor: \${data.value} - \${data.status.name}');
});

// Listen to errors
KioskSensorsPlugin.sensorErrors.listen((error) {
  print('Error from \${error.deviceType}: \${error.message}');
});
```

### SU Sensor Control

```dart
// Start SU sensor with IR threshold level
await KioskSensorsPlugin.startSensor(
  DeviceType.SU, 
  portName, 
  irLevel: 3
);

// Stop SU sensor
await KioskSensorsPlugin.stopSensor(DeviceType.SU);
```

### SI Device LED Control

```dart
// Set LED color (Red)
await KioskSensorsPlugin.setLedColor(
  portName: siPortName,
  r: 255,
  g: 0, 
  b: 0,
  seconds: 5, // Duration
);

// Set LED patterns
await KioskSensorsPlugin.setFlash(siPortName);    // Flash mode
await KioskSensorsPlugin.setSmooth(siPortName);   // Smooth mode  
await KioskSensorsPlugin.setBreathe(siPortName, 1); // Breathe pattern
await KioskSensorsPlugin.setStop(siPortName);     // Stop all effects
```

### Device Management

```dart
// Listen to device connections
KioskSensorsPlugin.deviceAttached.listen((deviceType) {
  print('\${deviceType.name} device attached');
});

KioskSensorsPlugin.deviceDetached.listen((deviceType) {
  print('\${deviceType.name} device detached');
});

// Request USB permissions
bool granted = await KioskSensorsPlugin.requestPermission(portName);
```

## Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

class SensorScreen extends StatefulWidget {
  @override
  _SensorScreenState createState() => _SensorScreenState();
}

class _SensorScreenState extends State<SensorScreen> {
  String? _suPort;
  int _distance = 0;
  SensorStatus _status = SensorStatus.UNKNOWN;

  @override
  void initState() {
    super.initState();
    _initializeSensors();
  }

  Future<void> _initializeSensors() async {
    // Initialize plugin
    await KioskSensorsPlugin.initialize();
    
    // Get available ports
    final ports = await KioskSensorsPlugin.getPorts(DeviceType.SU);
    if (ports.isNotEmpty) {
      setState(() => _suPort = ports.first);
    }

    // Listen to sensor data
    KioskSensorsPlugin.suSensorUpdates.listen((data) {
      setState(() {
        _distance = data.value;
        _status = data.status;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Kiosk Sensors')),
      body: Column(
        children: [
          Text('Distance: \${_distance}mm'),
          Text('Status: \${_status.name}'),
          ElevatedButton(
            onPressed: _suPort != null 
              ? () => KioskSensorsPlugin.startSensor(DeviceType.SU, _suPort!)
              : null,
            child: Text('Start Sensor'),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    KioskSensorsPlugin.dispose();
    super.dispose();
  }
}
```

## API Reference

### Streams

| Stream | Type | Description |
|--------|------|-------------|
| `suSensorUpdates` | `Stream<SensorUpdate>` | SU sensor readings |
| `siSensorUpdates` | `Stream<SensorUpdate>` | SI sensor readings |
| `deviceAttached` | `Stream<DeviceType>` | Device connection events |
| `deviceDetached` | `Stream<DeviceType>` | Device disconnection events |
| `sensorErrors` | `Stream<SensorError>` | Sensor error events |
| `permissionGranted` | `Stream<String>` | USB permission granted |

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `initialize()` | `Future<void>` | Initialize the plugin |
| `getPorts(DeviceType)` | `Future<List<String>>` | Get available ports |
| `startSensor(DeviceType, String, {int irLevel})` | `Future<void>` | Start sensor |
| `stopSensor(DeviceType)` | `Future<void>` | Stop sensor |
| `setLedColor({...})` | `Future<int>` | Set SI LED color |
| `setFlash(String)` | `Future<int>` | Set flash mode |
| `setBreathe(String, int)` | `Future<int>` | Set breathe pattern |

## Requirements

- Flutter 3.3.0+
- Android SDK 23+
- Compatible POS hardware (SU/SI devices)

## Troubleshooting

### Common Issues

1. **No devices found**: Ensure USB devices are properly connected and drivers are installed
2. **Permission denied**: The plugin handles USB permissions automatically, but ensure your app has the necessary permissions in AndroidManifest.xml
3. **Sensor not responding**: Check cable connections and try reinitializing the plugin

### Debug Information

Enable debug logging to see detailed plugin activity:

```dart
KioskSensorsPlugin.debugData.listen((message) {
  print('Plugin Debug: $message');
});
```

## License

Proprietary - Internal use only

## Support

For issues and questions, please contact the development team.