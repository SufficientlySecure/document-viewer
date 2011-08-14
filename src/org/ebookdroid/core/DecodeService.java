package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;

import java.util.List;

public interface DecodeService {

    void open(String fileName, String password);

    void decodePage(PageTreeNode node, int targetWidth, float zoom, DecodeCallback decodeCallback);

    void stopDecoding(PageTreeNode node, String reason);

    int getPageCount();

    List<OutlineLink> getOutline();

    CodecPageInfo getPageInfo(int pageIndex);

    void recycle();

    public interface DecodeCallback {

        void decodeComplete(CodecPage page, Bitmap bitmap);
    }
}
