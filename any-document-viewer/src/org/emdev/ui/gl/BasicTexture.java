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

import java.util.WeakHashMap;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.MathUtils;

// BasicTexture is a Texture corresponds to a real GL texture.
// The state of a BasicTexture indicates whether its data is loaded to GL memory.
// If a BasicTexture is loaded into GL memory, it has a GL texture id.
public abstract class BasicTexture implements Texture {

    @SuppressWarnings("unused")
    private static final LogContext LCTX = LogManager.root().lctx("BasicTexture");

    protected static final int UNSPECIFIED = -1;

    protected static final int STATE_UNLOADED = 0;
    protected static final int STATE_LOADED = 1;
    protected static final int STATE_ERROR = -1;

    protected int mId;
    protected int mState;

    protected int mWidth = UNSPECIFIED;
    protected int mHeight = UNSPECIFIED;

    private int mTextureWidth;
    private int mTextureHeight;

    protected GLCanvas mCanvasRef = null;
    private static WeakHashMap<BasicTexture, Object> sAllTextures = new WeakHashMap<BasicTexture, Object>();
    private static ThreadLocal<Class<?>> sInFinalizer = new ThreadLocal<Class<?>>();

    protected BasicTexture(final GLCanvas canvas, final int id, final int state) {
        setAssociatedCanvas(canvas);
        mId = id;
        mState = state;
        synchronized (sAllTextures) {
            sAllTextures.put(this, null);
        }
    }

    protected BasicTexture() {
        this(null, 0, STATE_UNLOADED);
    }

    protected void setAssociatedCanvas(final GLCanvas canvas) {
        mCanvasRef = canvas;
    }

    /**
     * Sets the content size of this texture. In OpenGL, the actual texture
     * size must be of power of 2, the size of the content may be smaller.
     */
    protected void setSize(final int width, final int height) {
        mWidth = width;
        mHeight = height;
        mTextureWidth = MathUtils.nextPowerOf2(width);
        mTextureHeight = MathUtils.nextPowerOf2(height);
    }

    public int getId() {
        return mId;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    // Returns the width rounded to the next power of 2.
    public int getTextureWidth() {
        return mTextureWidth;
    }

    // Returns the height rounded to the next power of 2.
    public int getTextureHeight() {
        return mTextureHeight;
    }

    @Override
    public void draw(final GLCanvas canvas, final int x, final int y) {
        canvas.drawTexture(this, x, y, getWidth(), getHeight());
    }

    @Override
    public void draw(final GLCanvas canvas, final int x, final int y, final int w, final int h) {
        canvas.drawTexture(this, x, y, w, h);
    }

    // onBind is called before GLCanvas binds this texture.
    // It should make sure the data is uploaded to GL memory.
    abstract protected boolean onBind(GLCanvas canvas);

    // Returns the GL texture target for this texture (e.g. GL_TEXTURE_2D).
    abstract protected int getTarget();

    public boolean isLoaded() {
        return mState == STATE_LOADED;
    }

    // recycle() is called when the texture will never be used again,
    // so it can free all resources.
    public void recycle() {
        freeResource();
    }

    // yield() is called when the texture will not be used temporarily,
    // so it can free some resources.
    // The default implementation unloads the texture from GL memory, so
    // the subclass should make sure it can reload the texture to GL memory
    // later, or it will have to override this method.
    public void yield() {
        freeResource();
    }

    private void freeResource() {
        final GLCanvas canvas = mCanvasRef;
        if (canvas != null && isLoaded()) {
            canvas.unloadTexture(this);
        }
        mState = STATE_UNLOADED;
        setAssociatedCanvas(null);
    }

    @Override
    protected void finalize() {
        sInFinalizer.set(BasicTexture.class);
        recycle();
        sInFinalizer.set(null);
    }

    // This is for deciding if we can call Bitmap's recycle().
    // We cannot call Bitmap's recycle() in finalizer because at that point
    // the finalizer of Bitmap may already be called so recycle() will crash.
    public static boolean inFinalizer() {
        return sInFinalizer.get() != null;
    }

    public static void yieldAllTextures() {
        synchronized (sAllTextures) {
            for (final BasicTexture t : sAllTextures.keySet()) {
                t.yield();
            }
        }
    }

    public static void invalidateAllTextures() {
        synchronized (sAllTextures) {
            for (final BasicTexture t : sAllTextures.keySet()) {
                t.mState = STATE_UNLOADED;
                t.setAssociatedCanvas(null);
            }
        }
    }
}
