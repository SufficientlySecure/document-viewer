package org.ebookdroid.xpsdroid.codec;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import android.content.ContentResolver;

public class XpsContext implements CodecContext {

    static {
        VuDroidLibraryLoader.load();
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return XpsDocument.openDocument(fileName);
    }

    @Override
    public void setContentResolver(final ContentResolver contentResolver) {
        // TODO
    }

    @Override
    public void recycle() {

    }

    @Override
    public long getContextHandle() {
        return 0;
    }
}
