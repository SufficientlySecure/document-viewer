package org.ebookdroid.core;


import org.ebookdroid.R;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.about);

		String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
		TextView title = (TextView) findViewById(R.id.about_title);
		title.setText("EbookDroid v" + version);
		
		TextView aboutText = (TextView) findViewById(R.id.about_text);
		byte[] buffer = null;
        try {
            InputStream input = getAssets().open("about.html");
            int size = input.available();
            buffer = new byte[size];
            input.read(buffer);
            input.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String text = new String(buffer);

        aboutText.setText(Html.fromHtml(text));
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
		
	}
}
