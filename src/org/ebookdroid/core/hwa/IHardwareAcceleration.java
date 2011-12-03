package org.ebookdroid.core.hwa;

import org.ebookdroid.core.utils.AndroidVersion;

import android.view.View;

public interface IHardwareAcceleration {

    public void setMode(View view, boolean accelerated);

    public class Factory {

        private static final IHardwareAcceleration instance = AndroidVersion.lessThan3x ? new OldHardwareAcceleration()
                : new NewHardwareAcceleration();

        public static IHardwareAcceleration getInstance() {
            return instance;
        }
    }
}
