import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin_method_channel.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelKioskSensorsPlugin platform = MethodChannelKioskSensorsPlugin();
  const MethodChannel channel = MethodChannel('kiosk_sensors_plugin');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return [];
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPorts returns empty list', () async {
    expect(await platform.getPorts(DeviceType.SU), []);
  });
}