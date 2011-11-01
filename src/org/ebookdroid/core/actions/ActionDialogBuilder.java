package org.ebookdroid.core.actions;

import org.ebookdroid.R;
import org.ebookdroid.utils.LengthUtils;

import android.app.AlertDialog;

public class ActionDialogBuilder extends AlertDialog.Builder {

    private final IActionController<?> actions;

    public ActionDialogBuilder(final IActionController<?> actions) {
        super(actions.getContext());
        this.actions = actions;
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

    public ActionDialogBuilder setMultiChoiceItems(int itemsId, int actionId, boolean... checkedItems) {
        final ActionEx action = actions.getOrCreateAction(actionId);
        super.setMultiChoiceItems(itemsId, LengthUtils.isNotEmpty(checkedItems) ? checkedItems : null, action);
        return this;
    }

}
