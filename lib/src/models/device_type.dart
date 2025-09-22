/// Enum representing the type of sensor device
enum DeviceType { SU, SI }

/// Extension methods for DeviceType
extension DeviceTypeExtension on DeviceType {
  /// Create a DeviceType from a string name
  static DeviceType fromName(String name) =>
      name.toUpperCase() == 'SU' ? DeviceType.SU : DeviceType.SI;
}
