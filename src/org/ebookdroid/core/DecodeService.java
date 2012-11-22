package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.ByteBufferBitmap;
import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.RectF;

import java.util.List;

public interface DecodeService extends CodecFeatures {

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

    IBitmapRef createThumbnail(boolean useEmbeddedIfAvailable, int width, int height, int pageNo, RectF region);

    ByteBufferBitmap createPageThumbnail(int width, int height, int pageNo, RectF region);

    interface DecodeCallback {

        void decodeComplete(CodecPage codecPage, ByteBufferBitmap bitmap, RectF croppedPageBounds);

    }

    interface SearchCallback {

        void searchComplete(Page page, List<? extends RectF> regions);

    }

}
