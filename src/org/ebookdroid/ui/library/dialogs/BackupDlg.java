package org.ebookdroid.ui.library.dialogs;

import org.ebookdroid.R;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.emdev.common.backup.BackupInfo;
import org.emdev.common.backup.BackupManager;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;

public class BackupDlg extends Dialog implements TextWatcher, ListView.OnItemLongClickListener,
        ListView.OnItemClickListener {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final EditText newBackupNameEdit;

    private final Button backupButton;

    private final Button removeButton;

    private final Button restoreButton;

    private final ListView backupsList;

    private final BackupInfoAdapter adapter;

    private final ActionController<BackupDlg> actions = new ActionController<BackupDlg>(this);

    public BackupDlg(final Context context) {
        super(context);

        setTitle(R.string.menu_backupsettings);
        setContentView(R.layout.backup_screen);

        adapter = new BackupInfoAdapter(getContext(), BackupManager.getAvailableBackups());

        newBackupNameEdit = (EditText) findViewById(R.id.newBackupNameEdit);
        newBackupNameEdit.addTextChangedListener(this);

        backupButton = (Button) findViewById(R.id.backupButton);
        actions.setActionForView(backupButton);

        restoreButton = (Button) findViewById(R.id.restoreBackupButton);
        actions.setActionForView(restoreButton);

        removeButton = (Button) findViewById(R.id.removeBackupButton);
        actions.setActionForView(removeButton);

        backupsList = (ListView) findViewById(R.id.backupsList);

        backupsList.setAdapter(adapter);

        backupsList.setOnItemClickListener(this);
        backupsList.setOnItemLongClickListener(this);

        updateControls(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LayoutUtils.maximizeWindow(getWindow());
    }

    private void updateAdapter() {
        adapter.setNotifyOnChange(false);
        try {
            adapter.clear();
            for (final BackupInfo info : BackupManager.getAvailableBackups()) {
                adapter.add(info);
            }
        } finally {
            adapter.setNotifyOnChange(true);
            adapter.notifyDataSetChanged();
        }
    }

    private void updateControls(final String newBackupName) {
        final int checked = getCheckedPositionsCount();
        backupButton.setEnabled(LengthUtils.isNotEmpty(newBackupName));
        restoreButton.setEnabled(1 == checked);
        removeButton.setEnabled(0 < checked);
    }

    @ActionMethod(ids = R.id.backupButton)
    public void backup(final ActionEx action) {
        final BackupInfo backup = new BackupInfo(newBackupNameEdit.getText().toString());
        BackupManager.backup(backup);
        newBackupNameEdit.setText("");
        updateAdapter();
    }

    @ActionMethod(ids = R.id.restoreBackupButton)
    public void restore(final ActionEx action) {
        if (getCheckedPositionsCount() == 1) {
            final int pos = getFirstCheckedPosition();
            if (pos != -1) {
                final BackupInfo backup = adapter.getItem(pos);
                BackupManager.restore(backup);
                backupsList.clearChoices();
            }
        }
    }

    @ActionMethod(ids = R.id.removeBackupButton)
    public void remove(final ActionEx action) {
        final SparseBooleanArray checked = backupsList.getCheckedItemPositions();
        if (checked != null) {
            for (int i = 0, n = checked.size(); i < n; i++) {
                final int pos = checked.keyAt(i);
                final boolean state = checked.valueAt(i);
                if (pos >= 0 && pos < adapter.getCount() && state) {
                    final BackupInfo backup = adapter.getItem(pos);
                    BackupManager.remove(backup);
                }
            }
            updateAdapter();
            backupsList.clearChoices();
        }
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void afterTextChanged(final Editable s) {
        updateControls(s.toString());
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        updateControls(newBackupNameEdit.getText().toString());
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        return true;
    }

    protected int getCheckedPositionsCount() {
        final SparseBooleanArray checked = backupsList.getCheckedItemPositions();
        int count = 0;
        for (int i = 0, n = checked != null ? checked.size() : 0; i < n; i++) {
            if (checked.valueAt(i)) {
                count++;
            }
        }
        return count;
    }

    protected int getFirstCheckedPosition() {
        final SparseBooleanArray checked = backupsList.getCheckedItemPositions();
        for (int i = 0, n = checked != null ? checked.size() : 0; i < n; i++) {
            if (checked.valueAt(i)) {
                return checked.keyAt(i);
            }
        }
        return -1;
    }

    private static class BackupInfoAdapter extends ArrayAdapter<BackupInfo> {

        public BackupInfoAdapter(final Context context, final Collection<BackupInfo> objects) {
            super(context, R.layout.list_item, R.id.list_item, new ArrayList<BackupInfo>(objects));
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final TextView text = (TextView) view.findViewById(R.id.list_item);
            final BackupInfo info = getItem(position);
            text.setText(SDF.format(info.getTimestamp()) + " " + info.name);
            return view;
        }

    }
}
