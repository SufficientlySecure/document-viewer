package org.emdev.ui.gl;

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

import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLU;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.emdev.utils.MathUtils;
import org.emdev.utils.collections.IntArray;

public class GLCanvasImpl implements GLCanvas {

    private static final float OPAQUE_ALPHA = 0.95f;

    private static final int OFFSET_FILL_RECT = 0;
    private static final int OFFSET_DRAW_LINE = 4;
    private static final int OFFSET_DRAW_RECT = 6;
    private static final float[] BOX_COORDINATES = { 0, 0, 1, 0, 0, 1, 1, 1, // used for filling a rectangle
            0, 0, 1, 1, // used for drawing a line
            0, 0, 0, 1, 1, 1, 1, 0 }; // used for drawing the outline of a rectangle

    final GL11 mGL;

    private final float mMatrixValues[] = new float[16];
    private final float mTextureMatrixValues[] = new float[16];

    private int mBoxCoords;

    private final GLState mGLState;

    private float mAlpha;
    private final ArrayList<ConfigState> mRestoreStack = new ArrayList<ConfigState>();
    private ConfigState mRecycledRestoreAction;

    private final RectF mDrawTextureSourceRect = new RectF();
    private final RectF mDrawTextureTargetRect = new RectF();
    private final float[] mTempMatrix = new float[32];
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();
    private int mScreenWidth;
    private int mScreenHeight;
    private final boolean mBlendEnabled = true;

    private final GLClipHelper mClipper;

    public GLCanvasImpl(final GL11 gl) {
        mGL = gl;
        mGLState = new GLState(gl);
        mClipper = new GLClipHelper(this);
        initialize();
    }

    @Override
    public void setSize(final int width, final int height) {
        mScreenWidth = width;
        mScreenHeight = height;
        mAlpha = 1.0f;

        final GL11 gl = mGL;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluOrtho2D(gl, 0, width, 0, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        final float matrix[] = mMatrixValues;
        Matrix.setIdentityM(matrix, 0);
        // to match the graphic coordinate system in android, we flip it vertically.
        Matrix.translateM(matrix, 0, 0, height, 0);
        Matrix.scaleM(matrix, 0, 1, -1, 1);
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    @Override
    public void setAlpha(final float alpha) {
        mAlpha = alpha;
    }

    @Override
    public float getAlpha() {
        return mAlpha;
    }

    @Override
    public void multiplyAlpha(final float alpha) {
        mAlpha *= alpha;
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(final int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private void initialize() {
        final GL11 gl = mGL;

        // First create an nio buffer, then create a VBO from it.
        final int size = BOX_COORDINATES.length * Float.SIZE / Byte.SIZE;
        final FloatBuffer xyBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        xyBuffer.put(BOX_COORDINATES, 0, BOX_COORDINATES.length).position(0);

        final int[] name = new int[1];
        GLId.glGenBuffers(1, name, 0);
        mBoxCoords = name[0];

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBoxCoords);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER, xyBuffer.capacity() * (Float.SIZE / Byte.SIZE), xyBuffer,
                GL11.GL_STATIC_DRAW);

        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, 0);

        // Enable the texture coordinate array for Texture 1
        gl.glClientActiveTexture(GL10.GL_TEXTURE1);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glClientActiveTexture(GL10.GL_TEXTURE0);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);

        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        // mMatrixValues and mAlpha will be initialized in setSize()
    }

    @Override
    public void setClipRect(final RectF bounds) {
        mClipper.setClipRect(bounds);
    }

    @Override
    public void setClipRect(final float left, final float top, final float width, final float height) {
        mClipper.setClipRect(left, top, width, height);
    }

    @Override
    public void setClipPath(final PointF... path) {
        mClipper.setClipPath(path);
    }

    @Override
    public void clearClipRect() {
        mClipper.clearClipRect();
    }

    @Override
    public void drawRect(final float x, final float y, final float width, final float height, final Paint paint) {
        final GL11 gl = mGL;

        mGLState.setColorMode(paint.getColor(), mAlpha);
        mGLState.setLineWidth(paint.getStrokeWidth());

        saveTransform();
        translate(x, y);
        scale(width, height, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_LINE_LOOP, OFFSET_DRAW_RECT, 4);

        restoreTransform();
    }

