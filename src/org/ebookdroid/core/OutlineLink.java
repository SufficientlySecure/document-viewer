package org.ebookdroid.core;

public class OutlineLink implements CharSequence {
	
	private final String title;
	private final String link;
	
	
	public OutlineLink(String t, String l)
	{
		title = t;
		link = l;
	}
	
	
	@Override
	public char charAt(int index) {
		return title.charAt(index);
	}

	@Override
	public int length() {
		return title.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return title.subSequence(start, end);
	}
	
	public String toString() 
	{
	    return title;
	}
	
	public String getLink()
	{
		return link;
	}

}
