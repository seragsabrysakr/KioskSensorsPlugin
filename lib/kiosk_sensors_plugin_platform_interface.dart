import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'src/models/device_type.dart';
import 'src/models/sensor_update.dart';
import 'src/models/sensor_error.dart';
import 'src/models/ports_update.dart';

import 'kiosk_sensors_plugin_method_channel.dart';

abstract class KioskSensorsPluginPlatform extends PlatformInterface {
  /// Constructs a KioskSensorsPluginPlatform.
  KioskSensorsPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static KioskSensorsPluginPlatform _instance = MethodChannelKioskSensorsPlugin();

  /// The default instance of [KioskSensorsPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelKioskSensorsPlugin].
  static KioskSensorsPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [KioskSensorsPluginPlatform] when
  /// they register themselves.
  static set instance(KioskSensorsPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  // ────────────────────────────────────────────────────────────
  //                   Streams
  // ────────────────────────────────────────────────────────────

  /// Stream of SU sensor updates
  Stream<SensorUpdate> get suSensorUpdates;

  /// Stream of SI sensor updates  
  Stream<SensorUpdate> get siSensorUpdates;

  /// Stream of device attachment events
  Stream<DeviceType> get deviceAttached;

  /// Stream of device detachment events
  Stream<DeviceType> get deviceDetached;

  /// Stream of SU ports updates
  Stream<PortsUpdate> get suPortsUpdated;

  /// Stream of SI ports updates
  Stream<PortsUpdate> get siPortsUpdated;

  /// Stream of sensor errors
  Stream<SensorError> get sensorErrors;

  /// Stream of permission granted events
  Stream<String> get permissionGranted;

  /// Stream of SU permission denied events
  Stream<String> get suPermissionDenied;

  /// Stream of SI permission denied events
  Stream<String> get siPermissionDenied;

  /// Stream of debug data
  Stream<String> get debugData;

  // ────────────────────────────────────────────────────────────
  //                   Methods
  // ────────────────────────────────────────────────────────────

  /// Initialize the plugin - must be called before using streams
  Future<void> initialize();

  /// Get available ports for the specified device type
  Future<List<String>> getPorts(DeviceType type);

  /// Start sensor polling for the specified device type and port
  Future<void> startSensor(DeviceType type, String portName, {int irLevel = 1});

  /// Stop sensor polling for the specified device type
  Future<void> stopSensor(DeviceType type);

  /// Request USB permission for the specified port
  Future<bool> requestPermission(String portName);

  /// Close SI port
  Future<int> closePort(String portName);

  /// Set LED color on SI device
  Future<int> setLedColor({
    required String portName,
    required int r,
    required int g,
    required int b,
    int seconds = 0,
    int minutes = 0,
    int type = 0,
  });

  /// Set flash mode on SI device
  Future<int> setFlash(String portName);

  /// Set smooth mode on SI device
  Future<int> setSmooth(String portName);

  /// Set stop mode on SI device
  Future<int> setStop(String portName);

  /// Get device status from SI device
  Future<List<int>> getDeviceStatus(String portName);

  /// Get firmware version from SI device
  Future<String?> getFirmwareVersion(String portName);

  /// Set breathe pattern on SI device
  Future<int> setBreathe(String portName, int pattern);

  /// Dispose resources
  void dispose();
}