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

    @Override
    public boolean isFeatureSupported(final int feature) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.DecodeService#open(java.lang.String, java.lang.String)
     */
    @Override
    public void open(final String fileName, final String password) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.DecodeService#decodePage(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.PageTreeNode)
     */
    @Override
    public void decodePage(final ViewState viewState, final PageTreeNode node) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.DecodeService#stopDecoding(org.ebookdroid.core.PageTreeNode, java.lang.String)
     */
    @Override
    public void stopDecoding(final PageTreeNode node, final String reason) {
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
    public CodecPageInfo getPageInfo(final int pageIndex) {
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
    public void updateViewState(final ViewState viewState) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.DecodeService#createThumbnail(booleam int, int, int, android.graphics.RectF)
     */
    @Override
    public BitmapRef createThumbnail(boolean useEmbeddedIfAvailable, final int width, final int height, final int pageNo, final RectF region) {
        return null;
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
    public void searchText(final Page page, final String pattern, final SearchCallback callback) {
    }

    @Override
    public void stopSearch(final String pattern) {
    }

}
