package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.utils.LengthUtils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends Activity {

    private static final Part[] PARTS = {
            // Start
            new Part(R.string.about_commmon_title, true, "about_common.html"),
            new Part(R.string.about_license_title, true, "about_license.html"),
            new Part(R.string.about_3dparty_title, true, "about_3rdparty.html"),
    // End
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.about);
        
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        String name = "EBookDroid";
        String version = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
            name = getResources().getString(packageInfo.applicationInfo.labelRes);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TextView title = (TextView) findViewById(R.id.about_title);
        title.setText(name + (LengthUtils.isNotEmpty(version) ? " v" + version : ""));

        ExpandableListView view = (ExpandableListView) findViewById(R.id.about_parts);
        view.setAdapter(new PartsAdapter());
        view.expandGroup(0);
    }

    private static class Part {

        final int labelId;
        final boolean html;
        final String fileName;
        CharSequence content;

        public Part(int labelId, boolean html, String fileName) {
            this.labelId = labelId;
            this.html = html;
            this.fileName = fileName;
        }

        public CharSequence getContent(final Context context) {
            if (content == null) {
                byte[] buffer = null;
                try {
                    InputStream input = context.getAssets().open(fileName);
                    int size = input.available();
                    buffer = new byte[size];
                    input.read(buffer);
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String text = new String(buffer);
                content = html ? Html.fromHtml(text) : text;
            }
            return content;
        }
    }

    public class PartsAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return PARTS.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Part getGroup(int groupPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public Part getChild(int groupPosition, int childPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View container = null;
            TextView view = null;
            if (convertView == null) {
                container = LayoutInflater.from(AboutActivity.this).inflate(R.layout.about_part, parent, false);
            } else {
                container = convertView;
            }
            view = (TextView) container.findViewById(R.id.about_partText);
            view.setText(getGroup(groupPosition).labelId);
            return container;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                ViewGroup parent) {
            TextView view = null;
            if (!(convertView instanceof TextView)) {
                view = new TextView(AboutActivity.this);
            } else {
                view = ((TextView) convertView);
            }
            view.setText(getChild(groupPosition, childPosition).getContent(AboutActivity.this));
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
