package com.mediamelon.smartstreaming;

import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class MMCellInfo{
    public static final String CELL_RADIO_UNKNOWN = "";
    public static final String CELL_RADIO_GSM = "gsm";
    public static final String CELL_RADIO_WCDMA = "wcdma";
    public static final String CELL_RADIO_CDMA = "cdma";
    public static final String CELL_RADIO_LTE = "lte";

    public static final int UNKNOWN_CID = -1;
    public static final int UNKNOWN_LAC = -1;
    public static final int UNKNOWN_SIGNAL_STRENGTH = -1000;
    public static final int UNKNOWN_ASU = -1;
    private static final String LOG_TAG = "";

    public String mCellRadio;
    public int mMcc;
    public int mMnc;
    public int mCid;
    public int mLac;
    public int mAsu;
    public int mTa;
    public int mPsc;
    public int mSignalStrength;

    public MMCellInfo() {
        reset();
    }

    static String getCellRadioTypeName(int networkType) {
        switch (networkType) {
            // If the network is either GSM or any high-data-rate variant of it, the radio
            // field should be specified as `gsm`. This includes `GSM`, `EDGE` and `GPRS`.
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return CELL_RADIO_GSM;

            // If the network is either UMTS or any high-data-rate variant of it, the radio
            // field should be specified as `wcdma`. This includes `UMTS`, `HSPA`, `HSDPA`,
            // `HSPA+` and `HSUPA`.
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return CELL_RADIO_WCDMA;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return CELL_RADIO_LTE;

            // If the network is either CDMA or one of the EVDO variants, the radio
            // field should be specified as `cdma`. This includes `1xRTT`, `CDMA`, `eHRPD`,
            // `EVDO_0`, `EVDO_A`, `EVDO_B`, `IS95A` and `IS95B`.
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return CELL_RADIO_CDMA;

            default:
                Log.e(LOG_TAG, "", new IllegalArgumentException("Unexpected network type: " + networkType));
                return CELL_RADIO_UNKNOWN;
        }
    }

    public String getCellRadio() {
        return mCellRadio;
    }

    public JSONObject toJSONObject() {
        final JSONObject obj = new JSONObject();

        try {
            obj.put("radioType", getCellRadio());
            obj.put("cellId", mCid);
            obj.put("locationAreaCode", mLac);
            obj.put("mobileCountryCode", mMcc);
            obj.put("mobileNetworkCode", mMnc);

            if (mSignalStrength != UNKNOWN_SIGNAL_STRENGTH) obj.put("signalStrength", mSignalStrength);
            if (mTa != UNKNOWN_CID) obj.put("timingAdvance", mTa);
            if (mPsc != UNKNOWN_CID) obj.put("psc", mPsc);
            if (mAsu != UNKNOWN_ASU) obj.put("asu", mAsu);

        } catch (JSONException jsonE) {
            throw new IllegalStateException(jsonE);
        }

        return obj;
    }

    void reset() {
        mCellRadio = CELL_RADIO_GSM;
        mMcc = UNKNOWN_CID;
        mMnc = UNKNOWN_CID;
        mLac = UNKNOWN_LAC;
        mCid = UNKNOWN_CID;
        mSignalStrength = UNKNOWN_SIGNAL_STRENGTH;
        mAsu = UNKNOWN_ASU;
        mTa = UNKNOWN_CID;
        mPsc = UNKNOWN_CID;
    }

    public void setGsmCellInfo(int mcc, int mnc, int lac, int cid, int asu) {
        mCellRadio = CELL_RADIO_GSM;
        mMcc = mcc != Integer.MAX_VALUE ? mcc : UNKNOWN_CID;
        mMnc = mnc != Integer.MAX_VALUE ? mnc : UNKNOWN_CID;
        mLac = lac != Integer.MAX_VALUE ? lac : UNKNOWN_LAC;
        mCid = cid != Integer.MAX_VALUE ? cid : UNKNOWN_CID;
        mAsu = asu;
    }

    public void setWcdmaCellInfo(int mcc, int mnc, int lac, int cid, int psc, int asu) {
        mCellRadio = CELL_RADIO_WCDMA;
        mMcc = mcc != Integer.MAX_VALUE ? mcc : UNKNOWN_CID;
        mMnc = mnc != Integer.MAX_VALUE ? mnc : UNKNOWN_CID;
        mLac = lac != Integer.MAX_VALUE ? lac : UNKNOWN_LAC;
        mCid = cid != Integer.MAX_VALUE ? cid : UNKNOWN_CID;
        mPsc = psc != Integer.MAX_VALUE ? psc : UNKNOWN_CID;
        mAsu = asu;
    }

    /**
     * @param mcc Mobile Country Code, Integer.MAX_VALUE if unknown
     * @param mnc Mobile Network Code, Integer.MAX_VALUE if unknown
     * @param ci  Cell Identity, Integer.MAX_VALUE if unknown
     * @param psc Physical Cell Id, Integer.MAX_VALUE if unknown
     * @param lac Tracking Area Code, Integer.MAX_VALUE if unknown
     * @param asu Arbitrary strength unit
     * @param ta  Timing advance
     */
    public void setLteCellInfo(int mcc, int mnc, int ci, int psc, int lac, int asu, int ta) {
        mCellRadio = CELL_RADIO_LTE;
        mMcc = mcc != Integer.MAX_VALUE ? mcc : UNKNOWN_CID;
        mMnc = mnc != Integer.MAX_VALUE ? mnc : UNKNOWN_CID;
        mLac = lac != Integer.MAX_VALUE ? lac : UNKNOWN_LAC;
        mCid = ci != Integer.MAX_VALUE ? ci : UNKNOWN_CID;
        mPsc = psc != Integer.MAX_VALUE ? psc : UNKNOWN_CID;
        mAsu = asu;
        mTa = ta;
    }

    void setCdmaCellInfo(int baseStationId, int networkId, int systemId, int dbm) {
        mCellRadio = CELL_RADIO_CDMA;
        mMnc = systemId != Integer.MAX_VALUE ? systemId : UNKNOWN_CID;
        mLac = networkId != Integer.MAX_VALUE ? networkId : UNKNOWN_LAC;
        mCid = baseStationId != Integer.MAX_VALUE ? baseStationId : UNKNOWN_CID;
        mSignalStrength = dbm;
    }
}
