import 'device_type.dart';

/// Wrapper for any sensor-level error coming from native side.
class SensorError {
  /// The error message
  final String message;
  
  /// The type of device that generated this error
  final DeviceType deviceType;

  SensorError({required this.message, required this.deviceType});

  /// Create a SensorError from a map (typically from platform channel)
  factory SensorError.fromMap(Map<String, dynamic> map) => SensorError(
        message: map['message'],
        deviceType: DeviceTypeExtension.fromName(map['deviceType']),
      );

  @override
  String toString() =>
      'SensorError(message: $message, deviceType: $deviceType)';
}
