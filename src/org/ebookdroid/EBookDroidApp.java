package org.ebookdroid;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.VMRuntimeHack;

public class EBookDroidApp extends BaseDroidApp {

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
}
