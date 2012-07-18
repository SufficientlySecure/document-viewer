package org.ebookdroid.common.keysbinding;

import org.ebookdroid.R;
import org.ebookdroid.ui.viewer.IActivityController;

import android.app.Dialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.emdev.common.android.AndroidVersion;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.adapters.ActionsAdapter;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;

public class KeyBindingsDialog extends Dialog {

    private final ActionsAdapter actionsAdapter;
    private final KeyGroups groups = new KeyGroups();

    public KeyBindingsDialog(final IActivityController base) {
        super(base.getContext());

        setTitle("Keys binding");

        actionsAdapter = new ActionsAdapter(getContext(), R.array.list_actions_ids, R.array.list_actions_labels);

        final ExpandableListView list = new ExpandableListView(getContext());
        final KeyGroups groups = initKeyActions();
        list.setAdapter(groups);
        LayoutUtils.fillInParent(null, list);

        setContentView(list);
    }

    private KeyGroups initKeyActions() {

        final KeyGroup management = groups.add("Management keys");
        management.addInterval(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_VOLUME_DOWN);
        management.addInterval(96 /* KeyEvent.KEYCODE_BUTTON_A */, 110/* KeyEvent.KEYCODE_BUTTON_MODE */);

        final KeyGroup phone = groups.add("Phone keys");

        phone.add(KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_SOFT_RIGHT);
        phone.add(KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL);
        phone.addInterval(KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_POUND);

        final KeyGroup keyboard = groups.add("Keyboard keys");
        keyboard.addInterval(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_Z);
        keyboard.add(KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_PERIOD);
        keyboard.add(KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_SPACE);
        keyboard.add(KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_SPACE);
        keyboard.addInterval(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_AT);
        keyboard.add(KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_SPACE);

        if (!AndroidVersion.lessThan3x) {
            keyboard.addInterval(/* KeyEvent.KEYCODE_PAGE_UP */92, /* (KeyEvent.KEYCODE_PAGE_DOWN */93);
            keyboard.addInterval(/* KeyEvent.KEYCODE_MOVE_HOME */122, /* KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN */163);
        }

        final KeyGroup service = groups.add("Service keys");
        service.add(KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_CLEAR);
        service.addInterval(KeyEvent.KEYCODE_NOTIFICATION, KeyEvent.KEYCODE_MUTE);
        return groups;
    }

    @Override
    protected void onStart() {
        LayoutUtils.maximizeWindow(getWindow());
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (final KeyGroup group : groups.groups) {
            for (final KeyAction action : group.actions) {
                final Integer actionId = action.action != null ? ActionEx.getActionId(action.action) : null;
                if (actionId != null) {
                    KeyBindingsManager.addAction(actionId, action.code);
                } else {
                    KeyBindingsManager.removeAction(action.code);
                }
            }
        }
        KeyBindingsManager.persist();
    }

    protected void updateAction(final Spinner view) {
        final KeyAction action = (KeyAction) view.getTag();
        final String name = action.action;
        if (LengthUtils.isNotEmpty(name)) {
            view.setSelection(actionsAdapter.getPosition(name));
            return;
        }
        view.setSelection(0);
    }

    public class KeyAction {

        final int code;
        final String label;
        String action;

        public KeyAction(final int code) {
            this.code = code;

            String label = KeyBindingsManager.keyCodeToString(code);
            this.label = label + " [" + code + "]";

            final Integer actionId = KeyBindingsManager.getAction(code);
            this.action = actionId != null ? ActionEx.getActionName(actionId) : null;
        }
    }

    public class KeyGroup {

        final String label;
        final Map<Integer, KeyAction> keys = new LinkedHashMap<Integer, KeyAction>();
        final List<KeyAction> actions = new ArrayList<KeyAction>();

        public KeyGroup(final String label) {
            this.label = label;
        }

        public void add(final int... codes) {
            for (final int code : codes) {
                final KeyAction value = new KeyAction(code);
                keys.put(code, value);
                actions.add(value);
            }
        }

        public void addInterval(final int first, final int last) {
            for (int code = first; code <= last; code++) {
                final KeyAction value = new KeyAction(code);
                keys.put(code, value);
                actions.add(value);
            }
        }
    }

    public class KeyGroups extends BaseExpandableListAdapter implements OnItemSelectedListener {

        final List<KeyGroup> groups = new ArrayList<KeyGroup>();

        public KeyGroup add(final String label) {
            final KeyGroup group = new KeyGroup(label);
            groups.add(group);
            return group;
        }

        @Override
        public int getGroupCount() {
            return groups.size();
        }

        @Override
        public KeyGroup getGroup(final int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public long getGroupId(final int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
                final ViewGroup parent) {
            View container = null;
            TextView view = null;
            if (convertView == null) {
                container = LayoutInflater.from(getContext()).inflate(R.layout.keybinding_group, parent, false);
            } else {
                container = convertView;
            }
            view = (TextView) container.findViewById(R.id.keybinding_groupText);
            view.setText(getGroup(groupPosition).label);
            return container;
        }

        @Override
        public int getChildrenCount(final int groupPosition) {
            return getGroup(groupPosition).actions.size();
        }

        @Override
        public KeyAction getChild(final int groupPosition, final int childPosition) {
            return getGroup(groupPosition).actions.get(childPosition);
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
                View convertView, final ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.keybinding_action, parent, false);
            }

            final KeyAction action = getChild(groupPosition, childPosition);

            final TextView keyView = (TextView) convertView.findViewById(R.id.keybinding_key);
            keyView.setText(action.label);

            final Spinner actionsView = (Spinner) convertView.findViewById(R.id.keybinding_actions);
            actionsView.setOnItemSelectedListener(this);
            actionsView.setAdapter(actionsAdapter);
            actionsView.setTag(action);
            updateAction(actionsView);

            return convertView;
        }

        @Override
        public long getChildId(final int groupPosition, final int childPosition) {
            return childPosition;
        }

        @Override
        public boolean isChildSelectable(final int groupPosition, final int childPosition) {
            return true;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
            final String actionId = actionsAdapter.getActionId(position);
            final KeyAction action = (KeyAction) parent.getTag();
            action.action = LengthUtils.unsafeString(actionId);
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            final KeyAction action = (KeyAction) parent.getTag();
            action.action = null;
        }
    }

}
