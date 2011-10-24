package org.ebookdroid.pdfdroid.codec;

import android.util.FloatMath;

public class FzGeometry {

    public static class fz_matrix {

        float a, b, c, d, e, f;

        public fz_matrix() {
        }

        public fz_matrix(float a, float b, float c, float d, float e, float f) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
        }
    };

    public static final fz_matrix fz_identity = new fz_matrix(1, 0, 0, 1, 0, 0);

    public static fz_matrix fz_concat(fz_matrix one, fz_matrix two) {
        fz_matrix dst = new fz_matrix();
        dst.a = one.a * two.a + one.b * two.c;
        dst.b = one.a * two.b + one.b * two.d;
        dst.c = one.c * two.a + one.d * two.c;
        dst.d = one.c * two.b + one.d * two.d;
        dst.e = one.e * two.a + one.f * two.c + two.e;
        dst.f = one.e * two.b + one.f * two.d + two.f;
        return dst;
    }

    public static fz_matrix fz_scale(float sx, float sy) {
        fz_matrix m = new fz_matrix();
        m.a = sx;
        m.b = 0;
        m.c = 0;
        m.d = sy;
        m.e = 0;
        m.f = 0;
        return m;
    }

    public static fz_matrix fz_shear(float h, float v) {
        fz_matrix m = new fz_matrix();
        m.a = 1;
        m.b = v;
        m.c = h;
        m.d = 1;
        m.e = 0;
        m.f = 0;
        return m;
    }

    public static fz_matrix fz_rotate(float theta) {
        fz_matrix m = new fz_matrix();
        float s;
        float c;

        while (theta < 0) {
            theta += 360;
        }
        while (theta >= 360) {
            theta -= 360;
        }

        if (Math.abs(0 - theta) < 0.1) {
            s = 0;
            c = 1;
        } else if (Math.abs(90.0f - theta) < 0.1) {
            s = 1;
            c = 0;
        } else if (Math.abs(180.0f - theta) < 0.1) {
            s = 0;
            c = -1;
        } else if (Math.abs(270.0f - theta) < 0.1) {
            s = -1;
            c = 0;
        } else {
            s = FloatMath.sin(theta * (float) Math.PI / 180);
            c = FloatMath.cos(theta * (float) Math.PI / 180);
        }

        m.a = c;
        m.b = s;
        m.c = -s;
        m.d = c;
        m.e = 0;
        m.f = 0;
        return m;
    }

    public static fz_matrix fz_translate(float tx, float ty) {
        fz_matrix m = new fz_matrix();
        m.a = 1;
        m.b = 0;
        m.c = 0;
        m.d = 1;
        m.e = tx;
        m.f = ty;
        return m;
    }

    public static fz_matrix fz_invert_matrix(fz_matrix src) {
        fz_matrix dst = new fz_matrix();
        float rdet = 1 / (src.a * src.d - src.b * src.c);
        dst.a = src.d * rdet;
        dst.b = -src.b * rdet;
        dst.c = -src.c * rdet;
        dst.d = src.a * rdet;
        dst.e = -src.e * dst.a - src.f * dst.c;
        dst.f = -src.e * dst.b - src.f * dst.d;
        return dst;
    }

}
