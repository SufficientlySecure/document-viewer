package org.ebookdroid;

public class EBookDroidLibraryLoader {

    private static boolean alreadyLoaded = false;

    public static boolean nativeGraphicsAvailable = false;

    public static void load() {
        if (alreadyLoaded) {
            return;
        }
        System.loadLibrary("ebookdroid");
        alreadyLoaded = true;
        nativeGraphicsAvailable = isNativeGraphicsAvailable();
    }

    public static native void free();

    private static native boolean isNativeGraphicsAvailable();
}
