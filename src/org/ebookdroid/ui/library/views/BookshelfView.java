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

package org.ebookdroid.ui.library.views;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BookShelfAdapter;

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

import org.emdev.ui.drawable.SpotlightDrawable;
import org.emdev.ui.drawable.TransitionDrawable;
import org.emdev.utils.LayoutUtils;

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

    private final IBrowserActivity base;
    private final BookShelfAdapter adapter;
    final String path;

    public BookshelfView(final IBrowserActivity base, final View shelves, final BookShelfAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;
        this.path = adapter != null ? adapter.getPath() : "";
        setCacheColorHint(0);
        setSelector(android.R.color.transparent);
        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_SPACING);
        if (adapter != null) {
            setAdapter(adapter);
        }
        LayoutUtils.fillInParent(shelves, this);
        final Resources r = getResources();
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, r.getDisplayMetrics());
        setColumnWidth((int) px);

        init(base.getContext());
        setOnItemClickListener(this);

        base.getActivity().registerForContextMenu(this);
    }

    private void init(final Context context) {
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

        final StateListDrawable drawable = new StateListDrawable();

        final SpotlightDrawable start = new SpotlightDrawable(context, this);
        start.disableOffset();
        final SpotlightDrawable end = new SpotlightDrawable(context, this, R.drawable.spotlight_blue);
        end.disableOffset();
        final TransitionDrawable transition = new TransitionDrawable(start, end);
        drawable.addState(new int[] { android.R.attr.state_pressed }, transition);

        final SpotlightDrawable normal = new SpotlightDrawable(context, this);
        drawable.addState(new int[] {}, normal);

        normal.setParent(drawable);
        transition.setParent(drawable);

        setSelector(drawable);
        setDrawSelectorOnTop(false);
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
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

    public void drawDecorations(final Canvas canvas, final int top, final int shelfHeight, final int width) {
        final Calendar now = new GregorianCalendar();
        Bitmap left;
        Bitmap right;
        int lOffset;
        int rOffset;

        final int date = now.get(Calendar.DATE);
        final int month = now.get(Calendar.MONTH);

        if ((date >= 23 && month == Calendar.DECEMBER) || (date <= 13 && month == Calendar.JANUARY)) {
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
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final BookNode node = adapter != null ? (BookNode) adapter.getItem(position) : null;
        if (node != null) {
            final File file = new File(node.path);
            if (!file.isDirectory()) {
                base.showDocument(Uri.fromFile(file));
            }
        }
    }
}
