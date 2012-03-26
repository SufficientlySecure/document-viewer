package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.ViewState;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.emdev.utils.concurrent.Flag;

public class DrawThread extends Thread {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    private final SurfaceHolder surfaceHolder;

    private final BlockingQueue<ViewState> queue = new ArrayBlockingQueue<ViewState>(16, true);

    private final Flag stop = new Flag();

    public DrawThread(final SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void finish() {
        stop.set();
        try {
            this.join();
        } catch (final InterruptedException e) {
        }
    }

    @Override
    public void run() {
        while (!stop.get()) {
            draw(false);
        }
    }

    protected void draw(final boolean useLastState) {
        final ViewState viewState = takeTask(1, TimeUnit.SECONDS, useLastState);
        if (viewState == null) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            EventPool.newEventDraw(viewState, canvas).process();
        } catch (final Throwable th) {
            LCTX.e("Unexpected error on drawing: " + th.getMessage(), th);
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public ViewState takeTask(final long timeout, final TimeUnit unit, final boolean useLastState) {
        ViewState task = null;
        try {
            task = queue.poll(timeout, unit);
            if (task != null && useLastState) {
                final ArrayList<ViewState> list = new ArrayList<ViewState>();
                if (queue.drainTo(list) > 0) {
                    task = list.get(list.size() - 1);
                }
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
        return task;
    }

    public void draw(final ViewState viewState) {
        if (viewState != null) {
            queue.offer(viewState);
        }
    }
}
