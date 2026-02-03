# Changes Made to Build Locally

This document records all modifications made to the original Sony internal project to make it compile and run on a local development environment.

---

## Summary

The original project was a Sony internal GNSS testing tool that depended on proprietary build infrastructure (`BuildLogic` module) and Sony-specific Gradle plugins. These dependencies were removed to allow the project to build independently.

---

## Changes Made

### 1. `settings.gradle.kts`

**Removed:** Reference to Sony's internal `BuildLogic` module.

```diff
 pluginManagement {
-    includeBuild("../BuildLogic")
     repositories {
         gradlePluginPortal()
         google()
         mavenCentral()
     }
 }
```

**Reason:** The `BuildLogic` folder (`C:\Users\Arkajyoti\Downloads\BuildLogic`) does not exist and contains Sony's proprietary build plugins.

---

### 2. `build.gradle.kts` (Root)

**Removed:** Sony-specific plugin declarations.

```diff
 plugins {
     alias(libs.plugins.android.application) apply false
-    alias(libs.plugins.pos.saveapks) apply false
-    alias(libs.plugins.pos.signrotate) apply false
-    alias(libs.plugins.pos.sonykeystore) apply false
 }
```

**Reason:** These plugins (`save_apks`, `sign_rotate`, `sony_keystore`) are defined in the missing `BuildLogic` module.

---

### 3. `app/build.gradle.kts`

**Removed:**
- Sony-specific plugin imports and usage
- Sony platform signing configurations
- Custom `saveApks` configuration

```diff
-import pos.plugins.sign_rotate.signRotate
-
 plugins {
     alias(libs.plugins.android.application)
-    alias(libs.plugins.pos.saveapks)
-    alias(libs.plugins.pos.signrotate)
-    alias(libs.plugins.pos.sonykeystore)
 }
```

```diff
 defaultConfig {
+    applicationId = "pos.modetest"
     minSdk = libs.versions.app.sdk.min.get().toInt()
     ...
 }
```

```diff
 buildTypes {
     debug {
         applicationIdSuffix = ".debug"
         versionNameSuffix = "-debug"
-        signingConfig = signingConfigs.getByName("platformSonyV2")
-        signRotate {
-            oldSigner = signingConfigs.getByName("platformSonyV1")
-        }
     }
     release {
         isMinifyEnabled = true
         isShrinkResources = true
         proguardFiles += getDefaultProguardFile("proguard-android-optimize.txt")
-        signingConfig = signingConfigs.getByName("platformSonyV2")
-        signRotate {
-            oldSigner = signingConfigs.getByName("platformSonyV1")
-        }
     }
 }
```

```diff
-saveApks {
-    defaultSavePath = rootDir.path
-    extraCopySpec = extraCopySpec.get()
-        .rename("-debug-androidTest", "-androidTest")
-    variants = (variants.get() + listOf(
-        "debug",
-        "debugAndroidTest",
-    ))
-}
```

**Reason:**
- Sony platform signing keys (`platformSonyV1`, `platformSonyV2`) are not available
- Custom plugins for APK management are not available
- App will now use default debug signing

---

### 4. `gradle/libs.versions.toml`

**Removed:** Sony plugin definitions from the version catalog.

```diff
 [plugins]
 android-application = { id = "com.android.application", version.ref = "agp" }
-pos-saveapks = { id = "pos.plugins.save_apks" }
-pos-signrotate = { id = "pos.plugins.sign_rotate" }
-pos-sonykeystore = { id = "pos.plugins.sony_keystore" }
```

**Reason:** These plugin IDs reference plugins defined in the missing `BuildLogic` module.

---

### 5. `app/src/main/java/pos/modetest/ReadConfigActivity.java`

**Removed:** References to missing `highlightLines` method and `DEFAULT_GPS_CONFIGS` constant.

```diff
-import android.text.Spannable;
-import android.text.SpannableString;
```

```diff
 mExecutor.execute(() -> {
-    Spannable content;
+    CharSequence content;
     if (updateItem != null) {
-        var config = ConfigUtils.readConfigByType(getApplicationContext(), updateItem);
-        if (ConfigUtils.DEFAULT_CONFIG_TYPES.contains(updateItem)) {
-            content = ConfigUtils.highlightLines(config, ConfigUtils.DEFAULT_GPS_CONFIGS);
-        } else {
-            content = SpannableString.valueOf(config);
-        }
+        content = ConfigUtils.readConfigByType(getApplicationContext(), updateItem);
     } else {
-        var config = ConfigUtils.readConfigsByType(getApplicationContext(), ConfigUtils.DEFAULT_CONFIG_TYPES);
-        content = ConfigUtils.highlightLines(config, ConfigUtils.DEFAULT_GPS_CONFIGS);
+        content = ConfigUtils.readConfigsByType(getApplicationContext(), ConfigUtils.DEFAULT_CONFIG_TYPES);
     }
     runOnUiThread(() -> binding.sectionContent.setText(content));
 });
```

**Reason:**
- `ConfigUtils.highlightLines()` method does not exist in the codebase
- `ConfigUtils.DEFAULT_GPS_CONFIGS` constant does not exist
- These were likely defined in Sony's internal code that wasn't included
- Config data is now displayed as plain text without syntax highlighting

---

## Impact of Changes

| Feature | Original | After Changes |
|---------|----------|---------------|
| **APK Signing** | Sony platform keys (V1/V2) with rotation | Default Android debug keys |
| **APK Output** | Copied to root directory | Standard Gradle output (`app/build/outputs/apk/`) |
| **Config Highlighting** | Syntax highlighting for GPS configs | Plain text display |
| **Privileged Permissions** | Works as system app on Sony devices | Requires manual ADB grants |

---

## Building the Project

After these changes, build the project using:

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/PosModeTest_v7.4.0-debug.apk
```

---

## Post-Install Setup

Some features require privileged permissions that must be granted via ADB:

```bash
# For log tracking feature (BlueskyTrackService)
adb shell pm grant pos.modetest.debug android.permission.READ_LOGS

# For dumpsys features (optional)
adb shell pm grant pos.modetest.debug android.permission.DUMP
adb shell pm grant pos.modetest.debug android.permission.PACKAGE_USAGE_STATS
```

---

## Notes

- The app package name in debug builds is `pos.modetest.debug` (with `.debug` suffix)
- Release builds require a signing configuration to be added manually
- Some Sony-specific features (Bluesky positioning) will not work on non-Sony devices
- Minimum Android version: 12 (API 31)
- Target Android version: 15 (API 36)
