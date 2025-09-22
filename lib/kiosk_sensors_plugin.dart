export 'src/models/device_type.dart';
export 'src/models/sensor_update.dart';
export 'src/models/sensor_error.dart';
export 'src/models/ports_update.dart';
export 'src/models/sensor_status.dart';

import 'kiosk_sensors_plugin_platform_interface.dart';
import 'src/models/device_type.dart';
import 'src/models/sensor_update.dart';
import 'src/models/sensor_error.dart';
import 'src/models/ports_update.dart';

/// Main plugin class providing access to kiosk sensor functionality
class KioskSensorsPlugin {
  static KioskSensorsPluginPlatform get _platform => KioskSensorsPluginPlatform.instance;

  // ────────────────────────────────────────────────────────────
  //                   Public streams (same as your current API)
  // ────────────────────────────────────────────────────────────

  /// Stream of SU sensor updates
  static Stream<SensorUpdate> get suSensorUpdates => _platform.suSensorUpdates;

  /// Stream of SI sensor updates  
  static Stream<SensorUpdate> get siSensorUpdates => _platform.siSensorUpdates;

  /// Stream of device attachment events
  static Stream<DeviceType> get deviceAttached => _platform.deviceAttached;

  /// Stream of device detachment events
  static Stream<DeviceType> get deviceDetached => _platform.deviceDetached;

  /// Stream of SU ports updates
  static Stream<PortsUpdate> get suPortsUpdated => _platform.suPortsUpdated;

  /// Stream of SI ports updates
  static Stream<PortsUpdate> get siPortsUpdated => _platform.siPortsUpdated;

  /// Stream of sensor errors
  static Stream<SensorError> get sensorErrors => _platform.sensorErrors;

  /// Stream of permission granted events
  static Stream<String> get permissionGranted => _platform.permissionGranted;

  /// Stream of SU permission denied events
  static Stream<String> get suPermissionDenied => _platform.suPermissionDenied;

  /// Stream of SI permission denied events
  static Stream<String> get siPermissionDenied => _platform.siPermissionDenied;

  /// Stream of debug data
  static Stream<String> get debugData => _platform.debugData;

  // ────────────────────────────────────────────────────────────
  //                   Public API methods (same interface)
  // ────────────────────────────────────────────────────────────

  /// Initialize the plugin - must be called before using streams
  static Future<void> initialize() => _platform.initialize();

  /// Get available ports for the specified device type
  static Future<List<String>> getPorts(DeviceType type) => _platform.getPorts(type);

  /// Start sensor polling for the specified device type and port
  static Future<void> startSensor(
    DeviceType type,
    String portName, {
    int irLevel = 1,
  }) => _platform.startSensor(type, portName, irLevel: irLevel);

  /// Stop sensor polling for the specified device type
  static Future<void> stopSensor(DeviceType type) => _platform.stopSensor(type);

  /// Request USB permission for the specified port
  static Future<bool> requestPermission(String portName) => _platform.requestPermission(portName);

  // ── SI-specific methods ──
  
  /// Close SI port
  static Future<int> closePort(String portName) => _platform.closePort(portName);

  /// Set LED color on SI device
  static Future<int> setLedColor({
    required String portName,
    required int r,
    required int g,
    required int b,
    int seconds = 0,
    int minutes = 0,
    int type = 0,
  }) => _platform.setLedColor(
    portName: portName,
    r: r,
    g: g,
    b: b,
    seconds: seconds,
    minutes: minutes,
    type: type,
  );

  /// Set flash mode on SI device
  static Future<int> setFlash(String portName) => _platform.setFlash(portName);

  /// Set smooth mode on SI device
  static Future<int> setSmooth(String portName) => _platform.setSmooth(portName);

  /// Set stop mode on SI device
  static Future<int> setStop(String portName) => _platform.setStop(portName);

  /// Get device status from SI device
  static Future<List<int>> getDeviceStatus(String portName) => _platform.getDeviceStatus(portName);

  /// Get firmware version from SI device
  static Future<String?> getFirmwareVersion(String portName) => _platform.getFirmwareVersion(portName);

  /// Set breathe pattern on SI device
  static Future<int> setBreathe(String portName, int pattern) => _platform.setBreathe(portName, pattern);

  /// Dispose resources
  static void dispose() => _platform.dispose();
}