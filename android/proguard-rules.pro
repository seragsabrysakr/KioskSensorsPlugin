# ProGuard rules for Kiosk Sensors Plugin
# Keep all classes from the POS SDK .aar files

# Ignore warnings for POS SDK classes
-dontwarn com.pos.poslibusb.MCS7840Device
-dontwarn com.pos.poslibusb.MCS7840Driver
-dontwarn com.pos.poslibusb.PosLibUsb
-dontwarn com.pos.poslibusb.PosLog
-dontwarn com.pos.poslibusb.UsbDeviceFilter
-dontwarn com.pos.poslibusb.Utils
-dontwarn com.pos.susdk.SUFunctions
-dontwarn com.pos.sisdk.SIFunctions

# Keep all POS SDK classes and their members
-keep class com.pos.poslibusb.** { *; }
-keep class com.pos.susdk.** { *; }
-keep class com.pos.sisdk.** { *; }

# Keep plugin classes
-keep class com.example.kiosk_sensors_plugin.** { *; }

# Keep Flutter plugin interfaces
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.embedding.** { *; }

# Keep native method names for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes with specific annotations
-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep USB related classes that might be accessed via reflection
-keep class android.hardware.usb.** { *; }

# Ignore warnings for common issues
-ignorewarnings
