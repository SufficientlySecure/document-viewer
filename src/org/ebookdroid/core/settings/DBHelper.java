package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    private static final String DB_1_CREATE = "create table book_settings (" + "book varchar(1024) primary key, "
            + "last_updated integer not null, " + "doc_page integer not null, " + "view_page integer not null,"
            + "single_page integer not null," + "page_align integer not null," + "split_pages integer not null" + ");";

    private static final String DB_1_BOOK_GET_ALL = "SELECT book, last_updated, doc_page, view_page, single_page, page_align, split_pages FROM book_settings order by last_updated DESC";

    private static final String DB_1_BOOK_GET_ONE = "SELECT book, last_updated, doc_page, view_page, single_page, page_align, split_pages FROM book_settings WHERE book=?";

    private static final String DB_1_BOOK_STORE = "INSERT OR REPLACE INTO book_settings (book, last_updated, doc_page, view_page, single_page, page_align, split_pages) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DB_1_BOOK_DEL_ALL = "DELETE FROM book_settings";

    public DBHelper(final Context context) {
        super(context, context.getPackageName() + ".settings", null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DB_1_CREATE);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    }

    public Map<String, BookSettings> getBookSettings() {
        final Map<String, BookSettings> map = new LinkedHashMap<String, BookSettings>();

        try {
            final SQLiteDatabase db = this.getReadableDatabase();
            try {
                final Cursor c = db.rawQuery(DB_1_BOOK_GET_ALL, null);
                if (c != null) {
                    try {
                        for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                            final BookSettings bs = createBookSettings(c);
                            map.put(bs.getFileName(), bs);
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
            Log.e("DBHelper", "Retrieving book settings failed: ", th);
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
            Log.e("DBHelper", "Retrieving book settings failed: ", th);
        }

        return null;
    }

    public boolean storeBookSettings(BookSettings bs) {
        try {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.beginTransaction();

                bs.lastUpdated = System.currentTimeMillis();

                Object[] args = new Object[] { bs.fileName, bs.lastUpdated, bs.currentDocPage, bs.currentViewPage,
                        bs.singlePage ? 1 : 0, bs.pageAlign.ordinal(), bs.splitPages ? 1 : 0 };

                db.execSQL(DB_1_BOOK_STORE, args);

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
            Log.e("DBHelper", "Update book settings failed: ", th);
        }
        return false;
    }

    public boolean deleteAll() {
        try {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.beginTransaction();

                db.execSQL(DB_1_BOOK_DEL_ALL, new Object[] {});

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
            Log.e("DBHelper", "Update book settings failed: ", th);
        }
        return false;
    }

    BookSettings createBookSettings(final Cursor c) {
        int index = 0;

        final BookSettings bs = new BookSettings(c.getString(index++));
        bs.lastUpdated = c.getLong(index++);
        bs.currentDocPage = c.getInt(index++);
        bs.currentViewPage = c.getInt(index++);
        bs.singlePage = c.getInt(index++) != 0;
        bs.pageAlign = PageAlign.values()[c.getInt(index++)];
        bs.splitPages = c.getInt(index++) != 0;

        return bs;
    }
}
