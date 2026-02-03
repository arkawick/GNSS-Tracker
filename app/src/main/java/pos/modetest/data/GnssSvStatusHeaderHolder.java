package pos.modetest.data;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import pos.modetest.R;

public class GnssSvStatusHeaderHolder extends GnssSvStatusHolder {

    private final Resources mResources;

    public GnssSvStatusHeaderHolder(@NonNull Context context) {
        // noinspection DataFlowIssue
        super(context, null, 0);
        mResources = context.getResources();
    }

    @Override
    public int getConstellationAsColor() {
        return mResources.getColor(R.color.tbl_header_bg, null);
    }

    @Override
    public String getPrn() {
        return mResources.getString(R.string.tbl_gnss_c1_prn);
    }

    @Override
    public String getCNRatio() {
        return mResources.getString(R.string.tbl_gnss_c2_cn_ratio);
    }

    @Override
    public String getFrequency() {
        return mResources.getString(R.string.tbl_gnss_c3_freq);
    }

    @Override
    public String getFlags() {
        return mResources.getString(R.string.tbl_gnss_c4_flag);
    }
}
