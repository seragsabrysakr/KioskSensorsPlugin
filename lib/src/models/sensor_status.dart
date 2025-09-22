/// Enum representing the status of a sensor reading
enum SensorStatus { CLOSE, FAR, UNKNOWN }

/// Extension methods for SensorStatus
extension SensorStatusExtension on SensorStatus {
  /// Create a SensorStatus from a string name
  static SensorStatus fromName(String name) {
    final upper = name.toUpperCase();
    return SensorStatus.values.firstWhere(
      (e) => e.name == upper,
      orElse: () => SensorStatus.UNKNOWN,
    );
  }
}
