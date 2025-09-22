package com.example.kiosk_sensors_plugin

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import com.example.pos_base.SiUtils
import com.pos.poslibusb.*
import com.pos.sisdk.SIFunctions
import com.pos.susdk.SUFunctions
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


class KioskSensorsPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var usbManager: UsbManager
    private lateinit var mcs7840: MCS7840Driver
    private var matchSuDevices = ArrayList<UsbDevice>()
    private var matchSiDevices = ArrayList<UsbDevice>()
    private lateinit var permissionIntent: PendingIntent
    private val ACTION_USB_PERMISSION = "com.android.hardware.USB_PERMISSION"


    private val DELAY: Long = 2000
    private val PERIOD: Long = 1000
    private val MAX_CHAR_BUFFER = 128
    private val handler = Handler(Looper.getMainLooper())
    private var currentSuPortName: String = ""
    private var currentSiPortName: String = ""
    private var currentIrLevel: Int = 1
    private var suSensorRunnable: Runnable? = null
    private var siSensorRunnable: Runnable? = null

    private var DEV_BAUD_RATE = 115200
    var PosLibUsbVersion: String = com.pos.poslibusb.BuildConfig.VERSION_NAME
    var SiSDKVersion: String = com.pos.sisdk.BuildConfig.VERSION_NAME
    var SiDemoVersion: String = BuildConfig.VERSION_NAME

    // 1️⃣  Add near other globals
    private enum class DevType { SU, SI }
    private var pendingPermissionFor: DevType? = null


    private val ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
    private val ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "kiosk_sensors_plugin")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext

        initializeUsb()
    }

    private fun initializeUsb() {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        mcs7840 = MCS7840Driver(context)

        permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )

        context.registerReceiver(usbPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION))
        context.registerReceiver(
            usbDeviceReceiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        )
        context.registerReceiver(
            usbDeviceReceiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        )

        try {
            val callback = PosLibUsb.Callback(usbManager, mcs7840)
            PosLibUsb.setCallback(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scanSuDevices()
        scanSiDevices()
        detectMCS7840Ports()

        Utils.logd("Initialized: PosLibUsb=$PosLibUsbVersion, SiSDK=$SiSDKVersion, SiDemo=$SiDemoVersion")
    }


    // Added missing detectMCS7840Ports function
    private fun detectMCS7840Ports(): Int {
        if (!::mcs7840.isInitialized) {
            return 0
        }
        return try {
            val numberOfPorts = mcs7840.MCS7840Detect(usbManager)
            // Update available ports if needed
            updateAvailableSuPorts()
            updateAvailableSiPorts()
            numberOfPorts
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    // Added helper function to update available ports
    private fun updateAvailableSuPorts() {
        val ports = ArrayList<String>()
        // Add USB device ports
        matchSuDevices.forEach { device ->
            if (device.vendorId != 0x9710 || device.productId !in 0x7800..0x784F) {
                ports.add(device.deviceName)
            }
        }
        // Add MCS7840 serial ports
        for (i in 0 until mcs7840.MCS7840GetTotalNumberOfPorts()) {
            ports.add("SerialCOM${i + 1}")
        }
        // Update Flutter side with available ports
        channel.invokeMethod("onSuPortsUpdated", ports)
    }

    private fun updateAvailableSiPorts() {
        val ports = ArrayList<String>()

        // Add only SI devices based on VID
        matchSiDevices.forEach { device ->
            if (device.vendorId == 0x0F10) { // Posiflex Vendor ID
                ports.add(device.deviceName)
            }
        }

        // Add MCS7840 serial ports (optional or for legacy support)
        for (i in 0 until mcs7840.MCS7840GetTotalNumberOfPorts()) {
            ports.add("SerialCOM${i + 1}")
        }

        // Send port list to Flutter
        channel.invokeMethod("onSiPortsUpdated", ports)
    }


    /* ========== rewrite usbPermissionReceiver ========== */
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return

            // 1) pull the device, bail out if null
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            }
            if (device == null) return   // after this line `device` is smart-cast to non-null

            // 2) granted or denied ?
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                handleDevicePermissionGranted(device)       // ✅ non-null now

                when (pendingPermissionFor) {
                    DevType.SU -> openSuPort(currentSuPortName, currentIrLevel)
                    DevType.SI -> openSiPort(currentSiPortName)
                    else       -> { /* nothing */ }
                }
            } else {
                val method = if (pendingPermissionFor == DevType.SU)
                    "onSuPermissionDenied" else "onSiPermissionDenied"
                channel.invokeMethod(method, device.deviceName)   // ✅ safe access
            }

            pendingPermissionFor = null        // always reset
        }
    }





    private val usbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    scanSuDevices()
                    scanSiDevices()
                    detectMCS7840Ports()
                    channel.invokeMethod("onSuDeviceAttached", null)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        handleSuDeviceDetached(it)
                    }
                }
            }
        }
    }

    // helper – put it near your other utils
    private fun UsbDevice.isSu(): Boolean =
        matchSuDevices.any { it.deviceId == this.deviceId }      // fast path
                || (vendorId == 0x0F10 && productId == 0x0100)       // fallback

    private fun UsbDevice.isSi(): Boolean =
        matchSiDevices.any { it.deviceId == this.deviceId }      // fast path
                || (vendorId == 0x0F10 && productId == 0x0200)       // fallback

    private fun scanSuDevices() {
        try {
            matchSuDevices = UsbDeviceFilter.getMatchingHostDevices(
                context,
                com.pos.susdk.R.xml.su_device_filter
            )
            updateAvailableSuPorts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanSiDevices() {
        try {
            matchSiDevices = UsbDeviceFilter.getMatchingHostDevices(
                context,
                com.pos.sisdk.R.xml.si_device_filter
            )

            updateAvailableSiPorts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDevicePermissionGranted(device: UsbDevice) {
        if (mcs7840.IsMCS7840Device(device)) {
            detectMCS7840Ports()
        }
        channel.invokeMethod("onPermissionGranted", device.deviceName)
    }

    private fun handleSuDeviceDetached(device: UsbDevice) {
        matchSuDevices.remove(device)
        if (SUFunctions.IsOpenPort(device.deviceName)) {
            stopSuSensorReading()
        }
        if (mcs7840.IsMCS7840Device(device)) {
            mcs7840.GetMCS7840Device(device)?.MCS7840CloseAll()
        }
        updateAvailableSuPorts()
        channel.invokeMethod("onSuDeviceDetached", device.deviceName)

    }

    private fun handleSiDeviceDetached(device: UsbDevice) {
        matchSiDevices.remove(device)
        if (SIFunctions.IsOpenPort(device.deviceName)) {
            stopSiSensorReading()
        }
        if (mcs7840.IsMCS7840Device(device)) {
            mcs7840.GetMCS7840Device(device)?.MCS7840CloseAll()
        }
        updateAvailableSiPorts()
        channel.invokeMethod("onSiDeviceDetached", device.deviceName)
    }

    /**  فتح حساس الـ SU مع إدارة صلاحية الـ USB بأمان */
    private fun startSUSensorReading(portName: String, irLevel: Int) {
        // إن كان هناك تشغيل سابق للحساس أوقِفه أولاً لعدم ترك Runnable قديم يعمل
        stopSuSensorReading()

        currentSuPortName = portName
        currentIrLevel   = irLevel

        // 1) الأجهزة ذات الـ /dev/ttyACM لا تحتاج طلب صلاحية عادةً
        if (portName.startsWith("/dev/ttyACM")) {
            openSuPort(portName, irLevel)
            return
        }

        // 2) ابحث عن الـ UsbDevice المطابق في القائمة
        val device = matchSuDevices.find { it.deviceName == portName }
            ?: run {
                channel.invokeMethod("onSuSensorError", "Device not found: $portName")
                return
            }

        // 3) امتلكنا صلاحية الوصول؟
        if (usbManager.hasPermission(device)) {
            openSuPort(portName, irLevel)
        } else {
            // خزن نوع الجهاز ليُعاد فتحه بعد منح الصلاحية
            pendingPermissionFor = DevType.SU
            usbManager.requestPermission(device, permissionIntent)
            // عند الرد يُعالَج في usbPermissionReceiver
        }
    }


    private fun startSiSensorReading(portName: String) {
        currentSiPortName = portName
        val siDevice = matchSiDevices.find { it.deviceName == portName }

        val mcsDevice = usbManager.deviceList.values.find { usbDevice ->
            val mcs = mcs7840.GetMCS7840Device(usbDevice)
            mcs?.MCS7840GetDevice()?.deviceName == portName
        }

        val device = siDevice ?: mcsDevice

        if (device != null) {
            if (!usbManager.hasPermission(device as UsbDevice)) {
                usbManager.requestPermission(device, permissionIntent)
            } else {
                openSiPort(portName)
            }
        } else {
            channel.invokeMethod("onSiSensorError", "SI device not found: $portName")
        }
    }



    private fun openSuPort(portName: String, irLevel: Int) {
        val result = SUFunctions.OpenPort(portName)
        if (result == 0) { // Success
            SUFunctions.SetThresholdValue(portName, irLevel)
            startSuSensorPolling()
            channel.invokeMethod("onSuSensorStarted", portName)
        } else {
            channel.invokeMethod("onSuSensorError", "Failed to open port: $result")
        }
    }

    private fun openSiPort(portName: String) {
        val result = SIFunctions.OpenPort(portName, DEV_BAUD_RATE)
        if (result == 0) { // Success
            startSiSensorPolling()
            channel.invokeMethod("onSiSensorStarted", portName)
        } else {
            channel.invokeMethod("onSiSensorError", "Failed to open port: $result")
        }
    }

    private fun startSuSensorPolling() {
        suSensorRunnable = object : Runnable {
            override fun run() {
                val readBuf = ByteArray(MAX_CHAR_BUFFER)
                SUFunctions.GetSensorValue(currentSuPortName, readBuf)
                val sensorValue =
                    ((readBuf[3].toInt() and 0xFF) shl 8) + (readBuf[2].toInt() and 0xFF)

                val status = when (readBuf[1].toInt()) {
                    0x43 -> "CLOSE"
                    0x41 -> "FAR"
                    else -> "UNKNOWN"
                }

                val result = mapOf(
                    "value" to sensorValue,
                    "status" to status
                )

                channel.invokeMethod("onSuSensorUpdate", result)
                handler.postDelayed(this, PERIOD)
            }
        }
        handler.postDelayed(suSensorRunnable!!, DELAY)
    }

    private fun startSiSensorPolling() {
        siSensorRunnable = object : Runnable {
            override fun run() {
                val readBuf = ByteArray(MAX_CHAR_BUFFER)
                SIFunctions.GetDevStatus(currentSiPortName, readBuf, readBuf.size)
                val sensorValue =
                    ((readBuf[3].toInt() and 0xFF) shl 8) + (readBuf[2].toInt() and 0xFF)

                val status = when (readBuf[1].toInt()) {
                    0x43 -> "CLOSE"
                    0x41 -> "FAR"
                    else -> "UNKNOWN"
                }

                val result = mapOf(
                    "value" to sensorValue,
                    "status" to status
                )

                channel.invokeMethod("onSiSensorUpdate", result)
                handler.postDelayed(this, PERIOD)
            }
        }
        handler.postDelayed(siSensorRunnable!!, DELAY)
    }

    private fun stopSuSensorReading() {
        suSensorRunnable?.let {
            handler.removeCallbacks(it)
        }
        suSensorRunnable = null
        if (currentSuPortName.isNotEmpty()) {
            SUFunctions.ClosePort(currentSuPortName)
            currentSuPortName = ""
            currentIrLevel = 1
            channel.invokeMethod("onSuSensorStopped", null)
        }
    }

    private fun stopSiSensorReading() {
        siSensorRunnable?.let { handler.removeCallbacks(it) }
        siSensorRunnable = null

        if (currentSiPortName.isNotEmpty()) {
            SIFunctions.ClosePort(currentSiPortName)
            currentSiPortName = ""
            currentIrLevel = 1
            channel.invokeMethod("onSiSensorStopped", null)
        }
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getSuPorts" -> {
                val ports = ArrayList<String>()
                matchSuDevices.forEach { ports.add(it.deviceName) }
                // Add MCS7840 serial ports
                for (i in 0 until mcs7840.MCS7840GetTotalNumberOfPorts()) {
                    ports.add("SerialCOM${i + 1}")
                }
                result.success(ports)
            }

            "getSiPorts" -> {
                val ports = ArrayList<String>()
                matchSiDevices.forEach { ports.add(it.deviceName) }
                // Add MCS7840 serial ports
                for (i in 0 until mcs7840.MCS7840GetTotalNumberOfPorts()) {
                    ports.add("SerialCOM${i + 1}")
                }
                result.success(ports)
            }

            "startSuSensor" -> {
                val portName = call.argument<String>("portName") ?: return
                val irLevel = call.argument<Int>("irLevel") ?: 1
                startSUSensorReading(portName, irLevel)
                result.success(null)
            }

            "startSiSensor" -> {
                val portName = call.argument<String>("portName") ?: return
                startSiSensorReading(portName)
                result.success(null)
            }

            "stopSuSensor" -> {
                stopSuSensorReading()
                result.success(null)
            }

            "stopSiSensor" -> {
                stopSiSensorReading()
                result.success(null)
            }

            "requestPermission" -> {
                val portName = call.argument<String>("portName") ?: return

                val suDevice = matchSuDevices.find { it.deviceName == portName }
                val siDevice = matchSiDevices.find { it.deviceName == portName }

                // SerialCOM fallback: match portName against internal USB device name
                val mcsDevice = usbManager.deviceList.values.find { usbDevice ->
                    val device = mcs7840.GetMCS7840Device(usbDevice)
                    device?.MCS7840GetDevice()?.deviceName == portName
                }

                val targetDevice = suDevice ?: siDevice ?: mcsDevice

                if (targetDevice != null) {
                    if (!usbManager.hasPermission(targetDevice)) {
                        usbManager.requestPermission(targetDevice, permissionIntent)
                        result.success(true)
                    } else {
                        result.success(false) // Already has permission
                    }
                } else {
                    result.error("DEVICE_NOT_FOUND", "No device matches portName: $portName", null)
                }
            }

            "siGetComList" -> {
                val list = SIFunctions.GetComList() ?: emptyList<String>()
                result.success(list)
            }

            "detectPorts" -> {
                val count = detectMCS7840Ports()
                result.success(count)
            }

            "siIsOpenPort" -> {
                val portName = call.argument<String>("portName") ?: return
                val isOpen = SIFunctions.IsOpenPort(portName)
                result.success(isOpen)
            }

            "siOpenPort" -> {
                val portName = call.argument<String>("portName") ?: return
                val resultCode = SIFunctions.OpenPort(portName, 0)
                result.success(resultCode)
            }

            "siClosePort" -> {
                val portName = call.argument<String>("portName") ?: return
                val resultCode = SIFunctions.ClosePort(portName)
                result.success(resultCode)
            }

            "siSetLedColor" -> {
                val portName = call.argument<String>("portName") ?: return
                val r = call.argument<Int>("r")?.toLong() ?: 0
                val g = call.argument<Int>("g")?.toLong() ?: 0
                val b = call.argument<Int>("b")?.toLong() ?: 0
                val seconds = call.argument<Int>("seconds")?.toLong() ?: 0
                val minutes = call.argument<Int>("minutes")?.toLong() ?: 0
                val type = call.argument<Int>("type")?.toLong() ?: 0
                val resultCode = SIFunctions.SetLedColor(portName, r, g, b, seconds, minutes, type)
                result.success(resultCode)
            }

            "siSetFlash" -> {
                val portName = call.argument<String>("portName") ?: return
                val resultCode = SIFunctions.SetFlash(portName)
                result.success(resultCode)
            }

            "siSetSmooth" -> {
                val portName = call.argument<String>("portName") ?: return
                val resultCode = SIFunctions.SetSmooth(portName)
                result.success(resultCode)
            }

            "siSetStop" -> {
                val portName = call.argument<String>("portName") ?: return
                val resultCode = SIFunctions.SetStop(portName)
                result.success(resultCode)
            }

            "siSetBreathe" -> {
                val portName = call.argument<String>("portName") ?: return
                val pattern = call.argument<Int>("pattern")?.toLong() ?: 0
                val resultCode = SIFunctions.SetBreathe(portName, pattern)
                result.success(resultCode)
            }

            "siGetStatus" -> {
                val portName = call.argument<String>("portName") ?: return
                val buffer = ByteArray(8)
                val resultCode = SIFunctions.GetDevStatus(portName, buffer, buffer.size)
                if (resultCode == 0) {
                    result.success(buffer.map { it.toInt() and 0xFF })
                } else {
                    result.error("STATUS_ERROR", "Failed to get device status", null)
                }
            }

            "siGetFirmware" -> {
                val portName = call.argument<String>("portName") ?: return
                val fw = SIFunctions.GetFirmwareVersion(portName)
                result.success(fw)
            }

            "siParsedStatus" -> {
                val portName = call.argument<String>("portName") ?: return
                val buffer = ByteArray(8)
                val resultCode = SIFunctions.GetDevStatus(portName, buffer, buffer.size)
                if (resultCode == 0) {
                    val parsed = mapOf(
                        "red" to (buffer[1].toInt() and 0xFF),
                        "green" to (buffer[3].toInt() and 0xFF),
                        "blue" to (buffer[5].toInt() and 0xFF),
                        "mode" to (buffer[6].toInt() and 0xFF)
                    )
                    result.success(parsed)
                } else {
                    result.error("STATUS_ERROR", "Failed to get device status", null)
                }
            }


            "siRefreshPorts" -> {
                scanSiDevices()
                detectMCS7840Ports()
                updateAvailableSiPorts()
                result.success(null)
            }

            "siShowColor" -> {
                val portName = call.argument<String>("portName") ?: return
                val r = call.argument<Int>("r") ?: 255
                val g = call.argument<Int>("g") ?: 255
                val b = call.argument<Int>("b") ?: 255
                val sec = call.argument<Int>("seconds") ?: 1
                val min = call.argument<Int>("minutes") ?: 0
                val type = call.argument<Int>("type") ?: 0

                SIFunctions.OpenPort(portName, 0)
                val res = SIFunctions.SetLedColor(
                    portName,
                    r.toLong(),
                    g.toLong(),
                    b.toLong(),
                    sec.toLong(),
                    min.toLong(),
                    type.toLong()
                )
                SIFunctions.ClosePort(portName)
                result.success(res)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        stopSuSensorReading()
        stopSiSensorReading()
        try {
            context.unregisterReceiver(usbPermissionReceiver)
            context.unregisterReceiver(usbDeviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
