package org.ebookdroid.common.settings.listeners;

import org.ebookdroid.common.settings.LibSettings;

public interface ILibSettingsChangeListener {

    void onLibSettingsChanged(LibSettings oldSettings, LibSettings newSettings, LibSettings.Diff diff);

}
