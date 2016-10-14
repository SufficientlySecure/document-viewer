package org.ebookdroid.common.settings.books;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

class DBAdapterV9 implements IDBAdapter {

    public static final int VERSION = 9;

    private static final String DB_BOOK_CREATE = "CREATE TABLE book_settings ("
            // Book file path
            + "book varchar(1024) primary key, "
            // Book settings
            + "book_json varchar(4096)"
            + ");"
    ;

    private static final String DB_BOOK_DEL = "DELETE FROM book_settings WHERE book=?";

    private static final String DB_BOOK_DROP = "DROP TABLE IF EXISTS book_settings";

    private static final String DB_BOOK_GET_ALL = "SELECT book, book_json FROM book_settings ORDER BY book ASC";

    private static final String DB_BOOK_GET_ONE = "SELECT book, book_json FROM book_settings WHERE book=?";

    private static final String DB_BOOK_STORE = "INSERT OR REPLACE INTO book_settings (book, book_json) VALUES (? , ?)";

    private final DBSettingsManager manager;

    public DBAdapterV9(final DBSettingsManager manager) {
        this.manager = manager;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DB_BOOK_CREATE);
    }

    @Override
    public void onDestroy(final SQLiteDatabase db) {
        db.execSQL(DB_BOOK_DROP);
    }

    private void endTransaction(final SQLiteDatabase db) {
        try {
            db.endTransaction();
        } catch (final Exception ex) {
        }
        manager.closeDatabase(db);
    }

    private void close(final Cursor c) {
        try {
            c.close();
        } catch (final Exception ex) {
        }
    }

    private LinkedHashMap<String, BookSettings> makeBookSettingsMap(List<BookSettings> books) {
        final LinkedHashMap<String, BookSettings> map = new LinkedHashMap<>();
        for (BookSettings bs : books) {
            map.put(bs.fileName, bs);
        }
        return map;
    }

    private List<BookSettings> getBookSettings(final String query, final String[] args) {
        final List<BookSettings> list = new ArrayList<>();

        try {
            final SQLiteDatabase db = manager.getReadableDatabase();
            try {
                final Cursor c = db.rawQuery(query, args);
                if (c != null) {
                    try {
                        for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
                            final BookSettings bs = createBookSettings(c);
                            list.add(bs);
                        }
                    } finally {
                        close(c);
                    }
                }
            } finally {
                manager.closeDatabase(db);
            }
        } catch (final Throwable th) {
            LCTX.e("Retrieving book settings failed: ", th);
        }
        return list;
    }

    @Override
    public Map<String, BookSettings> getAllBooks() {
        List<BookSettings> list =  getBookSettings(DB_BOOK_GET_ALL, null);
        return makeBookSettingsMap(list);
    }

    @Override
    public Map<String, BookSettings> getRecentBooks(final boolean all) {
        List<BookSettings> list = new ArrayList<>();

        for (BookSettings bs : getBookSettings(DB_BOOK_GET_ALL, null)) {
            if (bs.lastUpdated > 0) {
                list.add(bs);
            }
        }

        Collections.sort(list, new Comparator<BookSettings>() {
            @Override
            public int compare(BookSettings bs1, BookSettings bs2) {
                if (bs1.lastUpdated < bs2.lastUpdated) {
                    return 1;
                } else if (bs1.lastUpdated == bs2.lastUpdated) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        if (!all && list.size() > 0) {
            list = Collections.singletonList(list.get(0));
        }

        return makeBookSettingsMap(list);
    }

    @Override
    public BookSettings getBookSettings(final String fileName) {
        List<BookSettings> list = getBookSettings(DB_BOOK_GET_ONE, new String[]{ fileName });
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    private void logBookUpdate(final BookSettings bs) {
        if (LCTX.isDebugEnabled()) {
            try {
                LCTX.d("Store: " + bs.toJSON());
            } catch (JSONException e) {
                LCTX.d("Couldn't serialize " + bs.fileName);
            }
        }
    }

    private void storeBookSettings(final BookSettings bs, final SQLiteDatabase db) {
        logBookUpdate(bs);

        JSONObject bookJSON = null;
        try {
            bookJSON = bs.toJSON();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        final Object[] args = new Object[] {
                // File name
                bs.fileName,
                // Book json
                bookJSON.toString()
                // ...
        };

        db.execSQL(DB_BOOK_STORE, args);
    }

    private BookSettings createBookSettings(final Cursor c) {
        String fileName = c.getString(0);
        String jsonString = c.getString(1);

        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            BookSettings bs = new BookSettings(jsonObj);

            if (bs.fileName == null || !bs.fileName.equals(fileName)) {
                LCTX.e("Name mismatch: " + bs.fileName + " and " + fileName);
                return null;
            }

            return bs;
        } catch (JSONException e) {
            LCTX.e("Error parsing JSON: " + e);
            return null;
        }
    }

    @Override
    public void delete(final BookSettings current) {
        executeUpdate(new SQLBlock() {
            @Override
            public void run(SQLiteDatabase db) {
                db.execSQL(DB_BOOK_DEL, new Object[] { current.fileName });
            }
        });
    }

    @Override
    public boolean storeBookSettings(final List<BookSettings> list) {
        return executeUpdate(new SQLBlock() {
            @Override
            public void run(SQLiteDatabase db) {
                for (final BookSettings bs : list) {
                    if (bs.lastChanged > 0) {
                        bs.lastUpdated = System.currentTimeMillis();
                    }

                    storeBookSettings(bs, db);
                }
            }
        });
    }

    @Override
    public final boolean restoreBookSettings(final Collection<BookSettings> c) {
        return executeUpdate(new SQLBlock() {
            @Override
            public void run(SQLiteDatabase db) {
                for (final BookSettings bs : c) {
                    storeBookSettings(bs, db);
                }
            }
        });
    }

    @Override
    public boolean clearRecent() {
        // TODO: would be better if this was all in a transaction
        Map<String, BookSettings> allBooks = getAllBooks();
        for (BookSettings bs : allBooks.values()) {
            bs.lastUpdated = 0L;
        }
        return storeBookSettings(new ArrayList<>(allBooks.values()));
    }

    @Override
    public boolean deleteAll() {
        return executeUpdate(new SQLBlock() {
            @Override
            public void run(SQLiteDatabase db) {
                db.execSQL(DB_BOOK_DROP, new Object[] {});
                onCreate(db);
            }
        });
    }
    
    @Override
    public boolean deleteAllBookmarks() {
        // TODO: would be better if this was all in a transaction
        Map<String, BookSettings> allBooks = getAllBooks();
        for (BookSettings bs : allBooks.values()) {
            bs.bookmarks.clear();
        }
        storeBookSettings(new ArrayList<>(allBooks.values()));
        return true;
    }

    @Override
    public boolean removeBookFromRecents(final BookSettings bs) {
        bs.lastUpdated = 0;
        return storeBookSettings(Collections.singletonList(bs));
    }

    private interface SQLBlock {
        void run(SQLiteDatabase db);
    }

    private boolean executeUpdate(SQLBlock block) {
        try {
            final SQLiteDatabase db = manager.getWritableDatabase();
            try {
                db.beginTransaction();
                block.run(db);
                db.setTransactionSuccessful();

                return true;
            } finally {
                endTransaction(db);
            }
        } catch (final Throwable th) {
            LCTX.e("executeUpdate: ", th);
        }
        return false;
    }
}
