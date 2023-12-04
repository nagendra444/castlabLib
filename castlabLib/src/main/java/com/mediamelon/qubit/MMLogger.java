package com.mediamelon.qubit;

public class MMLogger {
    static final boolean LOG = true;
    public enum MMLogger_LogLevel{
        MMLogger_LogLevel_Error(0),
        MMLogger_LogLevel_Warning(1),
        MMLogger_LogLevel_Profile(2),
        MMLogger_LogLevel_Analyze(3),
        MMLogger_LogLevel_Info(4),
        MMLogger_LogLevel_Debug(5),
        MMLogger_LogLevel_Verbose(6);
        private final int value;

        MMLogger_LogLevel(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }

    public static final MMLogger_LogLevel LogLevel = MMLogger_LogLevel.MMLogger_LogLevel_Verbose;

    public static void i(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Info.getValue()){ 
            android.util.Log.i(tag, string);
        }
    }
    public static void e(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Error.getValue()) android.util.Log.e(tag, string);
    }
    public static void d(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Debug.getValue()) android.util.Log.d(tag, string);
    }
    public static void v(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Verbose.getValue()) android.util.Log.v(tag, string);
    }
    public static void w(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Warning.getValue()) android.util.Log.w(tag, string);
    }
    public static void p(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Profile.getValue()) android.util.Log.i(tag, string);
    }
    public static void a(String tag, String string) {
        if (LOG && LogLevel.getValue() >= MMLogger_LogLevel.MMLogger_LogLevel_Analyze.getValue()) android.util.Log.i(tag, string);
    }
}
