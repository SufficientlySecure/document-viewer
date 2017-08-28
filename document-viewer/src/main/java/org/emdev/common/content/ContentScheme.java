package org.emdev.common.content;

import org.emdev.ui.actions.ActionEx;
import org.sufficientlysecure.viewer.R;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.WorkerThread;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jcifs.smb.SmbFileInputStream;

import org.emdev.common.cache.CacheManager;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.progress.UIFileCopying;
import org.emdev.utils.LengthUtils;

public enum ContentScheme {

    FILE("file"),

    CONTENT("content", "[E-mail Attachment]", false) {

        @Override
        @WorkerThread
        public File loadToCache(final Uri uri, final String extension, final IProgressIndicator progress) throws IOException {
            final UIFileCopying ui = new UIFileCopying(R.string.msg_loading_book, 64 * 1024, progress);
            return CacheManager.createTempFile(uri, extension, ui);
        }

        @Override
        public String getResourceName(final ContentResolver cr, final Uri uri) {
            try {
                final Cursor c = cr.query(uri, null, null, null, null);
                c.moveToFirst();
                final int fileNameColumnId = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (fileNameColumnId >= 0) {
                    final String attachmentFileName = c.getString(fileNameColumnId);
                    c.close();
                    return LengthUtils.safeString(attachmentFileName, key);
                }
                c.close();
            } catch (final Throwable th) {
                th.printStackTrace();
            }
            return super.getResourceName(cr, uri);
        }
    },

    SMB("smb", "[SMB source]", true) {

        @Override
        @WorkerThread
        public File loadToCache(final Uri uri, final String extension, final IProgressIndicator progress) throws IOException {
            final UIFileCopying ui = new UIFileCopying(R.string.opds_loading_book, 64 * 1024, progress);
            return CacheManager.createTempDocument(new SmbFileInputStream(uri.toString()), uri.getLastPathSegment(), ui);
        }
    },

    HTTP("http", "[HTTP source]", true) {

        @Override
        @WorkerThread
        public File loadToCache(final Uri uri, final String extension, final IProgressIndicator progress) throws IOException {
            return loadFromURL(uri, progress);
        }
    },

    HTTPS("https", "[HTTPS source]", true) {

        @Override
        @WorkerThread
        public File loadToCache(final Uri uri, final String extension, final IProgressIndicator progress) throws IOException {
            return loadFromURL(uri, progress);
        }
    },

    UNKNOWN("", "", true);

    public final String scheme;

    /**
     * For schemes where temporary is true, this is used as a placeholder for the filename in
     * ViewerActivityController.
     *
     * @see org.ebookdroid.ui.viewer.ViewerActivityController#onPostCreate(Bundle, boolean)
     */
    public final String key;

    /**
     * false for file:// URI's, true otherwise.
     *
     * @see org.ebookdroid.ui.viewer.ViewerActivityController#onPostCreate(Bundle, boolean)
     */
    public final boolean temporary;

    /**
     * Whether a prompt should be given to save a local copy of the document for this ContentScheme.
     *
     * @see org.ebookdroid.ui.viewer.ViewerActivityController#closeActivity(ActionEx)
     */
    public final boolean promptForSave;

    private ContentScheme(final String scheme) {
        this.scheme = scheme;
        this.key = null;
        this.temporary = false;
        this.promptForSave = false;
    }

    private ContentScheme(final String scheme, final String key, final boolean promptForSave) {
        this.scheme = scheme;
        this.key = key;
        this.temporary = true;
        this.promptForSave = promptForSave;
    }

    @WorkerThread
    public File loadToCache(final Uri uri, final String extension, final IProgressIndicator progress) throws IOException {
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

    public static File loadFromURL(final Uri uri, final IProgressIndicator progress) throws IOException {
        final URL url = new URL(uri.toString());
        final UIFileCopying ui = new UIFileCopying(R.string.msg_loading_book, 64 * 1024, progress);
        return CacheManager.createTempDocument(url.openStream(), uri.getLastPathSegment(), ui);
    }
}
