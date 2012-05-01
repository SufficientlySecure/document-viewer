package org.ebookdroid;

import org.ebookdroid.common.log.LogContext;

public class EBookDroidLibraryLoader {

    private static final LogContext LCTX = LogContext.ROOT.lctx("LibraryLoader");

    private static boolean alreadyLoaded = false;

    public static boolean nativeGraphicsAvailable = false;

    public static void load() {
        if (alreadyLoaded) {
            return;
        }
        try {
            System.loadLibrary("ebookdroid");
            alreadyLoaded = true;
            nativeGraphicsAvailable = isNativeGraphicsAvailable();
            LCTX.i("Native graphics " + (nativeGraphicsAvailable ? "available" : "not available"));
        } catch (Throwable th) {
            LCTX.e("Native library cannot be loaded: ", th);
            throw new RuntimeException(th);
        }
    }

    public static native void free();

    private static native boolean isNativeGraphicsAvailable();
}
