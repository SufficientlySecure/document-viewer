package org.ebookdroid.core.codec;

import android.content.ContentResolver;

public interface CodecContext {

    CodecDocument openDocument(String fileName, String password);

    void setContentResolver(ContentResolver contentResolver);

    void recycle();

    long getContextHandle();
}
