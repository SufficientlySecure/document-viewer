package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

@Deprecated
public class CbrPage implements CodecPage {

    private final FileHeader fh;
    private final Archive rarFile;
    private Bitmap bitmap;
    private CodecPageInfo pageInfo;

    public CbrPage(final Archive file, final FileHeader fh) {
        this.rarFile = file;
        this.fh = fh;
    }

    private void decompress() {
        pageInfo = new CodecPageInfo();
        if (rarFile != null && fh != null) {
            try {
                Log.d("CbrPage", "Starting decompressing: " + fh.getFileNameString());
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rarFile.extractFile(fh, baos);
                baos.close();
                final Options opts = new Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                final byte[] byteArray = baos.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, opts);
                pageInfo.setHeight(bitmap.getHeight());
                pageInfo.setWidth(bitmap.getWidth());
                Log.d("CbrPage", "Finishing decompressing: " + fh.getFileNameString());
            } catch (final IOException e) {
                Log.d("CbrPage", "Can not decompress page: " + e.getMessage());
            } catch (final RarException e) {
                Log.d("CbrPage", "Can not decompress page: " + e.getMessage());
            }
        }
    }

    @Override
    public int getHeight() {
        return getPageInfo().getHeight();
    }

    private CodecPageInfo getPageInfo() {
        if (pageInfo == null) {
            decompress();
        }

        return pageInfo;
    }

    @Override
    public int getWidth() {
        return getPageInfo().getWidth();
    }

    @Override
    public void recycle() {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    @Override
    public Bitmap renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        if (bitmap == null) {
            decompress();
        }
        if (bitmap == null) {
            return null;
        }

        final Matrix matrix = new Matrix();
        matrix.postScale((float) width / getWidth(), (float) height / getHeight());
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        final Canvas c = new Canvas(bmp);
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        c.drawBitmap(bitmap, matrix, null);

        return bmp;
    }
}
