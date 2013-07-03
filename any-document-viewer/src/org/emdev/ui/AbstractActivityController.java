package org.emdev.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.AbstractComponentController;

public class AbstractActivityController<A extends Activity> extends AbstractComponentController<A> implements
        ActivityEvents {

    private static final AtomicLong SEQ = new AtomicLong();

    public final LogContext LCTX;

    public final long id;

    final int eventMask;

    protected AbstractActivityController(final A activity, final int... events) {
        super(activity);

        id = SEQ.getAndIncrement();
        LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("" + id);

        this.eventMask = ActivityEvents.Helper.merge(events);
    }

    public final A getActivity() {
        return getManagedComponent();
    }

    public final Context getContext() {
        return getManagedComponent();
    }

    public void beforeCreate(final A activity) {
    }

    public void beforeRecreate(final A activity) {
    }

    public void afterCreate(final A activity, final boolean recreated) {
    }

    public void onRestart(final boolean recreated) {
    }

    public void onPostCreate(final Bundle savedInstanceState, final boolean recreated) {
    }

    public void onStart(final boolean recreated) {
    }

    public void onResume(final boolean recreated) {
    }

    public void onPostResume(final boolean recreated) {
    }

    public void onPause(final boolean finishing) {
    }

    public void onStop(final boolean finishing) {
    }

    public void onDestroy(final boolean finishing) {
    }

}
