package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

class DBAdapterV6 extends DBAdapterV5 {

    public static final int VERSION = 6;

    public static final long F_NIGHT_MODE = 1 << 2;

    public static final long F_AUTO_LEVELS = 1 << 3;

    public static final String DB_BOOK_CREATE = "create table book_settings ("
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
            // View mode
            + "view_mode integer not null, "
            // Page align
            + "page_align integer not null, "
            // Page animation type
            + "page_animation integer not null, "
            // Book flags
            + "flags long not null, "
            // Offset x
            + "offset_x integer not null, "
            // Offset y
            + "offset_y integer not null, "
            // Contrast
            + "contrast integer not null, "
            // Exposure
            + "exposure integer not null"
            // ...
            + ");"
    //
    ;

    public static final String DB_BOOK_GET_ALL = "SELECT book, last_updated, doc_page, view_page, zoom, view_mode, page_align, page_animation, flags, offset_x, offset_y, contrast, exposure FROM book_settings where last_updated > 0 ORDER BY last_updated DESC";

    public static final String DB_BOOK_GET_ONE = "SELECT book, last_updated, doc_page, view_page, zoom, view_mode, page_align, page_animation, flags, offset_x, offset_y, contrast, exposure FROM book_settings WHERE book=?";

    public static final String DB_BOOK_STORE = "INSERT OR REPLACE INTO book_settings (book, last_updated, doc_page, view_page, zoom, view_mode, page_align, page_animation, flags, offset_x, offset_y, contrast, exposure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public DBAdapterV6(final DBSettingsManager manager) {
        super(manager);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DB_BOOK_CREATE);
        db.execSQL(DB_BOOKMARK_CREATE);
    }

    @Override
    public Map<String, BookSettings> getBookSettings(final boolean all) {
        return getBookSettings(DB_BOOK_GET_ALL, all);
    }

    @Override
    public BookSettings getBookSettings(final String fileName) {
        return getBookSettings(DB_BOOK_GET_ONE, fileName);
    }

    @Override
    protected void storeBookSettings(final BookSettings bs, final SQLiteDatabase db) {
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
                bs.viewMode.ordinal(),
                // Page align
                bs.pageAlign.ordinal(),
                // Page animation type
                bs.animationType.ordinal(),
                // Flags
                (bs.splitPages ? F_SPLIT_PAGES : 0) | (bs.cropPages ? F_CROP_PAGES : 0) | (bs.nightMode ? F_NIGHT_MODE : 0) | (bs.autoLevels ? F_AUTO_LEVELS : 0),
                // Offset x
                (int) (bs.offsetX * OFFSET_FACTOR),
                // Offset y
                (int) (bs.offsetY * OFFSET_FACTOR),
                // Contrast
                bs.contrast,
                // Contrast
                bs.exposure
        // ...
        };

        db.execSQL(DB_BOOK_STORE, args);

        updateBookmarks(bs, db);
    }

    @Override
    protected BookSettings createBookSettings(final Cursor c) {
        int index = 0;

        final BookSettings bs = new BookSettings(c.getString(index++));
        bs.lastUpdated = c.getLong(index++);
        bs.currentPage = new PageIndex(c.getInt(index++), c.getInt(index++));
        bs.zoom = c.getInt(index++);
        bs.viewMode = DocumentViewMode.getByOrdinal(c.getInt(index++));
        bs.pageAlign = PageAlign.values()[c.getInt(index++)];
        bs.animationType = PageAnimationType.values()[c.getInt(index++)];

        long flags = c.getLong(index++);
        bs.splitPages = (flags & F_SPLIT_PAGES) != 0;
        bs.cropPages = (flags & F_CROP_PAGES) != 0;
        bs.nightMode = (flags & F_NIGHT_MODE) != 0;
        bs.autoLevels = (flags & F_AUTO_LEVELS) != 0;

        bs.offsetX = c.getInt(index++) / OFFSET_FACTOR;
        bs.offsetY = c.getInt(index++) / OFFSET_FACTOR;

        bs.contrast = c.getInt(index++);
        bs.exposure = c.getInt(index++);

        return bs;
    }
}
