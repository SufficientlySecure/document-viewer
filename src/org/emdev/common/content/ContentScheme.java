package org.emdev.common.content;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

import jcifs.smb.SmbFileInputStream;

import org.emdev.common.cache.CacheManager;
import org.emdev.utils.LengthUtils;

public enum ContentScheme {

    FILE("file"),

    CONTENT("content", "[E-mail Attachment]") {

        @Override
        public File loadToCache(final Uri uri) throws IOException {
            return CacheManager.createTempFile(uri);
        }

        @Override
        public String getResourceName(final ContentResolver cr, final Uri uri) {
            try {
                final Cursor c = cr.query(uri, null, null, null, null);
                c.moveToFirst();
                final int fileNameColumnId = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (fileNameColumnId >= 0) {
                    final String attachmentFileName = c.getString(fileNameColumnId);
                    return LengthUtils.safeString(attachmentFileName, key);
                }
            } catch (final Throwable th) {
                th.printStackTrace();
            }
            return super.getResourceName(cr, uri);
        }
    },

    SMB("smb", "[SMB source]") {

        @Override
        public File loadToCache(final Uri uri) throws IOException {
            return CacheManager.createTempFile(new SmbFileInputStream(uri.toString()), uri.getLastPathSegment());
        }
    },

    UNKNOWN("", "");

    public final String scheme;

    public final String key;

    public final boolean temporary;

    private ContentScheme(final String scheme) {
        this.scheme = scheme;
        this.key = null;
        this.temporary = false;
    }

    private ContentScheme(final String scheme, final String key) {
        this.scheme = scheme;
        this.key = key;
        this.temporary = true;
    }

    public File loadToCache(final Uri uri) throws IOException {
        return null;
    }

    public String getResourceName(final ContentResolver cr, final Uri uri) {
        return getDefaultResourceName(uri, key);
    }

    public static String getDefaultResourceName(final Uri uri, final String defTitle) {
        return LengthUtils.safeString(uri.getLastPathSegment(), defTitle);
    }

    public static ContentScheme getScheme(final Intent intent) {
        return intent != null ? getScheme(intent.getScheme()) : UNKNOWN;
    }

    public static ContentScheme getScheme(final Uri uri) {
        return uri != null ? getScheme(uri.getScheme()) : UNKNOWN;
    }

    public static ContentScheme getScheme(final String scheme) {
        for (final ContentScheme s : values()) {
            if (s.scheme.equalsIgnoreCase(scheme)) {
                return s;
            }
        }
        return UNKNOWN;
    }

}
