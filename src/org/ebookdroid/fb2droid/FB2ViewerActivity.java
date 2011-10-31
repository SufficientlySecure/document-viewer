package org.ebookdroid.fb2droid;

import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.fb2droid.codec.FB2Context;

public class FB2ViewerActivity extends BaseViewerActivity {

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.BaseViewerActivity#createDecodeService()
     */
    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new FB2Context());
    }
}
