package org.ebookdroid.core.hwa;

import android.view.View;

class NewHardwareAcceleration implements IHardwareAcceleration {

    @Override
    public void setMode(View view, boolean accelerated) {
        view.setLayerType(accelerated ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
    }
}
