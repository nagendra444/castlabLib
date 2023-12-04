package com.mediamelon.qubit;

public class MMQFQubitInfoEvent {
    public class MMQFQubitInfoEventCode{
        public static final int MMQFPresentationInfoReceived = 1;
        public static final int MMQFMetadataFileParsed = 2;
        public static final int ABRSwitchDetected = 3;
        public static final int SeekDetected = 4;
    }
    public int code;
    public Object extraInfo;
}
