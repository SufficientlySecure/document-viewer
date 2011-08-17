package org.ebookdroid.core;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.SurfaceHolder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DrawThread extends Thread {

    private SurfaceHolder surfaceHolder;

    private AbstractDocumentView view;

    private final BlockingQueue<DrawTask> queue = new LinkedBlockingQueue<DrawThread.DrawTask>();

    public DrawThread(SurfaceHolder surfaceHolder, AbstractDocumentView view) {
        this.surfaceHolder = surfaceHolder;
        this.view = view;
    }

    public void finish() {
        queue.add(new DrawTask(null));
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void run() {
        Canvas canvas;
        while (true) {

            DrawTask task = null;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            if (task == null) {
                continue;
            }
            if (task.viewRect == null) {
                break;
            }
            canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas(null);
                synchronized (surfaceHolder) {
                    performDrawing(canvas, task);
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void performDrawing(Canvas canvas, DrawTask task) {
        view.drawView(canvas, task.viewRect);
    }

    public void draw(RectF viewRect) {
        if (viewRect != null) {
            queue.add(new DrawTask(viewRect));
        }
    }

    private static class DrawTask {

        final RectF viewRect;

        public DrawTask(RectF viewRect) {
            this.viewRect = viewRect != null ? new RectF(viewRect) : null;
        }
    }
}
