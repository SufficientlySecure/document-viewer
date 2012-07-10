package org.ebookdroid.common.backup;

import org.json.JSONObject;

public interface IBackupAgent {

    String key();

    JSONObject backup();

    void restore(JSONObject backup);
}
