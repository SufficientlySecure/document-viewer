package org.emdev.common.filesystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.listeners.ListenerProxy;

public class MediaManager extends BroadcastReceiver {

    private static final LogContext LCTX = LogManager.root().lctx("MediaManager");

    public static final ListenerProxy listeners = new ListenerProxy(Listener.class);

    private static final Map<String, ExternalMedia> mountedMedia = new TreeMap<String, ExternalMedia>();

    private static final MediaManager instance = new MediaManager();

    private MediaManager() {
    }

    public static void init(final Context context) {
        readMounts();
        context.registerReceiver(instance, createIntentFilter());
    }

    static void readMounts() {
        try {
            final BufferedReader in = new BufferedReader(new FileReader("/proc/mounts"));
            try {
                for (String s = in.readLine(); s != null; s = in.readLine()) {
                    processMountPoint(s);
                }
            } catch (final IOException ex) {
                LCTX.e("Reading mounting points failed: " + ex.getMessage());
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final FileNotFoundException ex) {
            LCTX.e("Reading mounting points failed: " + ex.getMessage());
        }
    }

    static void processMountPoint(final String s) {
        try {
            final MountedDevice d = new MountedDevice(s);
            if (filterDeviceName(d) && filterMountPath(d)) {
                final boolean rw = d.params.containsKey("rw");
                setMediaState(d.point, rw ? MediaState.MEDIA_MOUNTED : MediaState.MEDIA_MOUNTED_READ_ONLY);
            }
        } catch (final IllegalArgumentException th) {
            LCTX.w(th.getMessage());
        }
    }

    static boolean filterDeviceName(final MountedDevice d) {
        return d.device.startsWith("/dev/block/vold");
    }

    static boolean filterMountPath(final MountedDevice d) {
        return !d.point.endsWith("/secure/asec");
    }

    static IntentFilter createIntentFilter() {
        final IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        return f;
    }

    public static void onTerminate(final Context context) {
        context.unregisterReceiver(instance);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            final Bundle extras = intent.getExtras();
            final boolean readOnly = extras != null ? extras.getBoolean("read-only") : false;
            setMediaState(path, readOnly ? MediaState.MEDIA_MOUNTED : MediaState.MEDIA_MOUNTED_READ_ONLY);
            return;
        }

        if (Intent.ACTION_MEDIA_SHARED.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_SHARED);
            return;
        }

        if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_REMOVED);
            return;
        }

        if (Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_BAD_REMOVAL);
            return;
        }

        if (Intent.ACTION_MEDIA_UNMOUNTABLE.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_UNMOUNTABLE);
            return;
        }

        if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_UNMOUNTED);
            return;
        }

    }

    public static synchronized Collection<String> getReadableMedia() {
        final List<String> list = new ArrayList<String>(mountedMedia.size());
        for (final ExternalMedia m : MediaManager.mountedMedia.values()) {
            if (m.state.readable) {
                list.add(m.path);
            }
        }
        return list;
    }

    static synchronized void setMediaState(final String path, final MediaState newState) {
        MediaState oldState = null;
        ExternalMedia media = mountedMedia.get(path);
        if (media == null) {
            media = new ExternalMedia(path, newState);
            mountedMedia.put(path, media);
        } else {
            oldState = media.state;
            media.state = newState;
        }
        final Listener l = listeners.getListener();

        LCTX.d(path + " : " + oldState + " -> " + newState);

        l.onMediaStateChanged(path, oldState, media.state);
    }

    public static interface Listener {

        void onMediaStateChanged(String path, MediaState oldState, MediaState newState);
    }

    private static class MountedDevice {

        public final String device;
        public final String point;
        public final Map<String, String> params = new LinkedHashMap<String, String>();

        public MountedDevice(final String s) {
            final String[] fields = s.split("\\s");
            if (fields.length < 4) {
                throw new IllegalArgumentException("Bad moint point string: " + s);
            }
            device = fields[0];
            point = fields[1];

            final String[] pp = fields[3].split(",");
            for (int i = 0; i < pp.length; i++) {
                String key = pp[i];
                String val = key;

                final int index = key.indexOf("=");
                if (index != -1) {
                    key = key.substring(0, index);
                    val = val.substring(index + 1);
                }
                params.put(key, val);
            }
        }
    }
}
