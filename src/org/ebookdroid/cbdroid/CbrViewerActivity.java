package org.ebookdroid.cbdroid;

import org.ebookdroid.cbdroid.codec.CbrContext;
import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;

public class CbrViewerActivity extends BaseViewerActivity {

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new CbrContext());
    }

}
