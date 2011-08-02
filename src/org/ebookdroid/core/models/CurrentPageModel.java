package org.ebookdroid.core.models;

import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.EventDispatcher;

import android.util.Log;

public class CurrentPageModel extends EventDispatcher {

    private int currentPageIndex;

    public void setCurrentPageIndex(final int currentPageIndex) {
        if (this.currentPageIndex != currentPageIndex) {
            Log.d("DocModel", "Current page changed: " + this.currentPageIndex + " -> " + currentPageIndex);
            this.currentPageIndex = currentPageIndex;
            dispatch(new CurrentPageListener.CurrentPageChangedEvent(currentPageIndex));
        }
    }

    public int getCurrentPageIndex() {
        return this.currentPageIndex;
    }
}
