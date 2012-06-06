package org.ebookdroid.common.settings.books;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DBSettingsManager extends SQLiteOpenHelper implements IDBAdapter {

    public static final int DB_VERSION = 6;

    private final IDBAdapter adapter;

    private SQLiteDatabase upgragingInstance;

    public DBSettingsManager(final Context context) {
        super(context, context.getPackageName() + ".settings", null, DB_VERSION);
        adapter = createAdapter(DB_VERSION);
        try {
            final SQLiteDatabase db = getWritableDatabase();
            db.close();
        } catch (final Exception ex) {
            LCTX.e("Unexpected DB error: ", ex);
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        adapter.onCreate(db);
    }

    @Override
    public void onDestroy(final SQLiteDatabase db) {
        adapter.onDestroy(db);
    }

    protected IDBAdapter createAdapter(final int version) {
        switch (version) {
            case DBAdapterV1.VERSION:
                return new DBAdapterV1(this);
            case DBAdapterV2.VERSION:
                return new DBAdapterV2(this);
            case DBAdapterV3.VERSION:
                return new DBAdapterV3(this);
            case DBAdapterV4.VERSION:
                return new DBAdapterV4(this);
            case DBAdapterV5.VERSION:
                return new DBAdapterV5(this);
            case DBAdapterV6.VERSION:
            default:
                return new DBAdapterV6(this);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        upgragingInstance = db;
        LCTX.i("Upgrading from version " + oldVersion + " to version " + newVersion);
        try {
            final IDBAdapter oldAdapter = createAdapter(oldVersion);
            final IDBAdapter newAdapter = createAdapter(newVersion);
            switchAdapter(db, oldAdapter, newAdapter);
        } finally {
            upgragingInstance = null;
            LCTX.i("Upgrade finished");
        }
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        upgragingInstance = db;
        LCTX.i("Downgrading from version " + oldVersion + " to version " + newVersion);
        try {
            final IDBAdapter oldAdapter = createAdapter(oldVersion);
            final IDBAdapter newAdapter = createAdapter(newVersion);
            switchAdapter(db, newAdapter, oldAdapter);
        } finally {
            upgragingInstance = null;
            LCTX.i("Downgrade finished");
        }
    }

    public void switchAdapter(final SQLiteDatabase db, final IDBAdapter oldAdapter, final IDBAdapter newAdapter) {
        final Map<String, BookSettings> bookSettings = oldAdapter.getBookSettings(true);
        oldAdapter.deleteAll();
        oldAdapter.onDestroy(db);
        newAdapter.onCreate(db);
        newAdapter.storeBookSettings(bookSettings.values());
    }

    /**
     * {@inheritDoc}
     *
     * @see android.database.sqlite.SQLiteOpenHelper#getWritableDatabase()
     */
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        return upgragingInstance != null ? upgragingInstance : super.getWritableDatabase();
    }

    /**
     * {@inheritDoc}
     *
     * @see android.database.sqlite.SQLiteOpenHelper#getReadableDatabase()
     */
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        return upgragingInstance != null ? upgragingInstance : super.getReadableDatabase();
    }

    synchronized void closeDatabase(final SQLiteDatabase db) {
        if (db != upgragingInstance) {
            try {
                db.close();
            } catch (final Exception ex) {
            }
        }
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
        return bs.persistent ? adapter.storeBookSettings(bs) : false;
    }

    @Override
    public boolean storeBookSettings(final Collection<BookSettings> bs) {
        return adapter.storeBookSettings(bs);
    }

    @Override
    public boolean clearRecent() {
        return adapter.clearRecent();
    }

    @Override
    public boolean removeBookFromRecents(final BookSettings bs) {
        return adapter.removeBookFromRecents(bs);
    }

    @Override
    public void delete(final BookSettings current) {
        adapter.delete(current);
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
    public boolean updateBookmarks(final BookSettings bs) {
        return adapter.updateBookmarks(bs);
    }

    @Override
    public boolean deleteBookmarks(final String book, final List<Bookmark> bookmarks) {
        return adapter.deleteBookmarks(book, bookmarks);
    }

}
