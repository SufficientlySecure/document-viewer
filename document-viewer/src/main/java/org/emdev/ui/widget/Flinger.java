package org.emdev.ui.widget;

import android.os.SystemClock;

import java.util.concurrent.locks.ReentrantLock;

public class Flinger {

    private static final float FLING_DURATION_PARAM = 50f;
    private static final int DECELERATED_FACTOR = 3;
    private static final int DEFAULT_DURATION = 250;

    private static final int MODE_STOPPED = 0;
    private static final int MODE_SCROLL = 1;
    private static final int MODE_FLING = 2;

    private final ReentrantLock lock = new ReentrantLock();

    private int mode = MODE_STOPPED;

    private int startX, startY;
    private int minX, minY, maxX, maxY;
    private double sinAngle;
    private double cosAngle;
    private int duration;
    private int distance;
    private int finalX, finalY;

    private int currX, currY;
    private long startTime;
    private float oldProgress;

    public int getCurrX() {
        return currX;

    }

    public int getCurrY() {
        return currY;
    }

    public void startScroll(final int startX, final int startY, final int dx, final int dy) {
        lock.lock();
        try {
            mode = MODE_SCROLL;
            this.startX = startX;
            this.startY = startY;
            this.finalX = startX + dx;
            this.finalY = startY + dy;
            this.duration = DEFAULT_DURATION;
            this.startTime = SystemClock.uptimeMillis();
            if (DEFAULT_DURATION > 0) {
                final double velocityX = (double) (finalX - startX) / (DEFAULT_DURATION / 1000);
                final double velocityY = (double) (finalY - startY) / (DEFAULT_DURATION / 1000);
                final double velocity = Math.hypot(velocityX, velocityY);
                this.sinAngle = velocityY / velocity;
                this.cosAngle = velocityX / velocity;

                this.distance = (int) Math.round(velocity * DEFAULT_DURATION / 1000);
            }
            oldProgress = 0;
        } finally {
            lock.unlock();
        }
    }

    public void fling(final int startX, final int startY, final int velocityX, final int velocityY, final int minX,
            final int maxX, final int minY, final int maxY) {
        lock.lock();
        try {
            mode = MODE_FLING;
            this.startX = startX;
            this.startY = startY;
            this.currX = startX;
            this.currY = startY;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;

            final double velocity = Math.hypot(velocityX, velocityY);
            this.sinAngle = velocityY / velocity;
            this.cosAngle = velocityX / velocity;

            this.startTime = SystemClock.uptimeMillis();
            oldProgress = 0;

            this.duration = (int) Math.round(FLING_DURATION_PARAM
                    * Math.pow(Math.abs(velocity), 1.0 / (DECELERATED_FACTOR - 1)));

            // System.out.println("Start time:" + startTime + ", duration:" + duration + ", " + startY);
            this.distance = (int) Math.round(velocity * duration / DECELERATED_FACTOR / 1000);

            this.finalX = getX(1.0f);
            this.finalY = getY(1.0f);
        } finally {
            lock.unlock();
        }
    }

    public boolean computeScrollOffset() {
        lock.lock();
        try {
            if (isFinished()) {
                if (oldProgress == 0 || oldProgress == 1) {
                    // System.out.println("oldProgress == 0 || oldProgress == 1");
                    return false;
                } else {
                    currX = finalX;
                    currY = finalY;
                    oldProgress = 1;
                    // System.out.println("Finished: " + currY);
                    // It was not really finished, but it surely is now.
                    return false;
                }
            }
            // System.out.println("Flinger.computeScrollOffset(" + SystemClock.uptimeMillis() + ")");
            float progress = duration > 0 ? (float) (SystemClock.uptimeMillis() - startTime) / duration : 1;
            if (oldProgress == progress && progress != 0) {
                // System.out.println("oldProgress == progress && progress != 0");
                currX = finalX;
                currY = finalY;
                mode = MODE_STOPPED;
                return false;
            }
            progress = Math.min(progress, 1);
            // System.out.println("computeScrollOffset progress:" + progress);
            if (mode == MODE_FLING) {
                float f = progress;
                f = 1 - (float) Math.pow(1 - progress, DECELERATED_FACTOR);
                currX = getX(f);
                currY = getY(f);
                // System.out.println("computeScrollOffset(FLING):" + f + ", " + currY);
            } else {
                currX = (int) (startX + (finalX - startX) * progress);
                currY = (int) (startY + (finalY - startY) * progress);
                // System.out.println("computeScrollOffset(SCROLL):" + progress + ", " + currY);
            }
            oldProgress = progress;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean isFinished() {
        lock.lock();
        try {
            if (SystemClock.uptimeMillis() - startTime >= duration) {
                startTime = 0;
                duration = 0;
                mode = MODE_STOPPED;
            }
            return mode == MODE_STOPPED;
        } finally {
            lock.unlock();
        }
    }

    public void forceFinished() {
        lock.lock();
        try {
            if (isFinished()) {
                return;
            }
            startTime = 0;
            duration = 0;
            // System.out.println("Flinger.forceFinished(): " + oldProgress);
            if (oldProgress > 0) {
                if (mode == MODE_FLING) {
                    currX = getX(oldProgress);
                    currY = getY(oldProgress);
                } else {
                    currX = (int) (startX + (finalX - startX) * oldProgress);
                    currY = (int) (startY + (finalY - startY) * oldProgress);
                }
                oldProgress = 0;
            }
            mode = MODE_STOPPED;
        } finally {
            lock.unlock();
        }
    }

    public void abortAnimation() {
        lock.lock();
        try {
            startTime = 0;
            duration = 0;
            oldProgress = 0;
            mode = MODE_STOPPED;
        } finally {
            lock.unlock();
        }
    }

    private int getX(final float f) {
        int r = (int) Math.round(startX + f * distance * cosAngle);
        if (cosAngle > 0 && startX <= maxX) {
            r = Math.min(r, maxX);
        } else if (cosAngle < 0 && startX >= minX) {
            r = Math.max(r, minX);
        }
        return r;
    }

    private int getY(final float f) {
        int r = (int) Math.round(startY + f * distance * sinAngle);
        if (sinAngle > 0 && startY <= maxY) {
            r = Math.min(r, maxY);
        } else if (sinAngle < 0 && startY >= minY) {
            r = Math.max(r, minY);
        }
        return r;
    }
}
