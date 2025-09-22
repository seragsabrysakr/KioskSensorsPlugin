import 'device_type.dart';

/// List of available ports for one device family.
class PortsUpdate {
  /// List of available port names
  final List<String> ports;
  
  /// The type of device these ports are for
  final DeviceType deviceType;

  PortsUpdate({required this.ports, required this.deviceType});

  /// Create a PortsUpdate from a map (typically from platform channel)
  factory PortsUpdate.fromMap(Map<String, dynamic> map) => PortsUpdate(
        ports: List<String>.from(map['ports']),
        deviceType: DeviceTypeExtension.fromName(map['deviceType']),
      );
}
