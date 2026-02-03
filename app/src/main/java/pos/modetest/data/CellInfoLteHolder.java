package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoLteHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoLte mCellInfo;

    public CellInfoLteHolder(int slot, @NonNull CellInfoLte cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "LTE";
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityLte) mCellInfo.getCellIdentity();
        int phyId = cellId.getPci();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityLte) mCellInfo.getCellIdentity();
        String mcc = cellId.getMccString();
        String mnc = cellId.getMncString();
        return (mcc == null || mnc == null) ? EMPTY_TEXT_1C : String.format("%s-%s", mcc, mnc);
    }

    @Override
    public String getFrequency() {
        var cellId = (CellIdentityLte) mCellInfo.getCellIdentity();
        int freq = cellId.getEarfcn();
        return freq == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(freq);
    }
}
