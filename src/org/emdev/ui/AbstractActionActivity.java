package org.emdev.ui;

import org.ebookdroid.R;
import org.ebookdroid.ui.about.AboutActivity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionParameter;

public abstract class AbstractActionActivity<A extends Activity, C extends ActionController<A>> extends Activity {

    public static final String MENU_ITEM_SOURCE = "source";
    public static final String ACTIVITY_RESULT_DATA = "activityResultData";
    public static final String ACTIVITY_RESULT_CODE = "activityResultCode";
    public static final String ACTIVITY_RESULT_ACTION_ID = "activityResultActionId";

    private C controller;

    protected AbstractActionActivity() {
    }

    @Override
    public final Object onRetainNonConfigurationInstance() {
        return getController();
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public final C restoreController() {
        final Object last = this.getLastNonConfigurationInstance();
        if (last instanceof ActionController) {
            this.controller = (C) last;
            return controller;
        }
        return null;
    }

    public final C getController() {
        if (controller == null) {
            controller = createController();
        }
        return controller;
    }

    protected abstract C createController();

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        final int actionId = item.getItemId();
        final ActionEx action = getController().getOrCreateAction(actionId);
        if (action.getMethod().isValid()) {
            action.run();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setMenuSource(final Menu menu, final Object source) {
        for (int i = 0, n = menu.size(); i < n; i++) {
            final MenuItem item = menu.getItem(i);
            final SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                setMenuSource(subMenu, source);
            } else {
                final int itemId = item.getItemId();
                getController().getOrCreateAction(itemId).putValue(MENU_ITEM_SOURCE, source);
            }
        }
    }

    protected void setMenuParameters(final Menu menu, final IActionParameter... parameters) {
        for (int i = 0, n = menu.size(); i < n; i++) {
            final MenuItem item = menu.getItem(i);
            final SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                setMenuParameters(subMenu, parameters);
            } else {
                final int itemId = item.getItemId();
                final ActionEx action = getController().getOrCreateAction(itemId);
                for (final IActionParameter p : parameters) {
                    action.addParameter(p);
                }
            }
        }
    }

    protected void setMenuItemVisible(final Menu menu, final boolean visible, final int viewId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setVisible(visible);
        }
    }

    protected void setMenuItemEnabled(final Menu menu, final boolean enabled, final int viewId, final int enabledResId,
            final int disabledResId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setIcon(enabled ? enabledResId : disabledResId);
            v.setEnabled(enabled);
        }
    }

    protected void setMenuItemChecked(final Menu menu, final boolean checked, final int viewId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setChecked(checked);
        }
    }

    protected void setMenuItemChecked(final Menu menu, final boolean checked, final int viewId, final int checkedResId,
            final int uncheckedResId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setChecked(checked);
            v.setIcon(checked ? checkedResId : uncheckedResId);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final int actionId = item.getItemId();
        final ActionEx action = getController().getOrCreateAction(actionId);
        if (action.getMethod().isValid()) {
            action.run();
            return true;
        }
        return super.onContextItemSelected(item);
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
            final int actionId = data.getIntExtra(ACTIVITY_RESULT_ACTION_ID, 0);
            if (actionId != 0) {
                final ActionEx action = getController().getOrCreateAction(actionId);
                action.putValue(ACTIVITY_RESULT_CODE, Integer.valueOf(resultCode));
                action.putValue(ACTIVITY_RESULT_DATA, data);
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
