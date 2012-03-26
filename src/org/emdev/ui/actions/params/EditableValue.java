package org.emdev.ui.actions.params;

import android.widget.EditText;

public class EditableValue extends AbstractActionParameter {

    private final EditText input;

    public EditableValue(final String name, final EditText input) {
        super(name);
        this.input = input;
    }

    @Override
    public Object getValue() {
        return this.input.getText();
    }
}
