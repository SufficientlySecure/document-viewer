package org.ebookdroid.pdfdroid.codec;

import android.content.ContentResolver;

import org.ebookdroid.core.VuDroidLibraryLoader;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class PdfContext implements CodecContext
{
    static
    {
        VuDroidLibraryLoader.load();
    }

    public CodecDocument openDocument(String fileName, String password)
    {
        return PdfDocument.openDocument(fileName, password);
    }

    public void setContentResolver(ContentResolver contentResolver)
    {
        //TODO
    }
    
    public void recycle() {
   
    }

	@Override
	public long getContextHandle() {
		return 0;
	}
}
