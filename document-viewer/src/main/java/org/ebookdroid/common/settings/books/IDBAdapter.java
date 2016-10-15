package org.ebookdroid.common.settings.books;


import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

interface IDBAdapter {

    LogContext LCTX = LogManager.root().lctx("DBAdapter", false);

    /**
     * Create tables for book settings database
     * @param db
     */
    void onCreate(final SQLiteDatabase db);

    /**
     * Drop tables for book settings database
     * @param db
     */
    void onDestroy(final SQLiteDatabase db);

    /**
     * Get all book settings, with fileName as the map keys.
     * @return
     */
    Map<String, BookSettings> getAllBooks();

    /**
     * Returns books with last_updated > 0
     * @param all if false, returns at most one book
     * @return
     */
    Map<String, BookSettings> getRecentBooks(boolean all);

    /**
     * Returns book settings for the given fileName
     * @param fileName
     * @return
     */
    BookSettings getBookSettings(String fileName);

    /**
     * Persists the given list of book settings.
     *
     * For each book, if book.lastChanged is greater than 0, sets book.lastUpdated to the current time.
     * @param list
     * @return true on success
     */
    boolean storeBookSettings(List<BookSettings> list);

    /**
     * Stores the book settings, preserving the last_updated value from the BookSettings objects
     * @param c
     * @return true on success
     */
    boolean restoreBookSettings(Collection<BookSettings> c);

    /**
     * Set last_updated to 0 for all books.
     * @return true on success
     */
    boolean clearRecent();

    /**
     * Removes the given book from the book settings database
     * @param current
     */
    void delete(BookSettings current);

    /**
     * Deletes all book settings and bookmarks
     * @return true on success
     */
    boolean deleteAll();

    /**
     * Delete all bookmarks
     * @return true on success
     */
    boolean deleteAllBookmarks();

    /**
     * Set last_updated to 0 for the given book.
     * @return true on success
     */
    boolean removeBookFromRecents(BookSettings bs);
}
