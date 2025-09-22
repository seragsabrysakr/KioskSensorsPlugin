import 'device_type.dart';
import 'sensor_status.dart';

/// Object delivered on every sensor tick (value + interpreted status).
class SensorUpdate {
  /// The sensor reading value in millimeters
  final int value;
  
  /// The interpreted status of the sensor reading
  final SensorStatus status;
  
  /// The type of device that generated this update
  final DeviceType deviceType;

  SensorUpdate({
    required this.value,
    required this.status,
    required this.deviceType,
  });

  /// Create a SensorUpdate from a map (typically from platform channel)
  factory SensorUpdate.fromMap(Map<String, dynamic> map) => SensorUpdate(
        value: map['value'] as int,
        status: SensorStatusExtension.fromName(map['status']),
        deviceType: DeviceTypeExtension.fromName(map['deviceType']),
      );

  @override
  String toString() =>
      'SensorUpdate(value: $value, status: $status, deviceType: $deviceType)';
}
