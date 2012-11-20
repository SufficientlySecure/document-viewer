package org.emdev.common.filesystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import java.util.Map;
import java.util.TreeMap;

import org.emdev.utils.listeners.ListenerProxy;

public class MediaManager extends BroadcastReceiver {

    public static final Map<String, ExternalMedia> mountedMedia = new TreeMap<String, ExternalMedia>();

    public static final ListenerProxy listeners = new ListenerProxy(Listener.class);

    public void register(final Context context) {
        final IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        f.addAction(Intent.ACTION_MEDIA_NOFS);
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        context.registerReceiver(this, f);
    }

    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            final boolean readOnly = intent.getExtras().getBoolean("read-only");
            setMediaState(path, readOnly ? MediaState.MEDIA_MOUNTED : MediaState.MEDIA_MOUNTED_READ_ONLY);
            return;
        }

        if (Intent.ACTION_MEDIA_NOFS.equals(action)) {
            final Uri data = intent.getData();
            final String path = PathFromUri.retrieve(context.getContentResolver(), data);
            setMediaState(path, MediaState.MEDIA_NOFS);
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

    public void setMediaState(final String path, final MediaState newState) {
        ExternalMedia media = mountedMedia.get(path);
        if (media == null) {
            media = new ExternalMedia(path, newState);
            mountedMedia.put(path, media);
            final Listener l = listeners.getListener();
            l.onMediaStateChanged(path, null, media.state);
        } else {
            final MediaState oldState = media.state;
            media.state = newState;
            final Listener l = listeners.getListener();
            l.onMediaStateChanged(path, oldState, media.state);
        }
    }

    public static interface Listener {

        void onMediaStateChanged(String path, MediaState oldState, MediaState newState);
    }
}
