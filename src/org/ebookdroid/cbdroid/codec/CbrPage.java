package org.ebookdroid.cbdroid.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

public class CbrPage implements CodecPage {
	private FileHeader fh;
	private Archive rarFile;
	private Bitmap bitmap;
	private CodecPageInfo pageInfo;

	public CbrPage(Archive file, FileHeader fh) {
		this.rarFile = file;
		this.fh = fh;
	}

	private void decompress() {
		pageInfo = new CodecPageInfo();
		if (rarFile != null && fh != null) {
			try {
				Log.d("CbrPage", "Starting decompressing: " + fh.getFileNameString());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				rarFile.extractFile(fh, baos);
				baos.close();
				Options opts = new Options();
				opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
				byte[] byteArray = baos.toByteArray();
				bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, opts);
				pageInfo.setHeight(bitmap.getHeight());
				pageInfo.setWidth(bitmap.getWidth());
				Log.d("CbrPage", "Finishing decompressing: " + fh.getFileNameString());
			} catch (IOException e) {
				Log.d("CbrPage", "Can not decompress page: " + e.getMessage());
			} catch (RarException e) {
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
	public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds) {
		if (bitmap == null) {
			decompress();
		}
		if (bitmap == null) {
			return null;
		}

		Matrix matrix = new Matrix();
		matrix.postScale((float) width / getWidth(), (float) height / getHeight());
		matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
		matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

		Canvas c = new Canvas(bmp);
		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		paint.setAntiAlias(true);
		paint.setDither(true);
		c.drawBitmap(bitmap, matrix, null);

		return bmp;
	}
}
