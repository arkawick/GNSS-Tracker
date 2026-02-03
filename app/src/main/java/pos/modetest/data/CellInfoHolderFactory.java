package pos.modetest.data;

import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;

import androidx.annotation.NonNull;

public class CellInfoHolderFactory {
    public static CellInfoHolder makeFor(int slot, @NonNull CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoNr) {
            return new CellInfoNrHolder(slot, (CellInfoNr) cellInfo);
        } else if (cellInfo instanceof CellInfoLte) {
            return new CellInfoLteHolder(slot, (CellInfoLte) cellInfo);
        } else if (cellInfo instanceof CellInfoCdma) {
            return new CellInfoCdmaHolder(slot, (CellInfoCdma) cellInfo);
        } else if (cellInfo instanceof CellInfoGsm) {
            return new CellInfoGsmHolder(slot, (CellInfoGsm) cellInfo);
        } else if (cellInfo instanceof CellInfoWcdma) {
            return new CellInfoWcdmaHolder(slot, (CellInfoWcdma) cellInfo);
        } else if (cellInfo instanceof CellInfoTdscdma) {
            return new CellInfoTdscdmaHolder(slot, (CellInfoTdscdma) cellInfo);
        } else {
            return new CellInfoHolder(slot, cellInfo);
        }
    }
}
