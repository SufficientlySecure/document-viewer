package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DrawThread extends Thread {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    private final SurfaceHolder surfaceHolder;

    private final AbstractDocumentView view;

    private final BlockingQueue<DrawTask> queue = new ArrayBlockingQueue<DrawThread.DrawTask>(16, true);

    private long lastUpdate = 0;
    private static final long TIME_INTERVAL = 30;

    public DrawThread(final SurfaceHolder surfaceHolder, final AbstractDocumentView view) {
        this.surfaceHolder = surfaceHolder;
        this.view = view;
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
        Canvas canvas;
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

        while (true) {
            final DrawTask task = takeTask();
            if (task == null) {
                continue;
            }
            if (task.viewState == null) {
                break;
            }
            canvas = null;
            long interval = System.currentTimeMillis() - lastUpdate;
            if (interval < TIME_INTERVAL) {
                try {
                    Thread.sleep(TIME_INTERVAL - interval);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            long start = System.currentTimeMillis();
            try {
                lastUpdate = System.currentTimeMillis();
                canvas = surfaceHolder.lockCanvas(null);
                performDrawing(canvas, task);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error on drawing: " + th.getMessage(), th);
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
                Log.d("Time", "Draw time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private DrawTask takeTask() {
        DrawTask task = null;
        try {
            task = queue.poll(1, TimeUnit.SECONDS);
//            if (task != null) {
//                final ArrayList<DrawTask> list = new ArrayList<DrawTask>();
//                if (queue.drainTo(list) > 0) {
//                    task = list.get(list.size() - 1);
//                }
//            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
        return task;
    }

    private void performDrawing(final Canvas canvas, final DrawTask task) {
        final Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(canvas.getClipBounds(), paint);
        view.drawView(canvas, task.viewState);
    }

    public void draw(final ViewState viewState) {
        if (viewState != null) {
            queue.offer(new DrawTask(viewState));
        }
    }

    private static class DrawTask {

        final ViewState viewState;

        public DrawTask(final ViewState viewState) {
            this.viewState = viewState;
        }
    }
}
