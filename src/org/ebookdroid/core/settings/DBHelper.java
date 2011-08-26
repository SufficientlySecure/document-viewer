package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.log.LogContext;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    private static final LogContext LCTX = LogContext.ROOT.lctx("DBHelper");

    private static final int DB_VERSION = 2;

    private static final String DB_1_BOOK_CREATE = "create table book_settings ("
    // Book file path
            + "book varchar(1024) primary key, "
            // Last update time
            + "last_updated integer not null, "
            // Current document page
            + "doc_page integer not null, "
            // Current view page - dependent on view mode
            + "view_page integer not null, "
            // Page zoom
            + "zoom integer not null, "
            // Single page mode on/off
            + "single_page integer not null, "
            // Page align
            + "page_align integer not null, "
            // Page animation type
            + "page_animation integer not null, "
            // Split pages on/off
            + "split_pages integer not null" +
            // ...
            ");";

    private static final String DB_1_BOOK_GET_ALL = "SELECT book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages FROM book_settings ORDER BY last_updated DESC";

    private static final String DB_1_BOOK_GET_ONE = "SELECT book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages FROM book_settings WHERE book=?";

    private static final String DB_1_BOOK_STORE = "INSERT OR REPLACE INTO book_settings (book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DB_1_BOOK_DROP_ALL = "DROP TABLE IF EXISTS book_settings ";

    private static final String DB_2_BOOK_CREATE = DB_1_BOOK_CREATE;

    private static final String DB_2_BOOK_DROP_ALL = DB_1_BOOK_DROP_ALL;

    private static final String DB_2_BOOKMARK_CREATE = "create table bookmarks ("
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

    private static final String DB_2_BOOKMARK_DROP_ALL = "DROP TABLE bookmarks";

    private static final String DB_2_BOOKMARK_STORE = "INSERT OR REPLACE INTO bookmarks (book, doc_page, view_page, name) VALUES (?, ?, ?, ?)";

    private static final String DB_2_BOOKMARK_GET_ALL = "SELECT doc_page, view_page, name FROM bookmarks WHERE book = ? ORDER BY view_page ASC";

    private static final String DB_2_BOOKMARK_DEL_ALL = "DELETE FROM bookmarks WHERE book=?";

    public DBHelper(final Context context) {
        super(context, context.getPackageName() + ".settings", null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        switch (DB_VERSION) {
            case 1:
                db.execSQL(DB_1_BOOK_CREATE);
                break;
            case 2:
                db.execSQL(DB_2_BOOK_CREATE);
                db.execSQL(DB_2_BOOKMARK_CREATE);
            default:
                break;

        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Upgrage from v1 to v2
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(DB_2_BOOKMARK_CREATE);
            return;
        }

        // Downgrage from v2 to v1
        if (oldVersion == 2 && newVersion == 1) {
            db.execSQL(DB_2_BOOK_DROP_ALL, new Object[] {});
            db.execSQL(DB_2_BOOKMARK_DROP_ALL, new Object[] {});
            db.execSQL(DB_1_BOOK_CREATE);
            return;
        }
    }

    public Map<String, BookSettings> getBookSettings(final boolean all) {
        final Map<String, BookSettings> map = new LinkedHashMap<String, BookSettings>();

        try {
            final SQLiteDatabase db = this.getReadableDatabase();
            try {
                final Cursor c = db.rawQuery(DB_1_BOOK_GET_ALL, null);
                if (c != null) {
                    try {
                        for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                            final BookSettings bs = createBookSettings(c);
                            loadBookmarks(bs, db);
                            map.put(bs.getFileName(), bs);
                            if (!all) {
                                break;
                            }
                        }
                    } finally {
                        try {
                            c.close();
                        } catch (final Exception ex) {
                        }
                    }
                }
            } finally {
                db.close();
            }
        } catch (final Throwable th) {
            LCTX.e("Retrieving book settings failed: ", th);
        }

        return map;
    }

    public BookSettings getBookSettings(final String fileName) {
        try {
            final SQLiteDatabase db = this.getReadableDatabase();
            try {
                final Cursor c = db.rawQuery(DB_1_BOOK_GET_ONE, new String[] { fileName });
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            final BookSettings bs = createBookSettings(c);
                            loadBookmarks(bs, db);
                            return bs;
                        }
                    } finally {
                        try {
                            c.close();
                        } catch (final Exception ex) {
                        }
                    }
                }
            } finally {
                try {
                    db.close();
                } catch (final Exception ex) {
                }
            }
        } catch (final Throwable th) {
            LCTX.e("Retrieving book settings failed: ", th);
        }

        return null;
    }

    BookSettings createBookSettings(final Cursor c) {
        int index = 0;

        final BookSettings bs = new BookSettings(c.getString(index++), null);
        bs.lastUpdated = c.getLong(index++);
        bs.currentPage = new PageIndex(c.getInt(index++), c.getInt(index++));
        bs.zoom = c.getInt(index++);
        bs.singlePage = c.getInt(index++) != 0;
        bs.pageAlign = PageAlign.values()[c.getInt(index++)];
        bs.animationType = PageAnimationType.values()[c.getInt(index++)];
        bs.splitPages = c.getInt(index++) != 0;

        return bs;
    }

    public boolean storeBookSettings(final BookSettings bs) {
        try {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.beginTransaction();

                bs.lastUpdated = System.currentTimeMillis();

                final Object[] args = new Object[] {
                        // File name
                        bs.fileName,
                        // Last update
                        bs.lastUpdated,
                        // Current document page
                        bs.currentPage.docIndex,
                        // Current view page
                        bs.currentPage.viewIndex,
                        // Current page zoom
                        bs.zoom,
                        // Single page on/off
                        bs.singlePage ? 1 : 0,
                        // Page align
                        bs.pageAlign.ordinal(),
                        // Page animation type
                        bs.animationType.ordinal(),
                        // Split pages on/off
                        bs.splitPages ? 1 : 0 };

                db.execSQL(DB_1_BOOK_STORE, args);

                updateBookmarks(bs, db);

                db.setTransactionSuccessful();

                return true;
            } finally {
                try {
                    db.endTransaction();
                } catch (final Exception ex) {
                }
                try {
                    db.close();
                } catch (final Exception ex) {
                }
            }
        } catch (final Throwable th) {
            LCTX.e("Update book settings failed: ", th);
        }
        return false;
    }

    public boolean deleteAll() {
        try {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.beginTransaction();

                switch (DB_VERSION) {
                    case 1:
                        db.execSQL(DB_1_BOOK_DROP_ALL, new Object[] {});
                        break;
                    case 2:
                    default:
                        db.execSQL(DB_2_BOOK_DROP_ALL, new Object[] {});
                        db.execSQL(DB_2_BOOKMARK_DROP_ALL, new Object[] {});
                        break;
                }
                onCreate(db);

                db.setTransactionSuccessful();

                return true;
            } finally {
                try {
                    db.endTransaction();
                } catch (final Exception ex) {
                }
                try {
                    db.close();
                } catch (final Exception ex) {
                }
            }
        } catch (final Throwable th) {
            LCTX.e("Update book settings failed: ", th);
        }
        return false;
    }

    void loadBookmarks(final BookSettings book, final SQLiteDatabase db) {
        book.bookmarks.clear();
        if (checkVersion(2)) {
            final Cursor c = db.rawQuery(DB_2_BOOKMARK_GET_ALL, new String[] { book.fileName });
            if (c != null) {
                try {
                    for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                        final Bookmark bm = createBookmark(c);
                        book.bookmarks.add(bm);
                    }
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Bookmarks loaded for " + book.fileName+": " + book.bookmarks.size());
                    }
                } finally {
                    try {
                        c.close();
                    } catch (final Exception ex) {
                    }
                }
            }
        }
    }

    boolean updateBookmarks(final BookSettings book, final SQLiteDatabase db) {
        if (checkVersion(2)) {

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
                LCTX.d("Bookmarks stored for " + book.fileName+": " + book.bookmarks.size());
            }

            return true;
        }
        return false;
    }

    public boolean deleteBookmarks(final String book, final List<Bookmark> bookmarks) {
        try {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.beginTransaction();

                final Object[] delArgs = { book };
                db.execSQL(DB_2_BOOKMARK_DEL_ALL, delArgs);

                db.setTransactionSuccessful();

                return true;
            } finally {
                try {
                    db.endTransaction();
                } catch (final Exception ex) {
                }
                try {
                    db.close();
                } catch (final Exception ex) {
                }
            }
        } catch (final Throwable th) {
            LCTX.e("Deleting bookmarks failed: ", th);
        }
        return false;
    }

    Bookmark createBookmark(final Cursor c) {
        int index = 0;
        return new Bookmark(new PageIndex(c.getInt(index++), c.getInt(index++)), c.getString(index++));
    }

    boolean checkVersion(final int minimal) {
        return DB_VERSION >= minimal;
    }

}
