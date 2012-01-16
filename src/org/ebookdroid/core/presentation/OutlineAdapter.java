package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.OutlineLink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.util.List;

public class OutlineAdapter extends ArrayAdapter<OutlineLink> {

    private int margin;

    public OutlineAdapter(Context context, List<OutlineLink> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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

        OutlineLink item = getItem(position);
        view.setText(item.title.trim());

        RelativeLayout.LayoutParams l = (LayoutParams) view.getLayoutParams();
        if (firstTime) {
            margin = l.leftMargin;
        }
        l.leftMargin = Math.min(5, item.level + 1) * margin;

        return container;
    }
}
