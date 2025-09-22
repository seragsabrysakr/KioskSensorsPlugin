import 'dart:async';
import 'package:flutter/services.dart';

import 'kiosk_sensors_plugin_platform_interface.dart';
import 'src/models/device_type.dart';
import 'src/models/sensor_update.dart';
import 'src/models/sensor_error.dart';
import 'src/models/ports_update.dart';

/// An implementation of [KioskSensorsPluginPlatform] that uses method channels.
class MethodChannelKioskSensorsPlugin extends KioskSensorsPluginPlatform {
  /// The method channel used to interact with the native platform.
  static const MethodChannel _channel = MethodChannel('kiosk_sensors_plugin');

  // ────────────────────────────────────────────────────────────
  //                   Stream controllers
  // ────────────────────────────────────────────────────────────
  //
  // NOTE: all controllers are broadcast so multiple widgets can listen
  //       without exhausting the single-subscription default stream.

  final _siSensorUpdateController =
      StreamController<SensorUpdate>.broadcast();
  final _suSensorUpdateController =
      StreamController<SensorUpdate>.broadcast();
  final _deviceAttachedController =
      StreamController<DeviceType>.broadcast();
  final _deviceDetachedController =
      StreamController<DeviceType>.broadcast();
  final _suPortsUpdateController =
      StreamController<PortsUpdate>.broadcast();
  final _siPortsUpdateController =
      StreamController<PortsUpdate>.broadcast();
  final _sensorErrorController =
      StreamController<SensorError>.broadcast();
  final _permissionGrantedController =
      StreamController<String>.broadcast();
  final _suPermissionDeniedController =
      StreamController<String>.broadcast();
  final _siPermissionDeniedController =
      StreamController<String>.broadcast();
  final _debugDataController = StreamController<String>.broadcast();

  // ────────────────────────────────────────────────────────────
  //                   Public streams
  // ────────────────────────────────────────────────────────────
  //
  // Expose the underlying broadcast streams to the app.

  @override
  Stream<SensorUpdate> get siSensorUpdates =>
      _siSensorUpdateController.stream;

  @override
  Stream<SensorUpdate> get suSensorUpdates =>
      _suSensorUpdateController.stream;

  @override
  Stream<DeviceType> get deviceAttached =>
      _deviceAttachedController.stream;

  @override
  Stream<DeviceType> get deviceDetached =>
      _deviceDetachedController.stream;

  @override
  Stream<PortsUpdate> get siPortsUpdated =>
      _siPortsUpdateController.stream;

  @override
  Stream<PortsUpdate> get suPortsUpdated =>
      _suPortsUpdateController.stream;

  @override
  Stream<SensorError> get sensorErrors => _sensorErrorController.stream;

  @override
  Stream<String> get permissionGranted =>
      _permissionGrantedController.stream;

  @override
  Stream<String> get suPermissionDenied =>
      _suPermissionDeniedController.stream;

  @override
  Stream<String> get siPermissionDenied =>
      _siPermissionDeniedController.stream;

  @override
  Stream<String> get debugData => _debugDataController.stream;