    @Override
    public void drawRect(final RectF r, final Paint paint) {
        final GL11 gl = mGL;

        mGLState.setColorMode(paint.getColor(), mAlpha);
        mGLState.setLineWidth(paint.getStrokeWidth());

        saveTransform();
        translate(r.left, r.top);
        scale(r.width(), r.height(), 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_LINE_LOOP, OFFSET_DRAW_RECT, 4);

        restoreTransform();
    }

    @Override
    public void drawLine(final float x1, final float y1, final float x2, final float y2, final Paint paint) {
        final GL11 gl = mGL;

        mGLState.setColorMode(paint.getColor(), mAlpha);
        mGLState.setLineWidth(paint.getStrokeWidth());

        saveTransform();
        translate(x1, y1);
        scale(x2 - x1, y2 - y1, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, OFFSET_DRAW_LINE, 2);

        restoreTransform();
    }

    @Override
    public void fillRect(final float x, final float y, final float width, final float height, final int color) {
        mGLState.setColorMode(color, mAlpha);
        final GL11 gl = mGL;

        saveTransform();
        translate(x, y);
        scale(width, height, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, OFFSET_FILL_RECT, 4);

        restoreTransform();
    }

    @Override
    public void fillRect(final RectF r, final Paint p) {
        final int color = p.getColor();
        fillRect(r, color);
    }

    public void fillRect(final RectF r, final int color) {
        mGLState.setColorMode(color, mAlpha);

        final GL11 gl = mGL;

        saveTransform();
        translate(r.left, r.top);
        scale(r.width(), r.height(), 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, OFFSET_FILL_RECT, 4);

        restoreTransform();
    }

