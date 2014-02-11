package org.emdev.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BaseViewHolder {

    private View view;

    public void init(final View view) {
        this.view = view;
        view.setTag(this);
    }

    public final View getView() {
        return view;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BaseViewHolder> T getOrCreateViewHolder(final Class<T> clazz, final int resID,
            final View view, final ViewGroup parent) {
        if (view == null) {
            final Context context = parent.getContext();
            try {
                final T holder = clazz.newInstance();
                holder.init(LayoutInflater.from(context).inflate(resID, parent, false));
                return holder;
            } catch (final Throwable ex) {
                throw new RuntimeException("ViewHolder creation failed", ex);
            }
        }
        return (T) view.getTag();
    }

}
