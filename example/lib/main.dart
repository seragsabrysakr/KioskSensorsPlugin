import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:kiosk_sensors_plugin/kiosk_sensors_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _suPort;
  String? _siPort;
  String _statusText = 'Not initialized';
  int _sensorDistance = 0;
  SensorStatus _sensorStatus = SensorStatus.UNKNOWN;

  double _irLevel = 2;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    try {
      await KioskSensorsPlugin.initialize();

      final suPorts = await KioskSensorsPlugin.getPorts(DeviceType.SU);
      final siPorts = await KioskSensorsPlugin.getPorts(DeviceType.SI);

    setState(() {
        _suPort = suPorts.isNotEmpty ? suPorts.first : null;
        _siPort = siPorts.isNotEmpty ? siPorts.first : null;
        _statusText = 'SU: $_suPort, SI: $_siPort';
      });

      KioskSensorsPlugin.suSensorUpdates.listen((data) {
        if (mounted) {
          setState(() {
            _sensorDistance = data.value;
            _sensorStatus = data.status;
          });
        }

        log('📏 Sensor update → ${data.value}mm | ${data.status.name}');
      });

      KioskSensorsPlugin.sensorErrors.listen((error) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Sensor Error: ${error.message}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      });

      KioskSensorsPlugin.permissionGranted.listen((deviceName) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Permission granted for: $deviceName'),
              backgroundColor: Colors.green,
            ),
          );
        }
      });

    } catch (e) {
      setState(() => _statusText = '❌ Initialization failed: $e');
    }
  }

  Future<void> _startSU() async {
    final messenger = ScaffoldMessenger.of(context);
    
    try {
      if (_suPort == null) {
        messenger.showSnackBar(
          const SnackBar(content: Text('No SU port available')),
        );
        return;
      }

      await KioskSensorsPlugin.startSensor(DeviceType.SU, _suPort!, irLevel: _irLevel.toInt());
      
      messenger.showSnackBar(
        SnackBar(content: Text('Started SU sensor on $_suPort with IR level $_irLevel')),
      );
    } catch (e) {
      messenger.showSnackBar(
        SnackBar(content: Text('Failed to start SU: $e')),
      );
    }
  }

  Future<void> _stopSU() async {
    final messenger = ScaffoldMessenger.of(context);
    
    try {
      await KioskSensorsPlugin.stopSensor(DeviceType.SU);
      messenger.showSnackBar(
        const SnackBar(content: Text('SU sensor stopped')),
      );
    } catch (e) {
      messenger.showSnackBar(
        SnackBar(content: Text('Failed to stop SU: $e')),
      );
    }
  }

  Future<void> _setLED(int r, int g, int b) async {
    final messenger = ScaffoldMessenger.of(context);
    
    try {
      if (_siPort == null) {
        messenger.showSnackBar(
          const SnackBar(content: Text('No SI port available')),
        );
        return;
      }

      await KioskSensorsPlugin.startSensor(DeviceType.SI, _siPort!);
      await KioskSensorsPlugin.setLedColor(portName: _siPort!, r: r, g: g, b: b);
      
      messenger.showSnackBar(
        SnackBar(content: Text('LED set to R:$r G:$g B:$b successfully')),
      );
    } catch (e) {
      messenger.showSnackBar(
        SnackBar(content: Text('Failed to set LED: $e')),
      );
    }
  }

  @override
  void dispose() {
    KioskSensorsPlugin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Kiosk Sensors Plugin Example'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              Text(_statusText, style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              const Text('Sensor Output:', style: TextStyle(fontSize: 16)),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _sensorStatus == SensorStatus.CLOSE ? Colors.green.shade100 : Colors.red.shade100,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Distance: $_sensorDistance mm',
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    Text('Status: ${_sensorStatus.name}',
                        style: const TextStyle(fontSize: 16)),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              Row(
                children: [
                  ElevatedButton(onPressed: _startSU, child: const Text('Start SU')),
                  const SizedBox(width: 8),
                  ElevatedButton(onPressed: _stopSU, child: const Text('Stop SU')),
                ],
              ),
              const SizedBox(height: 24),

              const Text('IR Level (Threshold)', style: TextStyle(fontWeight: FontWeight.bold)),
              Slider(
                value: _irLevel,
                min: 1,
                max: 10,
                divisions: 9,
                label: _irLevel.toStringAsFixed(0),
                onChanged: (val) {
                  setState(() => _irLevel = val);
                },
                onChangeEnd: (val) => _startSU(),
              ),
              const SizedBox(height: 20),

              const Divider(height: 30),
              const Text('LED Test:', style: TextStyle(fontSize: 16)),
              Row(
                children: [
                  ElevatedButton(
                    onPressed: () => _setLED(255, 0, 0),
                    child: const Text('Red'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: () => _setLED(0, 255, 0),
                    child: const Text('Green'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: () => _setLED(0, 0, 255),
                    child: const Text('Blue'),
                  ),
                ],
              ),

              const Spacer(),
              ElevatedButton.icon(
                icon: const Icon(Icons.refresh),
                label: const Text('Reinitialize'),
                onPressed: _initialize,
              )
            ],
          ),
        ),
      ),
    );
  }
}