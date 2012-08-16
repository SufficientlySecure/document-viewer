package org.ebookdroid.droids.fb2.codec.parsers;

import org.ebookdroid.R;

import org.emdev.BaseDroidApp;
import org.emdev.utils.enums.ResourceConstant;

public enum FB2Parsers implements ResourceConstant {

    /**
     *
     */
    Standard(R.string.pref_fb2_xmlparser_standard),
    /**
     *
     */
    VTDEx(R.string.pref_fb2_xmlparser_vtd_ex),
    /**
     *
     */
    Duckbill(R.string.pref_fb2_xmlparser_duckbill);

    private final String resValue;

    private FB2Parsers(final int resId) {
        this.resValue = BaseDroidApp.context.getString(resId);
    }

    @Override
    public String getResValue() {
        return resValue;
    }

}
