package org.ebookdroid.core.touch;

import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.utils.AndroidVersion;

public interface IMultiTouchZoom extends IGestureDetector {

    public static class Factory {
        public static IMultiTouchZoom createImpl(final ZoomModel zoomModel) {
            if (AndroidVersion.is1x) {
                return new DummyGestureDetector();
            } else {
                try {
                    return ((IMultiTouchZoom) Class.forName(Factory.class.getPackage().getName() + ".MultiTouchZoomImpl")
                            .getConstructor(ZoomModel.class).newInstance(zoomModel));
                } catch (Exception e) {
                    return new DummyGestureDetector();
                }
            }
        }
    }
}
