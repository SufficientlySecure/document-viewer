package org.emdev.ui.adapters;

import org.ebookdroid.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ActionsAdapter extends BaseAdapter {

    private final String[] actionIds;
    private final String[] actionLabels;

    public ActionsAdapter(final Context context, final int actionIdsRes, final int actionLabelsRes) {
        actionIds = context.getResources().getStringArray(R.array.list_actions_ids);
        actionLabels = context.getResources().getStringArray(R.array.list_actions_labels);
    }

    @Override
    public int getCount() {
        return actionLabels.length;
    }

    @Override
    public String getItem(final int position) {
        return actionLabels[position];
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.list_dropdown_item,
                convertView, parent);

        holder.textView.setSingleLine();
        holder.textView.setMarqueeRepeatLimit(-1);
        holder.textView.setText(getItem(position));

        return holder.getView();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.list_item,
                convertView, parent);

        holder.textView.setText(getItem(position));

        return holder.getView();
    }

    public int getPosition(final String actionId) {
        for (int i = 0; i < actionIds.length; i++) {
            if (actionId.equals(actionIds[i])) {
                return i;
            }
        }
        return 0;
    }

    public String getActionId(final int position) {
        return actionIds[position];
    }

    public static class ViewHolder extends BaseViewHolder {

        TextView textView;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            this.textView = (TextView) convertView.findViewById(R.id.list_item);
        }
    }

}
