package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityGsm;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoGsmHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoGsm mCellInfo;

    public CellInfoGsmHolder(int slot, @NonNull CellInfoGsm cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "2G";
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityGsm) mCellInfo.getCellIdentity();
        int phyId = cellId.getCid();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityGsm) mCellInfo.getCellIdentity();
        String mcc = cellId.getMccString();
        String mnc = cellId.getMncString();
        return (mcc == null || mnc == null) ? EMPTY_TEXT_1C : String.format("%s-%s", mcc, mnc);
    }

    @Override
    public String getFrequency() {
        var cellId = (CellIdentityGsm) mCellInfo.getCellIdentity();
        int freq = cellId.getArfcn();
        return freq == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(freq);
    }
}