    @Override
    public void fillPoly(final int color, final PointF... path) {
        mGLState.setColorMode(color, mAlpha);
        final GL11 gl = mGL;

        final int bytes = 2 * path.length * Float.SIZE;
        final FloatBuffer xyBuffer = allocateDirectNativeOrderBuffer(bytes).asFloatBuffer();
        for (int i = 0; i < path.length; i++) {
            xyBuffer.put(path[i].x / mScreenWidth);
            xyBuffer.put(path[i].y / mScreenHeight);
        }
        xyBuffer.position(0);

        saveTransform();
        translate(0, 0);
        scale(mScreenWidth, mScreenHeight, 1);
        gl.glLoadMatrixf(mMatrixValues, 0);

        final int[] name = new int[1];
        GLId.glGenBuffers(1, name, 0);

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, name[0]);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER, bytes, xyBuffer, GL11.GL_DYNAMIC_DRAW);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, path.length);

        restoreTransform();

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBoxCoords);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, 0);
    }

    @Override
    public void drawPoly(final int color, final PointF... path) {
        mGLState.setColorMode(color, mAlpha);
        mGLState.setLineWidth(1.5f);
        final GL11 gl = mGL;

        final int bytes = 2 * path.length * Float.SIZE;
        final FloatBuffer xyBuffer = allocateDirectNativeOrderBuffer(bytes).asFloatBuffer();
        for (int i = 0; i < path.length; i++) {
            xyBuffer.put(path[i].x / mScreenWidth);
            xyBuffer.put(path[i].y / mScreenHeight);
        }
        xyBuffer.position(0);

        saveTransform();
        translate(0, 0);
        scale(mScreenWidth, mScreenHeight, 1);
        gl.glLoadMatrixf(mMatrixValues, 0);

        final int[] name = new int[1];
        GLId.glGenBuffers(1, name, 0);

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, name[0]);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER, bytes, xyBuffer, GL11.GL_DYNAMIC_DRAW);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, path.length);

        restoreTransform();

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBoxCoords);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, 0);
    }

    @Override
    public void translate(final float x, final float y, final float z) {
        Matrix.translateM(mMatrixValues, 0, x, y, z);
    }

    // This is a faster version of translate(x, y, z) because
    // (1) we knows z = 0, (2) we inline the Matrix.translateM call,
    // (3) we unroll the loop
    @Override
    public void translate(final float x, final float y) {
        final float[] m = mMatrixValues;
        m[12] += m[0] * x + m[4] * y;
        m[13] += m[1] * x + m[5] * y;
        m[14] += m[2] * x + m[6] * y;
        m[15] += m[3] * x + m[7] * y;
    }

    @Override
    public void scale(final float sx, final float sy, final float sz) {
        Matrix.scaleM(mMatrixValues, 0, sx, sy, sz);
    }

    @Override
    public void rotate(final float angle, final float x, final float y, final float z) {
        if (angle == 0) {
            return;
        }
        final float[] temp = mTempMatrix;
        Matrix.setRotateM(temp, 0, angle, x, y, z);
        Matrix.multiplyMM(temp, 16, mMatrixValues, 0, temp, 0);
        System.arraycopy(temp, 16, mMatrixValues, 0, 16);
    }

    @Override
    public void multiplyMatrix(final float matrix[], final int offset) {
        final float[] temp = mTempMatrix;
        Matrix.multiplyMM(temp, 0, mMatrixValues, 0, matrix, offset);
        System.arraycopy(temp, 0, mMatrixValues, 0, 16);
    }

    private void textureRect(final float x, final float y, final float width, final float height) {
        final GL11 gl = mGL;

        saveTransform();
        translate(x, y);
        scale(width, height, 1);
        gl.glColor4f(1, 1, 1, 1);
        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, OFFSET_FILL_RECT, 4);

        restoreTransform();
    }

    private void drawBoundTexture(final BasicTexture texture, final int x, final int y, final int width,
            final int height) {
        final float w = (float) texture.getWidth() / texture.getTextureWidth();
        final float h = (float) texture.getHeight() / texture.getTextureHeight();
        setTextureCoords(0, 0, w, h);
        textureRect(x, y, width, height);
    }

    @Override
    public void drawTexture(final BasicTexture texture, final int x, final int y, final int width, final int height) {
        drawTexture(texture, x, y, width, height, mAlpha);
    }

    private void drawTexture(final BasicTexture texture, final int x, final int y, final int width, final int height,
            final float alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }

        mGLState.setBlendEnabled(mBlendEnabled && (!texture.isOpaque() || alpha < OPAQUE_ALPHA));
        if (!bindTexture(texture)) {
            return;
        }
        mGLState.setTextureAlpha(alpha);
        drawBoundTexture(texture, x, y, width, height);
    }

    @Override
    public boolean drawTexture(final BasicTexture texture, RectF source, RectF target) {
        if (target.width() <= 0 || target.height() <= 0) {
            return false;
        }

        // Copy the input to avoid changing it.
        mDrawTextureSourceRect.set(source);
        mDrawTextureTargetRect.set(target);
        source = mDrawTextureSourceRect;
        target = mDrawTextureTargetRect;

        mGLState.setBlendEnabled(mBlendEnabled && (!texture.isOpaque() || mAlpha < OPAQUE_ALPHA));
        if (!bindTexture(texture)) {
            return false;
        }
        convertCoordinate(source, target, texture);
        setTextureCoords(source);
        mGLState.setTextureAlpha(mAlpha);
        textureRect(target.left, target.top, target.width(), target.height());

        return true;
    }

    @Override
    public void drawTexture(final BasicTexture texture, final float[] mTextureTransform, final int x, final int y,
            final int w, final int h) {
        mGLState.setBlendEnabled(mBlendEnabled && (!texture.isOpaque() || mAlpha < OPAQUE_ALPHA));
        if (!bindTexture(texture)) {
            return;
        }
        setTextureCoords(mTextureTransform);
        mGLState.setTextureAlpha(mAlpha);
        textureRect(x, y, w, h);
    }

    // This function changes the source coordinate to the texture coordinates.
    // It also clips the source and target coordinates if it is beyond the
    // bound of the texture.
    private void convertCoordinate(final RectF source, final RectF target, final BasicTexture texture) {

        final int width = texture.getWidth();
        final int height = texture.getHeight();
        final int texWidth = texture.getTextureWidth();
        final int texHeight = texture.getTextureHeight();
        // Convert to texture coordinates
        source.left /= texWidth;
        source.right /= texWidth;
        source.top /= texHeight;
        source.bottom /= texHeight;

        // Clip if the rendering range is beyond the bound of the texture.
        final float xBound = (float) width / texWidth;
        if (source.right > xBound) {
            target.right = target.left + target.width() * (xBound - source.left) / source.width();
            source.right = xBound;
        }
        final float yBound = (float) height / texHeight;
        if (source.bottom > yBound) {
            target.bottom = target.top + target.height() * (yBound - source.top) / source.height();
            source.bottom = yBound;
        }
    }

    private boolean bindTexture(final BasicTexture texture) {
        if (!texture.onBind(this)) {
            return false;
        }
        final int target = texture.getTarget();
        mGLState.setTextureTarget(target);
        mGL.glBindTexture(target, texture.getId());
        return true;
    }

    private static class GLState {

        private final GL11 mGL;

        private int mTexEnvMode = GL11.GL_REPLACE;
        private float mTextureAlpha = 1.0f;
        private int mTextureTarget = GL11.GL_TEXTURE_2D;
        private boolean mBlendEnabled = true;
        private float mLineWidth = 1.0f;

        public GLState(final GL11 gl) {
            mGL = gl;

            // Disable unused state
            gl.glDisable(GL10.GL_LIGHTING);

            // Enable used features
            gl.glEnable(GL10.GL_DITHER);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glEnable(GL10.GL_TEXTURE_2D);

            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

            // Set the background color
            gl.glClearColor(0f, 0f, 0f, 0f);
            gl.glClearStencil(0);

            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

            // We use 565 or 8888 format, so set the alignment to 2 bytes/pixel.
            gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 2);
        }

        public void setTexEnvMode(final int mode) {
            if (mTexEnvMode == mode) {
                return;
            }
            mTexEnvMode = mode;
            mGL.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, mode);
        }

        public void setLineWidth(final float width) {
            if (mLineWidth == width) {
                return;
            }
            mLineWidth = width;
            mGL.glLineWidth(width);
        }

        public void setTextureAlpha(final float alpha) {
            if (mTextureAlpha == alpha) {
                return;
            }
            mTextureAlpha = alpha;
            if (alpha >= OPAQUE_ALPHA) {
                // The alpha is need for those texture without alpha channel
                mGL.glColor4f(1, 1, 1, 1);
                setTexEnvMode(GL10.GL_REPLACE);
            } else {
                mGL.glColor4f(alpha, alpha, alpha, alpha);
                setTexEnvMode(GL10.GL_MODULATE);
            }
        }

        public void setColorMode(final int color, final float alpha) {
            setBlendEnabled(!MathUtils.isOpaque(color) || alpha < OPAQUE_ALPHA);

            // Set mTextureAlpha to an invalid value, so that it will reset
            // again in setTextureAlpha(float) later.
            mTextureAlpha = -1.0f;

            setTextureTarget(0);

            final float prealpha = (color >>> 24) * alpha * 65535f / 255f / 255f;
            mGL.glColor4x(Math.round(((color >> 16) & 0xFF) * prealpha), Math.round(((color >> 8) & 0xFF) * prealpha),
                    Math.round((color & 0xFF) * prealpha), Math.round(255 * prealpha));
        }

        // target is a value like GL_TEXTURE_2D. If target = 0, texturing is disabled.
        public void setTextureTarget(final int target) {
            if (mTextureTarget == target) {
                return;
            }
            if (mTextureTarget != 0) {
                mGL.glDisable(mTextureTarget);
            }
            mTextureTarget = target;
            if (mTextureTarget != 0) {
                mGL.glEnable(mTextureTarget);
            }
        }

        public void setBlendEnabled(final boolean enabled) {
            if (mBlendEnabled == enabled) {
                return;
            }
            mBlendEnabled = enabled;
            if (enabled) {
                mGL.glEnable(GL10.GL_BLEND);
            } else {
                mGL.glDisable(GL10.GL_BLEND);
            }
        }
    }

    @Override
    public GL11 getGLInstance() {
        return mGL;
    }

    @Override
    public void clearBuffer() {
        mGL.glClear(GL10.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void clearBuffer(final Paint p) {
        clearBuffer(p.getColor());
    }

    @Override
    public void clearBuffer(final int color) {
        /*
        final GL11 gl = mGL;
        final float prealpha = (color >>> 24) / 255f;
        final float red = ((color >> 16) & 0xFF) * prealpha;
        final float green = ((color >> 8) & 0xFF) * prealpha;
        final float blue = (color & 0xFF) * prealpha;
        final float alpha = 255 * prealpha;

        gl.glClearColor(red, green, blue, alpha);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
*/
        fillRect(0, 0, mScreenWidth, mScreenHeight, color);
    }

    private void setTextureCoords(final RectF source) {
        setTextureCoords(source.left, source.top, source.right, source.bottom);
    }

    private void setTextureCoords(final float left, final float top, final float right, final float bottom) {
        mGL.glMatrixMode(GL10.GL_TEXTURE);
        mTextureMatrixValues[0] = right - left;
        mTextureMatrixValues[5] = bottom - top;
        mTextureMatrixValues[10] = 1;
        mTextureMatrixValues[12] = left;
        mTextureMatrixValues[13] = top;
        mTextureMatrixValues[15] = 1;
        mGL.glLoadMatrixf(mTextureMatrixValues, 0);
        mGL.glMatrixMode(GL10.GL_MODELVIEW);
    }

    private void setTextureCoords(final float[] mTextureTransform) {
        mGL.glMatrixMode(GL10.GL_TEXTURE);
        mGL.glLoadMatrixf(mTextureTransform, 0);
        mGL.glMatrixMode(GL10.GL_MODELVIEW);
    }

    // unloadTexture and deleteBuffer can be called from the finalizer thread,
    // so we synchronized on the mUnboundTextures object.
    @Override
    public boolean unloadTexture(final BasicTexture t) {
        synchronized (mUnboundTextures) {
            if (!t.isLoaded()) {
                return false;
            }
            mUnboundTextures.add(t.mId);
            return true;
        }
    }

    @Override
    public void deleteBuffer(final int bufferId) {
        synchronized (mUnboundTextures) {
            mDeleteBuffers.add(bufferId);
        }
    }

    @Override
    public void deleteRecycledResources() {
        synchronized (mUnboundTextures) {
            IntArray ids = mUnboundTextures;
            if (ids.size() > 0) {
                GLId.glDeleteTextures(mGL, ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }

            ids = mDeleteBuffers;
            if (ids.size() > 0) {
                GLId.glDeleteBuffers(mGL, ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
        }
    }

    @Override
    public void save() {
        save(SAVE_FLAG_ALL);
    }

    @Override
    public void save(final int saveFlags) {
        final ConfigState config = obtainRestoreConfig();

        if ((saveFlags & SAVE_FLAG_ALPHA) != 0) {
            config.mAlpha = mAlpha;
        } else {
            config.mAlpha = -1;
        }

        if ((saveFlags & SAVE_FLAG_MATRIX) != 0) {
            System.arraycopy(mMatrixValues, 0, config.mMatrix, 0, 16);
        } else {
            config.mMatrix[0] = Float.NEGATIVE_INFINITY;
        }

        mRestoreStack.add(config);
    }

    @Override
    public void restore() {
        if (mRestoreStack.isEmpty()) {
            throw new IllegalStateException();
        }
        final ConfigState config = mRestoreStack.remove(mRestoreStack.size() - 1);
        config.restore(this);
        freeRestoreConfig(config);
    }

    private void freeRestoreConfig(final ConfigState action) {
        action.mNextFree = mRecycledRestoreAction;
        mRecycledRestoreAction = action;
    }

    private ConfigState obtainRestoreConfig() {
        if (mRecycledRestoreAction != null) {
            final ConfigState result = mRecycledRestoreAction;
            mRecycledRestoreAction = result.mNextFree;
            return result;
        }
        return new ConfigState();
    }

    private static class ConfigState {

        float mAlpha;
        float mMatrix[] = new float[16];
        ConfigState mNextFree;

        public void restore(final GLCanvasImpl canvas) {
            if (mAlpha >= 0) {
                canvas.setAlpha(mAlpha);
            }
            if (mMatrix[0] != Float.NEGATIVE_INFINITY) {
                System.arraycopy(mMatrix, 0, canvas.mMatrixValues, 0, 16);
            }
        }
    }

    private void saveTransform() {
        System.arraycopy(mMatrixValues, 0, mTempMatrix, 0, 16);
    }

    private void restoreTransform() {
        System.arraycopy(mTempMatrix, 0, mMatrixValues, 0, 16);
    }
}
