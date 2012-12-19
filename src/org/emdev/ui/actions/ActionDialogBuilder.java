package org.emdev.ui.actions;

import org.ebookdroid.R;

import android.app.AlertDialog;
import android.content.Context;

import org.emdev.utils.LengthUtils;

public class ActionDialogBuilder extends AlertDialog.Builder {

    private final IActionController<?> actions;
    private final Context context;

    public ActionDialogBuilder(final Context context, final IActionController<?> actions) {
        super(context);
        this.actions = actions;
        this.context = context;
    }

    public ActionDialogBuilder setPositiveButton(final int resId, final int actionId, final IActionParameter... params) {
        final ActionEx action = actions.getOrCreateAction(actionId);
        for (final IActionParameter ap : params) {
            action.addParameter(ap);
        }
        super.setPositiveButton(resId, action);
        return this;
    }

    public ActionDialogBuilder setPositiveButton(final int actionId, final IActionParameter... params) {
        final ActionEx action = actions.getOrCreateAction(actionId);
        for (final IActionParameter ap : params) {
            action.addParameter(ap);
        }
        super.setPositiveButton(android.R.string.ok, action);
        return this;
    }

    public ActionDialogBuilder setNegativeButton() {
        super.setNegativeButton(android.R.string.cancel, actions.getOrCreateAction(R.id.actions_no_action));
        return this;
    }

    public ActionDialogBuilder setNegativeButton(int resId) {
        super.setNegativeButton(resId, actions.getOrCreateAction(R.id.actions_no_action));
        return this;
    }

    public ActionDialogBuilder setNegativeButton(final int resId, final int actionId, final IActionParameter... params) {
        final ActionEx action = actions.getOrCreateAction(actionId);
        for (final IActionParameter ap : params) {
            action.addParameter(ap);
        }
        super.setNegativeButton(android.R.string.cancel, action);
        return this;
    }

    public ActionDialogBuilder setMultiChoiceItems(final int itemsId, final int actionId, final boolean... checkedItems) {
        final ActionEx action = actions.getOrCreateAction(actionId);
        super.setMultiChoiceItems(itemsId, LengthUtils.isNotEmpty(checkedItems) ? checkedItems : null, action);
        return this;
    }

    public ActionDialogBuilder setMessage(final int msgId, final Object... args) {
        setMessage(context.getResources().getString(msgId, args));
        return this;
    }
}
