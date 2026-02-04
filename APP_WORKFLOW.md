# PosModeTest (GNSS-Tracker) - Application Workflow

This document provides a comprehensive technical description of the internal workflow, architecture, and implementation details of the PosModeTest Android application.

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Application Lifecycle](#application-lifecycle)
4. [Permission System](#permission-system)
5. [Core Components Deep Dive](#core-components-deep-dive)
6. [Observer Architecture](#observer-architecture)
7. [Data Flow Architecture](#data-flow-architecture)
8. [Location Tracking Workflow](#location-tracking-workflow)
9. [GNSS Concepts & Data](#gnss-concepts--data)
10. [Network Monitoring Workflow](#network-monitoring-workflow)
11. [Background Logging Service](#background-logging-service)
12. [Configuration System](#configuration-system)
13. [UI Architecture](#ui-architecture)
14. [User Interaction Flow](#user-interaction-flow)
15. [Threading Model](#threading-model)
16. [Intent Utilities](#intent-utilities)
17. [Error Handling](#error-handling)
18. [Summary](#summary)

---

## Overview

PosModeTest is a professional-grade GNSS (Global Navigation Satellite System) testing and debugging tool designed for Android developers, device manufacturers, and QA engineers. It provides comprehensive real-time monitoring of:

| Category | Features |
|----------|----------|
| **Location** | GPS/GNSS coordinates, accuracy, altitude, speed, bearing, timestamps |
| **Satellites** | Individual satellite status, CN0 signal strength, constellation type, elevation/azimuth |
| **Raw GNSS** | GNSS measurements, navigation messages, multipath detection, AGC levels |
| **Cellular** | Multi-SIM support, cell tower info (LTE/NR/GSM/CDMA), network type, roaming status |
| **WiFi** | Connected networks, WiFi standard (4/5/6), SSID |
| **Data** | SUPL availability, APN information, data activity direction |
| **Logs** | Real-time filtering of LocationManagerService and Bluesky positioning logs |
| **Config** | GPS configuration files, carrier configs, system properties |

---

## Project Structure

```
GNSS-Tracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/pos/modetest/
│   │   │   │   ├── MainActivity.java          # Main UI controller (1,039 lines)
│   │   │   │   ├── ReadConfigActivity.java    # Configuration viewer activity
│   │   │   │   ├── BlueskyTrackService.java   # Background log capture service
│   │   │   │   │
│   │   │   │   ├── data/                      # Data holder classes for UI binding
│   │   │   │   │   ├── LocationHolder.java         # Wraps Location object
│   │   │   │   │   ├── GnssSvStatusHolder.java     # Single satellite status
│   │   │   │   │   ├── GnssSvStatusHeaderHolder.java # Table header
│   │   │   │   │   ├── CellInfoHolder.java         # Base cell info class
│   │   │   │   │   ├── CellInfoHeaderHolder.java   # Table header
│   │   │   │   │   ├── CellInfoHolderFactory.java  # Factory for cell types
│   │   │   │   │   ├── CellInfoLteHolder.java      # LTE cell info
│   │   │   │   │   ├── CellInfoNrHolder.java       # 5G NR cell info
│   │   │   │   │   ├── CellInfoGsmHolder.java      # GSM cell info
│   │   │   │   │   ├── CellInfoCdmaHolder.java     # CDMA cell info
│   │   │   │   │   ├── CellInfoTdscdmaHolder.java  # TD-SCDMA cell info
│   │   │   │   │   └── CellInfoWcdmaHolder.java    # WCDMA cell info
│   │   │   │   │
│   │   │   │   ├── observers/                 # System monitoring classes
│   │   │   │   │   ├── IObserver.java              # Observer interface
│   │   │   │   │   ├── LocationObserver.java       # GNSS/Location monitoring
│   │   │   │   │   ├── TelephonyObserver.java      # Cellular network monitoring
│   │   │   │   │   ├── DataObserver.java           # Mobile data monitoring
│   │   │   │   │   ├── WifiObserver.java           # WiFi monitoring
│   │   │   │   │   ├── NetworkObserver.java        # Base network observer
│   │   │   │   │   ├── LogObserver.java            # Base logcat observer
│   │   │   │   │   └── BlueskyLogObserver.java     # Bluesky-specific log filter
│   │   │   │   │
│   │   │   │   ├── utils/                     # Utility classes
│   │   │   │   │   ├── Constants.java              # App-wide constants
│   │   │   │   │   ├── ConfigUtils.java            # Configuration file reading
│   │   │   │   │   ├── FormatUtils.java            # Date/time formatting
│   │   │   │   │   ├── HelperUtils.java            # Permission checks, shortcuts
│   │   │   │   │   └── IntentUtils.java            # Intent creation helpers
│   │   │   │   │
│   │   │   │   └── widgets/                   # Custom UI components
│   │   │   │       └── Chronometer.java            # Custom timer widget
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_pos_mode_test.xml  # Main activity layout
│   │   │   │   │   ├── activity_read_config.xml    # Config viewer layout
│   │   │   │   │   ├── layout_loc_info.xml         # Location info section
│   │   │   │   │   ├── layout_gnss_info.xml        # GNSS status section
│   │   │   │   │   ├── layout_net_info.xml         # Network info section
│   │   │   │   │   ├── layout_sv_status_row.xml    # Satellite table row
│   │   │   │   │   └── layout_cell_info_row.xml    # Cell info table row
│   │   │   │   ├── menu/
│   │   │   │   │   ├── menu_pos_mode_test.xml      # Main activity menu
│   │   │   │   │   └── menu_read_config.xml        # Config viewer menu
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml                 # String resources
│   │   │   │   │   ├── colors.xml                  # Color definitions
│   │   │   │   │   └── arrays.xml                  # Spinner options
│   │   │   │   └── drawable/                       # Icons and graphics
│   │   │   │
│   │   │   └── AndroidManifest.xml            # App manifest with permissions
│   │   │
│   │   └── androidTest/                       # Instrumented tests
│   │
│   └── build.gradle.kts                       # App build configuration
│
├── build.gradle.kts                           # Root build configuration
├── settings.gradle.kts                        # Project settings
├── gradle/libs.versions.toml                  # Version catalog
└── gradle/wrapper/                            # Gradle wrapper
```

---

## Application Lifecycle

### Detailed State Machine

```
                              ┌─────────────────┐
                              │   APP KILLED    │
                              └────────┬────────┘
                                       │ Launch
                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              onCreate()                                   │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  1. EdgeToEdge.enable() - Fullscreen with transparent status bar   │  │
│  │  2. FLAG_KEEP_SCREEN_ON - Prevent screen timeout                   │  │
│  │  3. SCREEN_ORIENTATION_NOSENSOR - Lock orientation                 │  │
│  │  4. ActivityPosModeTestBinding.inflate() - View binding setup      │  │
│  │  5. setSupportActionBar(toolbar) - Setup action bar                │  │
│  │  6. ToneGenerator init - For BEEP on location fix                  │  │
│  │  7. Handler(Looper.getMainLooper()) - Main thread handler          │  │
│  │  8. getMainExecutor() / newSingleThreadExecutor() - Executors      │  │
│  │  9. ConnectivityManager init - Network monitoring                  │  │
│  │  10. Initialize aiding data deletion list (default: "all")         │  │
│  │  11. Create Observer instances:                                    │  │
│  │      - TelephonyObserver(context, listener, executor)              │  │
│  │      - DataObserver(context, listener, executor)                   │  │
│  │      - WifiObserver(context, listener, executor)                   │  │
│  │      - LocationObserver(context, listener, executor)               │  │
│  │  12. ContentObserver for A-GNSS setting changes                    │  │
│  │  13. WindowInsets listeners for navigation bar padding             │  │
│  │  14. Button click listeners (RUN, DELETE)                          │  │
│  │  15. doCheckPermission(ask=true, callback=null) - Initial check    │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              onResume()                                   │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  doResume():                                                       │  │
│  │  1. HelperUtils.requestAddShortcut() - Add launcher shortcut       │  │
│  │  2. doUpdateLocationOptions() - Read spinner values                │  │
│  │  3. doUpdateAgnssStatus() - Check assisted_gps_enabled setting     │  │
│  │  4. Register ContentObserver for A-GNSS URI                        │  │
│  │  5. doResetText() - Clear all display fields                       │  │
│  │  6. Reset observer listener maps (empty callbacks)                 │  │
│  │  7. doCheckPermission(ask=false, callback=doPermissionsGrantedStart)│  │
│  │                                                                    │  │
│  │  doPermissionsGrantedStart() [if permissions granted]:             │  │
│  │  1. doUpdateConfigData() - Load GPS config preview                 │  │
│  │  2. telephonyObserver.startObserving()                             │  │
│  │  3. dataObserver.startObserving()                                  │  │
│  │  4. wifiObserver.startObserving()                                  │  │
│  │  5. locationObserver.startObserving()                              │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                            ACTIVE STATE                                   │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  - All observers running, receiving system callbacks               │  │
│  │  - UI updated in real-time via listener interfaces                 │  │
│  │  - User can interact with controls (RUN, DELETE, spinners, menu)   │  │
│  │  - Timers running if location session active                       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              onPause()                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  doPause():                                                        │  │
│  │  1. Unregister A-GNSS ContentObserver                              │  │
│  │  2. Check permissions (if denied, skip remaining steps)            │  │
│  │  3. doStopLocating() - Stop any active location session            │  │
│  │  4. telephonyObserver.stopObserving()                              │  │
│  │  5. wifiObserver.stopObserving()                                   │  │
│  │  6. dataObserver.stopObserving()                                   │  │
│  │  7. locationObserver.stopObserving()                               │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │    STOPPED      │
                              └─────────────────┘
```

---

## Permission System

### Required Permissions

```xml
<!-- AndroidManifest.xml -->

<!-- Location Permissions (Critical) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_LOCATION_EXTRA_COMMANDS" />

<!-- Network Permissions -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PRIVILEGED_PHONE_STATE" />

<!-- Service Permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Protected Permissions (require ADB grant or system app) -->
<uses-permission android:name="android.permission.READ_LOGS" />
<uses-permission android:name="android.permission.DUMP" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.INTERACT_ACROSS_USERS" />
<uses-permission android:name="android.permission.BATTERY_STATS" />
```

### Permission Check Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     doCheckPermission(ask, callback)                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Collect all permissions from:                                          │
│  - DataObserver.permissions                                             │
│  - LocationObserver.permissions                                         │
│  - TelephonyObserver.permissions                                        │
│  - WifiObserver.permissions                                             │
│  - BlueskyTrackService.getPermissions()                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              HelperUtils.checkPermissions(context, perms)               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
               ALL GRANTED                     SOME DENIED
                    │                               │
                    ▼                               ▼
        ┌───────────────────┐           ┌───────────────────────────┐
        │ Execute callback  │           │ if (ask) {                │
        │ immediately       │           │   mOnPermissionsRunnable  │
        │                   │           │     = callback;           │
        │ Return GRANTED    │           │   requestPermissions()    │
        └───────────────────┘           │ }                         │
                                        │ Return DENIED             │
                                        └───────────────────────────┘
                                                    │
                                                    ▼
                                        ┌───────────────────────────┐
                                        │ onRequestPermissionsResult│
                                        │ - If granted: run callback│
                                        │ - If denied: show toast,  │
                                        │   open app settings       │
                                        └───────────────────────────┘
```

### Permission by Observer

| Observer | Permissions | Purpose |
|----------|-------------|---------|
| `LocationObserver` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS/GNSS access |
| `TelephonyObserver` | `ACCESS_FINE_LOCATION`, `READ_PHONE_STATE` | Cell info, subscriptions |
| `DataObserver` | `ACCESS_NETWORK_STATE`, `READ_PHONE_STATE` | Mobile data status |
| `WifiObserver` | `ACCESS_NETWORK_STATE` | WiFi network info |
| `BlueskyTrackService` | `POST_NOTIFICATIONS`, `READ_LOGS` | Foreground service, log reading |

---

## Core Components Deep Dive

### MainActivity.java (1,039 lines)

The central controller that orchestrates all app functionality.

#### Key Member Variables

```java
// System Services
private ConnectivityManager cm;

// Audio
private ToneGenerator toneGenerator;  // BEEP on fix

// Threading
private Handler mHandler;             // Main thread handler
private Executor mExecutor;           // Main thread executor
private Executor mBgExecutor;         // Background executor for geocoding

// Aiding Data
private List<String> mDelAdItems;     // Selected items to delete

// Observers
private TelephonyObserver telephonyObserver;
private DataObserver dataObserver;
private WifiObserver wifiObserver;
private LocationObserver locationObserver;

// State
private int mCurrentTtffMillis = 0;   // Time to first fix
private int mCurrentFixCount = 0;     // Number of fixes obtained
private Runnable mOnPermissionsRunnable;  // Callback after permission grant

// UI
private ActivityPosModeTestBinding mainBinding;  // View binding
```

#### Key Methods

| Method | Purpose |
|--------|---------|
| `onCreate()` | Initialize all components, observers, UI bindings |
| `onResume()` / `doResume()` | Start observers, register listeners |
| `onPause()` / `doPause()` | Stop observers, unregister listeners |
| `doStartLocating()` | Begin location tracking session |
| `doStopLocating()` | End location tracking session |
| `doUpdateLocationOptions()` | Read provider/mode/quality from spinners |
| `doDeleteAidingData()` | Send delete command to LocationManager |
| `doConfigDeleteAidingData()` | Show multi-select dialog for aiding data |
| `doUpdateAgnssStatus()` | Check assisted_gps_enabled system setting |
| `doUpdateConfigData()` | Load GPS config file preview |
| `doCheckPermission()` | Verify and request permissions |
| `updateGnssStatus()` | Update GNSS status text (Searching/Acquiring/Tracking) |
| `updateGnssStatusTable()` | Populate satellite table rows |
| `updateGnssMiscStatus()` | Update full tracking, multipath, AGC |
| `updateCellInfo()` | Populate cell info table rows |
| `doGeocode()` | Reverse geocode location to address |
| `soundBEEP()` | Play tone on location fix |

---

### ReadConfigActivity.java

Secondary activity for viewing detailed system configurations.

#### Features

- **Spinner Selection**: Choose which config type to view
- **Config Types Available**:
  - `GPS_DEBUG` - /system/etc/gps_debug.conf
  - `GPS_VENDOR` - /vendor/etc/gps.conf
  - `SYSPROP` - System properties (persist.sys.gps.*)
  - `CARRIER_CONFIG` - Carrier-specific GPS settings
  - `RESPROP` - Android resource configurations
  - `BUILD_PROP` - Build.* properties
  - `DUMP_LOC` - dumpsys location
  - `DUMP_GMS_*` - Google Mobile Services dumps
  - `DUMP_BATTERY` - Battery stats

---

### BlueskyTrackService.java

Foreground service for capturing location-related system logs.

#### Service Lifecycle

```java
// Constants
private static final int MIN_UPDATE_INTERVAL = 1000;  // 1 second
private static final int MAX_LOG_BUFF_SIZE = 10000;   // 10K lines

// Log Buffers
private final List<String> mAllLogsBuffer = new ArrayList<>();
private final List<String> mEnvLogsBuffer = new ArrayList<>();
private final List<String> mBlueskyLogsBuffer = new ArrayList<>();

// Counters
private int mAllLogsCount;
private int mEnvLogsCount;
private int mBlueskyLogsCount;
```

#### Notification

- Shows in status bar while running
- Displays log counts: Total / Env Bearing / Bluesky
- Updates every 1 second
- Swipe to dismiss stops the service

---

## Observer Architecture

### IObserver Interface

```java
public interface IObserver {
    void startObserving();
    void stopObserving();
}
```

### Observer Class Hierarchy

```
                    ┌─────────────┐
                    │  IObserver  │
                    └──────┬──────┘
                           │ implements
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│LocationObserver │ │TelephonyObserver│ │ NetworkObserver │
│                 │ │                 │ │   (abstract)    │
│ - LocationMgr   │ │ - TelephonyMgr  │ └────────┬────────┘
│ - GnssStatus    │ │ - SubscriptionMgr│          │ extends
│ - Measurements  │ │ - TelephonyCb   │    ┌─────┴─────┐
│ - NavMessages   │ └─────────────────┘    │           │
└─────────────────┘                        ▼           ▼
                                   ┌────────────┐ ┌────────────┐
                                   │DataObserver│ │WifiObserver│
                                   └────────────┘ └────────────┘

                    ┌─────────────┐
                    │ LogObserver │
                    │  (abstract) │
                    └──────┬──────┘
                           │ extends
                           ▼
                 ┌───────────────────┐
                 │BlueskyLogObserver │
                 └───────────────────┘
```

### LocationObserver Detailed

```java
public class LocationObserver implements IObserver {

    // Location Modes
    public enum LocationMode {
        SINGLE,   // One fix then stop
        TRACK,    // Continuous updates
        LAST      // Return cached location
    }

    // Aiding Data Options
    public static final String[] AIDING_DATA_OPTS = {
        "all", "ephemeris", "almanac", "position", "time",
        "iono", "utc", "health", "svdir", "svsteer",
        "sadata", "rti", "celldb-info"
    };

    // Callbacks registered
    private LocationListener mLocationListener;
    private GnssStatus.Callback mGnssStatusCallback;
    private GnssMeasurementsEvent.Callback mGnssMeasurementsCallback;
    private GnssNavigationMessage.Callback mGnssNavigationMessageCallback;
    private BroadcastReceiver mBroadcastReceiver;

    // Listener Interface
    public interface Listener {
        void onLocationModeChanged(boolean enabled);
        void onProvidersChanged(List<String> enabledProviders);
        void onGnssCapabilitiesChanged(GnssCapabilities caps, String hwModel, int yearOfHw);
        void onLocationChanged(Location location);
        void onGnssStarted();
        void onFirstFix(int ttffMillis);
        void onSatelliteStatusChanged(GnssStatus status);
        void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs);
        void onGnssNavigationMessageReceived(GnssNavigationMessage message);
        void onGnssFullTrackingChanged(boolean enabled);
        void onGnssStopped();
    }
}
```

### TelephonyObserver Detailed

```java
public class TelephonyObserver implements IObserver {

    // Per-SIM callback registration
    private class TelephonyCb extends TelephonyCallback implements
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.DataActivityListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.DisplayInfoListener {

        private final SubscriptionInfo mSubInfo;

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo);

        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo displayInfo);

        @Override
        public void onDataActivity(int direction);

        @Override
        public void onServiceStateChanged(ServiceState serviceState);
    }

    // Listener Interface
    public interface Listener {
        void onSubscriptionChanged(int slot, SubscriptionInfo subInfo);
        void onSubscriptionsChanged(Map<Integer, SubscriptionInfo> subInfoMap);
        void onCellInfoChanged(SubscriptionInfo subInfo, List<CellInfo> cellInfo);
        void onDisplayInfoChanged(SubscriptionInfo subInfo, TelephonyDisplayInfo info);
        void onDataActivity(SubscriptionInfo subInfo, int direction);
    }

    // Network Type String Mapping
    public static String getNetworkTypeString(int networkType, int overrideNetworkType) {
        // Returns: GPRS, EDGE, UMTS, LTE, LTE_CA, NR_NSA, NR, etc.
    }

    // Data Activity Symbols
    public static String getDataActivityString(int direction) {
        // Returns: ↓ (in), ↑ (out), ⇅ (both), ~ (dormant), - (none)
    }
}
```

---

## Data Flow Architecture

### Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ANDROID SYSTEM LAYER                                │
├───────────────┬───────────────┬───────────────┬───────────────┬─────────────┤
│LocationManager│TelephonyManager│ConnectivityMgr│  WifiManager  │   Logcat    │
│               │SubscriptionMgr│               │               │             │
└───────┬───────┴───────┬───────┴───────┬───────┴───────┬───────┴──────┬──────┘
        │               │               │               │              │
        │ Callbacks     │ Callbacks     │ Callbacks     │ Callbacks    │ stdout
        ▼               ▼               ▼               ▼              ▼
┌───────────────┐┌───────────────┐┌───────────────┐┌───────────────┐┌─────────┐
│ Location      ││ Telephony     ││ Data          ││ Wifi          ││ Bluesky │
│ Observer      ││ Observer      ││ Observer      ││ Observer      ││ LogObs  │
│               ││               ││               ││               ││         │
│ mObserver     ││ mObserve      ││ mObserve      ││ mObserve      ││ (runs   │
│ Executor      ││ Executor      ││ Executor      ││ Executor      ││ in svc) │
│ (background)  ││ (background)  ││ (background)  ││ (background)  ││         │
└───────┬───────┘└───────┬───────┘└───────┬───────┘└───────┬───────┘└────┬────┘
        │               │               │               │              │
        │ mExecutor     │ mExecutor     │ mExecutor     │ mExecutor    │
        │ (main thread) │ (main thread) │ (main thread) │ (main thread)│
        ▼               ▼               ▼               ▼              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           LISTENER LAYER                                     │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐                │
│  │ locationObserver│ │ telephonyObserver│ │ dataObserver   │                │
│  │ Listener        │ │ Listener         │ │ Listener       │ ...            │
│  │ (anonymous)     │ │ (anonymous)      │ │ (anonymous)    │                │
│  └────────┬────────┘ └────────┬─────────┘ └────────┬───────┘                │
│           │                   │                    │                         │
│           └───────────────────┼────────────────────┘                         │
│                               │                                              │
│                               ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        UI UPDATE METHODS                               │  │
│  │                                                                        │  │
│  │  Location Updates:                Network Updates:                     │  │
│  │  - mainBinding.layoutLocInfo     - mainBinding.layoutNetInfo           │  │
│  │    .setLoc(LocationHolder)         .setPhoneStatus()                   │  │
│  │  - mainBinding.fixTimer.stop()     .setDataStatus()                    │  │
│  │  - mCurrentFixCount++              .setRoamStatus()                    │  │
│  │  - soundBEEP()                     .setWifiStatus()                    │  │
│  │                                    .setSuplApn()                       │  │
│  │  GNSS Updates:                                                         │  │
│  │  - updateGnssStatus()           Cell Info Updates:                     │  │
│  │  - updateGnssStatusTable()      - updateCellInfo()                     │  │
│  │  - updateGnssMiscStatus()       - CellInfoHolderFactory.makeFor()      │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    ActivityPosModeTestBinding                        │    │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │    │
│  │  │layoutLocInfo│ │layoutGnssInfo│ │layoutNetInfo│ │ svTable     │    │    │
│  │  │             │ │             │ │             │ │ (TableLayout│    │    │
│  │  │ - latitude  │ │ - gnssStatus│ │ - phoneStatus│ │  satellite  │    │    │
│  │  │ - longitude │ │ - gnssTtff  │ │ - dataStatus│ │  rows)      │    │    │
│  │  │ - accuracy  │ │ - gnssSvStat│ │ - roamStatus│ │             │    │    │
│  │  │ - altitude  │ │ - gnssMisc  │ │ - wifiStatus│ ├─────────────┤    │    │
│  │  │ - speed     │ │             │ │ - suplApn   │ │cellInfoTable│    │    │
│  │  │ - bearing   │ │             │ │ - isAgnss   │ │ (TableLayout│    │    │
│  │  │ - time      │ │             │ │   Enabled   │ │  cell rows) │    │    │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Location Tracking Workflow

### State Machine

```
                    ┌──────────────┐
                    │    IDLE      │
                    │  (stopped)   │
                    └──────┬───────┘
                           │ RUN button pressed
                           │ doCheckPermission() passes
                           ▼
                    ┌──────────────┐
                    │   STARTING   │
                    │              │
                    │ doUpdate     │
                    │ LocationOpts │
                    └──────┬───────┘
                           │ locationObserver.startLocating()
                           │
           ┌───────────────┼───────────────┐
           │               │               │
      Mode=LAST       Mode=SINGLE      Mode=TRACK
           │               │               │
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │ Return     │  │ Request    │  │ Request    │
    │ cached     │  │ single     │  │ continuous │
    │ location   │  │ update     │  │ updates    │
    └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
          │               │               │
          │               ▼               ▼
          │        ┌─────────────────────────┐
          │        │      GNSS STARTED       │
          │        │                         │
          │        │ GnssStatusCallback      │
          │        │   .onStarted()          │
          │        │                         │
          │        │ Register:               │
          │        │ - GnssMeasurements      │
          │        │ - GnssNavigationMessage │
          │        └───────────┬─────────────┘
          │                    │
          │                    ▼
          │        ┌─────────────────────────┐
          │        │      SEARCHING          │
          │        │                         │
          │        │ No satellites visible   │
          │        │ CN0 = 0 for all SVs     │
          │        └───────────┬─────────────┘
          │                    │ onSatelliteStatusChanged
          │                    │ (some CN0 > 0)
          │                    ▼
          │        ┌─────────────────────────┐
          │        │      ACQUIRING          │
          │        │                         │
          │        │ Satellites visible      │
          │        │ but not enough for fix  │
          │        │ usedInFix = 0           │
          │        └───────────┬─────────────┘
          │                    │ onFirstFix(ttffMillis)
          │                    ▼
          │        ┌─────────────────────────┐
          │        │      TRACKING           │
          │        │                         │
          │        │ First fix obtained      │
          │        │ TTFF recorded           │
          │        │ Location updates coming │
          │        └───────────┬─────────────┘
          │                    │
          ▼                    ▼
    ┌────────────────────────────────────────┐
    │            LOCATION RECEIVED            │
    │                                         │
    │ onLocationChanged(location)             │
    │ - LocationHolder created                │
    │ - fixTimer.stop()                       │
    │ - mCurrentFixCount++                    │
    │ - soundBEEP()                           │
    │                                         │
    │ If SINGLE mode: → doStopLocating()      │
    │ If TRACK mode:  → continue receiving    │
    └───────────────────┬─────────────────────┘
                        │
                        │ RUN button pressed again
                        │ or app paused
                        ▼
                 ┌──────────────┐
                 │   STOPPING   │
                 │              │
                 │ stopLocating │
                 │ unregister   │
                 │ callbacks    │
                 └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │    IDLE      │
                 │  (stopped)   │
                 └──────────────┘
```

### Location Request Building

```java
// In LocationObserver.startLocating()
LocationRequest lr = (new LocationRequest.Builder(0))  // 0ms interval = fastest
    .setMaxUpdates(mMode == LocationMode.SINGLE ? 1 : Integer.MAX_VALUE)
    .setQuality(mQuality)  // HIGH_ACCURACY, BALANCED, or LOW_POWER
    .build();

lm.requestLocationUpdates(mProvider, lr, mObserverExecutor, mLocationListener);
lm.registerGnssStatusCallback(mObserverExecutor, mGnssStatusCallback);
```

### GNSS Measurements Processing

```java
// In mGnssMeasurementsCallback.onGnssMeasurementsReceived()

// Check full tracking mode
public static boolean getFullTracking(GnssMeasurementsEvent eventArgs) {
    if (Build.VERSION.SDK_INT >= UPSIDE_DOWN_CAKE && eventArgs != null) {
        return eventArgs.hasIsFullTracking() && eventArgs.isFullTracking();
    }
    return true;
}

// Count multipath satellites
public static int getMultipathSvCount(GnssMeasurementsEvent eventArgs) {
    return (int) eventArgs.getMeasurements().stream()
        .filter(m -> m.getMultipathIndicator() == MULTIPATH_INDICATOR_DETECTED)
        .count();
}

// Calculate average AGC
public static double getAgcAverage(GnssMeasurementsEvent eventArgs) {
    var agcList = eventArgs.getMeasurements().stream()
        .filter(GnssMeasurement::hasAutomaticGainControlLevelDb)
        .map(GnssMeasurement::getAutomaticGainControlLevelDb)
        .toList();
    return agcList.stream().reduce(Double::sum).orElse(0d) / agcList.size();
}
```

---

## GNSS Concepts & Data

### Satellite Constellations

| Constellation | SVID Range | Country | Satellites |
|---------------|------------|---------|------------|
| **GPS** | 1-32 | USA | 31 |
| **GLONASS** | 65-96 | Russia | 24 |
| **Galileo** | 1-36 | EU | 30 |
| **BeiDou** | 1-63 | China | 46 |
| **QZSS** | 193-202 | Japan | 4 |
| **IRNSS/NavIC** | 1-14 | India | 7 |
| **SBAS** | 120-158 | Various | Augmentation |

### Satellite Status Data (GnssSvStatusHolder)

```java
public class GnssSvStatusHolder {
    // From GnssStatus object
    private String svid;           // Satellite ID
    private String constellation;  // GPS, GLONASS, GALILEO, etc.
    private String cn0;            // Signal strength in dB-Hz (0-50+)
    private String elevation;      // Degrees above horizon (0-90)
    private String azimuth;        // Degrees from north (0-360)
    private String usedInFix;      // "Y" or "N"
    private String hasCarrierFreq; // "Y" or "N"
    private String hasEphemeris;   // "Y" or "N"
    private String hasAlmanac;     // "Y" or "N"
}
```

### Location Data (LocationHolder)

```java
public class LocationHolder {
    // Core position
    private String latitude;       // Decimal degrees (-90 to 90)
    private String longitude;      // Decimal degrees (-180 to 180)
    private String accuracy;       // Horizontal accuracy in meters

    // Altitude
    private String altitude;       // Meters above WGS84 ellipsoid
    private String altitudeAccuracy; // Vertical accuracy in meters

    // Motion
    private String speed;          // Meters per second
    private String speedAccuracy;  // Speed accuracy in m/s
    private String bearing;        // Degrees from north (0-360)
    private String bearingAccuracy;// Bearing accuracy in degrees

    // Timing
    private String time;           // UTC timestamp
    private String elapsedRealtime;// System uptime when fix obtained

    // Provider
    private String provider;       // "gps", "fused", "network"

    // Raw Location object
    private Location location;
}
```

### Aiding Data Types

| Type | Purpose | Effect of Deletion |
|------|---------|-------------------|
| `all` | All aiding data | Cold start (slowest TTFF) |
| `ephemeris` | Precise orbital data | Need to download (~30s) |
| `almanac` | Rough orbital data | Need to download (~12.5min) |
| `position` | Last known position | Wider initial search |
| `time` | GPS time reference | Need to synchronize |
| `iono` | Ionospheric corrections | Reduced accuracy |
| `utc` | UTC time offset | Timing errors |
| `health` | Satellite health flags | May use unhealthy SVs |
| `svdir` | Satellite direction | Wider sky search |
| `svsteer` | Steering data | Slower acquisition |
| `sadata` | Sensitivity assist | Need more signal |
| `rti` | Real-time integrity | May use bad data |
| `celldb-info` | Cell tower database | No cell-based assist |

### Warm vs Cold Start Presets

```java
// Warm start - keeps most data, fast reacquisition
public static final String[] AIDING_DATA_PRESET_WARM = {
    "ephemeris",  // Force download of fresh orbital data
    "utc"         // Refresh UTC offset
};

// Cold start - delete everything, simulate fresh device
public static final String[] AIDING_DATA_PRESET_COLD = {
    "all"         // Clear all aiding data
};
```

---

## Network Monitoring Workflow

### Multi-SIM Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SubscriptionManager                                   │
│                                                                          │
│  addOnSubscriptionsChangedListener()                                     │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    checkAndUpdateSubCbs()                                │
│                                                                          │
│  For each SIM slot index (0, 1, 2, ...):                                │
│                                                                          │
│    ┌─────────────────────────────────────────────────────────────────┐  │
│    │  Get SubscriptionInfo for slot                                  │  │
│    │                                                                 │  │
│    │  If SIM inserted and subId changed:                            │  │
│    │    1. Store SubscriptionInfo in mSubInfoMap                    │  │
│    │    2. Create TelephonyCb(subInfo)                              │  │
│    │    3. tm.createForSubscriptionId(subId)                        │  │
│    │         .registerTelephonyCallback(executor, callback)         │  │
│    │    4. Notify: onSubscriptionChanged(slot, subInfo)             │  │
│    │                                                                 │  │
│    │  If SIM removed:                                                │  │
│    │    1. Remove from mSubInfoMap                                  │  │
│    │    2. Clear TelephonyCb for slot                               │  │
│    │    3. Notify: onSubscriptionChanged(slot, null)                │  │
│    └─────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  Notify: onSubscriptionsChanged(mSubInfoMap)                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Cell Info Holder Factory

```java
public class CellInfoHolderFactory {
    public static CellInfoHolder makeFor(int slot, CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            return new CellInfoLteHolder(slot, (CellInfoLte) cellInfo);
        } else if (cellInfo instanceof CellInfoNr) {
            return new CellInfoNrHolder(slot, (CellInfoNr) cellInfo);
        } else if (cellInfo instanceof CellInfoGsm) {
            return new CellInfoGsmHolder(slot, (CellInfoGsm) cellInfo);
        } else if (cellInfo instanceof CellInfoCdma) {
            return new CellInfoCdmaHolder(slot, (CellInfoCdma) cellInfo);
        } else if (cellInfo instanceof CellInfoTdscdma) {
            return new CellInfoTdscdmaHolder(slot, (CellInfoTdscdma) cellInfo);
        } else if (cellInfo instanceof CellInfoWcdma) {
            return new CellInfoWcdmaHolder(slot, (CellInfoWcdma) cellInfo);
        }
        return null;
    }
}
```

### Network Type Mapping

```java
public static String getNetworkTypeString(int networkType, int overrideNetworkType) {
    return switch (networkType) {
        case NETWORK_TYPE_GPRS -> "GPRS";
        case NETWORK_TYPE_EDGE -> "EDGE";
        case NETWORK_TYPE_UMTS -> "UMTS";
        case NETWORK_TYPE_HSDPA -> "HSDPA";
        case NETWORK_TYPE_HSUPA -> "HSUPA";
        case NETWORK_TYPE_HSPA -> "HSPA";
        case NETWORK_TYPE_HSPAP -> "HSPAP";
        case NETWORK_TYPE_LTE -> switch (overrideNetworkType) {
            case OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE_CA";
            case OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE_ADV_PRO";
            case OVERRIDE_NETWORK_TYPE_NR_NSA -> "NR_NSA";      // 5G Non-Standalone
            case OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "NR_ADVANCED";
            default -> "LTE";
        };
        case NETWORK_TYPE_NR -> "NR";  // 5G Standalone
        // ... more types
        default -> "UNKNOWN";
    };
}
```

### SUPL (Secure User Plane Location) Detection

```java
// In DataObserver callback
if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
    // This network can be used for A-GNSS assistance data
    NetworkInfo info = cm.getNetworkInfo(network);
    String apnName = info.getExtraInfo();  // e.g., "supl" or "internet"
}
```

---

## Background Logging Service

### BlueskyLogObserver Pattern Matching

```java
public class BlueskyLogObserver extends LogObserver {

    // Logcat tag filters (passed to logcat -s)
    private static final List<String> TAGS = List.of(
        "-s",  // Silent mode, show only these tags
        "LocationManagerService:*",
        "LocSvc_ApiV02:*",        // Qualcomm location HAL
        "BlueskyManager:*",       // Sony Bluesky positioning
        "BlueskyRegistrant:*",
        "GCoreFlp:*"              // Google Fused Location Provider
    );

    // Regex patterns for categorization
    private static final String RGX_LMS = ".*LocationManagerService:.*";
    private static final String RGX_ENV_BEARING = ".*hasEnvironmentBearing.*";
    private static final String RGX_BLUESKY = ".*(Bluesky(Manager|Registrant)|GCoreFlp):.*";

    @Override
    public void onLogEvent(String logLine) {
        if (logLine.matches(RGX_LMS)) {
            mListener.onLocationManagerServiceLogEvent(logLine);
        } else if (logLine.matches(RGX_ENV_BEARING)) {
            mListener.onEnvBearingLogEvent(logLine);
        } else if (logLine.matches(RGX_BLUESKY)) {
            mListener.onBlueskyLogEvent(logLine);
        }
    }
}
```

### Log Buffer Management

```java
private void addAndCirculateLogs(List<String> buffer, int maxSize, String item) {
    buffer.add(item);
    if (buffer.size() > maxSize) {
        // Remove oldest 10% when buffer exceeds max
        buffer.subList(0, maxSize / 10).clear();
    }
}
```

---

## Configuration System

### ConfigUtils Configuration Types

```java
public enum ConfigTypes {
    GPS_DEBUG,      // /system/etc/gps_debug.conf
    SYSPROP,        // persist.sys.gps.* properties
    CARRIER_CONFIG, // Carrier-specific GPS settings
    GPS_VENDOR,     // /vendor/etc/gps.conf
    BUILD_PROP,     // Build.* system properties
    RESPROP,        // Android resource configs
    DUMP_LOC,       // dumpsys location
    DUMP_GMS_LMS,   // Google Location Manager Service
    DUMP_GMS_LS,    // Google Location Service
    DUMP_GMS_E911,  // Emergency location config
    DUMP_SUB_MGR,   // Subscription manager dump
    DUMP_BATTERY    // Battery stats
}
```

### GPS Configuration Files

```java
// Key paths
public static final String PATH_GPS_DEBUG_CONF = "/system/etc/gps_debug.conf";
public static final String PATH_GPS_CONF = "/vendor/etc/gps.conf";

// Key properties
public static final String PROP_GPS_CONF_NFW_CP = "NFW_CLIENT_CP";     // Non-Framework WiFi CP
public static final String PROP_GPS_CONF_NFW_SUPL = "NFW_CLIENT_SUPL"; // Non-Framework SUPL
```

### System Properties Read

```java
public static final List<String> SYSPROP_KEYS = List.of(
    "persist.sys.gps.lpp",              // LPP profile
    "persist.sys.gps.emergencypdn"      // Emergency PDN setting
);

public static String readSysProp(String propKey) {
    return HelperUtils.execCommandSync(new String[]{"getprop", propKey}).trim();
}
```

### Carrier Config Reading

```java
public static String readAllCarrierConfig(Context context, String keyPrefix) {
    CarrierConfigManager ccm = context.getSystemService(CarrierConfigManager.class);
    PersistableBundle cc = ccm.getConfig();

    return cc.keySet().stream()
        .filter(k -> k.startsWith(keyPrefix))  // e.g., "gps."
        .sorted()
        .map(key -> key + "=" + cc.get(key))
        .collect(Collectors.joining("\n"));
}
```

---

## UI Architecture

### Layout Hierarchy

```
activity_pos_mode_test.xml
├── CoordinatorLayout
│   ├── MaterialToolbar (@+id/toolbar)
│   │   └── Menu (overflow) → menu_pos_mode_test.xml
│   │
│   ├── NestedScrollView
│   │   └── LinearLayout (vertical)
│   │       │
│   │       ├── layout_loc_info.xml (@+id/layoutLocInfo)
│   │       │   ├── TextView: Latitude
│   │       │   ├── TextView: Longitude
│   │       │   ├── TextView: Accuracy (Horiz/Vert)
│   │       │   ├── TextView: Altitude
│   │       │   ├── TextView: Speed / Bearing
│   │       │   ├── TextView: Provider
│   │       │   └── TextView: Time
│   │       │
│   │       ├── layout_gnss_info.xml (@+id/layoutGnssInfo)
│   │       │   ├── TextView: gnssStatus (Searching/Acquiring/Tracking)
│   │       │   ├── TextView: gnssTtff (Time to First Fix)
│   │       │   ├── TextView: gnssSvStatus (Used/InView/Total)
│   │       │   └── TextView: gnssMiscStatus (FullTrack/Multipath/AGC)
│   │       │
│   │       ├── layout_net_info.xml (@+id/layoutNetInfo)
│   │       │   ├── TextView: phoneStatus (SIM1/LTE ↑↓)
│   │       │   ├── TextView: dataStatus (SIM1 SUPL)
│   │       │   ├── TextView: roamStatus (Home MCC-MNC)
│   │       │   ├── TextView: wifiStatus (SSID WiFi6)
│   │       │   ├── TextView: suplApn (APN name)
│   │       │   └── TextView: isAgnssEnabled (A-GNSS status)
│   │       │
│   │       ├── ViewSwitcher (@+id/switcherTblConf)
│   │       │   ├── TextView: conf (GPS config preview)
│   │       │   └── TableLayout: svTable (satellite table)
│   │       │
│   │       └── TableLayout: cellInfoTable (cell tower table)
│   │
│   └── LinearLayout (controls, bottom)
│       ├── Spinner: spinnerPosProv (GPS/Fused/Network)
│       ├── Spinner: spinnerPosMode (Single/Track/Last)
│       ├── Spinner: spinnerPosQualities (High/Balanced/Low)
│       ├── CheckBox: checkFullTrack
│       ├── ToggleButton: buttonRun
│       ├── Button: buttonDelete
│       ├── Chronometer: runTimer
│       └── Chronometer: fixTimer
```

### Data Binding Setup

```java
// In MainActivity.onCreate()
mainBinding = ActivityPosModeTestBinding.inflate(getLayoutInflater());
setContentView(mainBinding.getRoot());

// Usage examples
mainBinding.layoutLocInfo.setLoc(new LocationHolder(location));
mainBinding.layoutNetInfo.setPhoneStatus("SIM1/LTE ↑");
mainBinding.layoutGnssInfo.gnssStatus.setText("Tracking");
mainBinding.fixTimer.stop();
mainBinding.setFixCount(mCurrentFixCount);
```

### Dynamic Table Row Creation

```java
// Satellite table
private void updateGnssStatusTable(GnssStatus status) {
    mainBinding.svTable.removeAllViews();

    // Header row
    var binding = LayoutSvStatusRowBinding.inflate(getLayoutInflater(),
            mainBinding.svTable, true);
    binding.setSv(new GnssSvStatusHeaderHolder(context));

    // Data rows
    for (int i = 0; i < status.getSatelliteCount(); i++) {
        binding = LayoutSvStatusRowBinding.inflate(getLayoutInflater(),
                mainBinding.svTable, true);
        binding.setSv(new GnssSvStatusHolder(context, status, i));
    }
}

// Cell info table
private void updateCellInfo(Map<SubscriptionInfo, List<CellInfo>> cellInfoMap) {
    mainBinding.cellInfoTable.removeAllViews();

    // Header row
    var binding = LayoutCellInfoRowBinding.inflate(getLayoutInflater(),
            mainBinding.cellInfoTable, true);
    binding.setCellInfo(new CellInfoHeaderHolder(context));

    // Data rows per SIM
    for (var entry : cellInfoMap.entrySet()) {
        for (CellInfo cellInfo : entry.getValue()) {
            binding = LayoutCellInfoRowBinding.inflate(getLayoutInflater(),
                    mainBinding.cellInfoTable, true);
            binding.setCellInfo(CellInfoHolderFactory.makeFor(
                    entry.getKey().getSimSlotIndex(), cellInfo));
        }
    }
}
```

---

## User Interaction Flow

### Main Controls State Machine

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CONTROL SECTION                                 │
│                                                                              │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌──────────────┐ │
│  │   Provider    │  │     Mode      │  │    Quality    │  │  Full Track  │ │
│  │   Spinner     │  │    Spinner    │  │    Spinner    │  │   Checkbox   │ │
│  ├───────────────┤  ├───────────────┤  ├───────────────┤  ├──────────────┤ │
│  │ ○ GPS         │  │ ○ Single      │  │ ○ High Acc    │  │ □ Enable     │ │
│  │ ○ Fused       │  │ ○ Track       │  │ ○ Balanced    │  │   (forces    │ │
│  │ ○ Network     │  │ ○ Last        │  │ ○ Low Power   │  │    duty      │ │
│  └───────────────┘  └───────────────┘  └───────────────┘  │    cycle)    │ │
│                                                            └──────────────┘ │
│  Selection applied on next RUN button press                                 │
│                                                                              │
│  ┌────────────────────────────┐    ┌────────────────────────────┐          │
│  │         RUN Button         │    │       DELETE Button         │          │
│  │                            │    │                             │          │
│  │  State: OFF                │    │  Short Press:               │          │
│  │  Label: "RUN"              │    │    Delete with current      │          │
│  │  Action: doStartLocating() │    │    selection (default: all) │          │
│  │                            │    │                             │          │
│  │  State: ON                 │    │  Long Press:                │          │
│  │  Label: "STOP"             │    │    Show config dialog       │          │
│  │  Action: doStopLocating()  │    │    with checkboxes          │          │
│  └────────────────────────────┘    └────────────────────────────┘          │
│                                                                              │
│  ┌────────────────────────────┐    ┌────────────────────────────┐          │
│  │       Run Timer            │    │       Fix Timer             │          │
│  │  (total session duration)  │    │  (time since last fix)      │          │
│  │                            │    │                             │          │
│  │  Starts: doStartLocating() │    │  Starts: doStartLocating()  │          │
│  │  Stops: doStopLocating()   │    │  Stops: onLocationChanged() │          │
│  │  Resets: doResetText()     │    │  Resets: onLocationChanged()│          │
│  └────────────────────────────┘    └────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Menu Actions

```java
public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();

    if (itemId == R.id.menu_geocode) {
        // Reverse geocode current location to address
        mBgExecutor.execute(() -> doGeocode(location));

    } else if (itemId == R.id.menu_open_in_map) {
        // Open maps app with geo: URI
        startIntent(IntentUtils.createLocationViewingIntent(location));

    } else if (itemId == R.id.menu_radio_info) {
        // Hidden radio info activity (*#*#4636#*#*)
        startIntent(IntentUtils.createRadioInfoSettingsIntent());

    } else if (itemId == R.id.menu_airplane_mode) {
        // Airplane mode settings
        startIntent(IntentUtils.createAirplaneModeSettingsIntent());

    } else if (itemId == R.id.menu_location_settings) {
        // Location settings
        startIntent(IntentUtils.createLocationSettingsIntent());

    } else if (itemId == R.id.menu_app_info) {
        // App details in settings
        startIntent(IntentUtils.createAppDetailsSettingsIntent(this));

    } else if (itemId == R.id.menu_nfw_info) {
        // NFW (Non-Framework) location proxy app info
        startIntent(IntentUtils.createNfwDetailsSettingsIntent(this));

    } else if (itemId == R.id.menu_read_config) {
        // Open configuration viewer activity
        startIntent(new Intent(this, ReadConfigActivity.class));

    } else if (itemId == R.id.menu_bluesky_check) {
        // Check Bluesky capabilities and offer tracking
        showBlueskyCheckDialog();
    }
}
```

### Delete Aiding Data Dialog

```
┌─────────────────────────────────────────────────────────────┐
│              Select Aiding Data to Delete                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ☑ all           ☐ ephemeris      ☐ almanac                │
│  ☐ position      ☐ time           ☐ iono                   │
│  ☐ utc           ☐ health         ☐ svdir                  │
│  ☐ svsteer       ☐ sadata         ☐ rti                    │
│  ☐ celldb-info                                              │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  Presets:                                                    │
│  Warm: ephemeris, utc                                       │
│  Cold: all                                                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│      [Delete]         [Save]         [Cancel]               │
│                                                              │
└─────────────────────────────────────────────────────────────┘

After pressing [Delete]:

┌─────────────────────────────────────────────────────────────┐
│                   Deleting Aiding Data                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  all                                                         │
│                                                              │
│  Closes in 5 seconds                                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Threading Model

### Executor Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MainActivity                                     │
│                                                                          │
│  mHandler = Handler(Looper.getMainLooper())                             │
│  mExecutor = getMainExecutor()           ← UI updates (main thread)    │
│  mBgExecutor = newSingleThreadExecutor() ← Background work (geocoding) │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         Each Observer                                    │
│                                                                          │
│  mObserverExecutor = newSingleThreadExecutor()  ← System callbacks      │
│  mExecutor = (passed from MainActivity)          ← Listener callbacks   │
│                                                                          │
│  Flow:                                                                   │
│  SystemService → mObserverExecutor → process → mExecutor → Listener    │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      Thread Safety Pattern                               │
│                                                                          │
│  // In observer callback (background thread)                            │
│  @Override                                                               │
│  public void onLocationChanged(Location location) {                     │
│      // Switch to main thread for listener callback                     │
│      mExecutor.execute(() -> mListener.onLocationChanged(location));   │
│  }                                                                       │
│                                                                          │
│  // In MainActivity listener (main thread)                               │
│  public void onLocationChanged(Location location) {                     │
│      // Safe to update UI directly                                       │
│      mainBinding.layoutLocInfo.setLoc(new LocationHolder(location));   │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Intent Utilities

### IntentUtils Methods

```java
public class IntentUtils {

    // Open location in maps app
    public static Intent createLocationViewingIntent(Location location) {
        Uri uri = Uri.parse(String.format("geo:%f,%f?q=%f,%f",
            location.getLatitude(), location.getLongitude(),
            location.getLatitude(), location.getLongitude()));
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    // Hidden radio info activity
    public static Intent createRadioInfoSettingsIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.phone",
            "com.android.phone.settings.RadioInfo");
        return intent;
    }

    // Airplane mode settings
    public static Intent createAirplaneModeSettingsIntent() {
        return new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
    }

    // Location settings
    public static Intent createLocationSettingsIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    // App details settings
    public static Intent createAppDetailsSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    // NFW location proxy app settings
    public static Intent createNfwDetailsSettingsIntent(Context context) {
        String pkg = ConfigUtils.getNfwLocationPackage(context);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + pkg));
        return intent;
    }

    // Google Play Store for GMS update
    public static Intent createGmsPlaystoreIntent() {
        return new Intent(Intent.ACTION_VIEW,
            Uri.parse("market://details?id=com.google.android.gms"));
    }
}
```

---

## Error Handling

### Permission Denied

```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                       int[] grantResults) {
    if (requestCode == ALL_PERMISSIONS_REQUEST) {
        if (Arrays.stream(grantResults).allMatch(g -> g == PERMISSION_GRANTED)) {
            // Success - run saved callback
            if (mOnPermissionsRunnable != null) {
                mOnPermissionsRunnable.run();
                mOnPermissionsRunnable = null;
            }
        } else {
            // Failure - show message and open settings
            shortUserMessage("Please grant permissions");
            startIntent(IntentUtils.createAppDetailsSettingsIntent(this));
        }
    }
}
```

### Provider Unavailable

```java
private void doUpdateLocationOptions() {
    String provider = mainBinding.spinnerPosProv.getSelectedItem().toString();

    if (locationObserver.isProviderUnavailable(provider)) {
        shortUserMessage(String.format("\"%s\" Provider is not available", provider));
        mainBinding.buttonRun.setChecked(false);
        return;
    }
    // ... continue setup
}
```

### Intent Start Failure

```java
private void startIntent(Intent intent) {
    try {
        startActivity(intent);
    } catch (Exception e) {
        Log.w(TAG, null, e);
        shortUserMessage("Can't start: " + intent);
    }
}
```

### Geocoder Unavailable

```java
private void doHandleGeocodeResult(Location location, List<Address> addresses) {
    if (!Geocoder.isPresent()) {
        shortUserMessage("No Geocoder");
    } else if (addresses == null || addresses.isEmpty()) {
        shortUserMessage("No addresses");
    } else {
        shortUserMessage(addresses.get(0).getAddressLine(0));
    }
}
```

---

## Summary

### Architecture Patterns Used

| Pattern | Implementation |
|---------|----------------|
| **Observer Pattern** | All `*Observer` classes with `Listener` interfaces |
| **Factory Pattern** | `CellInfoHolderFactory` for polymorphic cell info |
| **Data Binding** | XML layouts bound to `*Holder` data classes |
| **Executor Pattern** | Thread-safe callbacks via dedicated executors |
| **Service Pattern** | `BlueskyTrackService` for background log capture |
| **Builder Pattern** | `LocationRequest.Builder`, `GnssMeasurementRequest.Builder` |

### Key Design Decisions

1. **Separation of Concerns**: Each observer handles exactly one system service
2. **Thread Safety**: All UI updates happen on main thread via executor
3. **Lifecycle Awareness**: Observers start/stop with activity lifecycle
4. **Modularity**: Background service operates independently
5. **Extensibility**: Factory pattern allows easy addition of new cell types
6. **Testability**: Listener interfaces enable mock testing

### Performance Considerations

- **Satellite Table**: Rows created dynamically, not recycled (acceptable for ~50 rows)
- **Log Buffering**: Circular buffer prevents memory growth (max 10K lines)
- **Background Executors**: System callbacks don't block UI thread
- **Screen Wake Lock**: Prevents sleep during testing sessions

This architecture provides a robust, maintainable GNSS debugging platform suitable for professional device testing and development.
