/*
 * Copyright (C) 2008 Romain Guy
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

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

public class SpotlightDrawable extends Drawable {
    private final Bitmap mBitmap;
    private final Paint mPaint;
    private final ViewGroup mView;

    private boolean mOffsetDisabled;
    private boolean mBlockSetBounds;
    private Drawable mParent;

    public SpotlightDrawable(Context context, ViewGroup view) {
        this(context, view, R.drawable.components_spotlight);
    }

    public SpotlightDrawable(Context context, ViewGroup view, int resource) {
        mView = view;
        mBitmap = BitmapManager.getResource(resource);

        mPaint = new Paint();
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }

    public void draw(Canvas canvas) {
        if (mView.hasWindowFocus()) {
            final Rect bounds = getBounds();
            canvas.save();
            canvas.drawBitmap(mBitmap, bounds.left, bounds.top, mPaint);
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        if (mBlockSetBounds) return;

        if (!mOffsetDisabled) {
            final int width = getIntrinsicWidth();
            final View view = mView.getChildAt(0);
            if (view != null) left -= (width - view.getWidth()) / 2.0f;
            right = left + width;
            bottom = top + getIntrinsicHeight();
        } else {
            right = left + getIntrinsicWidth();
            bottom = top + getIntrinsicHeight();
        }

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
    protected boolean onStateChange(int[] state) {
        invalidateSelf();
        return super.onStateChange(state);
    }

    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    public void disableOffset() {
        mOffsetDisabled = true;
    }
}
