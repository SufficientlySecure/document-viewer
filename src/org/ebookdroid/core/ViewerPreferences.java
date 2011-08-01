package org.ebookdroid.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class ViewerPreferences
{
    private SharedPreferences sharedPreferences;

    public ViewerPreferences(Context context)
    {
        sharedPreferences = context.getSharedPreferences("ViewerPreferences", 0);
    }

    public void addRecent(Uri uri)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("recent:" + uri.toString(), uri.toString() + "\n" + System.currentTimeMillis());
        editor.commit();
    }
    public void delRecent(Uri uri)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("recent:" + uri.toString());
        editor.commit();
    }

    public List<File> getRecent()
    {
        TreeMap<Long, File> treeMap = new TreeMap<Long, File>();
        for (String key : sharedPreferences.getAll().keySet())
        {
            if (key.startsWith("recent"))
            {
                String uriPlusDate = sharedPreferences.getString(key, null);
                String[] uriThenDate = uriPlusDate.split("\n");
                final File file = new File(URI.create(uriThenDate[0].replaceAll(" ","%20")));
                if(file.exists())
                {
                	treeMap.put(Long.parseLong(uriThenDate.length > 1 ? uriThenDate[1] : "0"), file);
                }
                else
                {
                	delRecent(Uri.fromFile(file));
                }      
            }
        }
        ArrayList<File> list = new ArrayList<File>(treeMap.values());
        Collections.reverse(list);
        return list;
    }
    
    public void clearRecent()
    {
    	SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet())
        {
            if (key.startsWith("recent"))
            	editor.remove(key);
        }
        editor.commit();
    }
}
