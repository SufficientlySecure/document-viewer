package org.ebookdroid.core.curl;

import android.graphics.PointF;
import android.util.FloatMath;

/**
 * Inner class used to represent a 2D point.
 */
public class Vector2D extends PointF {

    public Vector2D() {
        super();
    }

    public Vector2D(final float x, final float y) {
        super(x, y);
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }

    public float lengthSquared() {
        return (x * x) + (y * y);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Vector2D) {
            final Vector2D p = (Vector2D) o;
            return p.x == x && p.y == y;
        }
        return false;
    }

    public Vector2D reverse() {
        return new Vector2D(-x, -y);
    }

    public Vector2D sum(final Vector2D b) {
        return new Vector2D(x + b.x, y + b.y);
    }

    public Vector2D sub(final Vector2D b) {
        return new Vector2D(x - b.x, y - b.y);
    }

    public float dot(final Vector2D vec) {
        return (x * vec.x) + (y * vec.y);
    }

    public float cross(final Vector2D a, final Vector2D b) {
        return a.cross(b);
    }

    public float cross(final Vector2D vec) {
        return x * vec.y - y * vec.x;
    }

    public float distanceSquared(final Vector2D other) {
        final float dx = other.x - x;
        final float dy = other.y - y;

        return (dx * dx) + (dy * dy);
    }

    public float distance(final Vector2D other) {
        return (float) Math.sqrt(distanceSquared(other));
    }

    public float absdistancex(final Vector2D other) {
        return Math.abs(this.x - other.x);
    }

    public float absdistancey(final Vector2D other) {
        return Math.abs(this.y - other.y);
    }

    public float dotProduct(final Vector2D other) {
        return other.x * x + other.y * y;
    }

    public Vector2D normalize() {
        final float magnitude = (float) Math.sqrt(dotProduct(this));
        return new Vector2D(x / magnitude, y / magnitude);
    }

    public Vector2D mult(final float scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }
}
