package com.mediamelon.qubit;

public class MMQFQubitStatusCode {
    MMQFQubitStatusCode(int code)
    {
        statusCode = code;
    }

    public int status()
    {
        return statusCode;
    }
    public static final int MMQFErrInvalidArguments = -2;
    public static final int MMQFFailure = -1;
    public static final int MMQFUnknown = 0;
    public static final int MMQFSuccess = 1;
    public static final int MMQFPending = 2;
    public static final int MMQFQubitNotRequired = 3;
    public static final int MMQFCancelled = 4;
    public static final int MMQFOperationNotSupported = 5;
    public static final int MMQFABRNotSupported = 6;
    public static final int MMQFMetaFileNotReachable = 7;
    public static final int MMQFMetaFileMalformed = 8;
    public static final int MMQFVideoRepresentationInfoNotAvl = 9;
    private int statusCode;
}
