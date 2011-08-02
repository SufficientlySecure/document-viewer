package org.ebookdroid.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class ViewerPreferences {

    private final SharedPreferences sharedPreferences;

    public ViewerPreferences(final Context context) {
        sharedPreferences = context.getSharedPreferences("ViewerPreferences", 0);
    }

    public void addRecent(final Uri uri) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("recent:" + uri.toString(), uri.toString() + "\n" + System.currentTimeMillis());
        editor.commit();
    }

    public void delRecent(final Uri uri) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("recent:" + uri.toString());
        editor.commit();
    }

    public List<File> getRecent() {
        final TreeMap<Long, File> treeMap = new TreeMap<Long, File>();
        for (final String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("recent")) {
                final String uriPlusDate = sharedPreferences.getString(key, null);
                final String[] uriThenDate = uriPlusDate.split("\n");
                final File file = new File(URI.create(uriThenDate[0].replaceAll(" ", "%20")));
                if (file.exists()) {
                    treeMap.put(Long.parseLong(uriThenDate.length > 1 ? uriThenDate[1] : "0"), file);
                } else {
                    delRecent(Uri.fromFile(file));
                }
            }
        }
        final ArrayList<File> list = new ArrayList<File>(treeMap.values());
        Collections.reverse(list);
        return list;
    }

    public void clearRecent() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        for (final String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("recent")) {
                editor.remove(key);
            }
        }
        editor.commit();
    }
}
