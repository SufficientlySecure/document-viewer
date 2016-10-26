package org.ebookdroid.common.settings.books;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DBSettingsManagerMigrationTest {

    private static final String BOOK_FILENAME = "testfilename.pdf";
    private static final String BOOK2_FILENAME = "testfilename2.pdf";

    private static final String BOOK_TYPE_SPECIFIC_JSON = "{\"foo\":\"bar\"}";
    private static final String BOOK2_TYPE_SPECIFIC_JSON = "{\"foo\":\"baz\"}";

    private static List<Bookmark> createBook1Bookmarks() {
        ArrayList<Bookmark> bookmarks = new ArrayList<>();
        bookmarks.add(new Bookmark("bookmark", new PageIndex(12, 34), 5.0f, 10.0f));
        bookmarks.add(new Bookmark("bookmark2", new PageIndex(12, 34), 5.0f, 10.0f));
        return bookmarks;
    }

    private static BookSettings createBook1() {
        BookSettings bs = new BookSettings(BOOK_FILENAME);
        bs.lastUpdated = 123L;
        bs.firstPageOffset = 2;
        bs.currentPage = PageIndex.LAST;
        bs.zoom = 55;
        bs.splitPages = true;
        bs.splitRTL = true;
        bs.rotation = BookRotationType.AUTOMATIC;
        bs.viewMode = DocumentViewMode.HORIZONTAL_SCROLL;
        bs.pageAlign = PageAlign.HEIGHT;
        bs.animationType = PageAnimationType.CURLER;
        bs.bookmarks.addAll(createBook1Bookmarks());
        bs.cropPages = true;
        bs.offsetX = 4.5f;
        bs.offsetY = -1.5f;
        bs.nightMode = true;
        bs.positiveImagesInNightMode = true;
        bs.contrast = 20;
        bs.gamma = 15;
        bs.exposure = 10;
        bs.autoLevels = true;
        bs.typeSpecific = TestUtils.parseJSON(BOOK_TYPE_SPECIFIC_JSON);
        return bs;
    }

    private static void checkBook1(BookSettings bs) {
        assertThat(bs, is(notNullValue()));
        assertThat(bs.persistent, is(true));
        assertThat(bs.lastChanged, is(0L));
        assertThat(bs.fileName, is(BOOK_FILENAME));
        assertThat(bs.lastUpdated, is(123L));
        assertThat(bs.firstPageOffset, is(2));
        assertThat(bs.currentPage, is(PageIndex.LAST));
        assertThat(bs.zoom, is(55));
        assertThat(bs.splitPages, is(true));
        assertThat(bs.splitRTL, is(true));
        assertThat(bs.rotation, is(BookRotationType.AUTOMATIC));
        assertThat(bs.viewMode, is(DocumentViewMode.HORIZONTAL_SCROLL));
        assertThat(bs.pageAlign, is(PageAlign.HEIGHT));
        assertThat(bs.animationType, is(PageAnimationType.CURLER));
        assertThat(bs.bookmarks, is(createBook1Bookmarks()));
        assertThat(bs.cropPages, is(true));
        assertThat(bs.offsetX, is(4.5f));
        assertThat(bs.offsetY, is(-1.5f));
        assertThat(bs.nightMode, is(true));
        assertThat(bs.positiveImagesInNightMode, is(true));
        assertThat(bs.contrast, is(20));
        assertThat(bs.gamma, is(15));
        assertThat(bs.exposure, is(10));
        assertThat(bs.autoLevels, is(true));
        assertThat(bs.typeSpecific.toString(), is(BOOK_TYPE_SPECIFIC_JSON));
    }

    private static List<Bookmark> createBook2Bookmarks() {
        ArrayList<Bookmark> bookmarks = new ArrayList<>();
        bookmarks.add(new Bookmark("bookmark3", new PageIndex(13, 35), 0.0f, 0.0f));
        return bookmarks;
    }

    private static BookSettings createBook2() {
        BookSettings bs = new BookSettings(BOOK2_FILENAME);
        bs.lastUpdated = 456L;
        bs.firstPageOffset = 0;
        bs.currentPage = new PageIndex(44, 0);
        bs.zoom = 0;
        bs.splitPages = false;
        bs.splitRTL = false;
        bs.rotation = BookRotationType.UNSPECIFIED;
        bs.viewMode = DocumentViewMode.SINGLE_PAGE;
        bs.pageAlign = PageAlign.AUTO;
        bs.animationType = PageAnimationType.NONE;
        bs.bookmarks.addAll(createBook2Bookmarks());
        bs.cropPages = false;
        bs.offsetX = 100.0f;
        bs.offsetY = 0.0f;
        bs.nightMode = false;
        bs.positiveImagesInNightMode = false;
        bs.contrast = 5;
        bs.gamma = 4;
        bs.exposure = 3;
        bs.autoLevels = false;
        bs.typeSpecific = TestUtils.parseJSON(BOOK2_TYPE_SPECIFIC_JSON);
        return bs;
    }

    private static void checkBook2(BookSettings bs) {
        assertThat(bs, is(notNullValue()));
        assertThat(bs.persistent, is(true));
        assertThat(bs.lastChanged, is(0L));
        assertThat(bs.fileName, is(BOOK2_FILENAME));
        assertThat(bs.lastUpdated, is(456L));
        assertThat(bs.firstPageOffset, is(0));
        assertThat(bs.currentPage, is(new PageIndex(44, 0)));
        assertThat(bs.zoom, is(0));
        assertThat(bs.splitPages, is(false));
        assertThat(bs.splitRTL, is(false));
        assertThat(bs.rotation, is(BookRotationType.UNSPECIFIED));
        assertThat(bs.viewMode, is(DocumentViewMode.SINGLE_PAGE));
        assertThat(bs.pageAlign, is(PageAlign.AUTO));
        assertThat(bs.animationType, is(PageAnimationType.NONE));
        assertThat(bs.bookmarks, is(createBook2Bookmarks()));
        assertThat(bs.cropPages, is(false));
        assertThat(bs.offsetX, is(100f));
        assertThat(bs.offsetY, is(0f));
        assertThat(bs.nightMode, is(false));
        assertThat(bs.positiveImagesInNightMode, is(false));
        assertThat(bs.contrast, is(5));
        assertThat(bs.gamma, is(4));
        assertThat(bs.exposure, is(3));
        assertThat(bs.autoLevels, is(false));
        assertThat(bs.typeSpecific.toString(), is(BOOK2_TYPE_SPECIFIC_JSON));
    }

    private static void setupDB(DBSettingsManager manager) {
        assertThat(manager.storeBookSettings(createBook1()), is(true));
        assertThat(manager.storeBookSettings(createBook2()), is(true));
    }

    private static void checkDB(DBSettingsManager manager) {
        Map<String, BookSettings> allBooks = manager.getAllBooks();
        assertThat(allBooks.size(), is(2));
        assertThat(allBooks.containsKey(BOOK_FILENAME), is(true));
        assertThat(allBooks.containsKey(BOOK2_FILENAME), is(true));

        checkBook1(allBooks.get(BOOK_FILENAME));
        checkBook2(allBooks.get(BOOK2_FILENAME));
    }

    @Test
    public void testBook1InitialSettings() {
        checkBook1(createBook1());
    }

    @Test
    public void testBook2InitialSettings() {
        checkBook2(createBook2());
    }

    @Test
    public void test8To9() {
        Context ctx = InstrumentationRegistry.getTargetContext();
        ctx.deleteDatabase("test8To9.db");

        DBSettingsManager db = new DBSettingsManager(ctx, "test8To9.db", 8);
        setupDB(db);
        checkDB(db);
        db.close();

        db = new DBSettingsManager(ctx, "test8To9.db", 9);
        checkDB(db);
        db.close();
    }
}
