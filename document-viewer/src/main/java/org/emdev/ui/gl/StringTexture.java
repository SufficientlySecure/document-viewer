/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.emdev.ui.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint.FontMetricsInt;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.FloatMath;

import org.emdev.utils.CompareUtils;

// StringTexture is a texture shows the content of a specified String.
//
// To create a StringTexture, use the newInstance() method and specify
// the String, the font size, and the color.
public class StringTexture extends CanvasTexture {

    private TextPaint mPaint;
    private FontMetricsInt mMetrics;
    private String mText;
    private int mTextWidth;
    private int mTextHeight;
    private volatile boolean recycled;

    public StringTexture(final int maxWidth, final int maxHeight) {
        super(Math.max(1, maxWidth), Math.max(1, maxHeight));
    }

    public void setText(final String text, final TextPaint paint) {
        final String oldText = mText;
        final int oldTextHeight = mTextHeight;

        mPaint = paint;
        mMetrics = paint.getFontMetricsInt();
        mText = text;
        mTextWidth = (int) Math.ceil(mPaint.measureText(mText));
        mTextHeight = mMetrics.bottom - mMetrics.top;

        if (mTextWidth > mCanvasWidth) {
            mText = TextUtils.ellipsize(mText, mPaint, mCanvasWidth, TextUtils.TruncateAt.END).toString();
            mTextWidth = (int) Math.ceil(mPaint.measureText(mText));
        }
        if (!CompareUtils.equals(mText, oldText) || mTextHeight != oldTextHeight) {
            if (mBitmap != null) {
                mBitmap.eraseColor(0);
            }
            mContentValid = false;
            mWidth = UNSPECIFIED;
            mHeight = UNSPECIFIED;
        }
    }

    public String getText() {
        return mText;
    }

    public int getTextWidth() {
        return mTextWidth;
    }

    public int getTextHeight() {
        return mTextHeight;
    }

    @Override
    public void recycle() {
        recycled = true;
        super.recycle();
    }

    @Override
    protected void freeBitmap() {
        if (recycled) {
            super.freeBitmap();
        }
    }

    @Override
    protected void onDraw(final Canvas canvas, final Bitmap backing) {
        if (mText != null && mPaint != null && mMetrics != null) {
            canvas.translate(0, -mMetrics.ascent);
            canvas.drawText(mText, 0, 0, mPaint);
        }
    }
}
