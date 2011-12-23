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

package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.presentation.BookNode;
import org.ebookdroid.core.presentation.BookShelfAdapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class BookshelfView extends GridView implements OnItemClickListener {

    private Bitmap mShelfBackground;
    private Bitmap mShelfBackgroundLeft;
    private Bitmap mShelfBackgroundRight;
    private int mShelfWidth;
    private int mShelfHeight;

    private Bitmap mWebLeft;
    private Bitmap mWebRight;
    private Bitmap mPineLeft;
    private Bitmap mPineRight;

    private IBrowserActivity base;
    private BookShelfAdapter adapter;
    String path;

    public BookshelfView(IBrowserActivity base, View shelves, BookShelfAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;
        this.path = adapter.getPath();
        setCacheColorHint(0);
        setSelector(android.R.color.transparent);
        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_SPACING);
        setAdapter(adapter);
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, r.getDisplayMetrics());
        setColumnWidth((int) px);

        init(base.getContext());
        setOnItemClickListener(this);

    }

    private void init(Context context) {
        final Bitmap shelfBackground = BitmapManager.getResource(R.drawable.shelf_panel1);
        if (shelfBackground != null) {
            mShelfWidth = shelfBackground.getWidth();
            mShelfHeight = shelfBackground.getHeight();
            mShelfBackground = shelfBackground;
        }

        mShelfBackgroundLeft = BitmapManager.getResource(R.drawable.shelf_panel1_left);
        mShelfBackgroundRight = BitmapManager.getResource(R.drawable.shelf_panel1_right);

        mWebLeft = BitmapManager.getResource(R.drawable.web_left);
        mWebRight = BitmapManager.getResource(R.drawable.web_right);

        mPineLeft = BitmapManager.getResource(R.drawable.pine_left);
        mPineRight = BitmapManager.getResource(R.drawable.pine_right);

        StateListDrawable drawable = new StateListDrawable();

        SpotlightDrawable start = new SpotlightDrawable(context, this);
        start.disableOffset();
        SpotlightDrawable end = new SpotlightDrawable(context, this, R.drawable.spotlight_blue);
        end.disableOffset();
        TransitionDrawable transition = new TransitionDrawable(start, end);
        drawable.addState(new int[] { android.R.attr.state_pressed }, transition);

        final SpotlightDrawable normal = new SpotlightDrawable(context, this);
        drawable.addState(new int[] {}, normal);

        normal.setParent(drawable);
        transition.setParent(drawable);

        setSelector(drawable);
        setDrawSelectorOnTop(false);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int count = getChildCount();
        int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int shelfWidth = mShelfWidth;
        final int shelfHeight = mShelfHeight;
        final int width = getWidth();
        final int height = getHeight();

        for (int y = top; y < height; y += shelfHeight) {
            for (int x = 0; x < width; x += shelfWidth) {
                canvas.drawBitmap(mShelfBackground, x, y, null);
            }
            canvas.drawBitmap(mShelfBackgroundLeft, 0, y, null);
            canvas.drawBitmap(mShelfBackgroundRight, width - 15, y, null);
        }

        top = (count > 0) ? getChildAt(count - 1).getTop() + shelfHeight : 0;
        drawDecorations(canvas, top, shelfHeight, width);

        super.dispatchDraw(canvas);
    }

    public void drawDecorations(Canvas canvas, int top, final int shelfHeight, final int width) {
        Calendar now = new GregorianCalendar();
        Bitmap left;
        Bitmap right;
        int lOffset;
        int rOffset;

        int date = now.get(GregorianCalendar.DATE);
        int month = now.get(GregorianCalendar.MONTH);

        if ((date >= 20 && month == GregorianCalendar.DECEMBER)
                || (date <= 10 && month == GregorianCalendar.JANUARY)) {
            // New year
            left = mPineLeft;
            right = mPineRight;
            lOffset = 0;
            rOffset = mPineRight.getWidth();
        } else {
            left = mWebLeft;
            right = mWebRight;
            lOffset = 15;
            rOffset = mWebRight.getWidth() + 15;
        }

        canvas.drawBitmap(left, lOffset, top + 1, null);
        canvas.drawBitmap(right, width - rOffset, top + shelfHeight + 1, null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BookNode node = (BookNode) adapter.getItem(position);
        File file = new File(node.getPath());
        if (!file.isDirectory()) {
            base.showDocument(Uri.fromFile(file));
        }
    }
}
