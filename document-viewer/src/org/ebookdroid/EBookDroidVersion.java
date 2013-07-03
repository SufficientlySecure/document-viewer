package org.ebookdroid;

public enum EBookDroidVersion {

    PRIVATE, DEV, RELEASE_COMMON, RELEASE_ARCH, RELEASE_LEGACY;

    public static EBookDroidVersion get(final int versionCode) {
        if (versionCode == 0) {
            return PRIVATE;
        }
        final int subversion = versionCode % 10;
        switch (subversion) {
            case 0:
                return RELEASE_COMMON;
            case 9:
                return DEV;
            case 8:
                return RELEASE_LEGACY;
            default:
                return RELEASE_ARCH;
        }
    }
}
