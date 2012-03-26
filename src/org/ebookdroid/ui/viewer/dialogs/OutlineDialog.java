package org.ebookdroid.ui.viewer.dialogs;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.adapters.OutlineAdapter;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.List;

import org.emdev.ui.actions.ActionController;
import org.emdev.utils.LayoutUtils;

public class OutlineDialog extends Dialog implements OnItemClickListener {

    final IActivityController base;
    final List<OutlineLink> outline;
    final ActionController<OutlineDialog> actions;

    public OutlineDialog(final IActivityController base, final List<OutlineLink> outline) {
        super(base.getContext());
        this.base = base;
        this.outline = outline;
        this.actions = new ActionController<OutlineDialog>(base, this);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutUtils.maximizeWindow(getWindow());

        setTitle(R.string.outline_title);

        final ListView listView = new ListView(getContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setContentView(listView);

        final OutlineAdapter adapter = new OutlineAdapter(getContext(), outline);

        listView.setAdapter(adapter);

        final BookSettings bs = SettingsManager.getBookSettings();
        if (bs != null) {
            final int current = bs.currentPage.docIndex;
            for (int i = 0; i < adapter.getCount(); i++) {
                final OutlineLink item = adapter.getItem(i);
                if (current <= item.targetPage) {
                    listView.setItemChecked(i, true);
                    listView.setSelection(i);
                    break;
                }
            }
        }

        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        this.dismiss();
        actions.getOrCreateAction(R.id.actions_gotoOutlineItem).onItemClick(parent, view, position, id);
    }
}
