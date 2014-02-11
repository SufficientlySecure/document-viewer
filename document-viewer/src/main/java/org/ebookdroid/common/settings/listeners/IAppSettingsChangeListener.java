package org.ebookdroid.common.settings.listeners;

import org.ebookdroid.common.settings.AppSettings;

public interface IAppSettingsChangeListener {

    void onAppSettingsChanged(AppSettings oldSettings, AppSettings newSettings, AppSettings.Diff diff);

}
