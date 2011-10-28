package org.ebookdroid.core.settings.books;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DBSettingsManager extends SQLiteOpenHelper implements IDBAdapter {

    public static final int DB_VERSION = 2;

    private final IDBAdapter adapter;

    public DBSettingsManager(final Context context) {
        super(context, context.getPackageName() + ".settings", null, DB_VERSION);
        adapter = new DBAdapterV2(this);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        adapter.onCreate(db);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Upgrage from v1 to v2
        if (oldVersion == 1 && newVersion == 2) {
            switchAdapter(db, new DBAdapterV1(this), new DBAdapterV2(this));
        }
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Downgrage from v2 to v1
        if (oldVersion == 2 && newVersion == 1) {
            switchAdapter(db, new DBAdapterV2(this), new DBAdapterV1(this));
        }
    }

    public void switchAdapter(final SQLiteDatabase db, final IDBAdapter oldAdapter, final IDBAdapter newAdapter) {
        final Map<String, BookSettings> bookSettings = oldAdapter.getBookSettings(true);
        oldAdapter.deleteAll();
        newAdapter.onCreate(db);
        newAdapter.storeBookSettings(bookSettings.values());
    }

    @Override
    public Map<String, BookSettings> getBookSettings(final boolean all) {
        return adapter.getBookSettings(all);
    }

    @Override
    public BookSettings getBookSettings(final String fileName) {
        return adapter.getBookSettings(fileName);
    }

    @Override
    public boolean storeBookSettings(final BookSettings bs) {
        return adapter.storeBookSettings(bs);
    }

    @Override
    public boolean storeBookSettings(Collection<BookSettings> bs) {
        return adapter.storeBookSettings(bs);
    }

    @Override
    public boolean clearRecent() {
        return adapter.clearRecent();
    }

    @Override
    public boolean deleteAll() {
        return adapter.deleteAll();
    }

    @Override
    public boolean deleteAllBookmarks() {
        return adapter.deleteAllBookmarks();
    }

    @Override
    public boolean updateBookmarks(BookSettings bs) {
        return adapter.updateBookmarks(bs);
    }

    @Override
    public boolean deleteBookmarks(final String book, final List<Bookmark> bookmarks) {
        return adapter.deleteBookmarks(book, bookmarks);
    }

}
