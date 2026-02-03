package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_1C;

import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CellInfoCdmaHolder extends CellInfoHolder {

    @NonNull
    private final CellInfoCdma mCellInfo;

    public CellInfoCdmaHolder(int slot,@NonNull CellInfoCdma cellInfo) {
        super(slot, cellInfo);
        mCellInfo = cellInfo;
    }

    @Override
    public String getType() {
        return "C2K";
    }

    @Override
    public String getPhyId() {
        var cellId = (CellIdentityCdma) mCellInfo.getCellIdentity();
        int phyId = cellId.getSystemId();
        return phyId == CellInfo.UNAVAILABLE ? EMPTY_TEXT_1C : Objects.toString(phyId);
    }

    @Override
    public String getPLMN() {
        var cellId = (CellIdentityCdma) mCellInfo.getCellIdentity();
        int nid = cellId.getNetworkId();
        int bid = cellId.getBasestationId();
        return (nid == CellInfo.UNAVAILABLE || bid == CellInfo.UNAVAILABLE) ? EMPTY_TEXT_1C
                : String.format("%s-%s", nid, bid);
    }

    @Override
    public String getFrequency() {
        return Objects.toString(CellInfo.UNAVAILABLE);
    }
}
