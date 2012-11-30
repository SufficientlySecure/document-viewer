package org.emdev.ui.gl;

import android.opengl.GLSurfaceView.EGLConfigChooser;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class GLConfiguration {

    public static boolean stencilRequired = true;

    public static boolean use8888 = false;

    private static BaseEGLConfigChooser chooser;

    public static void checkConfiguration() {
        final EGL10 egl = (EGL10) EGLContext.getEGL();
        final EGLDisplay eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("Your device cannot support EGL");
        }

        final int[] version = new int[2];
        if (!egl.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("Your device cannot support EGL");
        }

        try {
            if (getConfigChooser().chooseConfig(egl, eglDisplay) == null) {
                throw new RuntimeException("Your device cannot support required GLES configuration");
            }
        } catch (final Throwable th) {
            throw new RuntimeException("Your device cannot support required GLES configuration");
        } finally {
            egl.eglTerminate(eglDisplay);
        }
    }

    public static EGLConfigChooser getConfigChooser() {
        if (chooser == null) {
            chooser = new BaseEGLConfigChooser();
        }
        return chooser;
    }
}
