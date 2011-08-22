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

    private PageTreeNode[] EMPTY_CHILDREN = {};

    final Page owner;
    final PageTreeNode root;

    final Map<Long, PageTreeNode> nodes = new HashMap<Long, PageTreeNode>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public PageTree(Page owner) {
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
        PageTreeNode root = new PageTreeNode(owner, null, 0, owner.pageType.getInitialRect(), 2);
        nodes.put(root.id, root);
        return root;
    }

    public PageTreeNode[] createChildren(PageTreeNode parent, final float newThreshold) {
        lock.writeLock().lock();
        try {
            PageTreeNode[] children = new PageTreeNode[splitMasks.length];
            for (int i = 0; i < splitMasks.length; i++) {
                children[i] = new PageTreeNode(owner, parent, getChildId(parent.id, i), splitMasks[i],
                        newThreshold);
                nodes.put(children[i].id, children[i]);
            }
            return children;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean recycleChildren(PageTreeNode parent) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < splitMasks.length; i++) {
                PageTreeNode child = nodes.remove(getChildId(parent.id, i));
                if (child != null) {
                    child.recycle();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    public PageTreeNode[] getChildren(PageTreeNode parent) {
        lock.readLock().lock();
        try {
            PageTreeNode node1 = nodes.get(getChildId(parent.id, 0));
            if (node1 == null) {
                return EMPTY_CHILDREN;
            }
            PageTreeNode[] res = new PageTreeNode[splitMasks.length];
            res[0] = node1;
            for (int i = 1; i < splitMasks.length; i++) {
                res[i] = nodes.get(getChildId(parent.id, i));
            }
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isHiddenByChildren(PageTreeNode parent, RectF viewRect, RectF pageBounds) {
        lock.readLock().lock();
        try {
            for (int i = 0; i < splitMasks.length; i++) {
                PageTreeNode child = nodes.get(getChildId(parent.id, i));
                if (child == null) {
                    return false;
                }
                if (!child.isVisible(viewRect, pageBounds)) {
                    return false;
                }
                if (child.getBitmap() == null && !child.decodingNow.get() && isHiddenByChildren(child, viewRect, pageBounds)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasChildren(PageTreeNode parent) {
        lock.readLock().lock();
        try {
            return null != nodes.get(getChildId(parent.id, 0));
        } finally {
            lock.readLock().unlock();
        }
    }

    private long getChildId(long parentId, int seq) {
        return parentId * splitMasks.length + seq + 1;
    }
}
