package pos.modetest.data;

import android.content.Context;
import android.content.res.Resources;
import android.location.GnssStatus;

import androidx.annotation.NonNull;

import java.util.Locale;

import pos.modetest.R;
import pos.modetest.utils.Constants;
import pos.modetest.utils.HelperUtils;

public class GnssSvStatusHolder {
    @NonNull
    private final GnssStatus mSvStatus;
    private final int mSvId;
    private final Resources mResources;

    private static final int PRN_OFFSET_QXDM_GLONASS = 64;
    private static final int PRN_OFFSET_QXDM_BEIDOU = 200;
    private static final int PRN_OFFSET_QXDM_GALILEO = 300;

    public GnssSvStatusHolder(@NonNull Context context, @NonNull GnssStatus svStatus, int svId) {
        mSvStatus = svStatus;
        mSvId = svId;
        mResources = context.getResources();
    }

    public int getConstellationAsColor() {
        return mResources.getColor(switch (mSvStatus.getConstellationType(mSvId)) {
            case GnssStatus.CONSTELLATION_BEIDOU -> R.color.sv_beidou;
            case GnssStatus.CONSTELLATION_GALILEO -> R.color.sv_galileo;
            case GnssStatus.CONSTELLATION_GLONASS -> R.color.sv_glonass;
            case GnssStatus.CONSTELLATION_GPS -> R.color.sv_gps;
            case GnssStatus.CONSTELLATION_IRNSS -> R.color.sv_irnss;
            case GnssStatus.CONSTELLATION_QZSS -> R.color.sv_qzss;
            case GnssStatus.CONSTELLATION_SBAS -> R.color.sv_sbas;
            case GnssStatus.CONSTELLATION_UNKNOWN -> R.color.sv_unknown;
            default -> android.R.color.transparent;
        }, null);
    }

    public String getPrn() {
        int prn = mSvStatus.getSvid(mSvId);
        // noinspection SwitchIntDef
        prn += switch (mSvStatus.getConstellationType(mSvId)) {
            case GnssStatus.CONSTELLATION_BEIDOU -> PRN_OFFSET_QXDM_BEIDOU;
            case GnssStatus.CONSTELLATION_GALILEO -> PRN_OFFSET_QXDM_GALILEO;
            case GnssStatus.CONSTELLATION_GLONASS -> PRN_OFFSET_QXDM_GLONASS;
            default -> 0;
        };
        return String.valueOf(prn);
    }

    public String getCNRatio() {
        float cn0 = mSvStatus.getCn0DbHz(mSvId);
        return Float.compare(cn0, 0.0f) != 0
                ? String.format(Locale.getDefault(), "%.02f", cn0) : Constants.EMPTY_TEXT_1C;
    }

    public String getFrequency() {
        if (!mSvStatus.hasCarrierFrequencyHz(mSvId)) return Constants.EMPTY_TEXT_1C;
        final double MHZ = 1_000_000d;
        double freqMHz = mSvStatus.getCarrierFrequencyHz(mSvId) / MHZ;
        int type = mSvStatus.getConstellationType(mSvId);
        // noinspection SwitchIntDef
        String sig = switch (type) {
            case GnssStatus.CONSTELLATION_GPS -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1575.42, 1d)) {
                    yield "L1";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1227.60, 1d)) {
                    yield "L2";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "L5";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_GLONASS -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1598.0625, 1607.0625, 0.1d)) {
                    yield "L1";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1242.9375, 1249.9375, 0.1d)) {
                    yield "L2";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1201.743, 1208.511, 0.1d)) {
                    yield "L3";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_GALILEO -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1575.42, 1d)) {
                    yield "E1";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1278.75, 1d)) {
                    yield "E6";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1191.795, 1d)) {
                    yield "E5";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "E5a";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1207.14, 1d)) {
                    yield "E5b";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_BEIDOU -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1561.098, 1d)) {
                    yield "B1I";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1575.42, 1d)) {
                    yield "B1C";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "B2a";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1207.14, 1d)) {
                    yield "B2b";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1268.52, 1d)) {
                    yield "B3";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_QZSS -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1575.42, 1d)) {
                    yield "L1";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1227.6, 1d)) {
                    yield "L2";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "L5";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1278.75, 1d)) {
                    yield "L6";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_IRNSS -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "L5";
                } else if (HelperUtils.checkFreqBand(freqMHz, 2492.028, 1d)) {
                    yield "S";
                } else {
                    yield null;
                }
            }
            case GnssStatus.CONSTELLATION_SBAS -> {
                if (HelperUtils.checkFreqBand(freqMHz, 1575.42, 1d)) {
                    yield "L1";
                } else if (HelperUtils.checkFreqBand(freqMHz, 1176.45, 1d)) {
                    yield "L5";
                } else {
                    yield null;
                }
            }
            default -> null;
        };
        return sig != null ? sig
                : String.format(Locale.getDefault(), "%.03f", freqMHz/1000);
    }

    public String getFlags() {
        return (mSvStatus.hasAlmanacData(mSvId) ? "A" : Constants.EMPTY_TEXT_1C) +
                (mSvStatus.hasEphemerisData(mSvId) ? "E" : Constants.EMPTY_TEXT_1C) +
                (mSvStatus.usedInFix(mSvId) ? "U" : Constants.EMPTY_TEXT_1C);
    }
}
