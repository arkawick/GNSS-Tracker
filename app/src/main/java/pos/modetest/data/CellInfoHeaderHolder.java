package pos.modetest.data;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import pos.modetest.R;

public class CellInfoHeaderHolder extends CellInfoHolder {

    private final Resources mResources;

    public CellInfoHeaderHolder(@NonNull Context context) {
        // noinspection DataFlowIssue
        super(-1, null);
        mResources = context.getResources();
    }

    @Override
    public String getSlot() {
        return mResources.getString(R.string.tbl_cell_c1_sim);
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public String getExtendedType() {
        return mResources.getString(R.string.tbl_cell_c2_type);
    }

    @Override
    public String getPhyId() {
        return mResources.getString(R.string.tbl_cell_c3_phy_id);
    }

    @Override
    public String getPLMN() {
        return mResources.getString(R.string.tbl_cell_c4_plmn);
    }

    @Override
    public String getStrength() {
        return mResources.getString(R.string.tbl_cell_c5_dbm);
    }

    @Override
    public String getFrequency() {
        return mResources.getString(R.string.tbl_cell_c6_freq);
    }
}
