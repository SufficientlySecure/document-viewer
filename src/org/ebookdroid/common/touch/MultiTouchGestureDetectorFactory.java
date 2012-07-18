package org.ebookdroid.common.touch;

import java.lang.reflect.Constructor;

import org.emdev.common.android.AndroidVersion;

public class MultiTouchGestureDetectorFactory {

    public static IGestureDetector create(final IMultiTouchListener mtListener) {
        if (!AndroidVersion.is1x) {
            try {
                final String pkg = MultiTouchGestureDetectorFactory.class.getPackage().getName();
                final Class<?> cls = Class.forName(pkg + ".MultiTouchGestureDetector");
                final Constructor<?> c = cls.getConstructor(IMultiTouchListener.class);
                return ((IGestureDetector) c.newInstance(mtListener));
            } catch (final Exception e) {
            }
        }
        return new DummyGestureDetector();
    }
}
