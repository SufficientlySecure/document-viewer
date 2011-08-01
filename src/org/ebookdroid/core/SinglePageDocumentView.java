package org.ebookdroid.core;

import org.ebookdroid.core.curl.SinglePageCurler;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * The Class SinglePageDocumentView.
 *
 * Used in single page view mode
 */
public class SinglePageDocumentView extends AbstractDocumentView {

    /** The curler. */
    private SinglePageCurler curler;

    /** The use curler flag. */
    private boolean useCurler = true;

	/**
     * Instantiates a new single page document view.
     *
     * @param baseActivity the base activity
     */
    public SinglePageDocumentView(BaseViewerActivity baseActivity) {
        super(baseActivity);
        updateUseAnimation();
    }

    /**
	 * Checks if is the use curler flag set.
	 *
	 * @return the use curler flag
	 */
    public boolean isUseCurler() {
		return useCurler;
    }

	/**
	 * Sets the use curler flag.
	 *
	 * @param useCurler
	 *            the new use curler flag
	 */
	public void setUseCurler(boolean useCurler) {
		this.useCurler = useCurler;
        if (useCurler) {
            initCurler();
        }
	}

  /**
   * Inits the view.
   *
   * @see org.ebookdroid.core.AbstractDocumentView#init()
   */
  @Override
  protected void init() {
      super.init();
        if (useCurler) {
            initCurler();
        }
    }

	/**
	 * Inits the curler.
	 */
	private void initCurler() {
		curler = new SinglePageCurler(this);
		curler.init();
  }

    @Override
    public void goToPageImpl(final int toPage) {
        if (toPage >= 0 && toPage <= getBase().getDocumentModel().getPageCount()) {
            getBase().getDocumentModel().setCurrentPageIndex(toPage);
            updatePageVisibility();
        }
        if (useCurler) {
        	curler.resetPageIndexes();
        }
    }

    @Override
    public int getCurrentPage() {
        return getBase().getDocumentModel().getCurrentPageIndex();
    }

    @Override
    protected void verticalConfigScroll(int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentPageIndex() + direction);

        invalidate();
    }

    @Override
    protected void verticalDpadScroll(int direction) {
        goToPageImpl(getBase().getDocumentModel().getCurrentPageIndex() + direction);
        invalidate();
    }

    @Override
    protected int getTopLimit() {
        RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.top > 0) ? 0 : (int) bounds.top;
    }

    @Override
    protected int getLeftLimit() {
        RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.left > 0) ? 0 : (int) bounds.left;
    }

    @Override
    protected int getBottomLimit() {
        RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.bottom < getHeight()) ? 0 : (int) bounds.bottom - getHeight();
    }

    @Override
    protected int getRightLimit() {
        RectF bounds = getBase().getDocumentModel().getCurrentPageObject().getBounds();
        return ((int) bounds.right < getWidth()) ? 0 : (int) bounds.right - getWidth();
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()), Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isCurlerDisabled()) {
            return super.onTouchEvent(event);
        } else {
            if (getBase().getMultiTouchZoom() != null) {
                if (getBase().getMultiTouchZoom().onTouchEvent(event)) {
                    return true;
                }

                if (getBase().getMultiTouchZoom().isResetLastPointAfterZoom()) {
                    setLastPosition(event);
                    getBase().getMultiTouchZoom().setResetLastPointAfterZoom(false);
                }
            }

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(event);

            return curler.onTouchEvent(event);
        }
    }

    private boolean isCurlerDisabled() {
      PageAlign align = getAlign();
      float zoom = getBase().getZoomModel().getZoom();
      return align != PageAlign.AUTO || zoom != 1.0f || !useCurler || curler == null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isCurlerDisabled()) {
            Page page = getBase().getDocumentModel().getCurrentPageObject();
            if (page != null) {
                page.draw(canvas);
            }
        } else {
            curler.onDraw(canvas);
        }
    }

    /**
     * Invalidate page sizes.
     */
    public void invalidatePageSizes() {
        if (!isInitialized()) {
            return;
        }
        float heightAccum = 0;

        int width = getWidth();
        int height = getHeight();
        float zoom = getBase().getZoomModel().getZoom();

        for (int i = 0; i < getBase().getDocumentModel().getPages().size(); i++) {
            Page page = getBase().getDocumentModel().getPages().get(i);

            PageAlign effectiveAlign = getAlign();
            if (getAlign() == PageAlign.AUTO) {
                float pageHeight = page.getPageHeight(width, zoom);
                if (pageHeight > height) {
                    effectiveAlign = PageAlign.HEIGHT;
                } else {
                    effectiveAlign = PageAlign.WIDTH;
                }
            }

            if (effectiveAlign == PageAlign.WIDTH) {
                float pageHeight = page.getPageHeight(width, zoom);
                page.setBounds(new RectF(0, ((height - pageHeight) / 2), width * zoom, pageHeight + ((height - pageHeight) / 2)));
                heightAccum += pageHeight;
            } else {
                float pageWidth = page.getPageWidth(height, zoom);
                page.setBounds(new RectF((width - pageWidth) / 2, 0, pageWidth + (width - pageWidth) / 2, height * zoom));
                heightAccum += height * zoom;
            }
        }
        if (useCurler && curler != null) {
            curler.setViewDrawn(false);
        }

    }

    @Override
    public boolean isPageTreeNodeVisible(PageTreeNode pageTreeNode) {
        return ((pageTreeNode.getParent() == null)&&(Math.abs(pageTreeNode.getPageIndex() - getCurrentPage()) <= getBase().getAppSettings().getPagesInMemory()))
        ||((pageTreeNode.getPageIndex() == getCurrentPage()) && (RectF.intersects(getViewRect(), pageTreeNode.getTargetRectF())));
    }

    @Override
    public boolean isPageVisible(Page page) {
        return (Math.abs(page.getIndex() - getCurrentPage()) <= getBase().getAppSettings().getPagesInMemory());
    }

	@Override
	public void updateUseAnimation() {
        setUseCurler(getBase().getAppSettings().getUseAnimation());
	}

}
