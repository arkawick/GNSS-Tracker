package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellInfo;
import android.telephony.SignalStrength;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoHolder {

    @NonNull
    private final CellInfo mCellInfo;
    private final int mSlot;

    public CellInfoHolder(int slot, @NonNull CellInfo cellInfo) {
        mSlot = slot;
        mCellInfo = cellInfo;
    }

    public String getSlot() {
        return mSlot < 0 ? EMPTY_TEXT_1C : Objects.toString(mSlot+1);
    }

    public String getType() {
        return "?";
    }

    public String getExtendedType() {
        String type = getType();
        if (mCellInfo.isRegistered()) {
            type = type + "+R";
        }
        return type;
    }

    public String getPhyId() {
        return EMPTY_TEXT_1C;
    }

    public String getPLMN() {
        return EMPTY_TEXT_1C;
    }

    public String getStrength() {
        int dbm = mCellInfo.getCellSignalStrength().getDbm();
        return dbm == SignalStrength.INVALID ? EMPTY_TEXT_1C : Objects.toString(dbm);
    }

    public String getFrequency() {
        return EMPTY_TEXT_1C;
    }
}

