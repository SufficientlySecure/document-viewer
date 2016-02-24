/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emdev.ui.gl;

import android.opengl.GLSurfaceView.EGLConfigChooser;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class BaseEGLConfigChooser implements EGLConfigChooser {

    private static final LogContext LCTX = LogManager.root().lctx("GLConfiguration");

    private final int mConfigSpec[] = new int[] { EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE,
            5, EGL10.EGL_ALPHA_SIZE, 0, EGL10.EGL_STENCIL_SIZE, 1, EGL10.EGL_NONE };

    private static final int[] ATTR_ID = { EGL10.EGL_RED_SIZE, EGL10.EGL_GREEN_SIZE, EGL10.EGL_BLUE_SIZE,
            EGL10.EGL_ALPHA_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE, EGL10.EGL_CONFIG_ID,
            EGL10.EGL_CONFIG_CAVEAT };

    private static final String[] ATTR_NAME = { "R", "G", "B", "A", "D", "S", "ID", "CAVEAT" };

    private void setConfigValue(int key, int value) {
        for (int i = 0; i < mConfigSpec.length; i += 2)  {
            if (mConfigSpec[i] == key) {
                mConfigSpec[i + 1] = value;
                break;
            }
        }
    }

    @Override
    public EGLConfig chooseConfig(final EGL10 egl, final EGLDisplay display) {
        final int[] numConfig = new int[1];
        if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, numConfig)) {
            throw new RuntimeException("eglChooseConfig failed");
        }

        if (numConfig[0] <= 0) {
            LCTX.i("No configurations found. Trying EGL_STENCIL_SIZE 0");
            setConfigValue(EGL10.EGL_STENCIL_SIZE, 0);

            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, numConfig)) {
                throw new RuntimeException("eglChooseConfig failed");
            }
        }

        if (numConfig[0] <= 0) {
            throw new RuntimeException("Your device cannot support required GLES configuration");
        }

        final EGLConfig[] configs = new EGLConfig[numConfig[0]];
        if (!egl.eglChooseConfig(display, mConfigSpec, configs, configs.length, numConfig)) {
            throw new RuntimeException();
        }

        return chooseConfig(egl, display, configs);
    }

    private EGLConfig chooseConfig(final EGL10 egl, final EGLDisplay display, final EGLConfig configs[]) {

        EGLConfig result = null;
        int minStencil = Integer.MAX_VALUE;
        final int value[] = new int[1];

        for (int i = 0, n = configs.length; i < n; ++i) {
            logConfig("Config found: ", egl, display, configs[i]);
            if (egl.eglGetConfigAttrib(display, configs[i], EGL10.EGL_RED_SIZE, value)) {
                if (GLConfiguration.use8888 && value[0] != 8) {
                    continue;
                }
                if (!GLConfiguration.use8888 && value[0] == 8) {
                    continue;
                }
            }

            if (egl.eglGetConfigAttrib(display, configs[i], EGL10.EGL_STENCIL_SIZE, value)) {
                if (value[0] == 0) {
                    continue;
                }
                if (value[0] < minStencil) {
                    minStencil = value[0];
                    result = configs[i];
                }
            } else {
                LCTX.e("eglGetConfigAttrib error: " + egl.eglGetError());
            }
        }
        if (result == null) {
            result = configs[0];
        }
        logConfig("Config chosen: ", egl, display, result);
        return result;
    }

    private void logConfig(final String prefix, final EGL10 egl, final EGLDisplay display, final EGLConfig config) {
        final int value[] = new int[1];
        final StringBuilder sb = new StringBuilder();
        for (int j = 0; j < ATTR_ID.length; j++) {
            value[0] = -1;
            egl.eglGetConfigAttrib(display, config, ATTR_ID[j], value);
            sb.append(ATTR_NAME[j] + value[0] + " ");
        }
        LCTX.i(prefix + sb.toString());
    }
}
