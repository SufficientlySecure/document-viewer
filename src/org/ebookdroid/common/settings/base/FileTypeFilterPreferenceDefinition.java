package org.ebookdroid.common.settings.base;

import org.ebookdroid.CodecType;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.emdev.utils.filesystem.FileExtensionFilter;

public class FileTypeFilterPreferenceDefinition {

    public final String prefix;
    public final Map<String, String> keys;

    public FileTypeFilterPreferenceDefinition(final String prefix) {
        this.prefix = prefix;
        Set<String> allExtensions = CodecType.getAllExtensions();
        keys = new LinkedHashMap<String, String>();
        for (final String ext : allExtensions) {
            keys.put(ext, prefix + ext);
        }
    }

    public FileExtensionFilter getPreferenceValue(final SharedPreferences prefs) {
        final Set<String> res = new HashSet<String>();
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            final String ext = entry.getKey();
            final String key = entry.getValue();

            if (!prefs.contains(key)) {
                prefs.edit().putBoolean(key, true).commit();
            }

            if (prefs.getBoolean(key, true)) {
                res.add(ext);
            }
        }
        return new FileExtensionFilter(res);
    }
}
