package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.models.SearchModel;
import org.ebookdroid.core.models.SearchModel.Matches;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.List;
import java.util.Queue;

import org.emdev.BaseDroidApp;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.gl.GLCanvas;
import org.emdev.ui.gl.StringTexture;
import org.emdev.utils.LengthUtils;

public class EventGLDraw implements IEvent {

    public final static LogContext LCTX = LogManager.root().lctx("EventDraw", false);

    private static final Paint LINK_PAINT = new Paint();
    private static final Paint BRIGHTNESS_FILTER = new Paint();

    private static final StringTexture t = new StringTexture(256, 128);

    private final Queue<EventGLDraw> eventQueue;

    public ViewState viewState;
    public PageTreeLevel level;
    public GLCanvas canvas;

    Paint brightnessFilter;
    RectF pageBounds;
    final RectF fixedPageBounds = new RectF();

    EventGLDraw(final Queue<EventGLDraw> eventQueue) {
        this.eventQueue = eventQueue;
    }

    void init(final ViewState viewState, final GLCanvas canvas) {
        this.viewState = viewState;
        this.level = PageTreeLevel.getLevel(viewState.zoom);
        this.canvas = canvas;
    }

    void init(final EventGLDraw event, final GLCanvas canvas) {
        this.viewState = event.viewState;
        this.level = event.level;
        this.canvas = canvas;
    }

    void release() {
        this.canvas = null;
        this.level = null;
        this.pageBounds = null;
        this.viewState = null;
        eventQueue.offer(this);
    }

    @Override
    public ViewState process() {
        try {
            if (canvas == null || viewState == null) {
                return viewState;
            }

            canvas.clearBuffer(viewState.paint.backgroundFillPaint);

            viewState.ctrl.drawView(this);

            return viewState;
        } finally {
            release();
        }
    }

    @Override
    public boolean process(final Page page) {
        pageBounds = viewState.getBounds(page);
        fixedPageBounds.set(pageBounds);
        fixedPageBounds.offset(-viewState.viewBase.x, -viewState.viewBase.y);

        canvas.setClipRect(fixedPageBounds);
        try {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("process(" + page.index + "): view=" + viewState.viewRect + ", page=" + pageBounds);
            }

            drawPageBackground(page);

            final boolean res = page.nodes.process(this, level, false);

            if (res) {
                drawPageLinks(page);
                drawHighlights(page);
            }

            return res;
        } finally {
            canvas.clearClipRect();
        }

    }

    @Override
    public boolean process(final PageTree nodes) {
        return process(nodes, level);
    }

    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return nodes.process(this, level, false);
    }

    @Override
    public boolean process(final PageTreeNode node) {
        final RectF nodeRect = node.getTargetRect(pageBounds);
        if (LCTX.isDebugEnabled()) {
            LCTX.d("process(" + node.fullId + "): view=" + viewState.viewRect + ", page=" + pageBounds + ", node="
                    + nodeRect);
        }

        if (!viewState.isNodeVisible(nodeRect)) {
            return false;
        }

        try {
            if (node.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, nodeRect, nodeRect)) {
                return true;
            }

            if (node.parent != null) {
                final RectF parentRect = node.parent.getTargetRect(pageBounds);
                if (node.parent.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, parentRect, nodeRect)) {
                    return true;
                }
            }

            return node.page.nodes.paintChildren(this, node, nodeRect);

        } finally {
            drawBrightnessFilter(nodeRect);
        }
    }

    public boolean paintChild(final PageTreeNode node, final PageTreeNode child, final RectF nodeRect) {
        final RectF childRect = child.getTargetRect(pageBounds);
        return child.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, childRect, nodeRect);
    }

    protected void drawPageBackground(final Page page) {
        canvas.fillRect(fixedPageBounds, viewState.paint.fillPaint);
    }

    protected void drawPageNumber(final Page page) {
        final TextPaint textPaint = viewState.paint.textPaint;
        textPaint.setTextSize(24 * viewState.zoom);
        textPaint.setTextAlign(Align.LEFT);

        final int offset = viewState.book != null ? viewState.book.firstPageOffset : 1;
        final String text = BaseDroidApp.context.getString(R.string.text_page) + " " + (page.index.viewIndex + offset);

        t.setText(text, textPaint);
        final int w = t.getTextWidth();
        final int h = t.getTextHeight();
        t.draw(canvas, (int) fixedPageBounds.centerX() - w / 2, (int) fixedPageBounds.centerY() - h / 2);
    }

    private void drawPageLinks(final Page page) {
        if (LengthUtils.isEmpty(page.links)) {
            return;
        }

        for (final PageLink link : page.links) {
            final RectF rect = page.getLinkSourceRect(pageBounds, link);
            if (rect != null) {
                rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
                LINK_PAINT.setColor(AppSettings.current().linkHighlightColor);
                canvas.fillRect(rect, LINK_PAINT);
            }
        }
    }

    private void drawHighlights(final Page page) {
        final SearchModel sm = viewState.ctrl.getBase().getSearchModel();
        final Matches matches = sm.getMatches(page);
        final List<? extends RectF> mm = matches != null ? matches.getMatches() : null;
        if (LengthUtils.isEmpty(mm)) {
            return;
        }

        final AppSettings app = AppSettings.current();
        final Paint p = new Paint();
        final Page cp = sm.getCurrentPage();
        final int cmi = sm.getCurrentMatchIndex();

        for (int i = 0; i < mm.size(); i++) {
            final boolean current = page == cp && i == cmi;
            final RectF link = mm.get(i);
            final RectF rect = page.getPageRegion(pageBounds, new RectF(link));
            rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
            p.setColor(current ? app.currentSearchHighlightColor : app.searchHighlightColor);
            canvas.fillRect(rect, p);
        }
    }

    protected void drawBrightnessFilter(final RectF nodeRect) {
        if (viewState.app.brightnessInNightModeOnly && !viewState.nightMode) {
            return;
        }

        if (viewState.app.brightness >= 100) {
            return;
        }

        final int alpha = 255 - viewState.app.brightness * 255 / 100;
        BRIGHTNESS_FILTER.setColor(Color.BLACK);
        BRIGHTNESS_FILTER.setAlpha(alpha);

        canvas.fillRect(fixedPageBounds, BRIGHTNESS_FILTER);
    }
}
