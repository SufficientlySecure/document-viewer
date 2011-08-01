package org.ebookdroid.core.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.ebookdroid.R;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.*;

public class BrowserAdapter extends BaseAdapter
{
    private final Context context;
    private File currentDirectory;
    private List<File> files = Collections.emptyList();
    private final FileFilter filter;

    public BrowserAdapter(Context context, FileFilter filter)
    {
        this.context = context;
        this.filter = filter;
    }

    public int getCount()
    {
        return files.size();
    }

    public File getItem(int i)
    {
        return files.get(i);
    }

    public long getItemId(int i)
    {
        return i;
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

    public View getView(int i, View view, ViewGroup viewGroup)
    {
        final View browserItem = LayoutInflater.from(context).inflate(R.layout.browseritem, viewGroup, false);
        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
        final File file = files.get(i);
        final TextView textView = (TextView) browserItem.findViewById(R.id.browserItemText);
        textView.setText(file.getName());
 
        if (currentDirectory != null && file.equals(currentDirectory.getParentFile()))
        {
            imageView.setImageResource(R.drawable.arrowup);
            textView.setText(file.getAbsolutePath());
        }
        else if (file.isDirectory())
        {
            imageView.setImageResource(R.drawable.folderopen);
            File[] listOfFiles = file.listFiles(filter);
            int folders = 0;
            int books = 0;
            if(listOfFiles!=null)
                for (int i1 = 0; i1 < listOfFiles.length; i1++)
                	if(listOfFiles[i1].isDirectory())
                		folders++;
                	else
                		books++;

            final TextView info = (TextView) browserItem.findViewById(R.id.browserItemInfo);
            info.setText("Folders: "+ folders + " Books: "+ books);
        }
        else
        {
            imageView.setImageResource(R.drawable.book);
            final TextView info = (TextView) browserItem.findViewById(R.id.browserItemInfo);
            info.setText(new SimpleDateFormat("dd MMM yyyy").format(file.lastModified()));
            
            final TextView fileSize = (TextView) browserItem.findViewById(R.id.browserItemfileSize);
            fileSize.setText(getFileSize(file.length()));
        }
        return browserItem;
    }

    public void setCurrentDirectory(File currentDirectory)
    {
        final File[] fileArray = currentDirectory.listFiles(filter);
        ArrayList<File> files = new ArrayList<File>(fileArray != null ? Arrays.asList(fileArray) : Collections.<File>emptyList());
        this.currentDirectory = currentDirectory;
        Collections.sort(files, new Comparator<File>()
        {
            public int compare(File o1, File o2)
            {
                if (o1.isDirectory() && o2.isFile()) return -1;
                if (o1.isFile() && o2.isDirectory()) return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (currentDirectory.getParentFile() != null)
        {
            files.add(0, currentDirectory.getParentFile());
        }
        setFiles(files);
    }

    public void setFiles(List<File> files)
    {
        this.files = files;
        notifyDataSetInvalidated();
    }

    public File getCurrentDirectory()
    {
        return currentDirectory;
    }
}
