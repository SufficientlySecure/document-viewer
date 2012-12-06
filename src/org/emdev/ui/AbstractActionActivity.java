package org.emdev.ui;

import org.ebookdroid.R;
import org.ebookdroid.ui.about.AboutActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.actions.ActionMethod;

public abstract class AbstractActionActivity<A extends Activity, C extends AbstractActivityController<A>> extends
        Activity implements ActivityEvents {

    private static final AtomicLong SEQ = new AtomicLong();

    public final LogContext LCTX;

    public final long id;

    final boolean shouldBeTaskRoot;
    final int eventMask;

    protected boolean recreated;
    C controller;

    protected AbstractActionActivity(final boolean shouldBeTaskRoot, final int... events) {
        id = SEQ.getAndIncrement();
        LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("" + id, true);

        this.shouldBeTaskRoot = shouldBeTaskRoot;
        this.eventMask = ActivityEvents.Helper.merge(events);
    }

    @Override
    public final Object onRetainNonConfigurationInstance() {
        return getController();
    }

    public final C getController() {
        if (controller == null) {
            controller = createController();
        }
        return controller;
    }

    protected abstract C createController();

    @Override
    @SuppressWarnings({ "unchecked", "deprecation" })
    protected final void onCreate(final Bundle savedInstanceState) {
        if (shouldBeTaskRoot && !isTaskRoot()) {
            super.onCreate(savedInstanceState);
            // Workaround for Android 2.1-
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onCreate(): close duplicated activity");
            }
            finish();
            return;
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate(): " + savedInstanceState);
        }
        // Check if controller was created before
        final Object last = this.getLastNonConfigurationInstance();
        if (last instanceof AbstractActivityController) {
            this.recreated = true;
            this.controller = (C) last;
            this.controller.setManagedComponent((A) this);
            if (Helper.enabled(this.controller.eventMask, BEFORE_RECREATE)) {
                if (this.controller.LCTX.isDebugEnabled()) {
                    this.controller.LCTX.d("beforeRecreate(): " + this);
                }
                this.controller.beforeRecreate((A) this);
            }
        } else {
            this.recreated = false;
            this.controller = createController();
            if ((this.controller.eventMask & BEFORE_CREATE) == BEFORE_CREATE) {
                if (this.controller.LCTX.isDebugEnabled()) {
                    this.controller.LCTX.d("beforeCreate(): " + this);
                }
                this.controller.beforeCreate((A) this);
            }
        }

        super.onCreate(savedInstanceState);

        if (Helper.enabled(this.eventMask, ON_CREATE)) {
            onCreateImpl(savedInstanceState);
        }

        if (Helper.enabled(this.controller.eventMask, AFTER_CREATE)) {
            if (this.controller.LCTX.isDebugEnabled()) {
                this.controller.LCTX.d("afterCreate(): " + recreated);
            }
            this.controller.afterCreate((A) this, recreated);
        }
    }

    protected void onCreateImpl(final Bundle savedInstanceState) {
    }

    @Override
    protected final void onRestart() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onRestart()");
        }

        super.onRestart();

        if (Helper.enabled(this.eventMask, ON_RESTART)) {
            onRestartImpl();
        }

        if (Helper.enabled(this.controller.eventMask, ON_RESTART)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onRestart(): " + recreated);
            }
            controller.onRestart(recreated);
        }
    }

    protected void onRestartImpl() {
    }

    @Override
    protected final void onStart() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onStart()");
        }

        super.onStart();

        if (Helper.enabled(this.eventMask, ON_START)) {
            onStartImpl();
        }

        if (Helper.enabled(this.controller.eventMask, ON_START)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onStart(): " + recreated);
            }
            controller.onStart(recreated);
        }
    }

    protected void onStartImpl() {
    }

    @Override
    protected final void onPostCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate(): " + savedInstanceState);
        }

        super.onPostCreate(savedInstanceState);

        if (Helper.enabled(this.eventMask, ON_POST_CREATE)) {
            onPostCreateImpl(savedInstanceState);
        }
        if (Helper.enabled(this.controller.eventMask, ON_POST_CREATE)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("savedInstanceState(): " + savedInstanceState + ", " + recreated);
            }
            controller.onPostCreate(savedInstanceState, recreated);
        }
    }

    protected void onPostCreateImpl(final Bundle savedInstanceState) {
    }

    @Override
    protected final void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }

        super.onResume();

        if (Helper.enabled(this.eventMask, ON_RESUME)) {
            onResumeImpl();
        }

        if (Helper.enabled(this.controller.eventMask, ON_RESUME)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onResume(): " + recreated);
            }
            controller.onResume(recreated);
        }
    }

    protected void onResumeImpl() {
    }

    @Override
    protected final void onPostResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPostResume()");
        }

        super.onPostResume();

        if (Helper.enabled(this.eventMask, ON_POST_RESUME)) {
            onPostResumeImpl();
        }

        if (Helper.enabled(this.controller.eventMask, ON_POST_RESUME)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onPostResume(): " + recreated);
            }
            controller.onPostResume(recreated);
        }
    }

    protected void onPostResumeImpl() {
    }

    @Override
    protected final void onPause() {
        final boolean finishing = isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + finishing);
        }

        super.onPause();

        if (Helper.enabled(this.eventMask, ON_PAUSE)) {
            onPauseImpl(finishing);
        }

        if (Helper.enabled(this.controller.eventMask, ON_PAUSE)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onPause(): " + finishing);
            }
            controller.onPause(finishing);
        }
    }

    protected void onPauseImpl(final boolean finishing) {
    }

    @Override
    protected final void onStop() {
        final boolean finishing = isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onStop(): " + finishing);
        }

        super.onStop();

        if (Helper.enabled(this.eventMask, ON_STOP)) {
            onStopImpl(finishing);
        }

        if (Helper.enabled(this.controller.eventMask, ON_STOP)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onStop(): " + finishing);
            }
            controller.onStop(finishing);
        }
    }

    protected void onStopImpl(final boolean finishing) {
    }

    @Override
    protected final void onDestroy() {
        if (shouldBeTaskRoot && !isTaskRoot()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onDestroy(): close duplicated activity");
            }
            super.onDestroy();
            return;
        }

        final boolean finishing = isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + finishing);
        }

        super.onDestroy();

        if (Helper.enabled(this.eventMask, ON_DESTROY)) {
            onDestroyImpl(finishing);
        }

        if (Helper.enabled(this.controller.eventMask, ON_DESTROY)) {
            if (controller.LCTX.isDebugEnabled()) {
                controller.LCTX.d("onDestroy(): " + finishing);
            }
            controller.onDestroy(finishing);
        }
    }

    protected void onDestroyImpl(final boolean finishing) {
    }

    @Override
    public final boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu != null) {
            updateMenuItems(menu);
        }
        return true;
    }

    protected void updateMenuItems(final Menu menu) {
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (onMenuItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        if (onMenuItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    protected boolean onMenuItemSelected(final MenuItem item) {
        final int actionId = item.getItemId();
        final ActionEx action = getController().getOrCreateAction(actionId);
        if (action.getMethod().isValid()) {
            ActionMenuHelper.setActionParameters(item, action);
            action.run();
            return true;
        }
        return false;
    }

    public final void onButtonClick(final View view) {
        final int actionId = view.getId();
        final ActionEx action = getController().getOrCreateAction(actionId);
        action.onClick(view);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (data != null) {
            final int actionId = data.getIntExtra(ActionMenuHelper.ACTIVITY_RESULT_ACTION_ID, 0);
            if (actionId != 0) {
                final ActionEx action = getController().getOrCreateAction(actionId);
                action.putValue(ActionMenuHelper.ACTIVITY_RESULT_CODE, Integer.valueOf(resultCode));
                action.putValue(ActionMenuHelper.ACTIVITY_RESULT_DATA, data);
                action.run();
            }
        }
    }

    public final void setActionForView(final int id) {
        final View view = findViewById(id);
        final ActionEx action = getController().getOrCreateAction(id);
        if (view != null && action != null) {
            view.setOnClickListener(action);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_about)
    public void showAbout(final ActionEx action) {
        final Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }
}
