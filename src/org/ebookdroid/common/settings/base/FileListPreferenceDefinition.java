package org.ebookdroid.common.settings.base;

import org.ebookdroid.EBookDroidApp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.File;
import java.util.Set;

import org.emdev.utils.StringUtils;

public class FileListPreferenceDefinition extends BasePreferenceDefinition {

    private final String defValue;

    public FileListPreferenceDefinition(final int keyRes, final int defValRes) {
        super(keyRes);
        defValue = EBookDroidApp.context.getString(defValRes);
    }

    public Set<String> getPreferenceValue(final SharedPreferences prefs) {
        if (!prefs.contains(key)) {
            prefs.edit().putString(key, defValue).commit();
        }
        return StringUtils.split(File.pathSeparator, prefs.getString(key, defValue));
    }

    public void setPreferenceValue(final Editor edit, final Set<String> paths) {
        edit.putString(key, StringUtils.merge(File.pathSeparator, paths));
    }
}
