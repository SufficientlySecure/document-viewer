package org.ebookdroid.opds;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public enum OPDSBookType {

    PDF(false, "application/pdf"), DJVU(false, "application/djvu"), FB2(true, "application/fb2");

    private final boolean supportZip;
    private final Set<String> mimeTypes;

    private OPDSBookType(boolean supportZip, String... mimeTypes) {
        this.supportZip = supportZip;
        this.mimeTypes = new LinkedHashSet<String>(Arrays.asList(mimeTypes));
    }

    public boolean isZipSupported() {
        return supportZip;
    }

    public static OPDSBookType getByMimeType(String mimeType) {
        for (OPDSBookType t : values()) {
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
    
    public static boolean isZippedContent(String mimeType) {
        return mimeType.endsWith("+zip");
    }
}
