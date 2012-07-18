package org.ebookdroid.common.settings.books;


import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

interface IDBAdapter {

    LogContext LCTX = LogManager.root().lctx("DBAdapter");

    void onCreate(final SQLiteDatabase db);

    void onDestroy(final SQLiteDatabase db);

    Map<String, BookSettings> getBookSettings(final boolean all);

    BookSettings getBookSettings(final String fileName);

    boolean storeBookSettings(final BookSettings bs);

    boolean restoreBookSettings(Collection<BookSettings> c);

    boolean clearRecent();

    void delete(BookSettings current);

    boolean deleteAll();

    boolean updateBookmarks(final BookSettings book);

    boolean deleteBookmarks(final String book, final List<Bookmark> bookmarks);

    boolean deleteAllBookmarks();

    boolean removeBookFromRecents(BookSettings bs);

    
}
