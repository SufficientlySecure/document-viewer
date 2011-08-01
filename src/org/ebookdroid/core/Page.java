package org.ebookdroid.core;

import org.ebookdroid.R;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class Page {
    public enum PageType {
        LEFT_PAGE, RIGHT_PAGE, FULL_PAGE
    }
    private final int index;
    private RectF bounds;
    private PageTreeNode node;
    private IViewerActivity base;
    private float aspectRatio;
    private final int documentPage;
    private final PageType pageType;

    public Page(IViewerActivity base, int index, int documentPage, PageType pt) {
        this.base = base;
        this.index = index;
        this.documentPage = documentPage;
        this.pageType = pt;
        boolean sliceLimit = base.getAppSettings().getSliceLimit();

        node = new PageTreeNode(base, getInitialPageRectF(), this, 1, null, sliceLimit);
    }

    private RectF getInitialPageRectF() {
        switch (pageType) {
        case FULL_PAGE:
            return new RectF(0, 0, 1, 1);
        case LEFT_PAGE:
            return new RectF(0, 0, 0.5f, 1);
        case RIGHT_PAGE:
            return new RectF(0.5f, 0, 1, 1);
        }
        return new RectF(0, 0, 1, 1);
    }

    public float getPageHeight(int mainWidth, float zoom) {
        return mainWidth / getAspectRatio() * zoom;
    }

    public float getPageWidth(int mainHeight, float zoom) {
        return mainHeight * getAspectRatio() * zoom;
    }

    public int getTop() {
        return Math.round(getBounds().top);
    }

    public void draw(Canvas canvas) {
      draw(canvas, false);
    }

    public void draw(Canvas canvas, boolean drawInvisible) {
      if (drawInvisible || isVisible()) {
        PagePaint paint = base.getAppSettings().getNightMode() ? PagePaint.NIGHT : PagePaint.DAY;

        canvas.drawRect(getBounds(), paint.getFillPaint());

        canvas.drawText(base.getContext().getString(R.string.text_page) + " " + (getIndex() + 1), getBounds().centerX(), getBounds().centerY(), paint.getTextPaint());
        node.draw(canvas, paint);
        canvas.drawLine(getBounds().left, getBounds().top, getBounds().right, getBounds().top, paint.getStrokePaint());
        canvas.drawLine(getBounds().left, getBounds().bottom, getBounds().right, getBounds().bottom, paint.getStrokePaint());
      }
  }

    public float getAspectRatio() {
        return aspectRatio;
    }

    private void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            base.getDocumentController().invalidatePageSizes();
        }
    }

    public void updateAspectRatio() {
        Log.d("DocModel", "Start update aspect ratio for page: " + this);
        try {
            DecodeService decodeService = base.getDocumentModel().getDecodeService();
            if (pageType == PageType.FULL_PAGE) {
                this.setAspectRatio(decodeService.getPageWidth(documentPage), decodeService.getPageHeight(documentPage));
            } else {
                this.setAspectRatio(decodeService.getPageWidth(documentPage) / 2, decodeService.getPageHeight(documentPage));
            }
        } finally {
            Log.d("DocModel", "Finish update aspect ratio for page: " + this);
        }
    }

    public boolean isVisible() {
        boolean pageVisible = base.getDocumentController().isPageVisible(this);
        //Log.d("DocModel", "Page visibility: " + this + " -> " + pageVisible);
        return pageVisible;
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0f / height);
    }

    public void setBounds(RectF pageBounds) {
        bounds = pageBounds;
        node.invalidateNodeBounds();
    }

    public void updateVisibility() {
        node.updateVisibility();
    }

    public void invalidate() {
        node.invalidate();
    }
    public RectF getBounds() {
        return bounds;
    }
    public int getIndex() {
        return index;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder("Page");
        buf.append("[");

        buf.append("index").append("=").append(index);
        buf.append(", ");
        buf.append("bounds").append("=").append(bounds);
        buf.append(", ");
        buf.append("aspectRatio").append("=").append(aspectRatio);

        buf.append("]");
        return buf.toString();
    }

    public int getDocumentPageIndex() {
        return documentPage;
    }

    public float getTargetRectScale() {
        switch (pageType) {
        case FULL_PAGE:
            return 1;
        case LEFT_PAGE:
        case RIGHT_PAGE:
            return 2;
        }
        return 1;
    }

    public float getTargetTranslate() {
        switch (pageType) {
        case FULL_PAGE:
        case LEFT_PAGE:
            return 0;
        case RIGHT_PAGE:
            return 1;
        }
        return 0;
    }

}
