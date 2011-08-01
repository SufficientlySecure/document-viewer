package org.ebookdroid.core.presentation;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.ebookdroid.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileListAdapter extends BaseExpandableListAdapter 
{
    	private final Context context;
    	private final FileFilter filter;
        ArrayList<File> dirsdata = new ArrayList<File>();
        ArrayList< ArrayList<File>> data = new ArrayList<ArrayList<File>>();
        
        
        public FileListAdapter(Context context, FileFilter filter)
        {
            this.context = context;
            this.filter = filter;
        }
        

        public File getChild(int groupPosition, int childPosition) {
            return data.get(groupPosition).get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return data.get(groupPosition).size();
        }
        
        private String getFileSize( long size) 
        {

            if (size > 1073741824) {
                return String.format("%.2f",size / 1073741824.0) + " GB";
            } else if (size > 1048576) {
                return String.format("%.2f",size / 1048576.0) + " MB";
            } else if (size > 1024) {
                return String.format("%.2f",size / 1024.0) + " KB";
            } else {
                return size + " B";
            }

        }
        

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	
        		if (convertView == null) 
        			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
        		final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
        		final File file = getChild(groupPosition, childPosition);
        		final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
        		textView.setText(file.getName());
       
        		imageView.setImageResource(R.drawable.book);
        		final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
        		info.setText(new SimpleDateFormat("dd MMM yyyy").format(file.lastModified()));
                  
        		final TextView fileSize = (TextView) convertView.findViewById(R.id.browserItemfileSize);
        		fileSize.setText(getFileSize(file.length()));
        		return convertView;
        }
        
        
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	
        	if (convertView == null) 
        		convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
            final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
            final File file = getGroup(groupPosition);
            final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
            textView.setText(file.getName());
     
            imageView.setImageResource(R.drawable.folderopen);
            File[] listOfFiles = file.listFiles(filter);
            int books = 0;
            if(listOfFiles!=null)
                for (int i1 = 0; i1 < listOfFiles.length; i1++)
                  	if(!listOfFiles[i1].isDirectory())
                   		books++;

            final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
            info.setText("Books: "+ books);

            return convertView;
        }

        public File getGroup(int groupPosition) {
            return dirsdata.get(groupPosition);
        }

        public int getGroupCount() {
            return dirsdata.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

       

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return false;
        }
        
        public void addFile(File file)
        {
        	if(file.isDirectory())
        		return;
        	File parent = file.getParentFile();
        	if(!dirsdata.contains(parent))
        		dirsdata.add(parent);	
        	int pos = dirsdata.indexOf(parent);
        	if(data.size()<= pos)
        	{
        		ArrayList<File> a= new  ArrayList<File>();
        		data.add(pos,a);
        	}
        	data.get(pos).add(file);
        	Collections.sort(data.get(pos), new Comparator<File>()
          	        {
          	            public int compare(File o1, File o2)
          	            {
          	                if (o1.isDirectory() && o2.isFile()) return -1;
          	                if (o1.isFile() && o2.isDirectory()) return 1;
          	                return o1.getName().compareTo(o2.getName());
          	            }
          	        });
        }
        
        public void clearData()
        {
        	dirsdata.clear();
        	data.clear();
        	notifyDataSetInvalidated();
        }

}
	
