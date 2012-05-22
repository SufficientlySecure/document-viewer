package org.ebookdroid.ui.viewer;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;

import android.app.Activity;
import android.content.Context;

import org.emdev.ui.actions.IActionController;
import org.emdev.ui.progress.IProgressIndicator;

public interface IActivityController extends IActionController<ViewerActivity> {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    IView getView();

    IViewController getDocumentController();

    IActionController<?> getActionController();

    ZoomModel getZoomModel();

    DecodingProgressModel getDecodingProgressModel();

    void jumpToPage(int viewIndex, float offsetX, float offsetY, boolean addToHistory);

    @Deprecated
    static interface IBookLoadTask extends IProgressIndicator {
    }

}
