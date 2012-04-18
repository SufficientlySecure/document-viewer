package org.ebookdroid.common.settings.books;

import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

class DBAdapterV4 extends DBAdapterV3 {

    public static final int VERSION = 4;

    public static final float OFFSET_FACTOR = 100000.0f;

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
            // Single page mode on/off
            + "single_page integer not null, "
            // Page align
            + "page_align integer not null, "
            // Page animation type
            + "page_animation integer not null, "
            // Split pages on/off
            + "split_pages integer not null, "
            // Crop pages on/off
            + "crop_pages integer not null, "
            // Offset x
            + "offset_x integer not null, "
            // Offset y
            + "offset_y integer not null"
            // ...
            + ");"
    //
    ;

    public static final String DB_BOOK_GET_ALL = "SELECT book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages, crop_pages, offset_x, offset_y FROM book_settings where last_updated > 0 ORDER BY last_updated DESC";

    public static final String DB_BOOK_GET_ONE = "SELECT book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages, crop_pages, offset_x, offset_y FROM book_settings WHERE book=?";

    public static final String DB_BOOK_STORE = "INSERT OR REPLACE INTO book_settings (book, last_updated, doc_page, view_page, zoom, single_page, page_align, page_animation, split_pages, crop_pages, offset_x, offset_y) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String DB_BOOKMARK_CREATE = "create table bookmarks ("
    // Book file path
            + "book varchar(1024) not null, "
            // Current document page
            + "doc_page integer not null, "
            // Current view page - dependent on view mode
            + "view_page integer not null, "
            // Bookmark name
            + "name varchar(1024) not null, "
            // Offset x
            + "offset_x integer not null, "
            // Offset y
            + "offset_y integer not null"
            // ...
            + ");";

    public static final String DB_BOOKMARK_STORE = "INSERT OR REPLACE INTO bookmarks (book, doc_page, view_page, name, offset_x, offset_y) VALUES (?, ?, ?, ?, ?, ?)";

    public static final String DB_BOOKMARK_GET_ALL = "SELECT doc_page, view_page, name, offset_x, offset_y FROM bookmarks WHERE book = ? ORDER BY view_page ASC";

    public DBAdapterV4(final DBSettingsManager manager) {
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
                bs.viewMode == DocumentViewMode.SINGLE_PAGE ? 1 : 0,
                // Page align
                bs.pageAlign.ordinal(),
                // Page animation type
                bs.animationType.ordinal(),
                // Split pages on/off
                bs.splitPages ? 1 : 0,
                // Crop pages on/off
                bs.cropPages ? 1 : 0,
                // Offset x
                (int) bs.offsetX,
                // Offset y
                (int) bs.offsetY
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
        bs.viewMode = c.getInt(index++) != 0 ? DocumentViewMode.SINGLE_PAGE : DocumentViewMode.VERTICALL_SCROLL;
        bs.pageAlign = PageAlign.values()[c.getInt(index++)];
        bs.animationType = PageAnimationType.values()[c.getInt(index++)];
        bs.splitPages = c.getInt(index++) != 0;
        bs.cropPages = c.getInt(index++) != 0;
        bs.offsetX = c.getInt(index++);
        bs.offsetY = c.getInt(index++);

        return bs;
    }

    @Override
    protected void loadBookmarks(final BookSettings book, final SQLiteDatabase db) {
        loadBookmarks(book, db, DB_BOOKMARK_GET_ALL);
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
                    bs.name,
                    // Offset x
                    (int) (bs.offsetX * OFFSET_FACTOR),
                    // Offset y
                    (int) (bs.offsetY * OFFSET_FACTOR)
            // ..
            };
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

        final float offsetX = c.getInt(index++) / OFFSET_FACTOR;
        final float offsetY = c.getInt(index++) / OFFSET_FACTOR;

        return new Bookmark(name, new PageIndex(docIndex, viewIndex), offsetX, offsetY);
    }

}
