package org.ebookdroid.common.settings.books;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.ebookdroid.common.settings.definitions.AppPreferences;
import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DBSettingsManagerTest {
    private DBSettingsManager m_manager;

    private BookSettings m_bs;

    private BookSettings sampleBookSettings() {
        BookSettings bs = new BookSettings(BS_FILENAME);
        // FIXME: shouldn't need to set these
        bs.rotation = BookRotationType.UNSPECIFIED;
        bs.viewMode = DocumentViewMode.VERTICALL_SCROLL;
        return bs;
    }

    private Bookmark m_b1;
    private Bookmark m_b1_dup;
    private Bookmark m_b2;

    @Before
    public void setup() {
        m_manager = new DBSettingsManager(InstrumentationRegistry.getTargetContext(), "DBSettingsManagerTest.settings");
        assertThat(m_manager, is(notNullValue()));
        assertThat(m_manager.deleteAll(), is(true));

        m_bs = sampleBookSettings();

        m_b1 = new Bookmark("bookmark", new PageIndex(12, 34), 5.0f, 10.0f);
        m_b1_dup = new Bookmark("bookmark", new PageIndex(12, 34), 5.0f, 10.0f);
        m_b2 = new Bookmark("bookmark2", new PageIndex(13, 35), 0.0f, 0.0f);
    }

    @After
    public void cleanUp() {
        m_manager.close();
    }

    private static final String BS_FILENAME = "testfilename.pdf";

    private void checkDefaults(BookSettings bs, long expectedLastUpdated) {
        assertThat(bs, is(notNullValue()));
        assertThat(bs.persistent, is(true));
        assertThat(bs.lastChanged, is(0L));
        assertThat(bs.fileName, is(BS_FILENAME));
        assertThat(Math.abs(bs.lastUpdated - expectedLastUpdated), is(lessThanOrEqualTo(1L)));
        assertThat(bs.firstPageOffset, is(1));
        assertThat(bs.currentPage, is(PageIndex.FIRST));
        assertThat(bs.zoom, is(100));
        assertThat(bs.splitPages, is(false));
        assertThat(bs.splitRTL, is(false));
        assertThat(bs.rotation, is(BookRotationType.UNSPECIFIED));
        assertThat(bs.viewMode, is(DocumentViewMode.VERTICALL_SCROLL));
        assertThat(bs.pageAlign, is(PageAlign.AUTO));
        assertThat(bs.animationType, is(PageAnimationType.NONE));
        assertThat(bs.bookmarks, is(empty()));
        assertThat(bs.cropPages, is(false));
        assertThat(bs.offsetX, is(0.0f));
        assertThat(bs.offsetY, is(0.0f));
        assertThat(bs.nightMode, is(false));
        assertThat(bs.positiveImagesInNightMode, is(false));
        assertThat(bs.contrast, is(AppPreferences.CONTRAST.defValue));
        assertThat(bs.gamma, is(AppPreferences.GAMMA.defValue));
        assertThat(bs.exposure, is(AppPreferences.EXPOSURE.defValue));
        assertThat(bs.autoLevels, is(false));
        assertThat(bs.typeSpecific, is(nullValue()));
    }

    private BookSettings roundTrip(BookSettings bs) {
        assertThat(m_manager.storeBookSettings(bs), is(true));
        BookSettings roundtrip = m_manager.getBookSettings(bs.fileName);

        assertThat(roundtrip, is(notNullValue()));
        return roundtrip;
    }

    @Test
    public void testPersistDefaultObject() {
        final long creationTime = System.currentTimeMillis();
        BookSettings bs = sampleBookSettings();
        checkDefaults(bs, creationTime);

        assertThat(m_manager.getBookSettings(BS_FILENAME), is(nullValue()));
        assertThat(m_manager.storeBookSettings(bs), is(true));

        BookSettings roundtrip = m_manager.getBookSettings(bs.fileName);
        assertThat(roundtrip, is(notNullValue()));

        checkDefaults(roundtrip, creationTime);
    }

    @Test
    public void testFirstPageOffset() {
        for (int testValue : new int[] {15, 45}) {
            m_bs.firstPageOffset = testValue;
            assertThat(roundTrip(m_bs).firstPageOffset, is(testValue));
        }
    }

    @Test
    public void testCurrentPage() {
        for (PageIndex testValue : new PageIndex[] { PageIndex.FIRST, PageIndex.LAST, PageIndex.NULL, new PageIndex(123, 456)}) {
            m_bs.currentPage = testValue;
            assertThat(roundTrip(m_bs).currentPage, is(testValue));
        }
    }

    @Test
    public void testZoom() {
        for (int testValue : new int[] {15, 45}) {
            m_bs.zoom = testValue;
            assertThat(roundTrip(m_bs).zoom, is(testValue));
        }
    }

    @Test
    public void testSplitPages() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.splitPages = testValue;
            assertThat(roundTrip(m_bs).splitPages, is(testValue));
        }
    }

    @Test
    public void testSplitRTL() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.splitRTL = testValue;
            assertThat(roundTrip(m_bs).splitRTL, is(testValue));
        }
    }

    @Test
    public void testRotation() {
        for (BookRotationType testValue : BookRotationType.values()) {
            m_bs.rotation = testValue;
            assertThat(roundTrip(m_bs).rotation, is(testValue));
        }
    }

    @Test
    public void testViewMode() {
        for (DocumentViewMode testValue : DocumentViewMode.values()) {
            m_bs.viewMode = testValue;
            assertThat(roundTrip(m_bs).viewMode, is(testValue));
        }
    }

    @Test
    public void testPageAlign() {
        for (PageAlign testValue : PageAlign.values()) {
            m_bs.pageAlign = testValue;
            assertThat(roundTrip(m_bs).pageAlign, is(testValue));
        }
    }

    @Test
    public void testAnimationType() {
        for (PageAnimationType testValue : PageAnimationType.values()) {
            m_bs.animationType = testValue;
            assertThat(roundTrip(m_bs).animationType, is(testValue));
        }
    }

    @Test
    public void testBookmarksEquality() {
        assertThat(m_b1, is(m_b1_dup));
        assertThat(m_b1, is(not(equalTo(m_b2))));
    }

    @Test
    public void testBookmarksEmpty() {
        assertThat(m_bs.bookmarks, is(empty()));
        assertThat(roundTrip(m_bs).bookmarks, is(empty()));
    }

    @Test
    public void testAddBookmark() {
        m_bs.bookmarks.addAll(Arrays.asList(m_b1));
        assertThat(roundTrip(m_bs).bookmarks, is(Arrays.asList(m_b1)));

        m_bs.bookmarks.clear();
        m_bs.bookmarks.addAll(Arrays.asList(m_b1, m_b2));
        assertThat(roundTrip(m_bs).bookmarks, is(Arrays.asList(m_b1, m_b2)));
    }

    @Test
    public void testDuplicateBookmarksAllowed() {
        m_bs.bookmarks.addAll(Arrays.asList(m_b1, m_b1, m_b2));
        assertThat(roundTrip(m_bs).bookmarks, is(Arrays.asList(m_b1, m_b1, m_b2)));

        m_bs.bookmarks.clear();
        m_bs.bookmarks.addAll(Arrays.asList(m_b1, m_b2));
        assertThat(roundTrip(m_bs).bookmarks, is(Arrays.asList(m_b1, m_b2)));
    }

    @Test
    public void testCropPages() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.cropPages = testValue;
            assertThat(roundTrip(m_bs).cropPages, is(testValue));
        }
    }

    @Test
    public void testOffsetX() {
        for (float testValue : new float[] { -1.0f, 3.3f }) {
            m_bs.offsetX = testValue;
            assertThat(roundTrip(m_bs).offsetX, is(testValue));
        }
    }

    @Test
    public void testOffsetY() {
        for (float testValue : new float[] { -1.0f, 3.3f }) {
            m_bs.offsetY = testValue;
            assertThat(roundTrip(m_bs).offsetY, is(testValue));
        }
    }

    @Test
    public void testNightMode() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.nightMode = testValue;
            assertThat(roundTrip(m_bs).nightMode, is(testValue));
        }
    }

    @Test
    public void testPositiveImagesInNightMode() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.positiveImagesInNightMode = testValue;
            assertThat(roundTrip(m_bs).positiveImagesInNightMode, is(testValue));
        }
    }

    @Test
    public void testContrast() {
        for (int testValue : new int[] { 1, 255 }) {
            m_bs.contrast = testValue;
            assertThat(roundTrip(m_bs).contrast, is(testValue));
        }
    }

    @Test
    public void testGamma() {
        for (int testValue : new int[] { 1, 255 }) {
            m_bs.gamma = testValue;
            assertThat(roundTrip(m_bs).gamma, is(testValue));
        }
    }

    @Test
    public void testExposure() {
        for (int testValue : new int[] { 1, 255 }) {
            m_bs.exposure = testValue;
            assertThat(roundTrip(m_bs).exposure, is(testValue));
        }
    }

    @Test
    public void testAutoLevels() {
        for (boolean testValue : new boolean[] { true, false }) {
            m_bs.autoLevels = testValue;
            assertThat(roundTrip(m_bs).autoLevels, is(testValue));
        }
    }

    @Test
    public void testTypeSpecific() {
        Map<String, String> m = new HashMap<>();
        m.put("foo", "bar");
        JSONObject testValue = new JSONObject(m);

        // JSONObject doesn't support equals()
        m_bs.typeSpecific = testValue;
        assertThat(roundTrip(m_bs).typeSpecific.toString(), is(testValue.toString()));

        m_bs.typeSpecific = null;
        assertThat(roundTrip(m_bs).typeSpecific, is(nullValue()));
    }
}
