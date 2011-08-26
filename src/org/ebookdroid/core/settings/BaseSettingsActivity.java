package org.ebookdroid.core.settings;

import org.ebookdroid.utils.LengthUtils;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import java.util.Arrays;

public class BaseSettingsActivity extends PreferenceActivity {

    protected void decoratePreferences(final String... keys) {
        for (final String key : keys) {
            decoratePreference(findPreference(key));
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
        final String value = textPrefs.getText();

        setPreferenceSummary(textPrefs, summary, value);
        textPrefs.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                setPreferenceSummary(textPrefs, summary, (String) newValue);
                return true;
            }
        });
    }

    protected void decorateListPreference(final ListPreference listPrefs) {
        final CharSequence summary = listPrefs.getSummary();
        final String value = listPrefs.getValue();

        setListPreferenceSummary(listPrefs, summary, value);
        listPrefs.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                setListPreferenceSummary(listPrefs, summary, (String) newValue);
                return true;
            }
        });
    }

    protected void setListPreferenceSummary(final ListPreference listPrefs, final CharSequence summary,
            final String value) {
        final int selected = Arrays.asList(listPrefs.getEntryValues()).indexOf(value);
        setPreferenceSummary(listPrefs, summary, selected != -1 ? (String) listPrefs.getEntries()[selected] : null);
    }

    protected void setPreferenceSummary(final Preference listPrefs, final CharSequence summary, final String value) {
        listPrefs.setSummary(summary + (LengthUtils.isNotEmpty(value) ? (": " + value) : ""));
    }

}
