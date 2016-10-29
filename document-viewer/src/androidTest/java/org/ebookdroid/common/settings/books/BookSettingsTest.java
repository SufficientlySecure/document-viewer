package org.ebookdroid.common.settings.books;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.io.IOUtils;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class BookSettingsTest {

    private static String openAssetAsString(String filename) {
        try {
            InputStream is = null;
            try {
                is = InstrumentationRegistry.getContext().getAssets().open(filename);
                return IOUtils.toString(is, "UTF-8");
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testLoadv277Book() {
        JSONObject json = TestUtils.parseJSON(openAssetAsString("book-v2.7.7.json"));

        BookSettings bs;
        try {
            bs = new BookSettings(json);
        }  catch (JSONException e) {
            throw new RuntimeException(e);
        }
        assertThat(bs, is(notNullValue()));
        assertThat(bs.persistent, is(true));
        assertThat(bs.lastChanged, is(0L));
        assertThat(bs.fileName, is("x.pdf"));
        assertThat(bs.lastUpdated, is(1477463637283L));
        assertThat(bs.firstPageOffset, is(1));
        assertThat(bs.currentPage, is(new PageIndex(2, 2)));
        assertThat(bs.zoom, is(100));
        assertThat(bs.splitPages, is(false));
        assertThat(bs.splitRTL, is(false));
        assertThat(bs.rotation, is(RotationType.UNSPECIFIED));
        assertThat(bs.viewMode, is(DocumentViewMode.VERTICALL_SCROLL));
        assertThat(bs.pageAlign, is(PageAlign.WIDTH));
        assertThat(bs.animationType, is(PageAnimationType.NONE));
        assertThat(bs.bookmarks, is(Matchers.<Bookmark>empty()));
        assertThat(bs.cropPages, is(false));
        assertThat(bs.offsetX, is(0f));
        assertThat(bs.offsetY, is(0.4187299907207489f));
        assertThat(bs.nightMode, is(false));
        assertThat(bs.positiveImagesInNightMode, is(false));
        assertThat(bs.contrast, is(100));
        assertThat(bs.gamma, is(100));
        assertThat(bs.exposure, is(100));
        assertThat(bs.autoLevels, is(false));
        assertThat(bs.typeSpecific, is(nullValue()));
    }
}
