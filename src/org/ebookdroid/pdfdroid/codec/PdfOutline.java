package org.ebookdroid.pdfdroid.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;

import android.util.Log;

public class PdfOutline
{
	private long docHandle;

    public List<OutlineLink> getOutline(long dochandle)
	{
		 List<OutlineLink> ls=new ArrayList<OutlineLink>();
		 docHandle = dochandle;
		 long outline = open(dochandle);
		 ttOutline(ls,outline);
		 free(outline);
		 return ls;
	}
	   
	private void ttOutline(List<OutlineLink> ls, long outline)
	{
		while(outline > 0)
    	{
			
			String title = getTitle(outline);
			String link = getLink(outline, docHandle);
    		if(title != null)
    		{
    			Log.d("PdfOutline", title);
    			ls.add(new OutlineLink(title, link));
    		}
            		
    		long child = getChild(outline);
    		ttOutline(ls,child);

    		outline = getNext(outline);
		 }
	 }
    
    private static native String getTitle(long outlinehandle);
    private static native String getLink(long outlinehandle, long dochandle);
    private static native long getNext(long outlinehandle);
    private static native long getChild(long outlinehandle);
    
	private static native long open(long dochandle);
	private static native void free(long dochandle);
}
