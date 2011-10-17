package org.ebookdroid.core.multitouch;

import org.ebookdroid.core.touch.IGestureDetector;

public interface MultiTouchZoom extends IGestureDetector {

    boolean isResetLastPointAfterZoom();

    void setResetLastPointAfterZoom(boolean resetLastPointAfterZoom);
}
