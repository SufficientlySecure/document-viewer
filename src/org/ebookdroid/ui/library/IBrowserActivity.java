package org.ebookdroid.ui.library;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;

public interface IBrowserActivity {

    Context getContext();

    Activity getActivity();

    void setCurrentDir(File newDir);

    void showDocument(Uri uri);

    void showProgress(final boolean show);

    void loadThumbnail(String path, ImageView imageView, int defaultResID);
}
