/*
 * Copyright (C) 2008 The Android Open Source Project, Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emdev.ui.drawable;

import android.graphics.*;
import android.graphics.drawable.Drawable;

class LayerDrawable extends Drawable implements Drawable.Callback {
    LayerState mLayerState;

    private int[] mPaddingL;
    private int[] mPaddingT;
    private int[] mPaddingR;
    private int[] mPaddingB;

    private final Rect mTmpRect = new Rect();
    private Drawable mParent;
    private boolean mBlockSetBounds;

    LayerDrawable(Drawable... layers) {
        this(null, layers);
    }

    LayerDrawable(LayerState state, Drawable... layers) {
        this(state);
        int length = layers.length;
        Rec[] r = new Rec[length];

        final LayerState layerState = mLayerState;
        for (int i = 0; i < length; i++) {
            r[i] = new Rec();
            r[i].mDrawable = layers[i];
            layers[i].setCallback(this);
            layerState.mChildrenChangingConfigurations |= layers[i].getChangingConfigurations();
        }
        layerState.mNum = length;
        layerState.mArray = r;

        ensurePadding();
    }

    LayerDrawable(LayerState state) {
        LayerState as = createConstantState(state);
        mLayerState = as;
        if (as.mNum > 0) {
            ensurePadding();
        }
    }

    LayerState createConstantState(LayerState state) {
        return new LayerState(state, this);
    }

    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public void draw(Canvas canvas) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.draw(canvas);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations()
                | mLayerState.mChangingConfigurations
                | mLayerState.mChildrenChangingConfigurations;
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            reapplyPadding(i, array[i]);
            padding.left = Math.max(padding.left, mPaddingL[i]);
            padding.top = Math.max(padding.top, mPaddingT[i]);
            padding.right = Math.max(padding.right, mPaddingR[i]);
            padding.bottom = Math.max(padding.bottom, mPaddingB[i]);
        }
        return true;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setVisible(visible, restart);
        }
        return changed;
    }

    @Override
    public void setDither(boolean dither) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setDither(dither);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setColorFilter(cf);
        }
    }

    @Override
    public int getOpacity() {
        return mLayerState.getOpacity();
    }

    @Override
    public boolean isStateful() {
        return mLayerState.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        boolean paddingChanged = false;
        boolean changed = false;
        for (int i = 0; i < N; i++) {
            final Rec r = array[i];
            if (r.mDrawable.setState(state)) {
                changed = true;
            }
            if (reapplyPadding(i, r)) {
                paddingChanged = true;
            }
        }
        if (paddingChanged) {
            onBoundsChange(getBounds());
        }
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        boolean paddingChanged = false;
        boolean changed = false;
        for (int i = 0; i < N; i++) {
            final Rec r = array[i];
            if (r.mDrawable.setLevel(level)) {
                changed = true;
            }
            if (reapplyPadding(i, r)) {
                paddingChanged = true;
            }
        }
        if (paddingChanged) {
            onBoundsChange(getBounds());
        }
        return changed;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        if (mBlockSetBounds) return;

        final int width = mLayerState.mArray[0].mDrawable.getIntrinsicWidth();
        left -= (width - (right - left)) / 2.0f;
        right = left + width;
        bottom = top + getIntrinsicHeight();
        super.setBounds(left, top, right, bottom);

        if (mParent != null) {
            mBlockSetBounds = true;
            mParent.setBounds(left, top, right, bottom);
            mBlockSetBounds = false;
        }
    }

    public void setParent(Drawable drawable) {
        mParent = drawable;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        int padL = 0, padT = 0, padR = 0, padB = 0;
        for (int i = 0; i < N; i++) {
            final Rec r = array[i];
            r.mDrawable.setBounds(bounds.left + r.mInsetL + padL,
                    bounds.top + r.mInsetT + padT,
                    bounds.right - r.mInsetR - padR,
                    bounds.bottom - r.mInsetB - padB);
            padL += mPaddingL[i];
            padR += mPaddingR[i];
            padT += mPaddingT[i];
            padB += mPaddingB[i];
        }
    }

    @Override
    public int getIntrinsicWidth() {
        int width = -1;
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        int padL = 0, padR = 0;
        for (int i = 0; i < N; i++) {
            final Rec r = array[i];
            int w = r.mDrawable.getIntrinsicWidth()
                    + r.mInsetL + r.mInsetR + padL + padR;
            if (w > width) {
                width = w;
            }
            padL += mPaddingL[i];
            padR += mPaddingR[i];
        }
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        int height = -1;
        final Rec[] array = mLayerState.mArray;
        final int N = mLayerState.mNum;
        int padT = 0, padB = 0;
        for (int i = 0; i < N; i++) {
            final Rec r = array[i];
            int h = r.mDrawable.getIntrinsicHeight() + r.mInsetT + r.mInsetB + +padT + padB;
            if (h > height) {
                height = h;
            }
            padT += mPaddingT[i];
            padB += mPaddingB[i];
        }
        return height;
    }

    private boolean reapplyPadding(int i, Rec r) {
        final Rect rect = mTmpRect;
        r.mDrawable.getPadding(rect);
        if (rect.left != mPaddingL[i] || rect.top != mPaddingT[i] ||
                rect.right != mPaddingR[i] || rect.bottom != mPaddingB[i]) {
            mPaddingL[i] = rect.left;
            mPaddingT[i] = rect.top;
            mPaddingR[i] = rect.right;
            mPaddingB[i] = rect.bottom;
            return true;
        }
        return false;
    }

    private void ensurePadding() {
        final int N = mLayerState.mNum;
        if (mPaddingL != null && mPaddingL.length >= N) {
            return;
        }
        mPaddingL = new int[N];
        mPaddingT = new int[N];
        mPaddingR = new int[N];
        mPaddingB = new int[N];
    }

    @Override
    public ConstantState getConstantState() {
        if (mLayerState.canConstantState()) {
            mLayerState.mChangingConfigurations = super.getChangingConfigurations();
            return mLayerState;
        }
        return null;
    }

    static class Rec {
        public Drawable mDrawable;
        public int mInsetL, mInsetT, mInsetR, mInsetB;
        public int mId;
    }

    static class LayerState extends ConstantState {
        int mNum;
        Rec[] mArray;

        int mChangingConfigurations;
        int mChildrenChangingConfigurations;

        private boolean mHaveOpacity = false;
        private int mOpacity;

        private boolean mHaveStateful = false;
        private boolean mStateful;

        private boolean mCheckedConstantState;
        private boolean mCanConstantState;

        LayerState(LayerState orig, LayerDrawable owner) {
            if (orig != null) {
                final Rec[] origRec = orig.mArray;
                final int N = orig.mNum;

                mNum = N;
                mArray = new Rec[N];

                mChangingConfigurations = orig.mChangingConfigurations;
                mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;

                for (int i = 0; i < N; i++) {
                    final Rec r = mArray[i] = new Rec();
                    final Rec or = origRec[i];
                    r.mDrawable = or.mDrawable.getConstantState().newDrawable();
                    r.mDrawable.setCallback(owner);
                    r.mInsetL = or.mInsetL;
                    r.mInsetT = or.mInsetT;
                    r.mInsetR = or.mInsetR;
                    r.mInsetB = or.mInsetB;
                    r.mId = or.mId;
                }

                mHaveOpacity = orig.mHaveOpacity;
                mOpacity = orig.mOpacity;
                mHaveStateful = orig.mHaveStateful;
                mStateful = orig.mStateful;
                mCheckedConstantState = mCanConstantState = true;
            } else {
                mNum = 0;
                mArray = null;
            }
        }

        @Override
        public Drawable newDrawable() {
            return new LayerDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public final int getOpacity() {
            if (mHaveOpacity) {
                return mOpacity;
            }

            final int N = mNum;
            final Rec[] array = mArray;
            int op = N > 0 ? array[0].mDrawable.getOpacity() : PixelFormat.TRANSPARENT;
            for (int i = 1; i < N; i++) {
                op = Drawable.resolveOpacity(op, array[i].mDrawable.getOpacity());
            }
            mOpacity = op;
            mHaveOpacity = true;
            return op;
        }

        public final boolean isStateful() {
            if (mHaveStateful) {
                return mStateful;
            }

            boolean stateful = false;
            final int N = mNum;
            final Rec[] array = mArray;
            for (int i = 0; i < N; i++) {
                if (array[i].mDrawable.isStateful()) {
                    stateful = true;
                    break;
                }
            }

            mStateful = stateful;
            mHaveStateful = true;
            return stateful;
        }

        public synchronized boolean canConstantState() {
            final Rec[] array = mArray;
            if (!mCheckedConstantState && array != null) {
                mCanConstantState = true;
                final int N = mNum;
                for (int i = 0; i < N; i++) {
                    if (array[i].mDrawable.getConstantState() == null) {
                        mCanConstantState = false;
                        break;
                    }
                }
                mCheckedConstantState = true;
            }

            return mCanConstantState;
        }
    }
}

