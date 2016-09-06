package org.emdev.ui.uimanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.BaseDroidApp;

@TargetApi(11)
public class UIManager3x implements IUIManager {

    private static final String SYS_UI_CLS = "com.android.systemui.SystemUIService";
    private static final String SYS_UI_PKG = "com.android.systemui";
    private static final ComponentName SYS_UI = new ComponentName(SYS_UI_PKG, SYS_UI_CLS);

    private static final String SU_PATH1 = "/system/bin/su";
    private static final String SU_PATH2 = "/system/xbin/su";
    private static final String AM_PATH = "/system/bin/am";

    private static final Map<ComponentName, Data> data = new HashMap<ComponentName, Data>() {

        /**
         *
         */
        private static final long serialVersionUID = -6627308913610357179L;

        @Override
        public Data get(final Object key) {
            Data existing = super.get(key);
            if (existing == null) {
                existing = new Data();
                put((ComponentName) key, existing);
            }
            return existing;
        }

    };

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        data.get(activity.getComponentName()).fullScreen = fullScreen;
        if (fullScreen) {
            stopSystemUI(activity);
        } else {
            startSystemUI(activity);
        }
    }

    @Override
    public void onMenuOpened(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen
                && data.get(activity.getComponentName()).fullScreenState.get()) {
            startSystemUI(activity);
        }
    }

    @Override
    public void onMenuClosed(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen
                && !data.get(activity.getComponentName()).fullScreenState.get()) {
            stopSystemUI(activity);
        }
    }

    @Override
    public void onPause(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen
                && data.get(activity.getComponentName()).fullScreenState.get()) {
            startSystemUI(activity);
        }
    }

    @Override
    public void onResume(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen
                && !data.get(activity.getComponentName()).fullScreenState.get()) {
            stopSystemUI(activity);
        }
    }

    @Override
    public void onDestroy(final Activity activity) {
        if (data.get(activity.getComponentName()).fullScreen
                && data.get(activity.getComponentName()).fullScreenState.get()) {
            startSystemUI(activity);
        }
    }

    @Override
    public boolean isTabletUi(final Activity activity) {
        return true;
    }

    protected void startSystemUI(final Activity activity) {
        if (isSystemUIRunning()) {
            data.get(activity.getComponentName()).fullScreenState.set(false);
            return;
        }
        exec(false, activity, AM_PATH, "startservice", "-n", SYS_UI.flattenToString());
    }

    protected void stopSystemUI(final Activity activity) {
        if (!isSystemUIRunning()) {
            data.get(activity.getComponentName()).fullScreenState.set(true);
            return;
        }

        final String su = getSuPath();
        if (su == null) {
            data.get(activity.getComponentName()).fullScreenState.set(false);
            return;
        }

        exec(true, activity, su, "-c", "service call activity 79 s16 " + SYS_UI_PKG);
    }

    protected boolean isSystemUIRunning() {
        final Context ctx = BaseDroidApp.context;
        final ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningServiceInfo> rsiList = am.getRunningServices(1000);

        for (final RunningServiceInfo rsi : rsiList) {
            LCTX.d("Service: " + rsi.service);
            if (SYS_UI.equals(rsi.service)) {
                LCTX.e("System UI service found");
                return true;
            }
        }
        return false;
    }

    protected void exec(final boolean expected, final Activity activity, final String... as) {
        (new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final boolean result = execImpl(as);
                    data.get(activity.getComponentName()).fullScreenState.set(result ? expected : !expected);
                } catch (final Throwable th) {
                    LCTX.e("Changing full screen mode failed: " + th.getCause());
                    data.get(activity.getComponentName()).fullScreenState.set(!expected);
                }
            }
        })).start();
    }

    private boolean execImpl(final String... as) {
        try {
            LCTX.d("Execute: " + Arrays.toString(as));
            final Process process = Runtime.getRuntime().exec(as);
            final InputStreamReader r = new InputStreamReader(process.getInputStream());
            final StringWriter w = new StringWriter();
            final char ac[] = new char[8192];
            int i = 0;
            do {
                i = r.read(ac);
                if (i > 0) {
                    w.write(ac, 0, i);
                }
            } while (i != -1);
            r.close();
            process.waitFor();

            final int exitValue = process.exitValue();
            final String text = w.toString();
            LCTX.d("Result code: " + exitValue);
            LCTX.d("Output:\n" + text);

            return 0 == exitValue;

        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getSuPath() {
        final File su1 = new File(SU_PATH1);
        if (su1.exists() && su1.isFile() && su1.canExecute()) {
            return SU_PATH1;
        }
        final File su2 = new File(SU_PATH2);
        if (su2.exists() && su2.isFile() && su2.canExecute()) {
            return SU_PATH2;
        }
        return null;
    }

    private static class Data {

        boolean fullScreen = false;
        boolean titleVisible = true;
        final AtomicBoolean fullScreenState = new AtomicBoolean();
    }

}
