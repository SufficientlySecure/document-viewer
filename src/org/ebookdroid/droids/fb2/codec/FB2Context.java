package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.EBookDroidLibraryLoader;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

public class FB2Context extends AbstractCodecContext {

    public static final int FB2_FEATURES = FEATURE_UNIFIED_PAGE_INTO | FEATURE_PARALLEL_PAGE_ACCESS
            | FEATURE_DOCUMENT_TEXT_SEARCH | FEATURE_EMBEDDED_COVER | FEATURE_EMBEDDED_OUTLINE
            | FEATURE_POSITIVE_IMAGES_IN_NIGHT_MODE;

    static {
        EBookDroidLibraryLoader.load();
    }

    public FB2Context() {
        super(FB2_FEATURES);
    }

    @Override
    public CodecDocument openDocument(final String fileName, final String password) {
        return new FB2Document(this, fileName);
    }
}
