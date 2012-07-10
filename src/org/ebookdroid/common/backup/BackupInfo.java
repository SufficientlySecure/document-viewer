package org.ebookdroid.common.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.enums.EnumUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class BackupInfo {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd.HHmmss");

    public final Type type;

    public final String name;

    Date timestamp;

    File file;

    BackupInfo() {
        this.type = Type.AUTO;
        this.timestamp = null;
        this.name = null;
    }

    public BackupInfo(final String name) {
        this.type = Type.USER;
        this.name = name;
        this.timestamp = null;
    }

    public BackupInfo(final File f) throws JSONException, ParseException {
        final JSONObject obj = fromJSON(f);
        if (obj == null) {
            throw new JSONException("Backup file cannot be loadedd");
        }
        this.type = EnumUtils.getByName(Type.class, obj.getString("type"), Type.USER);
        this.name = obj.getString("name");
        this.timestamp = SDF.parse(obj.getString("timestamp"));
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFileName() {
        return getFileName(type, timestamp);
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject root = new JSONObject();
        root.put("type", type.name());
        root.put("name", LengthUtils.safeString(name, "auto"));
        root.put("timestamp", SDF.format(timestamp));
        return root;
    }

    public static String getFileName(final Type type, final Date timestamp) {
        return type.name() + "." + SDF.format(timestamp) + ".jso";
    }

    public static JSONObject fromJSON(final File file) {
        final StringBuilder buf = new StringBuilder();
        try {
            final BufferedReader in = new BufferedReader(new FileReader(file));
            try {
                for (String s = in.readLine(); s != null; s = in.readLine()) {
                    buf.append(s).append("\n");
                }
                return new JSONObject(buf.toString());
            } catch (final Exception ex) {
                BackupManager.LCTX.e("Reading backup file failed: " + ex.getMessage());
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final FileNotFoundException ex) {
            BackupManager.LCTX.e("Reading backup file failed: " + ex.getMessage());
        }
        return null;
    }

    public static enum Type {
        AUTO, USER;
    }

}
