package org.ebookdroid.common.settings.books;

import org.ebookdroid.core.PageIndex;

import org.json.JSONException;
import org.json.JSONObject;

public class Bookmark {

    public boolean service;
    public String name;
    public PageIndex page;
    public float offsetX;
    public float offsetY;

    public Bookmark(final String name, final PageIndex page, final float offsetX, final float offsetY) {
        this(false, name, page, offsetX, offsetY);
    }

    public Bookmark(final boolean service, final String name, final PageIndex page, final float offsetX,
            final float offsetY) {
        this.service = service;
        this.name = name;
        this.page = page;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public Bookmark(final JSONObject obj) throws JSONException {
        this.service = false;
        this.name = obj.getString("name");
        this.page = new PageIndex(obj.getJSONObject("page"));
        this.offsetX = (float) obj.getDouble("offsetX");
        this.offsetY = (float) obj.getDouble("offsetY");
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("page", page.toJSON());
        obj.put("offsetX", offsetX);
        obj.put("offsetY", offsetY);
        return obj;
    }

    public int getActualIndex(final boolean splittingEnabled) {
        if (page.docIndex == page.viewIndex) {
            return page.docIndex;
        }
        return splittingEnabled ? page.viewIndex : page.docIndex;
    }

    @Override
    public String toString() {
        return name;
    }
}
