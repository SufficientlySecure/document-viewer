package org.ebookdroid.core;

import org.ebookdroid.core.actions.ActionController;
import org.ebookdroid.core.actions.ActionEx;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;

public abstract class AbstractActionActivity extends Activity {

    protected final ActionController<AbstractActionActivity> actions;

    protected AbstractActionActivity() {
        actions = new ActionController<AbstractActionActivity>(this, this);
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        final int actionId = item.getItemId();
        final ActionEx action = actions.getOrCreateAction(actionId);
        if (action.getMethod().isValid()) {
            action.run();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public final void onButtonClick(final View view) {
        final int actionId = view.getId();
        final ActionEx action = actions.getOrCreateAction(actionId);
        action.onClick(view);
    }

    public final void setActionForView(int id) {
        View view = findViewById(id);
        ActionEx action = actions.getOrCreateAction(id);
        if (view != null && action != null) {
            view.setOnClickListener(action);
        }
    }
}
