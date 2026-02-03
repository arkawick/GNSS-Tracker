package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoNr;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoNrHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoNr mCellInfo;

    public CellInfoNrHolder(int slot,@NonNull CellInfoNr cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "NR";
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityNr) mCellInfo.getCellIdentity();
        int phyId = cellId.getPci();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityNr) mCellInfo.getCellIdentity();
        String mcc = cellId.getMccString();
        String mnc = cellId.getMncString();
        return (mcc == null || mnc == null) ? EMPTY_TEXT_1C : String.format("%s-%s", mcc, mnc);
    }

    @Override
    public String getFrequency() {
        var cellId = (CellIdentityNr) mCellInfo.getCellIdentity();
        int freq = cellId.getNrarfcn();
        return freq == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(freq);
    }
}
