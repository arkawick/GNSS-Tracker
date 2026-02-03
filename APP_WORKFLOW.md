# PosModeTest (GNSS-Tracker) - Application Workflow

This document describes the internal workflow and architecture of the PosModeTest Android application.

---

## Table of Contents

1. [Overview](#overview)
2. [Application Lifecycle](#application-lifecycle)
3. [Core Components](#core-components)
4. [Data Flow Architecture](#data-flow-architecture)
5. [Location Tracking Workflow](#location-tracking-workflow)
6. [Network Monitoring Workflow](#network-monitoring-workflow)
7. [Background Logging Service](#background-logging-service)
8. [User Interaction Flow](#user-interaction-flow)

---

## Overview

PosModeTest is a GNSS (Global Navigation Satellite System) testing and debugging tool designed for Android developers and device manufacturers. It provides real-time monitoring of:

- GPS/GNSS location data
- Satellite status and signal strength
- Cellular network information (multi-SIM support)
- WiFi network status
- Raw GNSS measurements
- System logs related to location services

---

## Application Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     onCreate()                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. Enable edge-to-edge display                         │    │
│  │  2. Keep screen on, disable auto-rotate                 │    │
│  │  3. Initialize View Binding (ActivityPosModeTestBinding)│    │
│  │  4. Setup toolbar and tone generator                    │    │
│  │  5. Initialize all Observers:                           │    │
│  │     - TelephonyObserver (cellular monitoring)           │    │
│  │     - DataObserver (mobile data monitoring)             │    │
│  │     - WifiObserver (WiFi monitoring)                    │    │
│  │     - LocationObserver (GNSS monitoring)                │    │
│  │  6. Setup A-GNSS status observer                        │    │
│  │  7. Check and request permissions                       │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     onResume()                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. Request app shortcut                                │    │
│  │  2. Update location options from UI spinners            │    │
│  │  3. Register A-GNSS content observer                    │    │
│  │  4. Reset all display fields                            │    │
│  │  5. Start all observers (if permissions granted)        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ACTIVE STATE                                   │
│         (Observers running, UI updated in real-time)            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     onPause()                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. Stop location tracking                              │    │
│  │  2. Stop all observers                                  │    │
│  │  3. Unregister A-GNSS content observer                  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. MainActivity (`MainActivity.java`)

The central UI controller that:
- Manages all observer instances and their listeners
- Handles user interactions (buttons, spinners, menu items)
- Updates UI with real-time data from observers
- Controls the location tracking session

### 2. Observer Classes

| Observer | Purpose | Permissions Required |
|----------|---------|---------------------|
| `LocationObserver` | GNSS location, satellite status, measurements | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` |
| `TelephonyObserver` | SIM subscriptions, cell info, network type | `ACCESS_FINE_LOCATION`, `READ_PHONE_STATE` |
| `DataObserver` | Mobile data network availability, SUPL | `ACCESS_NETWORK_STATE`, `READ_PHONE_STATE` |
| `WifiObserver` | WiFi network connections | `ACCESS_NETWORK_STATE` |
| `BlueskyLogObserver` | System log filtering for Bluesky | `READ_LOGS` |

### 3. Data Holder Classes

Display model classes that wrap Android system objects for UI data binding:

```
┌──────────────────────┐     ┌──────────────────────┐
│   LocationHolder     │     │  GnssSvStatusHolder  │
│  ─────────────────   │     │  ─────────────────   │
│  - Location object   │     │  - SVID              │
│  - Formatted lat/lng │     │  - Constellation     │
│  - Accuracy          │     │  - CN0 (signal)      │
│  - Altitude          │     │  - Elevation/Azimuth │
│  - Speed/Bearing     │     │  - Used in fix       │
│  - Timestamp         │     │  - Has carrier freq  │
└──────────────────────┘     └──────────────────────┘

┌──────────────────────┐
│   CellInfoHolder     │
│  ─────────────────   │
│  Subclasses:         │
│  - CellInfoLteHolder │
│  - CellInfoNrHolder  │
│  - CellInfoGsmHolder │
│  - CellInfoCdmaHolder│
│  - etc.              │
└──────────────────────┘
```

---

## Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM SERVICES                       │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ LocationManager │ TelephonyManager│ ConnectivityMgr │  WifiManager  │
└────────┬────────┴────────┬────────┴────────┬────────┴───────┬───────┘
         │                 │                 │                │
         ▼                 ▼                 ▼                ▼
┌────────────────┐┌────────────────┐┌────────────────┐┌───────────────┐
│LocationObserver││TelephonyObserve││  DataObserver  ││ WifiObserver  │
│                ││                 ││                ││               │
│ - Location     ││ - Subscription  ││ - Network      ││ - WiFi info   │
│   Listener     ││   Listener      ││   Callback     ││   Callback    │
│ - GnssStatus   ││ - TelephonyCb   ││                ││               │
│   Callback     ││                 ││                ││               │
│ - Measurements ││                 ││                ││               │
│   Callback     ││                 ││                ││               │
└───────┬────────┘└───────┬─────────┘└───────┬────────┘└───────┬───────┘
        │                 │                  │                 │
        │    Listener     │    Listener      │    Listener     │
        │    Interface    │    Interface     │    Interface    │
        ▼                 ▼                  ▼                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          MAIN ACTIVITY                               │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Observer Listeners                          │  │
│  │  - locationObserverListener    - telephonyObserverListener    │  │
│  │  - dataObserverListener        - wifiObserverListener         │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      UI UPDATE METHODS                         │  │
│  │  - updateGnssStatus()          - updateCellInfo()             │  │
│  │  - updateGnssStatusTable()     - Data Binding setters         │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                     VIEW BINDING (UI)                          │  │
│  │  - layoutLocInfo      - layoutGnssInfo    - layoutNetInfo     │  │
│  │  - svTable            - cellInfoTable     - conf (config)     │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Location Tracking Workflow

### Starting a Location Session

```
┌─────────────────────────────────────────────────────────────────┐
│                  USER PRESSES "RUN" BUTTON                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               doCheckPermission() - Verify permissions           │
└─────────────────────────────────────────────────────────────────┘
                              │
                   ┌──────────┴──────────┐
                   │                     │
              GRANTED               DENIED
                   │                     │
                   ▼                     ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│    doStartLocating()    │   │  Request permissions    │
└─────────────────────────┘   │  → Settings redirect    │
           │                  └─────────────────────────┘
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  doUpdateLocationOptions()                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Read from UI Spinners:                                 │    │
│  │  - Provider: GPS / Fused / Network                      │    │
│  │  - Mode: Single / Track / Last                          │    │
│  │  - Quality: High Accuracy / Balanced / Low Power        │    │
│  │  - Full Tracking: Checkbox state                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              locationObserver.startLocating()                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Based on MODE:                                         │    │
│  │                                                         │    │
│  │  SINGLE/TRACK:                                          │    │
│  │    1. Build LocationRequest with quality settings       │    │
│  │    2. requestLocationUpdates() to LocationManager       │    │
│  │    3. registerGnssStatusCallback()                      │    │
│  │                                                         │    │
│  │  LAST:                                                  │    │
│  │    1. getLastKnownLocation() from LocationManager       │    │
│  │    2. Immediately return cached location                │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GNSS ENGINE STARTS                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  GnssStatusCallback.onStarted()                         │    │
│  │    → Register GnssMeasurementsCallback                  │    │
│  │    → Register GnssNavigationMessageCallback             │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SATELLITE ACQUISITION LOOP                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  onSatelliteStatusChanged() - Called repeatedly         │    │
│  │    → Update satellite table (SVID, CN0, elevation...)   │    │
│  │    → Update status: "Searching" → "Acquiring"           │    │
│  │                                                         │    │
│  │  onGnssMeasurementsReceived() - Called repeatedly       │    │
│  │    → Update full tracking status                        │    │
│  │    → Update multipath count, AGC average                │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FIRST FIX OBTAINED                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  onFirstFix(ttffMillis)                                 │    │
│  │    → Record Time To First Fix (TTFF)                    │    │
│  │    → Update status to "Tracking"                        │    │
│  │                                                         │    │
│  │  onLocationChanged(location)                            │    │
│  │    → Update LocationHolder with new fix                 │    │
│  │    → Stop timer, increment fix count                    │    │
│  │    → Play BEEP sound                                    │    │
│  │    → If SINGLE mode: stop locating                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Location Modes Explained

| Mode | Behavior |
|------|----------|
| **Single** | Request one location fix, then stop automatically |
| **Track** | Continuous location updates until manually stopped |
| **Last** | Return cached last known location immediately (no GNSS engine start) |

### Quality Settings

| Quality | Description |
|---------|-------------|
| **High Accuracy** | Uses all available sensors (GPS, Network, WiFi) |
| **Balanced** | Balance between accuracy and power consumption |
| **Low Power** | Prioritize battery over accuracy |

---

## Network Monitoring Workflow

### Telephony Observer Flow

```
┌─────────────────────────────────────────────────────────────────┐
│              SubscriptionManager.addOnSubscriptionsChangedListener│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                checkAndUpdateSubCbs()                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  For each SIM slot (0, 1, ...):                         │    │
│  │    1. Get active SubscriptionInfo                       │    │
│  │    2. If SIM changed:                                   │    │
│  │       - Create new TelephonyCb for subscription         │    │
│  │       - Register TelephonyCallback                      │    │
│  │       - Notify listener: onSubscriptionChanged()        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  TelephonyCb Callbacks                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  onCellInfoChanged()      → Update cell info table      │    │
│  │  onDisplayInfoChanged()   → Update network type display │    │
│  │  onDataActivity()         → Update data direction (↑↓)  │    │
│  │  onServiceStateChanged()  → Request cell info update    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Data Observer Flow

```
┌─────────────────────────────────────────────────────────────────┐
│         ConnectivityManager.registerNetworkCallback              │
│         (for CELLULAR networks with SUPL capability)            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  NetworkCallback Events                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  onAvailable()       → Add network to active map        │    │
│  │  onLost()            → Remove network from map          │    │
│  │  onCapabilitiesChanged() → Update network capabilities  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│          onDataNetworksChanged() → MainActivity                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Updates displayed:                                     │    │
│  │  - Data status (SIM1, SIM2, SUPL)                      │    │
│  │  - Roaming status (Home/Roaming + MCC-MNC)             │    │
│  │  - SUPL APN name                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Background Logging Service

### BlueskyTrackService Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│          Menu → "Bluesky Check" → "TRACK" Button                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 startService(BlueskyTrackService)                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     onStartCommand()                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. Check READ_LOGS permission (requires ADB grant)     │    │
│  │  2. Create notification channel                         │    │
│  │  3. Start foreground service with notification          │    │
│  │  4. Start BlueskyLogObserver                            │    │
│  │  5. Schedule periodic notification updates              │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  BlueskyLogObserver                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Filters logcat for patterns:                           │    │
│  │                                                         │    │
│  │  - LocationManagerService:*  → All LMS logs             │    │
│  │  - LocSvc_ApiV02:*          → Qualcomm location service │    │
│  │  - BlueskyManager:*         → Bluesky positioning logs  │    │
│  │  - BlueskyRegistrant:*      → Bluesky registration      │    │
│  │  - GCoreFlp:*               → Google fused location     │    │
│  │                                                         │    │
│  │  Regex filters:                                         │    │
│  │  - .*hasEnvironmentBearing.* → Environment bearing logs │    │
│  │  - .*(Bluesky...|GCoreFlp):* → Bluesky-specific logs    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Log Buffers (max 10,000 lines each)             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  mAllLogsBuffer     → All captured logs                 │    │
│  │  mEnvLogsBuffer     → Environment bearing logs only     │    │
│  │  mBlueskyLogsBuffer → Bluesky-specific logs only        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│            Notification updated every 1 second                   │
│            showing: Total / Env Bearing / Bluesky counts         │
└─────────────────────────────────────────────────────────────────┘
```

---

## User Interaction Flow

### Main UI Controls

```
┌─────────────────────────────────────────────────────────────────┐
│                         TOOLBAR MENU                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Geocode        → Reverse geocode current location      │    │
│  │  Open in Map    → Launch maps app with coordinates      │    │
│  │  Radio Info     → Open hidden radio info settings       │    │
│  │  Airplane Mode  → Open airplane mode settings           │    │
│  │  Location       → Open location settings                │    │
│  │  App Info       → Open app details in settings          │    │
│  │  NFW Info       → Check Non-Framework Location access   │    │
│  │  Logalong       → Launch companion logging app          │    │
│  │  GMS Update     → Open Play Store for GMS update        │    │
│  │  Read Config    → View GPS configuration files          │    │
│  │  Bluesky Check  → Test Bluesky capabilities + tracking  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      CONTROL SECTION                             │
│  ┌──────────────┬──────────────┬──────────────┬─────────────┐   │
│  │   Provider   │     Mode     │   Quality    │ Full Track  │   │
│  │   Spinner    │   Spinner    │   Spinner    │  Checkbox   │   │
│  ├──────────────┼──────────────┼──────────────┼─────────────┤   │
│  │ • GPS        │ • Single     │ • High Acc   │ □ Enable    │   │
│  │ • Fused      │ • Track      │ • Balanced   │             │   │
│  │ • Network    │ • Last       │ • Low Power  │             │   │
│  └──────────────┴──────────────┴──────────────┴─────────────┘   │
│                                                                  │
│  ┌───────────────────────────┬──────────────────────────────┐   │
│  │        RUN Button         │       DELETE Button          │   │
│  │   (Toggle: Start/Stop)    │  (Delete aiding data)        │   │
│  │                           │  Long press: Configure       │   │
│  └───────────────────────────┴──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Delete Aiding Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    SHORT PRESS: Delete Button                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Delete currently selected aiding data (default: "all") │    │
│  │  Shows countdown dialog (5 seconds)                     │    │
│  │  Sends delete_aiding_data command to LocationManager    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    LONG PRESS: Delete Button                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Opens multi-select dialog with options:                │    │
│  │  □ all           □ ephemeris      □ almanac             │    │
│  │  □ position      □ time           □ iono                │    │
│  │  □ utc           □ health         □ svdir               │    │
│  │  □ svsteer       □ sadata         □ rti                 │    │
│  │  □ celldb-info                                          │    │
│  │                                                         │    │
│  │  Presets shown:                                         │    │
│  │  - Warm: ephemeris, utc                                 │    │
│  │  - Cold: all                                            │    │
│  │                                                         │    │
│  │  Buttons: [Delete] [Save] [Cancel]                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary

The PosModeTest application follows a clean observer-based architecture:

1. **Separation of Concerns**: Each observer handles a specific system service
2. **Listener Pattern**: Observers communicate changes via listener interfaces
3. **Data Binding**: Holder classes wrap system objects for clean UI binding
4. **Executor Management**: Background work uses dedicated executors, UI updates on main thread
5. **Lifecycle Aware**: All observers start/stop with activity lifecycle
6. **Modular Design**: Background service operates independently for log capture

This design makes the app maintainable and allows each component to be tested independently while providing a comprehensive GNSS debugging experience.
