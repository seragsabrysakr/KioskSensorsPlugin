# Consumer ProGuard rules for Kiosk Sensors Plugin
# These rules will be automatically applied to apps that use this plugin

# Keep all POS SDK classes - required for .aar files to work correctly
-keep class com.pos.poslibusb.** { *; }
-keep class com.pos.susdk.** { *; }
-keep class com.pos.sisdk.** { *; }

# Suppress warnings for POS SDK classes
-dontwarn com.pos.poslibusb.**
-dontwarn com.pos.susdk.**
-dontwarn com.pos.sisdk.**

# Keep plugin classes that may be accessed via reflection
-keep class com.example.kiosk_sensors_plugin.** { *; }

# Keep USB classes that might be accessed dynamically
-keep class android.hardware.usb.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
