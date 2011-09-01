package org.ebookdroid.core;

import android.graphics.RectF;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private final PageTreeNode[] EMPTY_CHILDREN = {};

    final Page owner;
    final PageTreeNode root;

    final Map<Long, PageTreeNode> nodes = new HashMap<Long, PageTreeNode>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public PageTree(final Page owner) {
        this.owner = owner;
        this.root = createRoot();
    }

    public void recycle() {
        lock.writeLock().lock();
        try {
            root.recycle();
            nodes.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private PageTreeNode createRoot() {
        final PageTreeNode root = new PageTreeNode(owner, null, 0, owner.pageType.getInitialRect(), 2);
        nodes.put(root.id, root);
        return root;
    }

    public PageTreeNode[] createChildren(final PageTreeNode parent, final float newThreshold) {
        lock.writeLock().lock();
        try {
            final PageTreeNode[] children = new PageTreeNode[splitMasks.length];
            for (int i = 0; i < splitMasks.length; i++) {
                children[i] = new PageTreeNode(owner, parent, getChildId(parent.id, i), splitMasks[i], newThreshold);
                nodes.put(children[i].id, children[i]);
            }
            return children;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean recycleChildren(final PageTreeNode parent) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < splitMasks.length; i++) {
                final PageTreeNode child = nodes.remove(getChildId(parent.id, i));
                if (child != null) {
                    child.recycle();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    public PageTreeNode[] getChildren(final PageTreeNode parent) {
        lock.readLock().lock();
        try {
            final PageTreeNode node1 = nodes.get(getChildId(parent.id, 0));
            if (node1 == null) {
                return EMPTY_CHILDREN;
            }
            final PageTreeNode[] res = new PageTreeNode[splitMasks.length];
            res[0] = node1;
            for (int i = 1; i < splitMasks.length; i++) {
                res[i] = nodes.get(getChildId(parent.id, i));
            }
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isHiddenByChildren(final PageTreeNode parent, final ViewState viewState, final RectF pageBounds) {
        lock.readLock().lock();
        try {
            for (int i = 0; i < splitMasks.length; i++) {
                final PageTreeNode child = nodes.get(getChildId(parent.id, i));
                if (child == null) {
                    return false;
                }
                if (!viewState.isNodeVisible(child, pageBounds)) {
                    return false;
                }
                if (child.getBitmap() == null && !child.decodingNow.get()
                        && isHiddenByChildren(child, viewState, pageBounds)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasChildren(final PageTreeNode parent) {
        lock.readLock().lock();
        try {
            return null != nodes.get(getChildId(parent.id, 0));
        } finally {
            lock.readLock().unlock();
        }
    }

    private long getChildId(final long parentId, final int seq) {
        return parentId * splitMasks.length + seq + 1;
    }
}
