package pos.modetest.utils;

import static pos.modetest.utils.Constants.TAG_PREFIX;
import static pos.modetest.utils.FormatUtils.formatDateTime;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigUtils {
    private static final String TAG = TAG_PREFIX + "ConfigUtils";

    public static final String PATH_GPS_DEBUG_CONF = "/system/etc/gps_debug.conf";
    public static final String PATH_GPS_CONF = "/vendor/etc/gps.conf";

    public static final String PROP_GPS_CONF_NFW_CP = "NFW_CLIENT_CP";
    public static final String PROP_GPS_CONF_NFW_SUPL = "NFW_CLIENT_SUPL";

    public static final List<String> SYSPROP_KEYS = List.of(
            "persist.sys.gps.lpp",
            "persist.sys.gps.emergencypdn"
    );
    public static final List<String> SYSPROP_NAMES = List.of(
            "lpp_profile",
            "use_emergency_pdn_for_emergency_supl"
    );

    public static final String CC_PREFIX_GPS = "gps.";
    public static final String CC_GPS_NFW_PROXY_APPS = "gps.nfw_proxy_apps";

    public static final String RESPROP_PKG = "android";
    public static final Map<String, String> RESPROP_KEYS = Map.ofEntries(
            Map.entry("config_gnssParameters", "array"),
            Map.entry("config_enableNetworkLocationOverlay", "bool"),
            Map.entry("config_networkLocationProviderPackageName", "string"),
            Map.entry("config_enableFusedLocationOverlay", "bool"),
            Map.entry("config_fusedLocationProviderPackageName", "string"),
            Map.entry("config_useGnssHardwareProvider", "bool"),
            Map.entry("config_gnssLocationProviderPackageName", "string"),
            Map.entry("config_locationExtraPackageNames", "array"),
            Map.entry("config_enableGeocoderOverlay", "bool"),
            Map.entry("config_geocoderProviderPackageName", "string"),
            Map.entry("config_enableGeofenceOverlay", "bool"),
            Map.entry("config_geofenceProviderPackageName", "string"),
            Map.entry("config_enableGnssAssistanceOverlay", "bool"),
            Map.entry("config_gnssAssistanceProviderPackageName", "string"),
            Map.entry("config_locationProviderPackageNames", "array")
    );

    private static final String[] DUMPSYS_LOC_ARGS = new String[] {"location"};
    private static final String[] DUMPSYS_GMS_LMS_ARGS = new String[] {
            "activity",
            "service",
            "com.google.android.gms/com.google.android.location.internal.GoogleLocationManagerService"
    };
    private static final String[] DUMPSYS_GMS_LS_ARGS = new String[] {
            "activity",
            "service",
            "com.google.android.gms/com.google.android.location.internal.server.GoogleLocationService"
    };
    private static final String[] DUMPSYS_GMS_E911_ARGS = new String[] {
            "activity",
            "provider",
            "com.google.android.gms/.thunderbird.config.EmergencyConfigContentProvider"
    };
    private static final String[] DUMPSYS_SUB_MGR_ARGS = new String[] {"isub"};
    private static final String[] DUMPSYS_BATTERY_ARGS = new String[] {"battery"};

    public static final List<ConfigTypes> DEFAULT_CONFIG_TYPES = List.of(
            ConfigTypes.GPS_DEBUG,
            ConfigTypes.SYSPROP,
            ConfigTypes.CARRIER_CONFIG,
            ConfigTypes.GPS_VENDOR,
            ConfigTypes.RESPROP
    );

    public static final Map<ConfigTypes, String> TYPE_TITLES = Map.ofEntries(
            Map.entry(ConfigTypes.GPS_DEBUG, PATH_GPS_DEBUG_CONF),
            Map.entry(ConfigTypes.SYSPROP, "Sysprop"),
            Map.entry(ConfigTypes.CARRIER_CONFIG, "CarrierConfig"),
            Map.entry(ConfigTypes.GPS_VENDOR, PATH_GPS_CONF),
            Map.entry(ConfigTypes.BUILD_PROP, "Sysprop (Build)"),
            Map.entry(ConfigTypes.RESPROP, "res/config.xml"),
            Map.entry(ConfigTypes.DUMP_LOC, "dumpsys ("+ DUMPSYS_LOC_ARGS[0] +")"),
            Map.entry(ConfigTypes.DUMP_GMS_LMS, "dumpsys ("+ DUMPSYS_GMS_LMS_ARGS[2] +")"),
            Map.entry(ConfigTypes.DUMP_GMS_LS, "dumpsys ("+ DUMPSYS_GMS_LS_ARGS[2] +")"),
            Map.entry(ConfigTypes.DUMP_GMS_E911, "dumpsys ("+ DUMPSYS_GMS_E911_ARGS[2] +")"),
            Map.entry(ConfigTypes.DUMP_SUB_MGR, "dumpsys ("+ DUMPSYS_SUB_MGR_ARGS[0] +")"),
            Map.entry(ConfigTypes.DUMP_BATTERY, "dumpsys ("+ DUMPSYS_BATTERY_ARGS[0] +")")
    );

    public static String readSysProp(@NonNull String propKey) {
        String prop = HelperUtils.execCommandSync(new String[]{
                "getprop",
                propKey
        }).trim();
        Log.v(TAG, String.format("readSysProp(%s) -> %s", propKey, prop));
        return prop;
    }

    public static String readAllSysProps(@NonNull List<String> keys, @Nullable List<String> names) {
        StringBuilder buf = new StringBuilder();
        if (names != null && keys.size() != names.size()) {
            throw new IndexOutOfBoundsException("keys and names should have the same size");
        }
        for (int i = 0; i < keys.size(); i++) {
            String propKey = keys.get(i);
            String prop = readSysProp(propKey);
            if (prop.isBlank()) {
                continue;
            }
            String name = names != null ? names.get(i) : "";
            String displayKey = name.isBlank() ? propKey : name.toUpperCase();
            buf.append(String.format("%s=%s",
                    displayKey, prop
            ));
            buf.append("\n");
        }
        return buf.toString();
    }

    @Nullable
    public static String readCarrierConfig(@NonNull Context context, @NonNull String key) {
        Log.d(TAG, String.format("readCarrierConfig(%s)", key));
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        var ccm = context.getSystemService(CarrierConfigManager.class);
        var cc = ccm.getConfig();
        if (cc == null || cc.size() <= 2) {
            Log.w(TAG, "CarrierConfig is not available because SIM is not ready");
            return null;
        }
        return Objects.requireNonNullElse(cc.get(key), "").toString();
    }

    public static String getNfwLocationPackage(@NonNull Context context) {
        final String LOC_ATTRIB_PKG = "com.sonymobile.gps.locationattribution";
        String pkg = readCarrierConfig(context, CC_GPS_NFW_PROXY_APPS);
        if (pkg == null || pkg.isBlank())
            pkg = readConfFileItem(PATH_GPS_CONF, PROP_GPS_CONF_NFW_CP);
        if (pkg == null || pkg.isBlank())
            pkg = readConfFileItem(PATH_GPS_CONF, PROP_GPS_CONF_NFW_SUPL);
        if (pkg == null || pkg.isBlank())
            pkg = LOC_ATTRIB_PKG;
        Log.d(TAG, "getNfwLocationPackage() -> " + pkg);
        return pkg;
    }

    public static String readAllCarrierConfig(@NonNull Context context, @NonNull String keyPrefix) {
        Log.d(TAG, String.format("readAllCarrierConfig(%s)", keyPrefix));
        StringBuilder buf = new StringBuilder();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return buf.toString();
        }
        var ccm = context.getSystemService(CarrierConfigManager.class);
        var cc = ccm.getConfig();
        if (cc == null || cc.size() <= 2) {
            Log.w(TAG, "CarrierConfig is not available because SIM is not ready");
            return buf.toString();
        }
        var keyList = cc.keySet().stream()
                .filter(k -> k.startsWith(keyPrefix))
                .sorted()
                .toArray(String[]::new);
        for (var key : keyList) {
            var config = Objects.requireNonNullElse(cc.get(key), "").toString();
            if (config.isBlank()) {
                continue;
            }
            var keyParts = key.split("\\.");
            var displayKey = (keyParts.length != 0 ? keyParts[keyParts.length - 1] : key)
                    .toUpperCase();
            buf.append(String.format("%s=%s\n", displayKey, config));
        }
        return buf.toString();
    }

    @NonNull
    public static String getTitleForCarrierConfig() {
        try {
            int sub = SubscriptionManager.getDefaultSubscriptionId();
            int slot = SubscriptionManager.getSlotIndex(sub);
            if (slot == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                return "none";
            } else {
                return "SIM" + (slot + 1);
            }
        } catch (Exception e) {
            Log.w(TAG, "getTitleForCarrierConfig", e);
            return "";
        }
    }

    public static String readAllResourceProps(@NonNull Context context,
                                              @NonNull Map<String, String> propKeys,
                                              @NonNull String pkg) {
        final String NULL_REF = "@0";
        StringBuilder buf = new StringBuilder();
        try {
            var pm = context.getPackageManager();
            var res = pm.getResourcesForApplication(pkg);
            propKeys.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(set -> {
                var key = set.getKey();
                var type = set.getValue();
                // noinspection DiscouragedApi
                var id = res.getIdentifier(key, type, pkg);
                var prop = "";
                if (id != 0) {
                    if (type.equals("array")) {
                        try (TypedArray ta = res.obtainTypedArray(id)) {
                            List<String> items = new ArrayList<>();
                            for (int i = 0; i < ta.length(); i++) {
                                var tmp = ta.getString(i);
                                if (tmp != null && !tmp.isBlank()) {
                                    items.add(tmp);
                                }
                            }
                            if (items.isEmpty())
                                return;
                            prop = items.toString();
                        }
                    } else {
                        TypedValue tv = new TypedValue();
                        res.getValue(id, tv, true);
                        prop = tv.coerceToString().toString();
                    }
                    if (NULL_REF.equals(prop))
                        return;
                }
                Log.v(TAG, String.format("readAllResourceProps(%s) -> %s", key, prop));
                buf.append(String.format("%s=%s\n",
                        key.replaceFirst("config_", ""), prop));
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return buf.toString();
    }

    public static String readConfFileItem(@NonNull String filepath, @NonNull String item) {
        Log.d(TAG, String.format("readConfFileItem(%s, %s)", filepath, item));
        String buf = "";
        File f = new File(filepath);
        try (var fStream = new FileInputStream(f)) {
            var props = new Properties();
            props.load(fStream);
            if (!props.isEmpty()) {
                buf = props.getProperty(item, buf);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return buf;
    }

    public static String readConfFile(@NonNull String filepath, boolean sorted) {
        Log.d(TAG, String.format("readConfFile(%s)", filepath));
        StringBuilder buf = new StringBuilder();
        File f = new File(filepath);
        try (var fStream = new FileInputStream(f)) {
            if (sorted) {
                var props = new Properties();
                props.load(fStream);
                if (!props.isEmpty()) {
                    var sortedKeys = props.stringPropertyNames().stream()
                            .sorted()
                            .toArray(String[]::new);
                    for (var key : sortedKeys) {
                        buf.append(String.format("%s=%s\n", key, props.getProperty(key)));
                    }
                }
            } else {
                var br = new BufferedReader(new InputStreamReader(fStream));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#"))
                        continue;
                    var propSet = line.split("=", 2);
                    var prop = propSet[0].strip();
                    var value = propSet[1].strip();
                    if (prop.isBlank() || value.isBlank())
                        continue;
                    buf.append(String.format("%s=%s\n", prop, value));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return buf.toString();
    }

    public static String readDumpsys(@NonNull String[] args) {
        String[] cmd = new String[1 + args.length];
        cmd[0] = "dumpsys";
        System.arraycopy(args, 0, cmd, 1, args.length);
        String dump = HelperUtils.execCommandSync(cmd);
        Log.v(TAG, String.format("readDumpsys(%s) -> %d", String.join(" ", args), dump.length()));
        return dump;
    }

    public static String readBuildProps() {
        String props = "[Build.*]"
                + "\nBOARD            : " + Build.BOARD
                + "\nBOOTLOADER       : " + Build.BOOTLOADER
                + "\nBRAND            : " + Build.BRAND
                + "\nDEVICE           : " + Build.DEVICE
                + "\nDISPLAY          : " + Build.DISPLAY
                + "\nFINGERPRINT      : " + Build.FINGERPRINT
                + "\nHARDWARE         : " + Build.HARDWARE
                + "\nHOST             : " + Build.HOST
                + "\nID               : " + Build.ID
                + "\nMANUFACTURER     : " + Build.MANUFACTURER
                + "\nMODEL            : " + Build.MODEL
                + "\nODM_SKU          : " + Build.ODM_SKU
                + "\nPRODUCT          : " + Build.PRODUCT
                + "\nSKU              : " + Build.SKU
                + "\nSOC_MANUFACTURER : " + Build.SOC_MANUFACTURER
                + "\nSOC_MODEL        : " + Build.SOC_MODEL
                + "\nSUPPORTED_32_BIT_ABIS :" + Arrays.toString(Build.SUPPORTED_32_BIT_ABIS)
                + "\nSUPPORTED_64_BIT_ABIS :" + Arrays.toString(Build.SUPPORTED_64_BIT_ABIS)
                + "\nSUPPORTED_ABIS        :" + Arrays.toString(Build.SUPPORTED_ABIS)
                + "\nTAGS             : " + Build.TAGS
                + "\nTIME             : " + Build.TIME + " (" + formatDateTime(Build.TIME) + ")"
                + "\nTYPE             : " + Build.TYPE
                + "\nUSER             : " + Build.USER
                + "\n[Build.VERSION.*]"
                + "\n  BASE_OS                    : " + Build.VERSION.BASE_OS
                + "\n  CODENAME                   : " + Build.VERSION.CODENAME
                + "\n  INCREMENTAL                : " + Build.VERSION.INCREMENTAL
                + "\n  MEDIA_PERFORMANCE_CLASS    : " + Build.VERSION.MEDIA_PERFORMANCE_CLASS
                + "\n  PREVIEW_SDK_INT            : " + Build.VERSION.PREVIEW_SDK_INT
                + "\n  RELEASE                    : " + Build.VERSION.RELEASE
                + "\n  RELEASE_OR_CODENAME        : " + Build.VERSION.RELEASE_OR_CODENAME
                + "\n  RELEASE_OR_PREVIEW_DISPLAY : " +
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ? Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY : "")
                + "\n  SDK_INT                    : " + Build.VERSION.SDK_INT
                + "\n  SDK_INT_FULL               : " + Build.VERSION.SDK_INT_FULL
                + "\n  SECURITY_PATCH             : " + Build.VERSION.SECURITY_PATCH
                + "\n[Build.*()]"
                + "\ngetRadioVersion  : " + Build.getRadioVersion()
                + "\ngetSerial        : " + Build.getSerial()
                + "\ngetFingerprintedPartitions : \n" + Build.getFingerprintedPartitions()
                        .stream().map(p -> String.format("  %10s : %s", p.getName(), p.getFingerprint()))
                        .collect(Collectors.joining("\n"))
                + "\n";
        Log.v(TAG, String.format("readBuildProps() -> %d", props.length()));
        return props;
    }

    public static String readConfigByType(@NonNull Context context, ConfigTypes type) {
        switch (type) {
            case DUMP_LOC,
                    DUMP_GMS_LMS,
                    DUMP_GMS_LS,
                    DUMP_GMS_E911,
                    DUMP_SUB_MGR,
                    DUMP_BATTERY -> {
                var dump_perms = new String[]{
                        Manifest.permission.DUMP,
                        Manifest.permission.PACKAGE_USAGE_STATS,
                        Manifest.permission.BATTERY_STATS,
                        "android.permission.INTERACT_ACROSS_USERS"
                };
                if (!HelperUtils.checkPermissions(context, dump_perms)) {
                    return "Please Grant all of the following protected permissions\n"
                            + String.join("\n", dump_perms) + "\n";
                }
            }
        }
        return switch (type) {
            case GPS_DEBUG -> readConfFile(PATH_GPS_DEBUG_CONF, true);
            case SYSPROP -> readAllSysProps(ConfigUtils.SYSPROP_KEYS, SYSPROP_NAMES);
            case CARRIER_CONFIG -> readAllCarrierConfig(context, CC_PREFIX_GPS);
            case GPS_VENDOR -> readConfFile(ConfigUtils.PATH_GPS_CONF, false);
            case BUILD_PROP -> readBuildProps();
            case RESPROP -> readAllResourceProps(context, RESPROP_KEYS, RESPROP_PKG);
            case DUMP_LOC -> readDumpsys(DUMPSYS_LOC_ARGS);
            case DUMP_GMS_LMS -> readDumpsys(DUMPSYS_GMS_LMS_ARGS);
            case DUMP_GMS_LS -> readDumpsys(DUMPSYS_GMS_LS_ARGS);
            case DUMP_GMS_E911 -> readDumpsys(DUMPSYS_GMS_E911_ARGS);
            case DUMP_SUB_MGR -> readDumpsys(DUMPSYS_SUB_MGR_ARGS);
            case DUMP_BATTERY -> readDumpsys(DUMPSYS_BATTERY_ARGS);
        };
    }

    @RequiresPermission(value = Manifest.permission.DUMP, conditional = true)
    public static String readConfigsByType(@NonNull Context context, Collection<ConfigTypes> types) {
        StringBuilder buf = new StringBuilder();
        for(var type : types) {
            var section = readConfigByType(context, type);
            if(!section.isBlank()) {
                buf.append("# ");
                buf.append(TYPE_TITLES.get(type));
                if (type == ConfigTypes.CARRIER_CONFIG) {
                    var extra = getTitleForCarrierConfig();
                    if (!extra.isBlank()) {
                        buf.append(" (");
                        buf.append(extra);
                        buf.append(")");
                    }
                }
                buf.append("\n");
                buf.append(section);
                buf.append("-------\n");
            }
        }
        return buf.toString();
    }

    public enum ConfigTypes {
        GPS_DEBUG,
        SYSPROP,
        CARRIER_CONFIG,
        GPS_VENDOR,
        BUILD_PROP,
        RESPROP,
        DUMP_LOC,
        DUMP_GMS_LMS,
        DUMP_GMS_LS,
        DUMP_GMS_E911,
        DUMP_SUB_MGR,
        DUMP_BATTERY,
    }
}
