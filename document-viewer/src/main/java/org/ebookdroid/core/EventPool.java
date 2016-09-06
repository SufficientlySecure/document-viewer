package org.ebookdroid.core;

import android.graphics.PointF;

import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.emdev.ui.gl.GLCanvas;

public class EventPool {

    private static final ConcurrentLinkedQueue<EventGLDraw> glEvents = new ConcurrentLinkedQueue<EventGLDraw>();
    private static final ConcurrentLinkedQueue<EventReset> resetEvents = new ConcurrentLinkedQueue<EventReset>();

    private static final ConcurrentLinkedQueue<EventScrollUp> scrollUpEvents = new ConcurrentLinkedQueue<EventScrollUp>();
    private static final ConcurrentLinkedQueue<EventScrollDown> scrollDownEvents = new ConcurrentLinkedQueue<EventScrollDown>();
    private static final ConcurrentLinkedQueue<EventScrollTo> scrollToEvents = new ConcurrentLinkedQueue<EventScrollTo>();
    private static final ConcurrentLinkedQueue<EventChildLoaded> childLoadedEvents = new ConcurrentLinkedQueue<EventChildLoaded>();

    private static final ConcurrentLinkedQueue<EventZoomIn> zoomInEvents = new ConcurrentLinkedQueue<EventZoomIn>();
    private static final ConcurrentLinkedQueue<EventZoomOut> zoomOutEvents = new ConcurrentLinkedQueue<EventZoomOut>();

    public static EventGLDraw newGLEventDraw(final ViewState viewState, final GLCanvas canvas) {
        EventGLDraw event = glEvents.poll();
        if (event == null) {
            event = new EventGLDraw(glEvents);
        }
        event.init(viewState, canvas);
        return event;
    }

    public static EventReset newEventReset(final AbstractViewController ctrl, final InvalidateSizeReason reason,
            final boolean clearPages) {
        EventReset event = resetEvents.poll();
        if (event == null) {
            event = new EventReset(resetEvents);
        }
        event.init(ctrl, reason, clearPages);
        return event;
    }

    public static AbstractEventScroll<?> newEventScroll(final AbstractViewController ctrl, final int delta) {
        AbstractEventScroll<?> event = null;
        if (delta > 0) {
            event = scrollDownEvents.poll();
            if (event == null) {
                event = new EventScrollDown(scrollDownEvents);
            }
        } else {
            event = scrollUpEvents.poll();
            if (event == null) {
                event = new EventScrollUp(scrollUpEvents);
            }
        }
        event.init(ctrl);
        return event;
    }

    public static EventScrollTo newEventScrollTo(final AbstractViewController ctrl, final int viewIndex) {
        EventScrollTo event = scrollToEvents.poll();
        if (event == null) {
            event = new EventScrollTo(scrollToEvents);
        }
        event.init(ctrl, viewIndex);
        return event;
    }

    public static EventChildLoaded newEventChildLoaded(final AbstractViewController ctrl, final PageTreeNode child) {
        EventChildLoaded event = childLoadedEvents.poll();
        if (event == null) {
            event = new EventChildLoaded(childLoadedEvents);
        }
        event.init(ctrl, child);
        return event;
    }

    public static AbstractEventZoom<?> newEventZoom(final AbstractViewController ctrl, final float oldZoom,
            final float newZoom, final boolean committed, PointF center) {
        AbstractEventZoom<?> event = null;
        if (newZoom > oldZoom) {
            event = zoomInEvents.poll();
            if (event == null) {
                event = new EventZoomIn(zoomInEvents);
            }
        } else {
            event = zoomOutEvents.poll();
            if (event == null) {
                event = new EventZoomOut(zoomOutEvents);
            }
        }
        event.init(ctrl, oldZoom, newZoom, committed, center);
        return event;
    }
}
