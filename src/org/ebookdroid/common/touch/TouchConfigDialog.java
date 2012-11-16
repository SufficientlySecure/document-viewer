package org.ebookdroid.common.touch;

import org.ebookdroid.R;
import org.ebookdroid.common.touch.TouchManager.ActionRef;
import org.ebookdroid.common.touch.TouchManager.Region;
import org.ebookdroid.common.touch.TouchManager.Touch;
import org.ebookdroid.common.touch.TouchManager.TouchProfile;
import org.ebookdroid.ui.viewer.IActivityController;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.DialogController;
import org.emdev.ui.adapters.ActionsAdapter;

public class TouchConfigDialog extends Dialog {

    private final TouchManagerView view;
    private final DialogController<TouchConfigDialog> actions;

    private final TouchProfile profile;
    private RegionWrapper wrapper;

    private final RegionsAdapter adapter;
    private final ActionsAdapter actionsAdapter;

    private final Spinner stList;
    private final Spinner dtList;
    private final Spinner ltList;
    private final Spinner tftList;
    private Spinner regionList;

    public TouchConfigDialog(final IActivityController base, final TouchManagerView view, final TouchProfile profile,
            final Region region) {
        super(base.getContext());
        this.view = view;
        this.profile = profile;
        this.actions = new DialogController<TouchConfigDialog>(this);

        setTitle("Tap configuration");
        setContentView(R.layout.tap_zones_config);

        final ActionSelectionListener actionListener = new ActionSelectionListener();

        actionsAdapter = new ActionsAdapter(getContext(), R.array.list_actions_ids, R.array.list_actions_labels);

        stList = (Spinner) this.findViewById(R.id.tapZonesConfigSingleAction);
        stList.setAdapter(actionsAdapter);
        stList.setTag(TouchManager.Touch.SingleTap);
        stList.setOnItemSelectedListener(actionListener);

        dtList = (Spinner) this.findViewById(R.id.tapZonesConfigDoubleAction);
        dtList.setAdapter(actionsAdapter);
        dtList.setTag(TouchManager.Touch.DoubleTap);
        dtList.setOnItemSelectedListener(actionListener);

        ltList = (Spinner) this.findViewById(R.id.tapZonesConfigLongAction);
        ltList.setAdapter(actionsAdapter);
        ltList.setTag(TouchManager.Touch.LongTap);
        ltList.setOnItemSelectedListener(actionListener);

        tftList = (Spinner) this.findViewById(R.id.tapZonesConfigTwoFingerAction);
        tftList.setAdapter(actionsAdapter);
        tftList.setTag(TouchManager.Touch.TwoFingerTap);
        tftList.setOnItemSelectedListener(actionListener);

        adapter = new RegionsAdapter(getContext(), wraps(profile.regions));
        regionList = (Spinner) this.findViewById(R.id.tapZonesConfigRegions);
        regionList.setAdapter(adapter);
        regionList.setSelection(profile.regions.indexOf(region));
        regionList.setOnItemSelectedListener(new RegionSelectionListener());

        actions.connectViewToAction(R.id.tapZonesConfigClear);
        actions.connectViewToAction(R.id.tapZonesConfigDelete);
        actions.connectViewToAction(R.id.tapZonesConfigReset);

        // for (Region r : this.profile.regions) {
        // System.out.println("TouchConfigDialog.TouchConfigDialog(): " + r);
        // }
    }

    @Override
    protected void onStop() {
        super.onStop();

        profile.regions.clear();
        for (int i = 0; i < adapter.getCount(); i++) {
            profile.regions.add(adapter.getItem(i).r);
        }

        // for (Region r : this.profile.regions) {
        // System.out.println("TouchConfigDialog.onStop(): " + r);
        // }

        TouchManager.persist();

        view.invalidate();
    }

    protected void updateAction(final Spinner view) {
        if (wrapper != null) {
            final TouchManager.Touch t = (Touch) view.getTag();
            final ActionRef ref = wrapper.r.getAction(t);
            if (ref != null) {
                final String name = ref.name;
                view.setSelection(actionsAdapter.getPosition(name));
                return;
            }
        }
        view.setSelection(0);
    }

    @ActionMethod(ids = R.id.tapZonesConfigReset)
    public void resetRegion(final ActionEx action) {
        if (wrapper != null) {
            for (final Region r : profile.regions) {
                if (r.getRect().equals(wrapper.r.getRect())) {
                    wrapper.r = new Region(r);
                    updateAction(stList);
                    updateAction(dtList);
                    updateAction(ltList);
                    updateAction(tftList);
                    return;
                }
            }
        }
    }

    @ActionMethod(ids = R.id.tapZonesConfigClear)
    public void clearRegion(final ActionEx action) {
        if (wrapper != null) {
            wrapper.r.clear();
            updateAction(stList);
            updateAction(dtList);
            updateAction(ltList);
            updateAction(tftList);
        }
    }

    @ActionMethod(ids = R.id.tapZonesConfigDelete)
    public void deleteRegion(final ActionEx action) {
        if (wrapper != null) {
            adapter.remove(wrapper);
            wrapper = (RegionWrapper) regionList.getSelectedItem();
        }
    }

    private List<RegionWrapper> wraps(final List<Region> list) {
        final List<RegionWrapper> res = new ArrayList<RegionWrapper>(list.size());
        for (final Region r : list) {
            res.add(new RegionWrapper(r));
        }
        return res;
    }

    private final class RegionSelectionListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
            wrapper = adapter.getItem(position);
            // System.out.println("onItemSelected(): " + wrapper);
            updateAction(stList);
            updateAction(dtList);
            updateAction(ltList);
            updateAction(tftList);
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            wrapper = null;
            updateAction(stList);
            updateAction(dtList);
            updateAction(ltList);
            updateAction(tftList);
        }
    }

    private final class ActionSelectionListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
            if (wrapper != null) {
                final Integer actionId = ActionEx.getActionId(actionsAdapter.getActionId(position));
                if (actionId != null) {
                    wrapper.r.setAction((Touch) parent.getTag(), actionId, true);
                } else {
                    wrapper.r.clear((Touch) parent.getTag());
                }
            }
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            if (wrapper != null) {
                wrapper.r.clear((Touch) parent.getTag());
            }
        }
    }

    final class RegionsAdapter extends ArrayAdapter<RegionWrapper> {

        RegionsAdapter(final Context context, final List<RegionWrapper> objects) {
            super(context, R.layout.list_item, R.id.list_item, objects);
            setDropDownViewResource(R.layout.list_dropdown_item);
        }
    }

    final class RegionWrapper {

        public Region r;

        public RegionWrapper(final Region r) {
            this.r = new Region(r);
        }

        @Override
        public String toString() {
            final Rect rect = r.getRect();
            return "[ " + rect.left + "%, " + rect.top + "% - " + rect.right + "%, " + rect.bottom + "% ]";
        }
    }
}
