package org.ebookdroid;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.AppSettings.Diff;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.VMRuntimeHack;
import org.emdev.common.backup.BackupManager;

public class EBookDroidApp extends BaseDroidApp implements IAppSettingsChangeListener {

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        SettingsManager.init(this);
        CacheManager.init(this);

        VMRuntimeHack.preallocateHeap(AppSettings.current().heapPreallocate);

        SettingsManager.addListener(this);
        onAppSettingsChanged(null, AppSettings.current(), null);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        BitmapManager.clear("on Low Memory: ");
    }

    @Override
    public void onAppSettingsChanged(AppSettings oldSettings, AppSettings newSettings, Diff diff) {

        BitmapManager.setPartSize(1 << newSettings.bitmapSize);
        BitmapManager.setUseEarlyRecycling(newSettings.useEarlyRecycling);
        BitmapManager.setUseBitmapHack(newSettings.useBitmapHack);

        BackupManager.setMaxNumberOfAutoBackups(newSettings.maxNumberOfAutoBackups);
    }
}
