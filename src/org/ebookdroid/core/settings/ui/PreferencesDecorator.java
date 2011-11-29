package org.ebookdroid.core.settings.ui;

import org.ebookdroid.core.DecodeMode;
import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.AndroidVersion;
import org.ebookdroid.utils.LengthUtils;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author whippet
 * 
 */
public class PreferencesDecorator implements IPreferenceContainer {

    private final Map<String, CharSequence> summaries = new HashMap<String, CharSequence>();

    private final Map<String, CompositeListener> listeners = new HashMap<String, CompositeListener>();

    private final IPreferenceContainer parent;

    public PreferencesDecorator(final IPreferenceContainer parent) {
        this.parent = parent;
    }

    @Override
    public Preference findPreference(final CharSequence key) {
        return parent.findPreference(key);
    }

    public void decorateSettings() {
        decorateBooksSettings();
        decorateBrowserSettings();
        decorateMemorySettings();
        decorateRenderSettings();
        decorateScrollSettings();
        decorateUISettings();
    }

    public void decorateBooksSettings() {
        decoratePreferences("book_align", "book_animationType");
        addAnimationTypeListener("book_animationType", "book_align");
    }

    public void decorateBrowserSettings() {
        decoratePreferences("brautoscandir");
    }

    public void decorateMemorySettings() {
        decoratePreferences("pagesinmemory", "decodemode", "maximagesize");

        addListener("decodemode", new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                enableMaxImageSizePref(DecodeMode.getByResValue(newValue.toString()));
                return true;
            }
        });

        enableMaxImageSizePref(SettingsManager.getAppSettings().getDecodeMode());
    }

    protected void enableMaxImageSizePref(final DecodeMode decodeMode) {
        final Preference pref = findPreference("maximagesize");
        pref.setEnabled(DecodeMode.LOW_MEMORY == decodeMode);
    }

    public void decorateRenderSettings() {
        decoratePreferences("align", "animationType");
        addAnimationTypeListener("animationType", "align");

        decoratePreferences("djvu_rendering_mode");
    }

    public void decorateScrollSettings() {
        decoratePreferences("tapsize", "scrollheight");
    }

    public void decorateUISettings() {
        decoratePreferences("rotation", "brightness");
        findPreference("fullscreen").setEnabled(!AndroidVersion.is3x);
    }

    protected void decoratePreferences(final String... keys) {
        for (final String key : keys) {
            decoratePreference(parent.findPreference(key));
        }
    }

    protected void decoratePreference(final Preference pref) {
        if (pref instanceof ListPreference) {
            decorateListPreference((ListPreference) pref);
        } else if (pref instanceof EditTextPreference) {
            decorateEditPreference((EditTextPreference) pref);
        }
    }

    protected void decorateEditPreference(final EditTextPreference textPrefs) {
        final CharSequence summary = textPrefs.getSummary();
        summaries.put(textPrefs.getKey(), summary);

        final String value = textPrefs.getText();

        setPreferenceSummary(textPrefs, value);

        addListener(textPrefs, new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                setPreferenceSummary(textPrefs, (String) newValue);
                return true;
            }
        });
    }

    protected void decorateListPreference(final ListPreference listPrefs) {
        final CharSequence summary = listPrefs.getSummary();
        summaries.put(listPrefs.getKey(), summary);

        final String value = listPrefs.getValue();

        setListPreferenceSummary(listPrefs, value);

        addListener(listPrefs, new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                setListPreferenceSummary(listPrefs, (String) newValue);
                return true;
            }
        });
    }

    protected void setListPreferenceSummary(final ListPreference listPrefs, final String value) {
        final int selected = Arrays.asList(listPrefs.getEntryValues()).indexOf(value);
        setPreferenceSummary(listPrefs, selected != -1 ? (String) listPrefs.getEntries()[selected] : null);
    }

    protected void setPreferenceSummary(final Preference listPrefs, final String value) {
        final CharSequence summary = summaries.get(listPrefs.getKey());
        listPrefs.setSummary(summary + (LengthUtils.isNotEmpty(value) ? (": " + value) : ""));
    }

    protected void addListener(final String key, final OnPreferenceChangeListener l) {
        final Preference pref = parent.findPreference(key);
        if (pref != null) {
            addListener(pref, l);
        }
    }

    protected void addListener(final Preference pref, final OnPreferenceChangeListener l) {
        final String key = pref.getKey();
        CompositeListener cl = listeners.get(key);
        if (cl == null) {
            cl = new CompositeListener();
            pref.setOnPreferenceChangeListener(cl);
            listeners.put(key, cl);
        }
        cl.add(l);
    }

    protected void addAnimationTypeListener(final String source, final String target) {
        addListener(source, new AnimationTypeListener(target));
    }

    protected static class CompositeListener implements OnPreferenceChangeListener {

        final List<OnPreferenceChangeListener> listeners = new LinkedList<Preference.OnPreferenceChangeListener>();

        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            for (final OnPreferenceChangeListener l : listeners) {
                if (!l.onPreferenceChange(preference, newValue)) {
                    return false;
                }
            }
            return true;
        }

        public boolean add(OnPreferenceChangeListener object) {
            return listeners.add(object);
        }
    }

    protected class AnimationTypeListener implements OnPreferenceChangeListener {

        private final String relatedKey;

        public AnimationTypeListener(final String relatedKey) {
            this.relatedKey = relatedKey;
        }

        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final PageAnimationType type = PageAnimationType.get(newValue.toString());
            if (type != null && type != PageAnimationType.NONE) {
                final ListPreference alignPref = (ListPreference) findPreference(relatedKey);
                alignPref.setValue(PageAlign.AUTO.getResValue());
                setListPreferenceSummary(alignPref, alignPref.getValue());
            }
            return true;
        }
    }

}
