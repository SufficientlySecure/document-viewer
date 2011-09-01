package org.ebookdroid.core;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

public interface DecodeService {

    void open(String fileName, String password);

    void decodePage(PageTreeNode node, ViewState viewState, DecodeCallback decodeCallback, boolean nativeResolution);

    void stopDecoding(PageTreeNode node, String reason);

    int getPageCount();

    List<OutlineLink> getOutline();

    CodecPageInfo getPageInfo(int pageIndex);

    void recycle();

    Rect getScaledSize(float viewWidth, float pageWidth, float pageHeight, RectF nodeBounds, float zoom);

    void updateViewState(ViewState viewState);

    interface DecodeCallback {

        void decodeComplete(CodecPage page, Bitmap bitmap);
    }
}
