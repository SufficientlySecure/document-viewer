package org.ebookdroid.pdfdroid;

import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.pdfdroid.codec.PdfContext;

public class PdfViewerActivity extends BaseViewerActivity {

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new PdfContext());
    }
}
