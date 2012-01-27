package org.ebookdroid.core;

import org.ebookdroid.core.bitmaps.Bitmaps;
import org.ebookdroid.core.log.LogContext;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.LinkedList;
import java.util.List;

public class PageTree {

    private static final LogContext LCTX = Page.LCTX;

    static RectF[] splitMasks = {
            // Left Top
            new RectF(0, 0, 0.5f, 0.5f),
            // Right top
            new RectF(0.5f, 0, 1.0f, 0.5f),
            // Left Bottom
            new RectF(0, 0.5f, 0.5f, 1.0f),
            // Right Bottom
            new RectF(0.5f, 0.5f, 1.0f, 1.0f), };

    static final int ZOOM_THRESHOLD = 2;

    final Page owner;
    final PageTreeNode root;

    final SparseArray<PageTreeNode> nodes = new SparseArray<PageTreeNode>();

    public PageTree(final Page owner) {
        this.owner = owner;
        this.root = createRoot();
    }

    public boolean recycleAll(List<Bitmaps> bitmapsToRecycle, boolean includeRoot) {
        boolean res = false;
        int oldCount = bitmapsToRecycle.size();
        for (int index = 0; index < nodes.size(); index++) {
            PageTreeNode node = nodes.valueAt(index);
            if (includeRoot || node.id != 0) {
                res |= node.recycle(bitmapsToRecycle);
            }
        }
        if (nodes.size() > 1) {
            nodes.clear();
            nodes.append(0, root);
        }
        int newCount = bitmapsToRecycle.size();
        if (LCTX.isDebugEnabled()) {
            if (newCount != oldCount) {
                LCTX.d("Recycle children for: " + owner.index + " : " + (newCount - oldCount));
            }
        }
        return res;
    }

    private PageTreeNode createRoot() {
        final PageTreeNode root = new PageTreeNode(owner, ZOOM_THRESHOLD);
        nodes.append(0, root);
        return root;
    }

    public boolean createChildren(final PageTreeNode parent, final float newThreshold) {
        int childId = getFirstChildId(parent.id);
        for (int i = 0; i < splitMasks.length; i++, childId++) {
            PageTreeNode child = new PageTreeNode(owner, parent, childId, splitMasks[i], newThreshold);
            nodes.append(childId, child);
        }
        return true;
    }

    public boolean recycleChildren(final PageTreeNode parent, List<Bitmaps> bitmapsToRecycle) {
        if (parent.id == 0) {
            return recycleAll(bitmapsToRecycle, false);
        } else {
            return recycleChildrenImpl(parent, bitmapsToRecycle);
        }
    }

    private boolean recycleChildrenImpl(final PageTreeNode parent, List<Bitmaps> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        PageTreeNode child = nodes.get(childId);
        if (child == null) {
            return false;
        }
        int oldCount = bitmapsToRecycle.size();

        LinkedList<PageTreeNode> nodesToRemove = new LinkedList<PageTreeNode>();
        nodesToRemove.add(child);
        nodes.remove(childId);

        childId++;
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            child = nodes.get(childId);
            if (child != null) {
                nodesToRemove.add(child);
                nodes.remove(childId);
            }
        }

        while (!nodesToRemove.isEmpty()) {
            child = nodesToRemove.removeFirst();
            child.recycle(bitmapsToRecycle);

            childId = (int) getFirstChildId(child.id);
            for (int end = childId + splitMasks.length; childId < end; childId++) {
                child = nodes.get(childId);
                if (child != null) {
                    nodesToRemove.add(child);
                    nodes.remove(childId);
                }
            }
        }

        int newCount = bitmapsToRecycle.size();
        if (LCTX.isDebugEnabled()) {
            if (newCount != oldCount) {
                LCTX.d("Recycle children for: " + parent.getFullId() + " : " + (newCount - oldCount));
            }
        }

        return true;
    }

    public boolean allChildrenHasBitmap(final ViewState viewState, final PageTreeNode parent, final PagePaint paint) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child == null || !child.hasBitmap()) {
                return false;
            }
        }
        return true;
    }

    public void drawChildren(final Canvas canvas, final ViewState viewState, final RectF pageBounds,
            final PageTreeNode parent, final PagePaint paint) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child != null) {
                child.draw(canvas, viewState, pageBounds, paint);
            }
        }
    }

    public void onPositionChanged(final ViewState viewState, final RectF pageBounds, final PageTreeNode parent,
            final List<PageTreeNode> nodesToDecode, final List<Bitmaps> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child != null) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode, bitmapsToRecycle);
            }
        }
    }

    public void onZoomChanged(final float oldZoom, final ViewState viewState, final boolean committed,
            final RectF pageBounds, final PageTreeNode parent, final List<PageTreeNode> nodesToDecode,
            final List<Bitmaps> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child != null) {
                child.onZoomChanged(oldZoom, viewState, committed, pageBounds, nodesToDecode, bitmapsToRecycle);
            }
        }
    }

    public boolean isHiddenByChildren(final PageTreeNode parent, final ViewState viewState, final RectF pageBounds) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            final PageTreeNode child = nodes.get(childId);
            if (child == null) {
                return false;
            }
            if (!child.hasBitmap() && !child.decodingNow.get()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasChildren(final PageTreeNode parent) {
        int childId = getFirstChildId(parent.id);
        return null != nodes.get(childId);
    }

    private int getFirstChildId(final long parentId) {
        return (int) (parentId * splitMasks.length + 1);
    }

}
