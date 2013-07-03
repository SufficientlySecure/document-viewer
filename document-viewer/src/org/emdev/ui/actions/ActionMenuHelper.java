package org.emdev.ui.actions;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.io.Serializable;

public class ActionMenuHelper {

    public static final String MENU_ITEM_SOURCE = "source";
    public static final String ACTIVITY_RESULT_DATA = "activityResultData";
    public static final String ACTIVITY_RESULT_CODE = "activityResultCode";
    public static final String ACTIVITY_RESULT_ACTION_ID = "activityResultActionId";

    public static void setActionParameters(final MenuItem item, final ActionEx action) {
        final Intent intent = item.getIntent();
        final Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) {
            for (final String key : extras.keySet()) {
                final ExtraWrapper w = (ExtraWrapper) extras.getSerializable(key);
                action.putValue(key, w != null ? w.data : null);
            }
        }
    }

    public static void setMenuSource(final IActionController<?> c, final Menu menu, final Object source) {
        for (int i = 0, n = menu.size(); i < n; i++) {
            final MenuItem item = menu.getItem(i);
            final SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                setMenuSource(c, subMenu, source);
            } else {
                setMenuItemSource(c, item, source);
            }
        }
    }

    public static void setMenuItemSource(final IActionController<?> c, final MenuItem item, final Object source) {
        final int itemId = item.getItemId();
        c.getOrCreateAction(itemId).putValue(MENU_ITEM_SOURCE, source);
    }

    public static void setMenuItemExtra(final Menu menu, final int itemId, final String name, final Object data) {
        final MenuItem item = menu.findItem(itemId);
        if (item != null) {
            setMenuItemExtra(item, name, data);
        }
    }

    public static void setMenuItemExtra(final MenuItem item, final String name, final Object data) {
        Intent intent = item.getIntent();
        if (intent == null) {
            intent = new Intent();
            item.setIntent(intent);
        }
        intent.putExtra(name, new ExtraWrapper(data));
    }

    public static void setMenuParameters(final IActionController<?> c, final Menu menu,
            final IActionParameter... parameters) {
        for (int i = 0, n = menu.size(); i < n; i++) {
            final MenuItem item = menu.getItem(i);
            final SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                setMenuParameters(c, subMenu, parameters);
            } else {
                final int itemId = item.getItemId();
                final ActionEx action = c.getOrCreateAction(itemId);
                for (final IActionParameter p : parameters) {
                    action.addParameter(p);
                }
            }
        }
    }

    public static void setMenuItemVisible(final Menu menu, final boolean visible, final int viewId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setVisible(visible);
        }
    }

    public static void setMenuItemEnabled(final Menu menu, final boolean enabled, final int viewId,
            final int enabledResId, final int disabledResId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setIcon(enabled ? enabledResId : disabledResId);
            v.setEnabled(enabled);
        }
    }

    public static void setMenuItemChecked(final Menu menu, final boolean checked, final int viewId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setChecked(checked);
        }
    }

    public static void setMenuItemChecked(final Menu menu, final boolean checked, final int viewId,
            final int checkedResId, final int uncheckedResId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setChecked(checked);
            v.setIcon(checked ? checkedResId : uncheckedResId);
        }
    }

    private static final class ExtraWrapper implements Serializable {

        /**
         * Serial version UID
         */
        private static final long serialVersionUID = -5109930164496309305L;

        public Object data;

        private ExtraWrapper(final Object data) {
            super();
            this.data = data;
        }
    }

}
