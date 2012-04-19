package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

public interface DecodeService {

    void open(String fileName, String password);

    void decodePage(ViewState viewState, PageTreeNode node);

    void searchText(Page page, String pattern, SearchCallback callback);

    void stopSearch(String pattern);

    void stopDecoding(PageTreeNode node, String reason);

    int getPageCount();

    List<OutlineLink> getOutline();

    CodecPageInfo getUnifiedPageInfo();

    CodecPageInfo getPageInfo(int pageIndex);

    void recycle();

    void updateViewState(ViewState viewState);

    BitmapRef createThumbnail(int width, int height, int pageNo, RectF region);

    boolean isPageSizeCacheable();

    int getPixelFormat();

    Bitmap.Config getBitmapConfig();

    interface DecodeCallback {

        void decodeComplete(CodecPage codecPage, BitmapRef bitmap, Rect bitmapBounds, RectF croppedPageBounds);

    }

    interface SearchCallback {

        void searchComplete(Page page, List<? extends RectF> regions);

    }

}
