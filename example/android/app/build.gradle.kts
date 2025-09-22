plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.kiosk_sensors_plugin_example"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.example.kiosk_sensors_plugin_example"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 23
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    packagingOptions {
        pickFirst("**/libPosLibUsb.so")
        pickFirst("lib/arm64-v8a/libPosLibUsb.so")
        pickFirst("lib/armeabi-v7a/libPosLibUsb.so")
        pickFirst("lib/x86/libPosLibUsb.so")
        pickFirst("lib/x86_64/libPosLibUsb.so")
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Runtime dependencies for the AAR files (now in app/libs/)
    debugImplementation(files("libs/PosLibUsb-debug_1.0.16.aar"))
    releaseImplementation(files("libs/PosLibUsb-release_1.0.16.aar"))
    
    debugImplementation(files("libs/SuSDK-debug_2.1.7.aar"))
    releaseImplementation(files("libs/SuSDK-release_2.1.7.aar"))
    
    debugImplementation(files("libs/SiSDK-debug_2.0.11.aar"))
    releaseImplementation(files("libs/SiSDK-release_2.0.11.aar"))
}

flutter {
    source = "../.."
}
