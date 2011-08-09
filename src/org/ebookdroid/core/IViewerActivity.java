package org.ebookdroid.core;

import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.multitouch.MultiTouchZoom;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.BookSettings;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public interface IViewerActivity {

    Context getContext();

    Activity getActivity();

    AppSettings getAppSettings();

    BookSettings getBookSettings();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    View getView();

    IDocumentViewController getDocumentController();

    ZoomModel getZoomModel();

    MultiTouchZoom getMultiTouchZoom();

    DecodingProgressModel getDecodingProgressModel();

    void createDocumentView();
}
