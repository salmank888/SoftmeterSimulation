package com.example.skhalid.softmetersimulation;

/**
 * Constant values reused in this sample.
 */
public final class Constants {

    public static final int SUCCESS_RESULT = 0;

    public static final int FAILURE_RESULT = 1;

    public static final int GPS = 2;

    public static final int MSG_SOFTMETER_POWER_ON = 3;

    public static final int MSG_SOFTMETER_POWER_OFF = 4;

    public static final int MSG_LOCATION_CHANGED = 5;

    public static final int MSG_DISABLE_FIELDS = 6;

    public static final int MSG_ENABLE_FIELDS = 7;

    public static final int INFO = 8;

    public static final int WARNING = 9;

    public static final int MSG_MON_RSP = 10;

//    public static final int MSG_MON_RSP = 11;

    public static final int MSG_RCF = 11111;  // RCF - Request Configuration File

    public static final int MSG_CF_RCV = 12222;  // CF - Receive Configuration File

    public static final int MSG_MON= 13333;  // MON - Meter On

    public static final int MSG_QTD_RCV = 14444;  // QTD - Receive Query Trip Data

    public static final int MSG_RTD = 15555;  // RTD - Receive Trip Data

    public static final int MSG_TOFF = 16666;  // TOFF - Time Off

    public static final int MSG_MOFF = 17777;  // MOFF - Meter Off

    public static final int MSG_TON = 18888;  // TON - Time On



    public static final char EOT = (char) 0x4;
    public static final char COLSEPARATOR = '^';
    public static final char ROWSEPARATOR = '~';
    public static final char BODYSEPARATOR = (char) 0x2;






    public static final String PACKAGE_NAME =
      "com.example.skhalid.softmetersimulation";

    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";

    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";

    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
}
