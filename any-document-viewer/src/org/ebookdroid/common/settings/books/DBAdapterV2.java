package org.ebookdroid.common.settings.books;

import org.ebookdroid.core.PageIndex;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import org.emdev.utils.LengthUtils;

class DBAdapterV2 extends DBAdapterV1 {

    public static final int VERSION = 2;

    public static final String DB_BOOKMARK_CREATE = "create table bookmarks ("
    // Book file path
            + "book varchar(1024) not null, "
            // Current document page
            + "doc_page integer not null, "
            // Current view page - dependent on view mode
            + "view_page integer not null, "
            // Bookmark name
            + "name varchar(1024) not null"
            // ...
            + ");";

    public static final String DB_BOOKMARK_DROP = "DROP TABLE bookmarks";

    public static final String DB_BOOKMARK_STORE = "INSERT OR REPLACE INTO bookmarks (book, doc_page, view_page, name) VALUES (?, ?, ?, ?)";

    public static final String DB_BOOKMARK_GET_ALL = "SELECT doc_page, view_page, name FROM bookmarks WHERE book = ? ORDER BY view_page ASC";

    public static final String DB_BOOKMARK_DEL_ALL = "DELETE FROM bookmarks WHERE book=?";

    public static final String DB_BOOKMARKS_DEL = "DELETE FROM bookmarks";

    public DBAdapterV2(final DBSettingsManager manager) {
        super(manager);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DB_BOOK_CREATE);
        db.execSQL(DB_BOOKMARK_CREATE);
    }

    @Override
    public void onDestroy(final SQLiteDatabase db) {
        db.execSQL(DB_BOOK_DROP);
        db.execSQL(DB_BOOKMARK_DROP);
    }

    @Override
    public boolean deleteAll() {
        try {
            final SQLiteDatabase db = manager.getWritableDatabase();
            try {
                db.beginTransaction();

                db.execSQL(DB_BOOK_DROP, new Object[] {});
                db.execSQL(DB_BOOKMARK_DROP, new Object[] {});

                onCreate(db);

                db.setTransactionSuccessful();

                return true;
            } finally {
                endTransaction(db);
            }
        } catch (final Throwable th) {
            LCTX.e("Update book settings failed: ", th);
        }
        return false;
    }

    @Override
    public boolean deleteAllBookmarks() {
        try {
            final SQLiteDatabase db = manager.getWritableDatabase();
            try {
                db.beginTransaction();

                db.execSQL(DB_BOOKMARKS_DEL, new Object[] {});

                db.setTransactionSuccessful();

                return true;
            } finally {
                endTransaction(db);
            }
        } catch (final Throwable th) {
            LCTX.e("Update book settings failed: ", th);
        }
        return false;
    }

    @Override
    public boolean deleteBookmarks(final String book, final List<Bookmark> bookmarks) {
        try {
            final SQLiteDatabase db = manager.getWritableDatabase();
            try {
                db.beginTransaction();

                final Object[] delArgs = { book };
                db.execSQL(DB_BOOKMARK_DEL_ALL, delArgs);

                db.setTransactionSuccessful();

                return true;
            } finally {
                endTransaction(db);
            }
        } catch (final Throwable th) {
            LCTX.e("Deleting bookmarks failed: ", th);
        }
        return false;
    }

    @Override
    protected void loadBookmarks(final BookSettings book, final SQLiteDatabase db) {
        loadBookmarks(book, db, DB_BOOKMARK_GET_ALL);
    }

    protected final void loadBookmarks(final BookSettings book, final SQLiteDatabase db, final String query) {
        book.bookmarks.clear();
        final Cursor c = db.rawQuery(query, new String[] { LengthUtils.safeString(book.fileName) });
        if (c != null) {
            try {
                for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                    final Bookmark bm = createBookmark(c);
                    book.bookmarks.add(bm);
                }
                // if (LCTX.isDebugEnabled()) {
                // LCTX.d("Bookmarks loaded for " + book.fileName + ": " + book.bookmarks.size());
                // }
            } finally {
                close(c);
            }
        }
    }

    @Override
    protected void updateBookmarks(final BookSettings book, final SQLiteDatabase db) {
        final Object[] delArgs = { book.fileName };
        db.execSQL(DB_BOOKMARK_DEL_ALL, delArgs);

        for (final Bookmark bs : book.bookmarks) {
            final Object[] args = new Object[] {
                    // Book name
                    book.fileName,
                    // Bookmark document page
                    bs.page.docIndex,
                    // Bookmark view page
                    bs.page.viewIndex,
                    // Bookmark name
                    bs.name, };
            db.execSQL(DB_BOOKMARK_STORE, args);
        }

        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Bookmarks stored for " + book.fileName + ": " + book.bookmarks.size());
        // }
    }

    protected Bookmark createBookmark(final Cursor c) {
        int index = 0;
        final int docIndex = c.getInt(index++);
        final int viewIndex = c.getInt(index++);
        final String name = c.getString(index++);
        return new Bookmark(name, new PageIndex(docIndex, viewIndex), 0, 0);
    }
}
