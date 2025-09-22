import 'package:flutter_test/flutter_test.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

void main() {
  group('KioskSensorsPlugin', () {
    test('device type conversion works correctly', () {
      expect(DeviceTypeExtension.fromName('SU'), DeviceType.SU);
      expect(DeviceTypeExtension.fromName('SI'), DeviceType.SI);
      expect(DeviceTypeExtension.fromName('su'), DeviceType.SU);
    });

    test('sensor status conversion works correctly', () {
      expect(SensorStatusExtension.fromName('CLOSE'), SensorStatus.CLOSE);
      expect(SensorStatusExtension.fromName('FAR'), SensorStatus.FAR);
      expect(SensorStatusExtension.fromName('INVALID'), SensorStatus.UNKNOWN);
    });
  });
}