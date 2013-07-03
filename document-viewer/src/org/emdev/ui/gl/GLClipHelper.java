package org.emdev.ui.gl;

import android.graphics.PointF;
import android.graphics.RectF;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class GLClipHelper {

    private int count = 0;
    private final ClipRegion[] regions = new ClipRegion[256];

    final GLCanvasImpl canvas;

    GLClipHelper(final GLCanvasImpl canvas) {
        this.canvas = canvas;
    }

    void setClipRect(final RectF bounds) {
        setClipRect(bounds.left, bounds.top, bounds.width(), bounds.height());
    }

    void setClipRect(final float left, final float top, final float width, final float height) {
        final GL11 gl = canvas.mGL;
        push(gl, new ClipRect(left, top, width, height));
        drawClipRegions(gl);
    }

    void setClipPath(final PointF... path) {
        final GL11 gl = canvas.mGL;
        push(gl, new ClipPath(path));
        drawClipRegions(gl);
    }

    void push(final GL11 gl, final ClipRegion clipRect) {
        if (count == 0) {
            gl.glEnable(GL10.GL_STENCIL_TEST);
        }
        regions[count++] = clipRect;
    }

    void drawClipRegions(final GL11 gl) {
        gl.glClear(GL10.GL_STENCIL_BUFFER_BIT);

        gl.glColorMask(false, false, false, false);
        gl.glStencilFunc(GL10.GL_ALWAYS, 1, ~0);
        gl.glStencilOp(GL10.GL_INCR, GL10.GL_INCR, GL10.GL_INCR);

        for (int i = 0; i < count; i++) {
            regions[i].draw(canvas);
        }

        gl.glColorMask(true, true, true, true);
        gl.glStencilFunc(GL10.GL_EQUAL, count, ~0);
        gl.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_KEEP);
    }

    void clearClipRect() {
        if (count <= 0) {
            return;
        }

        final GL11 gl = canvas.mGL;
        count--;
        regions[count] = null;

        if (count > 0) {
            drawClipRegions(gl);
        } else {
            gl.glDisable(GL10.GL_STENCIL_TEST);
        }
    }

    static interface ClipRegion {

        void draw(final GLCanvas canvas);
    }

    static class ClipRect implements ClipRegion {

        final float left, top, width, height;

        ClipRect(final float left, final float top, final float width, final float height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        @Override
        public void draw(final GLCanvas canvas) {
            canvas.fillRect(left, top, width, height, 1);
        }

    }

    static class ClipPath implements ClipRegion {

        final PointF[] path;

        ClipPath(final PointF... path) {
            this.path = path;
        }

        @Override
        public void draw(final GLCanvas canvas) {
            canvas.fillPoly(1, path);
        }
    }

}
