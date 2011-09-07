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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;

public class BookshelfView extends GridView implements OnItemClickListener {

    private Bitmap mShelfBackground;
    private int mShelfWidth;
    private int mShelfHeight;

    private Bitmap mWebLeft;
    private Bitmap mWebRight;
    private int mWebRightWidth;

    private IBrowserActivity base;
    private BooksAdapter adapter;
    public BookshelfView(IBrowserActivity base, BooksAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;
        init(base.getContext());
        setColumnWidth(160);
        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_COLUMN_WIDTH);
        setAdapter(adapter);
        setOnItemClickListener(this);
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }

    private void init(Context context) {
        final Resources resources = getResources();
        final Bitmap shelfBackground = BitmapFactory.decodeResource(resources, R.drawable.shelf_panel);
        if (shelfBackground != null) {
            mShelfWidth = shelfBackground.getWidth();
            mShelfHeight = shelfBackground.getHeight();
            mShelfBackground = shelfBackground;
        }

        mWebLeft = BitmapFactory.decodeResource(resources, R.drawable.web_left);

        final Bitmap webRight = BitmapFactory.decodeResource(resources, R.drawable.web_right);
        mWebRightWidth = webRight.getWidth();
        mWebRight = webRight;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int count = getChildCount();
        int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int shelfWidth = mShelfWidth;
        final int shelfHeight = mShelfHeight;
        final int width = getWidth();
        final int height = getHeight();
        final Bitmap background = mShelfBackground;

        for (int x = 0; x < width; x += shelfWidth) {
            for (int y = top; y < height; y += shelfHeight) {
                canvas.drawBitmap(background, x, y, null);
            }
        }

        top = (count > 0) ? getChildAt(count - 1).getTop() + shelfHeight : 0;
        canvas.drawBitmap(mWebLeft, 0.0f, top + 1, null);
        canvas.drawBitmap(mWebRight, width - mWebRightWidth, top + shelfHeight + 1, null);

        super.dispatchDraw(canvas);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BooksAdapter.Node node = (BooksAdapter.Node) adapter.getItem(position);
        File file = new File(node.getPath());
        if (!file.isDirectory()) {
            base.showDocument(Uri.fromFile(file));
        }
    }
    
}
