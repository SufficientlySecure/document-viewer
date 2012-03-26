package org.ebookdroid.ui.viewer.adapters;

import org.ebookdroid.R;
import org.ebookdroid.core.codec.OutlineLink;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.util.List;

public class OutlineAdapter extends ArrayAdapter<OutlineLink> {

    private int margin;

    private final Drawable background;
    private final Drawable selected;

    public OutlineAdapter(final Context context, final List<OutlineLink> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        Resources resources = getContext().getResources();
        background = resources.getDrawable(R.drawable.outline_background);
        selected = resources.getDrawable(R.drawable.outline_background_selected);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View container = null;
        TextView view = null;
        boolean firstTime = false;
        if (convertView == null) {
            container = LayoutInflater.from(getContext()).inflate(R.layout.outline_item, parent, false);
            firstTime = true;
        } else {
            container = convertView;
        }
        view = (TextView) container.findViewById(R.id.outline_title);

        final OutlineLink item = getItem(position);
        view.setText(item.title.trim());

        boolean selected = false;
        for (ViewParent p = parent; p != null; p = p.getParent()) {
            if (p instanceof ListView) {
                selected = ((ListView) p).isItemChecked(position);
                break;
            }
        }

        container.setBackgroundDrawable(selected ? this.selected : this.background);

        final RelativeLayout.LayoutParams l = (LayoutParams) view.getLayoutParams();
        if (firstTime) {
            margin = l.leftMargin;
        }
        l.leftMargin = Math.min(5, item.level + 1) * margin;

        return container;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
