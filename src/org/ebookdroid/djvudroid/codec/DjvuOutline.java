package org.ebookdroid.djvudroid.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.OutlineLink;

import android.util.Log;

public class DjvuOutline {
		private long docHandle;
	   
	   public List<OutlineLink> getOutline(long dochandle)
	   {
		   List<OutlineLink> ls=new ArrayList<OutlineLink>();
		   docHandle = dochandle;
		   long expr = open(docHandle);
		   ttOutline(ls,expr);
		   return ls;
	   }
	   
	   private void ttOutline(List<OutlineLink> ls, long expr)
	   {
		   while(expConsp(expr)) 
		   {
	    		String title = getTitle(expr);
	    		String link = getLink(expr, docHandle);
	    		if(title != null)
	    		{
	    			Log.d("DjvuOutline", title);
	    			ls.add(new OutlineLink(title, link));
	    		}
	    		long child = getChild(expr);
	    		ttOutline(ls, child);

	    		expr = getNext(expr);
		   }
	   
	   }   

	private static native long open(long dochandle);
	private static native boolean expConsp(long expr);
	
    private static native String getTitle(long expr);
    private static native String getLink(long expr, long dochandle);
    private static native long getNext(long expr);
    private static native long getChild(long expr);
	
}
