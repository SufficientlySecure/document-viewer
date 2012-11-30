package org.emdev.ui.widget;

import android.os.SystemClock;

public class Flinger {

    private static final float FLING_DURATION_PARAM = 50f;
    private static final int DECELERATED_FACTOR = 3;
    private static final int DEFAULT_DURATION = 250;

    private static final int MODE_STOPPED = 0;
    private static final int MODE_SCROLL = 1;
    private static final int MODE_FLING = 2;

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

    public int getFinalX() {
        return finalX;
    }

    public int getFinalY() {
        return finalY;
    }

    public int getDuration() {
        return duration;
    }

    public int getCurrX() {
        return currX;

    }

    public int getCurrY() {
        return currY;
    }

    public void startScroll(final int startX, final int startY, final int dx, final int dy) {
        startScroll(startX, startY, dx, dy, DEFAULT_DURATION);
    }

    private void startScroll(final int startX, final int startY, final int dx, final int dy, final int duration) {
        mode = MODE_SCROLL;
        this.startX = startX;
        this.startY = startY;
        this.finalX = startX + dx;
        this.finalY = startY + dy;
        this.duration = duration;
        this.startTime = SystemClock.uptimeMillis();
        if (duration > 0) {
            final double velocityX = (double) (finalX - startX) / (duration / 1000);
            final double velocityY = (double) (finalY - startY) / (duration / 1000);
            final double velocity = Math.hypot(velocityX, velocityY);
            this.sinAngle = velocityY / velocity;
            this.cosAngle = velocityX / velocity;

            this.distance = (int) Math.round(velocity * duration / 1000);
        }
        oldProgress = 0;
    }

    public void fling(final int startX, final int startY, final int velocityX, final int velocityY, final int minX,
            final int maxX, final int minY, final int maxY) {
        mode = MODE_FLING;
        this.startX = startX;
        this.startY = startY;
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

        // System.out.println("Start time:" + startTime + ", duration:" + duration);
        this.distance = (int) Math.round(velocity * duration / DECELERATED_FACTOR / 1000);

        this.finalX = getX(1.0f);
        this.finalY = getY(1.0f);
    }

    public boolean computeScrollOffset() {
        if (isFinished()) {
            return false;
        }
        // System.out.println("Flinger.computeScrollOffset(" + SystemClock.uptimeMillis() + ")");
        float progress = duration > 0 ? (float) (SystemClock.uptimeMillis() - startTime) / duration : 1;
        if (oldProgress == progress && progress != 0) {
            // System.out.println("oldProgress == progress && progress != 0");
            mode = MODE_SCROLL;
            return false;
        }
        progress = Math.min(progress, 1);
        // System.out.println("computeScrollOffset progress:" + progress);
        if (mode == MODE_FLING) {
            float f = progress;
            f = 1 - (float) Math.pow(1 - progress, DECELERATED_FACTOR);
            currX = getX(f);
            currY = getY(f);
            // System.out.println("computeScrollOffset(FLING):" + f);
        } else {
            currX = (int) (startX + (finalX - startX) * progress);
            currY = (int) (startY + (finalY - startY) * progress);
            // System.out.println("computeScrollOffset(SCROLL):" + progress);
        }
        oldProgress = progress;
        return true;
    }

    public boolean isFinished() {
        if (SystemClock.uptimeMillis() - startTime >= duration) {
            startTime = 0;
            duration = 0;
            oldProgress = 0;
            mode = MODE_STOPPED;
        }
        return mode == MODE_STOPPED;
    }

    public void forceFinished() {
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
    }

    public void abortAnimation() {
        startTime = 0;
        duration = 0;
        oldProgress = 0;
        mode = MODE_STOPPED;
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
