package org.emdev.ui.uimanager;

import org.ebookdroid.EBookDroidApp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TargetApi(11)
public class UIManager3x implements IUIManager {

    private static final String SYS_UI_CLS = "com.android.systemui.SystemUIService";
    private static final String SYS_UI_PKG = "com.android.systemui";
    private static final ComponentName SYS_UI = new ComponentName(SYS_UI_PKG, SYS_UI_CLS);

    private static final String SU_PATH1 = "/system/bin/su";
    private static final String SU_PATH2 = "/system/xbin/su";
    private static final String AM_PATH = "/system/bin/am";

    private boolean hwaEnabled = false;

    private boolean fullScreen = false;

    private final AtomicBoolean fullScreenState = new AtomicBoolean();

    @Override
    public void setTitleVisible(final Activity activity, final boolean visible) {
        try {
            final Window window = activity.getWindow();
            if (!visible) {
                window.requestFeature(Window.FEATURE_NO_TITLE);
            } else {
                window.requestFeature(Window.FEATURE_ACTION_BAR);
                window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                activity.setProgressBarIndeterminate(true);
                activity.setProgressBarIndeterminateVisibility(true);
                window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, 1);
            }
        } catch (final Throwable th) {
            LCTX.e("Error on requestFeature call: " + th.getMessage());
        }
    }

    @Override
    public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
        this.fullScreen = fullScreen;
        if (fullScreen) {
            stopSystemUI();
        } else {
            startSystemUI();
        }
    }

    @Override
    public void setHardwareAccelerationEnabled(final boolean enabled) {
        this.hwaEnabled = enabled;
    }

    @Override
    public void setHardwareAccelerationMode(final View view, final boolean accelerated) {
        if (this.hwaEnabled && view != null) {
            view.setLayerType(accelerated ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void openOptionsMenu(final Activity activity, final View view) {
        activity.openOptionsMenu();
    }

    @Override
    public void onMenuOpened(final Activity activity) {
        if (fullScreen && fullScreenState.get()) {
            startSystemUI();
        }
    }

    @Override
    public void onMenuClosed(final Activity activity) {
        if (fullScreen && !fullScreenState.get()) {
            stopSystemUI();
        }
    }

    @Override
    public void onPause(final Activity activity) {
        if (fullScreen && fullScreenState.get()) {
            startSystemUI();
        }
    }

    @Override
    public void onResume(final Activity activity) {
        if (fullScreen && !fullScreenState.get()) {
            stopSystemUI();
        }
    }

    @Override
    public void onDestroy(final Activity activity) {
        if (fullScreen && fullScreenState.get()) {
            startSystemUI();
        }
    }

    protected void startSystemUI() {
        if (isSystemUIRunning()) {
            fullScreenState.set(false);
            return;
        }
        exec(false, AM_PATH, "startservice", "-n", SYS_UI.flattenToString());
    }

    protected void stopSystemUI() {
        if (!isSystemUIRunning()) {
            fullScreenState.set(true);
            return;
        }

        final String su = getSuPath();
        if (su == null) {
            fullScreenState.set(false);
            return;
        }

        exec(true, su, "-c", "service call activity 79 s16 " + SYS_UI_PKG);
    }

    protected boolean isSystemUIRunning() {
        final Context ctx = EBookDroidApp.context;
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

    protected void exec(final boolean expected, final String... as) {
        (new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final boolean result = execImpl(as);
                    fullScreenState.set(result ? expected : !expected);
                } catch (final Throwable th) {
                    LCTX.e("Changing full screen mode failed: " + th.getCause());
                    fullScreenState.set(!expected);
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

}
