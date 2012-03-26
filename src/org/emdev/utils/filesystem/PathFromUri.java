package org.emdev.utils.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class PathFromUri {

    public static String retrieve(final ContentResolver resolver, final Uri uri) {
        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        final Cursor cursor = resolver.query(uri, new String[] { "_data" }, null, null, null);
        if ((cursor != null) && cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        throw new RuntimeException("Can't retrieve path from uri: " + uri.toString());
    }
}
