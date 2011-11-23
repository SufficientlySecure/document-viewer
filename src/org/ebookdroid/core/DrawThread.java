package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DrawThread extends Thread {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    private final SurfaceHolder surfaceHolder;

    private final BlockingQueue<DrawTask> queue = new ArrayBlockingQueue<DrawThread.DrawTask>(16, true);

    public DrawThread(final SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void finish() {
        queue.add(new DrawTask(null));
        try {
            this.join();
        } catch (final InterruptedException e) {
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        while (draw()) {
        }
    }

    public boolean draw() {
        final DrawTask task = takeTask(1, TimeUnit.SECONDS);
        if (task != null) {
            if (task.viewState == null) {
                return false;
            }
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas(null);
                performDrawing(canvas, task);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error on drawing: " + th.getMessage(), th);
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
        return true;
    }

    public DrawTask takeTask(long timeout, TimeUnit unit) {
        DrawTask task = null;
        try {
            task = queue.poll(timeout, unit);
            if (task != null) {
                final ArrayList<DrawTask> list = new ArrayList<DrawTask>();
                if (queue.drainTo(list) > 0) {
                    task = list.get(list.size() - 1);
                }
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
        return task;
    }

    public void performDrawing(final Canvas canvas, final DrawTask task) {
        final PagePaint paint = task.viewState.nightMode ? PagePaint.NIGHT : PagePaint.DAY;
        canvas.drawRect(canvas.getClipBounds(), paint.backgroundFillPaint);
        task.viewState.ctrl.drawView(canvas, task.viewState);
    }

    public void draw(final ViewState viewState) {
        if (viewState != null) {
            queue.offer(new DrawTask(viewState));
        }
    }

    public static class DrawTask {

        final ViewState viewState;

        public DrawTask(final ViewState viewState) {
            this.viewState = viewState;
        }
    }
}
