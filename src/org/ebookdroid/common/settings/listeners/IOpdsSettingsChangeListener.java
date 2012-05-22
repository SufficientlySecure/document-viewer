package org.ebookdroid.common.settings.listeners;

import org.ebookdroid.common.settings.OpdsSettings;

public interface IOpdsSettingsChangeListener {

    void onOpdsSettingsChanged(OpdsSettings oldSettings, OpdsSettings newSettings, OpdsSettings.Diff diff);

}
