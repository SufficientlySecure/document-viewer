package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecPageInfo;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import java.util.List;

public interface DecodeService {

    void setContentResolver(ContentResolver contentResolver);

    void open(Uri fileUri, String password);

    void decodePage(PageTreeNode node, int targetWidth, float zoom, DecodeCallback decodeCallback);

    void stopDecoding(PageTreeNode node, String reason);

    int getEffectivePagesWidth(int targetWidth);

    int getEffectivePagesHeight(int targetWidth);

    int getPageCount();

    public List<OutlineLink> getOutline();

    int getPageWidth(int pageIndex);

    CodecPageInfo getPageInfo(int pageIndex);

    int getPageHeight(int pageIndex);

    void recycle();

    public interface DecodeCallback {

        void decodeComplete(Bitmap bitmap);
    }
}
