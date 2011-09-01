package org.ebookdroid.core;

public class EBookDroidLibraryLoader {

    private static boolean alreadyLoaded = false;

    public static void load() {
        if (alreadyLoaded) {
            return;
        }
        System.loadLibrary("ebookdroid");
        alreadyLoaded = true;
    }
    
    public static native void free();
}
