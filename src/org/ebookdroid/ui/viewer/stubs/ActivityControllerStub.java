package org.ebookdroid.ui.viewer.stubs;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.app.Activity;
import android.content.Context;

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.IActionController;

public class ActivityControllerStub extends ActionController<ViewerActivity> implements IActivityController {

    public static final ActivityControllerStub STUB = new ActivityControllerStub();

    public static final DocumentModel MODEL_STUB = new DocumentModel(null);

    private ActivityControllerStub() {
        super(null, null);
    }

    @Override
    public Context getContext() {
        return EBookDroidApp.context;
    }

    @Override
    public Activity getActivity() {
        return null;
    }

    @Override
    public DecodeService getDecodeService() {
        return null;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return MODEL_STUB;
    }

    @Override
    public IView getView() {
        return ViewStub.STUB;
    }

    @Override
    public IViewController getDocumentController() {
        return ViewContollerStub.STUB;
    }

    @Override
    public IActionController<?> getActionController() {
        return null;
    }

    @Override
    public ZoomModel getZoomModel() {
        return null;
    }

    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return null;
    }

    @Override
    public IViewController switchDocumentController() {
        return null;
    }

    @Override
    public void jumpToPage(final int viewIndex, final float offsetX, final float offsetY) {
    }
}
