package org.ebookdroid.ui.viewer.viewers;

import org.ebookdroid.ui.viewer.IActivityController;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class ScrollEventThread extends Thread {

    private final IActivityController base;

    private final BlockingQueue<OnScrollEvent> queue = new LinkedBlockingQueue<OnScrollEvent>();

    private final ConcurrentLinkedQueue<OnScrollEvent> pool = new ConcurrentLinkedQueue<OnScrollEvent>();

    ScrollEventThread(final IActivityController base) {
        super("ScrollEventThread");
        this.base = base;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final OnScrollEvent event = queue.poll(1, TimeUnit.SECONDS);
                if (event == null) {
                    continue;
                }
                process(event);
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
        // System.out.println("ScrollEventThread.run(): finished");
    }

    void onScrollChanged(final int curX, final int curY, final int oldX, final int oldY) {
        OnScrollEvent event = pool.poll();
        if (event != null) {
            event.reuse(curX, curY, oldX, oldY);
        } else {
            event = new OnScrollEvent(curX, curY, oldX, oldY);
        }
        queue.offer(event);
    }

    private void process(final OnScrollEvent event) {
        // final long t1 = System.currentTimeMillis();
        try {
            final int dX = event.m_curX - event.m_oldX;
            final int dY = event.m_curY - event.m_oldY;

            base.getDocumentController().onScrollChanged(dX, dY);

        } catch (final Throwable th) {
            th.printStackTrace();
        } finally {
            pool.add(event);
            // final long t2 = System.currentTimeMillis();
            // System.out.println("ScrollEventThread.onScrollChanged(): " + (t2 - t1) + " ms, " + pool.size());
        }
    }

    final static class OnScrollEvent {

        int m_oldX;
        int m_curY;
        int m_curX;
        int m_oldY;

        OnScrollEvent(final int curX, final int curY, final int oldX, final int oldY) {
            reuse(curX, curY, oldX, oldY);
        }

        void reuse(final int curX, final int curY, final int oldX, final int oldY) {
            m_oldX = oldX;
            m_curY = curY;
            m_curX = curX;
            m_oldY = oldY;
        }

    }
}
