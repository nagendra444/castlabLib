package com.mediamelon.qubit;

public class MMQFQubitErrorEvent {
    public class MMQFQubitErrorEventCode{
        public static final int MMQFQubitMemNotAvailable = 1;
        public static final int MMQFFatalError = 2;
    }
    public int code;
    public Object extraInfo;
}
