package org.emdev.ui.widget;

import android.os.SystemClock;

public class Flinger {

    private static final float FLING_DURATION_PARAM = 50f;
    private static final int DECELERATED_FACTOR = 4;
    private static final int DEFAULT_DURATION = 250;
    private static final int SCROLL_MODE = 0;
    private static final int FLING_MODE = 1;

    private int mode;

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

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, DEFAULT_DURATION);
    }

    private void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mode = SCROLL_MODE;
        this.startX = startX;
        this.startY = startY;
        this.finalX = startX + dx;
        this.finalY = startY + dy;
        this.duration = duration;
        this.startTime = SystemClock.uptimeMillis();
        if (duration > 0) {
            double velocityX = (double) (finalX - startX) / (duration / 1000);
            double velocityY = (double) (finalY - startY) / (duration / 1000);
            double velocity = Math.hypot(velocityX, velocityY);
            this.sinAngle = velocityY / velocity;
            this.cosAngle = velocityX / velocity;

            this.distance = (int) Math.round(velocity * duration / 1000);
        }
        oldProgress = 0;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        mode = FLING_MODE;
        this.startX = startX;
        this.startY = startY;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;

        double velocity = Math.hypot(velocityX, velocityY);
        this.sinAngle = velocityY / velocity;
        this.cosAngle = velocityX / velocity;

        this.startTime = SystemClock.uptimeMillis();
        oldProgress = 0;

        this.duration = (int) Math.round(FLING_DURATION_PARAM
                * Math.pow(Math.abs(velocity), 1.0 / (DECELERATED_FACTOR - 1)));

        this.distance = (int) Math.round(velocity * duration / DECELERATED_FACTOR / 1000);

        this.finalX = getX(1.0f);
        this.finalY = getY(1.0f);
    }

    public boolean computeScrollOffset() {
        float progress = duration > 0 ? (SystemClock.uptimeMillis() - startTime) / duration : 1;
        if (oldProgress == progress && progress != 0) {
            return false;
        }
        progress = Math.min(progress, 1);
        float f = progress;
        if (mode == FLING_MODE) {
            f = 1 - (float) Math.pow(1 - progress, DECELERATED_FACTOR);
        }
        currX = getX(f);
        currY = getY(f);
        oldProgress = progress;
        return true;
    }

    public boolean isFinished() {
        return SystemClock.uptimeMillis() - startTime >= duration;
    }

    public void forceFinished(boolean b) {
        startTime = 0;
        duration = 0;
        currX = getX(1);
        currY = getY(1);
    }

    public void abortAnimation() {
        startTime = 0;
        duration = 0;
    }

    private int getX(float f) {
        int r = (int) Math.round(startX + f * distance * cosAngle);
        if (cosAngle > 0 && startX <= maxX) {
            r = Math.min(r, maxX);
        } else if (cosAngle < 0 && startX >= minX) {
            r = Math.max(r, minX);
        }
        return r;
    }

    private int getY(float f) {
        int r = (int) Math.round(startY + f * distance * sinAngle);
        if (sinAngle > 0 && startY <= maxY) {
            r = Math.min(r, maxY);
        } else if (sinAngle < 0 && startY >= minY) {
            r = Math.max(r, minY);
        }
        return r;
    }
}
