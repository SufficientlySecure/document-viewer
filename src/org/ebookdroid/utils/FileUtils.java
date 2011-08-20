package org.ebookdroid.utils;

import java.text.SimpleDateFormat;


public class FileUtils {


    public static final String getFileSize(final long size) {

        if (size > 1073741824) {
            return String.format("%.2f", size / 1073741824.0) + " GB";
        } else if (size > 1048576) {
            return String.format("%.2f", size / 1048576.0) + " MB";
        } else if (size > 1024) {
            return String.format("%.2f", size / 1024.0) + " KB";
        } else {
            return size + " B";
        }

    }
    
    public static final String getFileDate(final long time)
    {
        return new SimpleDateFormat("dd MMM yyyy").format(time);
    }
}
