package org.ebookdroid.core;

public class VuDroidLibraryLoader
{
    private static boolean alreadyLoaded = false;

    public static void load()
    {
        if (alreadyLoaded)
        {
            return;
        }
        System.loadLibrary("ebookdroid");
        alreadyLoaded = true;
    }
}
