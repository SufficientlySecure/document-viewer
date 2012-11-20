package org.emdev.common.filesystem;

import android.os.Environment;

import org.emdev.utils.enums.ResourceConstant;

public enum MediaState implements ResourceConstant {

    /**
     * The media is present but not mounted.
     */
    MEDIA_UNMOUNTED(Environment.MEDIA_UNMOUNTED, false, false),

    /**
     * The media is present and being disk-checked
     */
    MEDIA_CHECKING(Environment.MEDIA_CHECKING, false, false),

    /**
     * The media is present but is blank or is using an unsupported filesystem
     */
    MEDIA_NOFS(Environment.MEDIA_NOFS, false, false),

    /**
     * The media is present and mounted at its mount point with read/write access.
     */
    MEDIA_MOUNTED(Environment.MEDIA_MOUNTED, true, true),

    /**
     * The media is present and mounted at its mount point with read only access.
     */
    MEDIA_MOUNTED_READ_ONLY(Environment.MEDIA_MOUNTED_READ_ONLY, true, false),

    /**
     * The media is present not mounted, and shared via USB mass storage.
     */
    MEDIA_SHARED(Environment.MEDIA_SHARED, false, false),

    /**
     * The media is not present.
     */
    MEDIA_REMOVED(Environment.MEDIA_REMOVED, false, false),

    /**
     * The media was removed before it was unmounted.
     */
    MEDIA_BAD_REMOVAL(Environment.MEDIA_BAD_REMOVAL, false, false),

    /**
     * The media is present but cannot be mounted. Typically this happens if the file system on the media is
     * corrupted.
     */
    MEDIA_UNMOUNTABLE(Environment.MEDIA_UNMOUNTABLE, false, false);

    public final String resValue;

    public final boolean readable;
    public final boolean writable;

    private MediaState(final String value, final boolean readable, final boolean writable) {
        this.resValue = value;
        this.readable = readable;
        this.writable = writable;
    }

    @Override
    public String getResValue() {
        return resValue;
    }

}
