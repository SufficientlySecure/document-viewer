package org.ebookdroid.cbdroid;

import org.ebookdroid.cbdroid.codec.CbzContext;
import org.ebookdroid.core.BaseViewerActivity;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;

public class CbzViewerActivity extends BaseViewerActivity{

	@Override
	protected DecodeService createDecodeService() {
		return new DecodeServiceBase(new CbzContext());
	}

}
