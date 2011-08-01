package org.ebookdroid.cbdroid.codec;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

public class CbzPage implements CodecPage {
	private ZipEntry zipEntry;
	private ZipFile zipFile;
	private Bitmap bitmap;
	private CodecPageInfo pageInfo;

	public CbzPage(ZipFile file, ZipEntry zipEntry) {
		this.zipFile = file;
		this.zipEntry = zipEntry;
	}

	private void decompress() {
		if (zipFile != null && zipEntry != null) {
			try {
				Log.d("CbzPage", "Starting decompressing: "+ zipEntry.getName());
				InputStream is = zipFile.getInputStream(zipEntry);
				Options opts = new Options();
				opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
				bitmap = BitmapFactory.decodeStream(is, null, opts );
				pageInfo = new CodecPageInfo();
				pageInfo.setHeight(bitmap.getHeight());
				pageInfo.setWidth(bitmap.getWidth());
				is.close();
				Log.d("CbzPage", "Finishing decompressing: "+ zipEntry.getName());
			} catch (IOException e) {
					Log.d("CbzPage", "Can not decompress page: "+ e.getMessage());
			}
		}
	}

	@Override
	public int getHeight() {
		return getPageInfo().getHeight();
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
	public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds) {
		if (bitmap == null) {
			decompress();
		}
		if (bitmap == null) {
			return null;
		}
		
        Matrix matrix = new Matrix();
        matrix.postScale((float)width / getWidth(), (float)height / getHeight());
        matrix.postTranslate(-pageSliceBounds.left*width, -pageSliceBounds.top*height);
        matrix.postScale(1/pageSliceBounds.width(), 1/pageSliceBounds.height());

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        Canvas c = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        c.drawBitmap(bitmap, matrix, null);
        
		return bmp;
	}

	public CodecPageInfo getPageInfo() {
		if (pageInfo == null) {
			if (zipFile != null && zipEntry != null) {
				try {
					Log.d("CbzPage", "Starting decompressing: "+ zipEntry.getName());
					InputStream is = zipFile.getInputStream(zipEntry);
					Options opts = new Options();
					opts.inJustDecodeBounds = true;
					BitmapFactory.decodeStream(is,null, opts);
					pageInfo = new CodecPageInfo();
					pageInfo.setHeight(opts.outHeight);
					pageInfo.setWidth(opts.outWidth);
					is.close();
					Log.d("CbzPage", "Finishing decompressing: "+ zipEntry.getName());
				} catch (IOException e) {
						Log.d("CbzPage", "Can not decompress page: "+ e.getMessage());
				}
			}
		}
		return pageInfo;
	}

}
