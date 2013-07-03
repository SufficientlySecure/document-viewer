package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.core.ViewState;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class DrawQueue {

    private static final LogContext LCTX = LogManager.root().lctx("Imaging");

    private final ArrayBlockingQueue<ViewState> queue = new ArrayBlockingQueue<ViewState>(16, true);

    private final ArrayList<ViewState> list = new ArrayList<ViewState>();

    public DrawQueue() {
    }

    public ViewState takeLastTask() {
        ViewState task = null;
        try {
            // Workaround for possible ConcurrentModificationException
            while (true) {
                list.clear();
                try {
                    if (queue.drainTo(list) > 0) {
                        final int last = list.size() - 1;
                        task = list.get(last);
                        for (int i = 0; i < last; i++) {
                            final ViewState vs = list.get(i);
                            if (vs != null) {
                                vs.releaseAfterDraw();
                            }
                        }
                    }
                    break;
                } catch (final Throwable ex) {
                    // Go to next attempt
                    LCTX.e("Unexpected error on retrieving last view state from draw queue: " + ex.getMessage());
                }
            }
        } catch (final Throwable ex) {
            // Go to next attempt
            LCTX.e("Unexpected error on retrieving view state from draw queue: " + ex.getMessage());
        }
        return task;
    }

    public ViewState takeFirstTask() {
        ViewState task = null;
        try {
            task = queue.poll(0, TimeUnit.MILLISECONDS);
        } catch (final Throwable ex) {
            // Go to next attempt
            LCTX.e("Unexpected error on retrieving view state from draw queue: " + ex.getMessage());
        }
        return task;
    }

    public void draw(final ViewState viewState) {
        if (viewState != null) {
            // Workaround for possible ConcurrentModificationException
            viewState.addedToDrawQueue();
            while (true) {
                try {
                    queue.offer(viewState);
                    break;
                } catch (final Throwable ex) {
                    // Go to next attempt
                    LCTX.e("Unexpected error on adding view state to draw queue: " + ex.getMessage());
                }
            }
        }
    }
}
