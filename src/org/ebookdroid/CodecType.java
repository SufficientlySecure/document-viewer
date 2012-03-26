package org.ebookdroid;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.droids.cbx.CbrContext;
import org.ebookdroid.droids.cbx.CbzContext;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.ebookdroid.droids.fb2.codec.FB2Context;
import org.ebookdroid.droids.pdf.codec.PdfContext;
import org.ebookdroid.droids.xps.codec.XpsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum CodecType {

    PDF(PdfContext.class, "pdf"),

    DJVU(DjvuContext.class, "djvu", "djv"),

    XPS(XpsContext.class, "xps", "oxps"),

    CBZ(CbzContext.class, "cbz"),

    CBR(CbrContext.class, "cbr"),

    FB2(FB2Context.class, "fb2", "fb2.zip");

    private final static Map<String, CodecType> extensionToActivity;

    static {
        extensionToActivity = new HashMap<String, CodecType>();
        for (final CodecType a : values()) {
            for (final String ext : a.getExtensions()) {
                extensionToActivity.put(ext.toLowerCase(), a);
            }
        }
    }

    private final Class<? extends CodecContext> contextClass;

    private final String[] extensions;

    private CodecType(final Class<? extends CodecContext> contextClass, final String... extensions) {
        this.contextClass = contextClass;
        this.extensions = extensions;
    }

    public Class<? extends CodecContext> getContextClass() {
        return contextClass;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public static Set<String> getAllExtensions() {
        return extensionToActivity.keySet();
    }

    public static CodecType getByUri(final String uri) {
        final String uriString = uri.toLowerCase();
        for (final String ext : extensionToActivity.keySet()) {
            if (uriString.endsWith("." + ext)) {
                return extensionToActivity.get(ext);
            }
        }
        return null;
    }

    public static CodecType getByExtension(final String ext) {
        return extensionToActivity.get(ext.toLowerCase());
    }
}
