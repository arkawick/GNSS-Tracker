package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoWcdma;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoWcdmaHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoWcdma mCellInfo;

    public CellInfoWcdmaHolder(int slot,@NonNull CellInfoWcdma cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "3G";
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityWcdma) mCellInfo.getCellIdentity();
        int phyId = cellId.getPsc();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityWcdma) mCellInfo.getCellIdentity();
        String mcc = cellId.getMccString();
        String mnc = cellId.getMncString();
        return (mcc == null || mnc == null) ? EMPTY_TEXT_1C : String.format("%s-%s", mcc, mnc);
    }

    @Override
    public String getFrequency() {
        var cellId = (CellIdentityWcdma) mCellInfo.getCellIdentity();
        int freq = cellId.getUarfcn();
        return freq == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(freq);
    }
}