  /// Must be called once (e.g. in main()) before listening to any stream.
  @override
  Future<void> initialize() async {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  // ────────────────────────────────────────────────────────────
  //              Internal method-channel dispatcher
  // ────────────────────────────────────────────────────────────

  Future<void> _handleMethodCall(MethodCall call) async {
    final method = call.method;
    final args = call.arguments;
    _debugDataController.add('method: $method\narguments: $args\n');

    switch (method) {
      // ── SENSOR UPDATES ──────────────────────────────────────
      case 'onSuSensorUpdate':
        _suSensorUpdateController.add(SensorUpdate.fromMap({
          ...Map<String, dynamic>.from(args),
          'deviceType': 'SU',
        }));
        break;

      case 'onSiSensorUpdate':
        _siSensorUpdateController.add(SensorUpdate.fromMap({
          ...Map<String, dynamic>.from(args),
          'deviceType': 'SI',
        }));
        break;

      // ── SENSOR ERRORS ───────────────────────────────────────
      case 'onSuSensorError':
        _sensorErrorController.add(SensorError.fromMap({
          ...Map<String, dynamic>.from(args),
          'deviceType': 'SU',
        }));
        break;

      case 'onSiSensorError':
        _sensorErrorController.add(SensorError.fromMap({
          ...Map<String, dynamic>.from(args),
          'deviceType': 'SI',
        }));
        break;

      // ── DEVICE HOT-PLUG EVENTS ──────────────────────────────
      case 'onDeviceAttached':
        if (args != null) {
          // Kotlin may send null
          _deviceAttachedController.add(DeviceTypeExtension.fromName(args));
        }
        break;

      case 'onDeviceDetached':
        if (args != null) {
          _deviceDetachedController.add(DeviceTypeExtension.fromName(args));
        }
        break;

      // ── PORT LIST UPDATES ───────────────────────────────────
      case 'onSuPortsUpdated':
        _suPortsUpdateController.add(PortsUpdate.fromMap({
          'ports': List<String>.from(args),
          'deviceType': 'SU',
        }));
        break;

      case 'onSiPortsUpdated':
        _siPortsUpdateController.add(PortsUpdate.fromMap({
          'ports': List<String>.from(args),
          'deviceType': 'SI',
        }));
        break;

      // ── PERMISSION FLOW ─────────────────────────────────────
      case 'onPermissionGranted':
        _permissionGrantedController.add(args as String);
        break;

      case 'onSuPermissionDenied':
        _suPermissionDeniedController.add(args as String);
        break;

      case 'onSiPermissionDenied':
        _siPermissionDeniedController.add(args as String);
        break;

      // ── FALLBACK ────────────────────────────────────────────
      default:
        _debugDataController.add('Unhandled method: $method');
    }
  }

  // ────────────────────────────────────────────────────────────
  //                    Public API wrappers
  // ────────────────────────────────────────────────────────────

  /// Fetch a fresh list of COM/USB ports from native side.
  @override
  Future<List<String>> getPorts(DeviceType type) async {
    final List result = await _channel
        .invokeMethod(type == DeviceType.SU ? 'getSuPorts' : 'getSiPorts');
    return result.cast<String>();
  }

  /// Start continuous sensor polling.
  /// * For SU you may provide an optional IR threshold level.
  @override
  Future<void> startSensor(
    DeviceType type,
    String portName, {
    int irLevel = 1,
  }) async {
    if (type == DeviceType.SU) {
      await _channel.invokeMethod('startSuSensor', {
        'portName': portName,
        'irLevel': irLevel,
      });
    } else {
      await _channel.invokeMethod('startSiSensor', {
        'portName': portName,
      });
    }
  }

  /// Stop polling for the selected device type.
  @override
  Future<void> stopSensor(DeviceType type) async {
    await _channel
        .invokeMethod(type == DeviceType.SU ? 'stopSuSensor' : 'stopSiSensor');
  }

  /// Ask Android for permission to access a specific USB device.
  @override
  Future<bool> requestPermission(String portName) async {
    return await _channel
        .invokeMethod('requestPermission', {'portName': portName});
  }

  // ── SI-specific helper methods (LED, breathing, status …) ──

  @override
  Future<int> closePort(String portName) async =>
      await _channel.invokeMethod('siClosePort', {'portName': portName});

  @override
  Future<int> setLedColor({
    required String portName,
    required int r,
    required int g,
    required int b,
    int seconds = 0,
    int minutes = 0,
    int type = 0,
  }) async =>
      await _channel.invokeMethod('siSetLedColor', {
        'portName': portName,
        'r': r,
        'g': g,
        'b': b,
        'seconds': seconds,
        'minutes': minutes,
        'type': type,
      });

  @override
  Future<int> setFlash(String portName) async =>
      await _channel.invokeMethod('siSetFlash', {'portName': portName});

  @override
  Future<int> setSmooth(String portName) async =>
      await _channel.invokeMethod('siSetSmooth', {'portName': portName});

  @override
  Future<int> setStop(String portName) async =>
      await _channel.invokeMethod('siSetStop', {'portName': portName});

  @override
  Future<List<int>> getDeviceStatus(String portName) async =>
      (await _channel.invokeMethod('siGetDevStatus', {'portName': portName}))
          .cast<int>();

  @override
  Future<String?> getFirmwareVersion(String portName) => _channel
      .invokeMethod<String>('siGetFirmwareVersion', {'portName': portName});

  @override
  Future<int> setBreathe(String portName, int pattern) async =>
      await _channel.invokeMethod<int>(
          'siSetBreathe', {'portName': portName, 'pattern': pattern}) ??
      -1;

  // ────────────────────────────────────────────────────────────
  //                   Cleanup helpers
  // ────────────────────────────────────────────────────────────
  //
  // Call this when the app shuts down or when hot-reloading the plugin.

  @override
  void dispose() {
    _suSensorUpdateController.close();
    _siSensorUpdateController.close();
    _deviceAttachedController.close();
    _deviceDetachedController.close();
    _suPortsUpdateController.close();
    _siPortsUpdateController.close();
    _sensorErrorController.close();
    _permissionGrantedController.close();
    _suPermissionDeniedController.close();
    _siPermissionDeniedController.close();
    _debugDataController.close();
  }
}
