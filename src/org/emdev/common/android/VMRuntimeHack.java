package org.emdev.common.android;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public final class VMRuntimeHack {

    private static final LogContext LCTX = LogManager.root().lctx("VMRuntimeHack");

    private static Object runtime = null;
    private static Method trackAllocation = null;
    private static Method trackFree = null;

    static {
        boolean success = false;
        try {
            Class<?> cl = Class.forName("dalvik.system.VMRuntime");
            Method getRt = cl.getMethod("getRuntime", new Class[0]);
            runtime = getRt.invoke(null, new Object[0]);
            trackAllocation = cl.getMethod("trackExternalAllocation", new Class[] { long.class });
            trackFree = cl.getMethod("trackExternalFree", new Class[] { long.class });
            success = true;
        } catch (ClassNotFoundException e) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        if (!success) {
            LCTX.i("VMRuntime hack does not work!");
            runtime = null;
            trackAllocation = null;
            trackFree = null;
        } else {
            LCTX.i("VMRuntime hack initialized!");
        }
    }

    public static boolean trackAlloc(long size) {
        if (runtime == null)
            return false;
        try {
            Object res = trackAllocation.invoke(runtime, Long.valueOf(size));
            return (res instanceof Boolean) ? (Boolean) res : true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static boolean trackFree(long size) {
        if (runtime == null)
            return false;
        try {
            Object res = trackFree.invoke(runtime, Long.valueOf(size));
            return (res instanceof Boolean) ? (Boolean) res : true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    /**
     * Preallocate heap.
     * 
     * @param size
     *            the size in megabytes
     * @return the object
     */
    public static Object preallocateHeap(int size) {
        if (size <= 0) {
            LCTX.i("No heap preallocation");
            return null;
        }
        int i = size;
        LCTX.i("Trying to preallocate " + size + "Mb");
        while (i > 0) {
            try {
                byte[] tmp = new byte[i * 1024 * 1024];
                tmp[(int) (size - 1)] = (byte) size;
                Log.i("VMRuntimeHack", "Preallocated " + i + "Mb");
                tmp = null;
                return tmp;
            } catch (OutOfMemoryError e) {
                i--;
            } catch (IllegalArgumentException e) {
                i--;
            }
        }
        LCTX.i("Heap preallocation failed");
        return null;
    }
}
