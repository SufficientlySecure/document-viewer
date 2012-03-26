package org.ebookdroid.common.settings.base;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.common.log.LogContext;

public class BasePreferenceDefinition {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Settigns");

    public final String key;

    public BasePreferenceDefinition(final int keyRes) {
        key = EBookDroidApp.context.getString(keyRes);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BasePreferenceDefinition) {
            final BasePreferenceDefinition that = (BasePreferenceDefinition) obj;
            return this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return this.key;
    }
}
