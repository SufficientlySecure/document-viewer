package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.BackupSettings;
import org.ebookdroid.common.settings.SettingsManager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.emdev.common.backup.BackupManager;
import org.emdev.common.backup.IBackupAgent;
import org.emdev.utils.LengthUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DBSettingsManager extends SQLiteOpenHelper implements IDBAdapter, IBackupAgent {

    public static final String BACKUP_KEY = "recent-books";

    public static final int DB_VERSION = 8;

    private final IDBAdapter adapter;

    private SQLiteDatabase upgragingInstance;

    private SQLiteDatabase m_db;

    public DBSettingsManager(final Context context) {
        super(context, context.getPackageName() + ".settings", null, DB_VERSION);
        adapter = createAdapter(DB_VERSION);
        try {
            m_db = getWritableDatabase();
        } catch (final Exception ex) {
            LCTX.e("Unexpected DB error: ", ex);
        }
        BackupManager.addAgent(this);
    }

    public void close() {
        if (m_db != null) {
            try {
                m_db.close();
            } catch (Exception ex) {
            }
            m_db = null;
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
                return new DBAdapterV6(this);
            case DBAdapterV7.VERSION:
                return new DBAdapterV7(this);
            case DBAdapterV8.VERSION:
            default:
                return new DBAdapterV8(this);
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
        final Map<String, BookSettings> bookSettings = oldAdapter.getAllBooks();
        oldAdapter.deleteAll();
        oldAdapter.onDestroy(db);
        newAdapter.onCreate(db);
        newAdapter.restoreBookSettings(bookSettings.values());
    }

    /**
     * {@inheritDoc}
     *
     * @see android.database.sqlite.SQLiteOpenHelper#getWritableDatabase()
     */
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (upgragingInstance != null) {
            return upgragingInstance;
        }

        if (m_db != null && m_db.isOpen()) {
            return m_db;
        }
        LCTX.d("New DB connection created: " + m_db);
        m_db = super.getWritableDatabase();
        return m_db;
    }

    /**
     * {@inheritDoc}
     *
     * @see android.database.sqlite.SQLiteOpenHelper#getReadableDatabase()
     */
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        if (upgragingInstance != null) {
            return upgragingInstance;
        }

        if (m_db != null && m_db.isOpen()) {
            return m_db;
        }
        return super.getReadableDatabase();
    }

    synchronized void closeDatabase(final SQLiteDatabase db) {
        if (db != upgragingInstance && db != m_db) {
            try {
                db.close();
            } catch (final Exception ex) {
            }
            LCTX.d("DB connection closed: " + m_db);
        }
    }

    @Override
    public Map<String, BookSettings> getAllBooks() {
        return adapter.getAllBooks();
    }

    @Override
    public Map<String, BookSettings> getRecentBooks(final boolean all) {
        return adapter.getRecentBooks(all);
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
    public boolean storeBookSettings(final List<BookSettings> list) {
        return adapter.storeBookSettings(list);
    }

    @Override
    public boolean restoreBookSettings(Collection<BookSettings> c) {
        return adapter.restoreBookSettings(c);
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

    @Override
    public String key() {
        return BACKUP_KEY;
    }

    @Override
    public JSONObject backup() {
        final BookBackupType backupType = BackupSettings.current().bookBackup;
        final JSONObject root = new JSONObject();
        if (backupType == BookBackupType.NONE) {
            return root;
        }
        try {
            final JSONArray books = new JSONArray();
            root.put("books", books);
            final Map<String, BookSettings> m = backupType == BookBackupType.RECENT ? getRecentBooks(true)
                    : getAllBooks();
            for (final BookSettings bs : m.values()) {
                final JSONObject obj = bs.toJSON();
                books.put(obj);
            }
        } catch (final JSONException ex) {
            SettingsManager.LCTX.e("Error on recent book backup: " + ex.getMessage());
        }
        return root;
    }

    @Override
    public void restore(final JSONObject backup) {
        try {
            final List<BookSettings> list = new ArrayList<BookSettings>();
            final JSONArray books = backup.getJSONArray("books");
            if (LengthUtils.isNotEmpty(books)) {
                for (int i = 0, n = books.length(); i < n; i++) {
                    final JSONObject obj = books.getJSONObject(i);
                    list.add(new BookSettings(obj));
                }
            }

            if (deleteAll()) {
                restoreBookSettings(list);
            }
            SettingsManager.onRecentBooksChanged();
        } catch (final JSONException ex) {
            SettingsManager.LCTX.e("Error on recent book restoring: " + ex.getMessage());
        }
    }
}
