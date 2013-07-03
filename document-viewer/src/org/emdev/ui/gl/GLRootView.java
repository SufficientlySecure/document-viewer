/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Process;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

// The root component of all <code>GLView</code>s. The rendering is done in GL
// thread while the event handling is done in the main thread.  To synchronize
// the two threads, the entry points of this package need to synchronize on the
// <code>GLRootView</code> instance unless it can be proved that the rendering
// thread won't access the same thing as the method. The entry points include:
// (1) The public methods of HeadUpDisplay
// (2) The public methods of CameraHeadUpDisplay
// (3) The overridden methods in GLRootView.
public class GLRootView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final LogContext LCTX = LogManager.root().lctx("GLRootView");

    public static final int FLAG_INITIALIZED = 1;
    public static final int FLAG_NEED_LAYOUT = 2;

    protected GL11 mGL;
    protected GLCanvas mCanvas;

    protected int mFlags = FLAG_NEED_LAYOUT;
    protected volatile boolean mRenderRequested = false;

    protected final ReentrantLock mRenderLock = new ReentrantLock();
    protected final Condition mFreezeCondition = mRenderLock.newCondition();
    protected boolean mFreeze;

    protected long mLastDrawFinishTime;
    protected boolean mInDownState = false;
    protected boolean mFirstDraw = true;

    public GLRootView(final Context context) {
        this(context, null);
    }

    public GLRootView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mFlags |= FLAG_INITIALIZED;
        setEGLConfigChooser(GLConfiguration.getConfigChooser());
        setRenderer(this);
        if (GLConfiguration.use8888) {
            getHolder().setFormat(PixelFormat.RGBA_8888);
        } else {
            getHolder().setFormat(PixelFormat.RGB_565);
        }
    }

    @Override
    public void requestRender() {
        if (mRenderRequested) {
            return;
        }
        mRenderRequested = true;
        super.requestRender();
    }

    public void requestLayoutContentPane() {
        mRenderLock.lock();
        try {
            if ((mFlags & FLAG_NEED_LAYOUT) != 0) {
                return;
            }

            // "View" system will invoke onLayout() for initialization(bug ?), we
            // have to ignore it since the GLThread is not ready yet.
            if ((mFlags & FLAG_INITIALIZED) == 0) {
                return;
            }

            mFlags |= FLAG_NEED_LAYOUT;
            requestRender();
        } finally {
            mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;

        final int w = getWidth();
        final int h = getHeight();

        // Do the actual layout.
        LCTX.i("layout content pane " + w + "x" + h);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        if (changed) {
            requestLayoutContentPane();
        }
    }

    /**
     * Called when the context is created, possibly after automatic destruction.
     */
    // This is a GLSurfaceView.Renderer callback
    @Override
    public void onSurfaceCreated(final GL10 gl1, final EGLConfig config) {
        final GL11 gl = (GL11) gl1;
        if (mGL != null) {
            // The GL Object has changed
            LCTX.i("GLObject has changed from " + mGL + " to " + gl);
        }
        mRenderLock.lock();
        try {
            mGL = gl;
            mCanvas = new GLCanvasImpl(gl);
            BasicTexture.invalidateAllTextures();
        } finally {
            mRenderLock.unlock();
        }

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Called when the OpenGL surface is recreated without destroying the
     * context.
     */
    // This is a GLSurfaceView.Renderer callback
    @Override
    public void onSurfaceChanged(final GL10 gl1, final int width, final int height) {
        LCTX.i("onSurfaceChanged: " + width + "x" + height + ", gl10: " + gl1.toString());
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        mCanvas.setSize(width, height);
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        mRenderLock.lock();

        while (mFreeze) {
            mFreezeCondition.awaitUninterruptibly();
        }

        try {
            onDrawFrameLocked(gl);
        } finally {
            mRenderLock.unlock();
        }

    }

    protected void onDrawFrameLocked(final GL10 gl) {

        // release the unbound textures and deleted buffers.
        mCanvas.deleteRecycledResources();

        // reset texture upload limit
        UploadedTexture.resetUploadLimit();

        mRenderRequested = false;

        if ((mFlags & FLAG_NEED_LAYOUT) != 0) {
            layoutContentPane();
        }

        mCanvas.save(GLCanvas.SAVE_FLAG_ALL);

        // render
        draw(this.mCanvas);

        mCanvas.restore();

        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }
    }

    protected void draw(final GLCanvas canvas) {
    }

    public void lockRenderThread() {
        mRenderLock.lock();
    }

    public void unlockRenderThread() {
        mRenderLock.unlock();
    }

    @Override
    public void onPause() {
        unfreeze();
        super.onPause();
    }

    public void freeze() {
        mRenderLock.lock();
        mFreeze = true;
        mRenderLock.unlock();
    }

    public void unfreeze() {
        mRenderLock.lock();
        mFreeze = false;
        mFreezeCondition.signalAll();
        mRenderLock.unlock();
    }

    // We need to unfreeze in the following methods and in onPause().
    // These methods will wait on GLThread. If we have freezed the GLRootView,
    // the GLThread will wait on main thread to call unfreeze and cause dead
    // lock.
    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int w, final int h) {
        unfreeze();
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        unfreeze();
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        unfreeze();
        super.surfaceDestroyed(holder);
    }

    @Override
    protected void onDetachedFromWindow() {
        unfreeze();
        super.onDetachedFromWindow();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            unfreeze();
        } finally {
            super.finalize();
        }
    }
}
