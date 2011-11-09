package org.ebookdroid.core.settings.books;

import org.ebookdroid.core.PageIndex;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

class DBAdapterV2 extends DBAdapterV1 {

    public static final int VERSION = 2;

    public static final String DB_2_BOOK_CREATE = DB_1_BOOK_CREATE;

    public static final String DB_2_BOOK_DROP_ALL = DB_1_BOOK_DROP_ALL;

    public static final String DB_2_BOOKMARK_CREATE = "create table bookmarks ("
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

    public static final String DB_2_BOOKMARK_DROP_ALL = "DROP TABLE bookmarks";

    public static final String DB_2_BOOKMARK_STORE = "INSERT OR REPLACE INTO bookmarks (book, doc_page, view_page, name) VALUES (?, ?, ?, ?)";

    public static final String DB_2_BOOKMARK_GET_ALL = "SELECT doc_page, view_page, name FROM bookmarks WHERE book = ? ORDER BY view_page ASC";

    public static final String DB_2_BOOKMARK_DEL_ALL = "DELETE FROM bookmarks WHERE book=?";

    public DBAdapterV2(final DBSettingsManager manager) {
        super(manager);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DB_2_BOOK_CREATE);
        db.execSQL(DB_2_BOOKMARK_CREATE);
    }

    @Override
    public void onDestroy(final SQLiteDatabase db) {
        db.execSQL(DB_2_BOOK_DROP_ALL);
        db.execSQL(DB_2_BOOKMARK_DROP_ALL);
    }

    @Override
    public boolean deleteAll() {
        try {
            final SQLiteDatabase db = manager.getWritableDatabase();
            try {
                db.beginTransaction();

                db.execSQL(DB_2_BOOK_DROP_ALL, new Object[] {});
                db.execSQL(DB_2_BOOKMARK_DROP_ALL, new Object[] {});

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

                db.execSQL(DB_2_BOOKMARK_DROP_ALL, new Object[] {});
                db.execSQL(DB_2_BOOKMARK_CREATE);

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
                db.execSQL(DB_2_BOOKMARK_DEL_ALL, delArgs);

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
    void loadBookmarks(final BookSettings book, final SQLiteDatabase db) {
        book.bookmarks.clear();
        final Cursor c = db.rawQuery(DB_2_BOOKMARK_GET_ALL, new String[] { book.fileName });
        if (c != null) {
            try {
                for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                    final Bookmark bm = createBookmark(c);
                    book.bookmarks.add(bm);
                }
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("Bookmarks loaded for " + book.fileName + ": " + book.bookmarks.size());
                }
            } finally {
                close(c);
            }
        }
    }

    @Override
    void updateBookmarks(final BookSettings book, final SQLiteDatabase db) {
        final Object[] delArgs = { book.fileName };
        db.execSQL(DB_2_BOOKMARK_DEL_ALL, delArgs);

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
            db.execSQL(DB_2_BOOKMARK_STORE, args);
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Bookmarks stored for " + book.fileName + ": " + book.bookmarks.size());
        }
    }

    Bookmark createBookmark(final Cursor c) {
        int index = 0;
        return new Bookmark(new PageIndex(c.getInt(index++), c.getInt(index++)), c.getString(index++));
    }

}
