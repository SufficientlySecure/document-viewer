package org.ebookdroid.ui.viewer.adapters;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.ui.viewer.views.BookmarkView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collections;

import org.emdev.ui.actions.IActionController;

public final class BookmarkAdapter extends BaseAdapter {

    public final BookSettings bookSettings;
    final IActionController<?> actions;
    final Page lastPage;

    final Bookmark start;
    final Bookmark end;
    final Context context;

    public BookmarkAdapter(final Context context, final IActionController<?> actions, final Page lastPage,
            final BookSettings bookSettings) {
        this.context = context;
        this.actions = actions;
        this.lastPage = lastPage;
        this.bookSettings = bookSettings;

        this.start = new Bookmark(true, context.getString(R.string.bookmark_start), PageIndex.FIRST, 0, 0);
        this.end = new Bookmark(true, context.getString(R.string.bookmark_end), PageIndex.LAST, 0, 1);

        Collections.sort(bookSettings.bookmarks);
    }

    public void add(final Bookmark... bookmarks) {
        for (final Bookmark bookmark : bookmarks) {
            bookSettings.bookmarks.add(bookmark);
        }
        Collections.sort(bookSettings.bookmarks);
        SettingsManager.storeBookSettings(bookSettings);
        notifyDataSetChanged();
    }

    public void update(Bookmark b) {
        Collections.sort(bookSettings.bookmarks);
        SettingsManager.storeBookSettings(bookSettings);
        notifyDataSetInvalidated();
    }

    public void remove(final Bookmark b) {
        if (!b.service) {
            bookSettings.bookmarks.remove(b);
            SettingsManager.storeBookSettings(bookSettings);
            notifyDataSetChanged();
        }
    }

    public void clear() {
        bookSettings.bookmarks.clear();
        SettingsManager.storeBookSettings(bookSettings);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return 2 + bookSettings.bookmarks.size();
    }

    public boolean hasUserBookmarks() {
        return !bookSettings.bookmarks.isEmpty();
    }

    @Override
    public Object getItem(final int index) {
        return getBookmark(index);
    }

    public Bookmark getBookmark(final int index) {
        if (index == 0) {
            return start;
        }
        if (index - 1 < bookSettings.bookmarks.size()) {
            return bookSettings.bookmarks.get(index - 1);
        }
        return end;
    }

    @Override
    public long getItemId(final int index) {
        return index;
    }

    @Override
    public View getView(final int index, View itemView, final ViewGroup parent) {
        if (itemView == null) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark, parent, false);

            final BookmarkView text = (BookmarkView) itemView.findViewById(R.id.bookmarkName);
            text.setActions(actions);

            final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
            bar.setProgressDrawable(context.getResources().getDrawable(R.drawable.viewer_goto_dlg_progress));
        }

        final Bookmark b = getBookmark(index);

        final TextView text = (TextView) itemView.findViewById(R.id.bookmarkName);
        text.setText(b.name);
        text.setTag(b);

        final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
        bar.setMax(lastPage != null ? lastPage.index.viewIndex : 0);
        bar.setProgress(b.page.viewIndex);

        final View btn = itemView.findViewById(R.id.bookmark_remove);
        if (b.service) {
            btn.setVisibility(View.GONE);
        } else {
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(actions.getOrCreateAction(R.id.actions_showDeleteBookmarkDlg));
            btn.setTag(b);
        }

        return itemView;
    }
}
