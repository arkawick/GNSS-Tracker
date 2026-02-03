package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityTdscdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoTdscdma;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoTdscdmaHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoTdscdma mCellInfo;

    public CellInfoTdscdmaHolder(int slot, @NonNull CellInfoTdscdma cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "3G"; // FDD W-CDMA
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityTdscdma) mCellInfo.getCellIdentity();
        int phyId = cellId.getCid();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityTdscdma) mCellInfo.getCellIdentity();
        String mcc = cellId.getMccString();
        String mnc = cellId.getMncString();
        return (mcc == null || mnc == null) ? EMPTY_TEXT_1C : String.format("%s-%s", mcc, mnc);
    }

    @Override
    public String getFrequency() {
        var cellId = (CellIdentityTdscdma) mCellInfo.getCellIdentity();
        int freq = cellId.getUarfcn();
        return freq == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(freq);
    }
}
