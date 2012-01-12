package org.ebookdroid.core;

import org.ebookdroid.core.bitmaps.BitmapRef;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.List;

public class PageTree {

    private static RectF[] splitMasks = {
            // Left Top
            new RectF(0, 0, 0.5f, 0.5f),
            // Right top
            new RectF(0.5f, 0, 1.0f, 0.5f),
            // Left Bottom
            new RectF(0, 0.5f, 0.5f, 1.0f),
            // Right Bottom
            new RectF(0.5f, 0.5f, 1.0f, 1.0f), };

    final Page owner;
    final PageTreeNode root;

    final SparseArray<PageTreeNode> nodes = new SparseArray<PageTreeNode>();

    public PageTree(final Page owner) {
        this.owner = owner;
        this.root = createRoot();
    }

    public void recycle(List<BitmapRef> bitmapsToRecycle) {
        for (int index = 0; index < nodes.size(); index++) {
            PageTreeNode node = nodes.valueAt(index);
            node.recycle(bitmapsToRecycle, false);
        }
        nodes.clear();
    }

    private PageTreeNode createRoot() {
        final PageTreeNode root = new PageTreeNode(owner, null, 0, owner.type.getInitialRect(), 2);
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

    public boolean recycleChildren(final PageTreeNode parent, List<BitmapRef> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            final PageTreeNode child = nodes.get(childId);
            if (child != null) {
                nodes.remove(childId);
                child.recycle(bitmapsToRecycle, true);
            } else {
                break;
            }
        }
        return false;
    }

    public boolean allChildrenHasBitmap(final ViewState viewState, final PageTreeNode parent, final PagePaint paint) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child == null || !child.hasBitmap(viewState, paint)) {
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
            } else {
                break;
            }
        }
    }

    public void onPositionChanged(final ViewState viewState, final RectF pageBounds, final PageTreeNode parent,
            final List<PageTreeNode> nodesToDecode, final List<BitmapRef> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child != null) {
                child.onPositionChanged(viewState, pageBounds, nodesToDecode, bitmapsToRecycle);
            } else {
                break;
            }
        }
    }

    public void onZoomChanged(final float oldZoom, final ViewState viewState, final boolean committed,
            final RectF pageBounds, final PageTreeNode parent, final List<PageTreeNode> nodesToDecode,
            final List<BitmapRef> bitmapsToRecycle) {
        int childId = (int) getFirstChildId(parent.id);
        for (int end = childId + splitMasks.length; childId < end; childId++) {
            PageTreeNode child = nodes.get(childId);
            if (child != null) {
                child.onZoomChanged(oldZoom, viewState, committed, pageBounds, nodesToDecode, bitmapsToRecycle);
            } else {
                break;
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
            if (!viewState.isNodeVisible(child, pageBounds)) {
                return false;
            }
            if (!child.hasBitmap() && !child.decodingNow.get() && isHiddenByChildren(child, viewState, pageBounds)) {
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
        return (int)(parentId * splitMasks.length + 1);
    }

}
