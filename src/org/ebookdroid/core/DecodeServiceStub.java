package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.RectF;

import java.util.List;

public class DecodeServiceStub implements DecodeService {

    private static final CodecPageInfo DEFAULT = new CodecPageInfo(0, 0);

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#open(java.lang.String, java.lang.String)
     */
    @Override
    public void open(String fileName, String password) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#decodePage(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.PageTreeNode)
     */
    @Override
    public void decodePage(ViewState viewState, PageTreeNode node) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#stopDecoding(org.ebookdroid.core.PageTreeNode, java.lang.String)
     */
    @Override
    public void stopDecoding(PageTreeNode node, String reason) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getPageCount()
     */
    @Override
    public int getPageCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getOutline()
     */
    @Override
    public List<OutlineLink> getOutline() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getUnifiedPageInfo()
     */
    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return DEFAULT;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getPageInfo(int)
     */
    @Override
    public CodecPageInfo getPageInfo(int pageIndex) {
        return DEFAULT;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#recycle()
     */
    @Override
    public void recycle() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#updateViewState(org.ebookdroid.core.ViewState)
     */
    @Override
    public void updateViewState(ViewState viewState) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#createThumbnail(int, int, int, android.graphics.RectF)
     */
    @Override
    public BitmapRef createThumbnail(int width, int height, int pageNo, RectF region) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#isPageSizeCacheable()
     */
    @Override
    public boolean isPageSizeCacheable() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getPixelFormat()
     */
    @Override
    public int getPixelFormat() {
        return PixelFormat.RGBA_8888;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.DecodeService#getBitmapConfig()
     */
    @Override
    public Config getBitmapConfig() {
        return Config.ARGB_8888;
    }

    @Override
    public void searchText(Page page, String pattern, SearchCallback callback) {
    }

    @Override
    public void stopSearch(String pattern) {
    }

}
