package org.ebookdroid.common.settings.books;

import org.json.JSONException;
import org.json.JSONObject;

public class TestUtils {
    public static JSONObject parseJSON(String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
