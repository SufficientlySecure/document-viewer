package org.ebookdroid.core;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DrawThread extends Thread {

    // private static final LogContext LCTX = LogContext.ROOT.lctx("View");

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
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

        while (true) {
            DrawTask task = takeTask();
            if (task == null) {
                continue;
            }
            if (task.viewRect == null) {
                break;
            }
            canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas(null);
                performDrawing(canvas, task);
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private DrawTask takeTask() {
        DrawTask task = null;
        try {
            ArrayList<DrawTask> list = new ArrayList<DrawTask>();
            task = queue.take();
            if (queue.drainTo(list) > 0) {
                task = list.get(list.size() - 1);
            }
            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("Draw tasks: " + (task != null ? list.size() + 1 : 0));
            // }
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        return task;
    }

    private void performDrawing(Canvas canvas, DrawTask task) {
        view.drawView(canvas, task.viewRect);
    }

    public void draw(RectF viewRect) {
        if (viewRect != null) {
            // if (LCTX.isDebugEnabled()) {
            // LCTX.d("New draw task: " + viewRect);
            // }
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
