package org.ebookdroid.core;

import org.ebookdroid.core.actions.IActionController;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.touch.IMultiTouchListener;

import android.app.Activity;
import android.content.Context;

public interface IViewerActivity {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    IDocumentView getView();

    IDocumentViewController getDocumentController();

    IActionController<?> getActionController();

    ZoomModel getZoomModel();

    IMultiTouchListener getMultiTouchListener();

    DecodingProgressModel getDecodingProgressModel();

    IDocumentViewController switchDocumentController();

    static interface IBookLoadTask {

        void setProgressDialogMessage(int resourceID, Object... args);

    }

}
