package org.ebookdroid.core.codec;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.graphics.Bitmap;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractCodecContext implements CodecContext {

    private static final AtomicLong SEQ = new AtomicLong();

    private static Integer densityDPI;

    final int supportedFeatures;

    private long contextHandle;

    /**
     * Constructor.
     *
     * @param supportedFeatures
     *            supported features list
     */
    protected AbstractCodecContext(final int supportedFeatures) {
        this(SEQ.incrementAndGet(), supportedFeatures);
    }

    /**
     * Constructor.
     *
     * @param contextHandle
     *            contect handler
     */
    protected AbstractCodecContext(final long contextHandle, final int supportedFeatures) {
        this.contextHandle = contextHandle;
        this.supportedFeatures = supportedFeatures;
    }

    @Override
    protected final void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#recycle()
     */
    @Override
    public final void recycle() {
        if (!isRecycled()) {
            freeContext();
            contextHandle = 0;
        }
    }

    protected void freeContext() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#isRecycled()
     */
    @Override
    public final boolean isRecycled() {
        return contextHandle == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getContextHandle()
     */
    @Override
    public final long getContextHandle() {
        return contextHandle;
    }

    @Override
    public boolean isFeatureSupported(final int feature) {
        return (supportedFeatures & feature) != 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getBitmapConfig()
     */
    @Override
    public Bitmap.Config getBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }

    public static int getWidthInPixels(final float pdfWidth) {
        return getSizeInPixels(pdfWidth, AppSettings.current().getXDpi(ViewerActivity.DM.xdpi));
    }

    public static int getHeightInPixels(final float pdfHeight) {
        return getSizeInPixels(pdfHeight, AppSettings.current().getYDpi(ViewerActivity.DM.ydpi));
    }

    public static int getSizeInPixels(final float pdfHeight, float dpi) {
        if (dpi == 0) {
            // Archos fix
            dpi = getDensityDPI();
        }
        if (dpi < 72) { // Density lover then 72 is to small
            dpi = 72; // Set default density to 72
        }
        return (int) (pdfHeight * dpi / 72);
    }

    private static int getDensityDPI() {
        if (densityDPI == null) {
            try {
                final Field f = ViewerActivity.DM.getClass().getDeclaredField("densityDpi");
                densityDPI = ((Integer) f.get(ViewerActivity.DM));
            } catch (final Throwable ex) {
                densityDPI = Integer.valueOf(120);
            }
        }
        return densityDPI.intValue();
    }
}
