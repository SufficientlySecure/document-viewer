package org.emdev.common.textmarkup.line;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.graphics.Rect;

import org.emdev.common.textmarkup.image.IImageData;

public class Image extends AbstractLineElement {

    public final IImageData data;
    private final Paint paint;

    public Image(final IImageData data, final boolean inline) {
        super(data.getImageRect(inline));
        this.data = data;
        this.paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, final float left,
            final float right, final int nightmode) {
        if (left < x + width && x < right) {
            final Bitmap bmp = data.getBitmap();

            paint.setXfermode(null);
            if (nightmode != 0) {
                paint.setXfermode(new PixelXorXfermode(-1));
                c.drawRect(x, y - height, (int) (x + width), y, paint);
            }

            if (bmp != null) {
                c.drawBitmap(bmp, null, new Rect(x, y - height, (int) (x + width), y), paint);
                bmp.recycle();
            } else {
                c.drawRect(new Rect(x, y - height, (int) (x + width), y), paint);
            }
        }
        return width;
    }
}
