import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('KioskSensorsPlugin Integration Tests', () {
    testWidgets('plugin initializes without error', (WidgetTester tester) async {
      await KioskSensorsPlugin.initialize();
      // Plugin should initialize successfully
    });

    testWidgets('can get ports list', (WidgetTester tester) async {
      await KioskSensorsPlugin.initialize();
      final suPorts = await KioskSensorsPlugin.getPorts(DeviceType.SU);
      final siPorts = await KioskSensorsPlugin.getPorts(DeviceType.SI);
      
      expect(suPorts, isA<List<String>>());
      expect(siPorts, isA<List<String>>());
    });
  });
}