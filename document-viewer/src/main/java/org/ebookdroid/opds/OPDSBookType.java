package org.ebookdroid.opds;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public enum OPDSBookType {

    /**
     *
     */
    PDF(false, "application/pdf"),
    /**
     *
     */
    DJVU(false, "application/djvu", "image/djvu", "image/x-djvu", "image/vnd.djvu"),
    /**
     *
     */
    FB2(true, "application/fb2", "text/fb2+xml"),
    /**
     *
     */
    CBZ(false, "application/x-cbz");
    /**
    *
    */
//    CBR(false, "application/x-cbr");

    private final boolean supportZip;
    private final Set<String> mimeTypes;

    private OPDSBookType(final boolean supportZip, final String... mimeTypes) {
        this.supportZip = supportZip;
        this.mimeTypes = new LinkedHashSet<String>(Arrays.asList(mimeTypes));
    }

    public boolean isZipSupported() {
        return supportZip;
    }

    public static OPDSBookType getByMimeType(final String mimeType) {
        for (final OPDSBookType t : values()) {
            if (t.mimeTypes.contains(mimeType)) {
                return t;
            }
            if (isZippedContent(mimeType)) {
                if (t.mimeTypes.contains(mimeType.substring(0, mimeType.length() - "+zip".length()))) {
                    return t;
                }
            }
        }
        return null;
    }

    public static boolean isZippedContent(final String mimeType) {
        return mimeType.endsWith("+zip");
    }
}
