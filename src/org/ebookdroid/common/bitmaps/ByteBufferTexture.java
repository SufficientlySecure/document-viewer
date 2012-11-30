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

package org.ebookdroid.common.bitmaps;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.gl.BasicTexture;
import org.emdev.ui.gl.GLCanvas;
import org.emdev.ui.gl.GLId;

// UploadedTextures use a Bitmap for the content of the texture.
//
// Subclasses should implement onGetBitmap() to provide the Bitmap and
// implement onFreeBitmap(mBitmap) which will be called when the Bitmap
// is not needed anymore.
//
// isContentValid() is meaningful only when the isLoaded() returns true.
// It means whether the content needs to be updated.
//
// The user of this class should call recycle() when the texture is not
// needed anymore.
//
// By default an UploadedTexture is opaque (so it can be drawn faster without
// blending). The user or subclass can override it using setOpaque().
public class ByteBufferTexture extends BasicTexture {

    @SuppressWarnings("unused")
    private static final LogContext LCTX = LogManager.root().lctx("Texture");

    private static int[] sTextureId = new int[1];

    private static float[] sCropRect = new float[4];

    private boolean mOpaque = true;

    protected ByteBufferBitmap mBitmap;

    public ByteBufferTexture(final ByteBufferBitmap bitmap) {
        super(null, 0, STATE_UNLOADED);
        this.mBitmap = bitmap;
        setSize(mBitmap.getWidth(), mBitmap.getHeight());
    }

    protected void freeBitmap() {
        if (mBitmap != null) {
            mBitmap = null;
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    /**
     * Updates the content on GPU's memory.
     *
     * @param canvas
     */
    public void updateContent(final GLCanvas canvas) {
        if (!isLoaded()) {
            uploadToCanvas(canvas);
        }
    }

    private void uploadToCanvas(final GLCanvas canvas) {
        final GL11 gl = canvas.getGLInstance();

        if (mBitmap != null) {
            try {
                final int bWidth = mBitmap.getWidth();
                final int bHeight = mBitmap.getHeight();
                final int texWidth = getTextureWidth();
                final int texHeight = getTextureHeight();

                // Define a vertically flipped crop rectangle for
                // OES_draw_texture.
                // The four values in sCropRect are: left, bottom, width, and
                // height. Negative value of width or height means flip.
                sCropRect[0] = 0;
                sCropRect[1] = bHeight;
                sCropRect[2] = bWidth;
                sCropRect[3] = -bHeight;

                // Upload the bitmap to a new texture.
                GLId.glGenTextures(1, sTextureId, 0);
                gl.glBindTexture(GL10.GL_TEXTURE_2D, sTextureId[0]);
                gl.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                gl.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

                if (bWidth == texWidth && bHeight == texHeight) {
                    gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, texWidth, texHeight, 0, GL10.GL_RGBA,
                            GL10.GL_UNSIGNED_BYTE, mBitmap.getPixels());
                } else {
                    gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, texWidth, texHeight, 0, GL10.GL_RGBA,
                            GL10.GL_UNSIGNED_BYTE, null);
                    gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bWidth, bHeight, GL10.GL_RGBA,
                            GL10.GL_UNSIGNED_BYTE, mBitmap.getPixels());
                }
            } finally {
                freeBitmap();
            }
            // Update texture state.
            setAssociatedCanvas(canvas);
            mId = sTextureId[0];
            mState = STATE_LOADED;
        } else {
            mState = STATE_ERROR;
            LCTX.e("Texture load fail, no bitmap");
        }
    }

    @Override
    protected boolean onBind(final GLCanvas canvas) {
        updateContent(canvas);
        return isLoaded();
    }

    @Override
    protected int getTarget() {
        return GL10.GL_TEXTURE_2D;
    }

    public void setOpaque(final boolean isOpaque) {
        mOpaque = isOpaque;
    }

    @Override
    public boolean isOpaque() {
        return mOpaque;
    }

    @Override
    public void recycle() {
        super.recycle();
        if (mBitmap != null) {
            freeBitmap();
        }
    }
}
