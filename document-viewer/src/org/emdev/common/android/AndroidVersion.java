package org.emdev.common.android;

public class AndroidVersion {

    public static final int VERSION = getVersion();

    public static final boolean is2x = 5 <= VERSION && VERSION <= 10;

    public static final boolean lessThan3x = VERSION <= 10;

    public static final boolean is3x = 11 <= VERSION && VERSION <= 13;

    public static final boolean lessThan4x = VERSION <= 13;

    public static final boolean is4x = 14 <= VERSION;

    public static final boolean is40x = 14 <= VERSION && VERSION <= 15;

    public static final boolean is41x = 16 == VERSION;

    public static final boolean is42x = 17 == VERSION;

    public static final boolean is43x = 18 == VERSION;

    public static final boolean is44x = 19 <= VERSION;

    private static int getVersion() {
        try {
            return Integer.parseInt(android.os.Build.VERSION.SDK);
        } catch (Throwable th) {
            return 3;
        }
    }

}
