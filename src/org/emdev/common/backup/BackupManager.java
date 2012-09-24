package org.emdev.common.backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.emdev.BaseDroidApp;
import org.emdev.common.backup.BackupInfo.Type;
import org.emdev.common.filesystem.CompositeFilter;
import org.emdev.common.filesystem.FileExtensionFilter;
import org.emdev.common.filesystem.FilePrefixFilter;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.LengthUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackupManager {

    public static final LogContext LCTX = LogManager.root().lctx("BackupManager");

    static File BACKUP_FOLDER = new File(BaseDroidApp.APP_STORAGE, "backups");

    private static final FileExtensionFilter BACKUP_FILTER = new FileExtensionFilter("jso");

    private static final CompositeFilter AUTO_BACKUP_FILTER = new CompositeFilter(true, new FilePrefixFilter(Type.AUTO
            .name().toLowerCase() + "."), BACKUP_FILTER);

    private static final Map<String, IBackupAgent> agents = new TreeMap<String, IBackupAgent>();

    private static int maxNumberOfAutoBackups = 1;

    public static void addAgent(final IBackupAgent agent) {
        agents.put(agent.key(), agent);
    }

    public static Collection<BackupInfo> getAvailableBackups() {
        final Set<BackupInfo> backups = new TreeSet<BackupInfo>();

        BACKUP_FOLDER.mkdirs();
        final File[] list = BACKUP_FILTER.listFiles(BACKUP_FOLDER);
        for (final File f : list) {
            try {
                backups.add(new BackupInfo(f));
            } catch (final Exception ex) {
            }
        }
        return backups;
    }

    public static List<BackupInfo> getAutoBackups() {
        final Set<BackupInfo> backups = new TreeSet<BackupInfo>();

        BACKUP_FOLDER.mkdirs();

        final File[] files = BACKUP_FOLDER.listFiles(AUTO_BACKUP_FILTER);
        if (LengthUtils.isNotEmpty(files)) {
            for (final File f : files) {
                try {
                    backups.add(new BackupInfo(f));
                } catch (final Exception ex) {
                    f.delete();
                }
            }
        }
        return new ArrayList<BackupInfo>(backups);
    }

    public static boolean backup() {
        final List<BackupInfo> old = getAutoBackups();
        if (backupImpl(new BackupInfo())) {
            final int count = LengthUtils.length(old);
            if (count + 1 > maxNumberOfAutoBackups) {
                for (int i = maxNumberOfAutoBackups - 1; i < count; i++) {
                    remove(old.get(i));
                }
            }
            return true;
        }
        return false;
    }

    public static boolean backup(final BackupInfo backup) {
        if (backup.type == BackupInfo.Type.USER) {
            return backupImpl(backup);
        }
        return false;
    }

    public static boolean restore(final BackupInfo backup) {
        return restoreImpl(backup);
    }

    public static boolean remove(final BackupInfo backup) {
        final File file = new File(BACKUP_FOLDER, backup.getFileName());
        return file.delete();
    }

    static boolean backupImpl(final BackupInfo backup) {
        try {
            backup.timestamp = new Date();
            final String json = buildBackup(backup);

            BACKUP_FOLDER.mkdirs();

            final File file = new File(BACKUP_FOLDER, backup.getFileName());
            final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(json);
            writer.close();

            return true;
        } catch (final Throwable th) {
            LCTX.e("Unexpected error", th);
        }
        return false;
    }

    private static String buildBackup(final BackupInfo backup) throws JSONException {
        final JSONObject root = backup.toJSON();

        final JSONObject chunks = new JSONObject();
        root.put("chunks", chunks);
        for (final IBackupAgent agent : agents.values()) {
            chunks.put(agent.key(), agent.backup());
        }

        return root.toString(2);
    }

    static boolean restoreImpl(final BackupInfo backup) {
        final File file = new File(BACKUP_FOLDER, backup.getFileName());
        if (!file.exists()) {
            return false;
        }

        final Map<String, JSONObject> m = new HashMap<String, JSONObject>();

        try {
            final JSONObject root = BackupInfo.fromJSON(file);
            final JSONObject chunks = root.optJSONObject("chunks");
            final JSONArray names = chunks != null ? chunks.names() : null;
            if (LengthUtils.isNotEmpty(names)) {
                for (int i = 0, n = names.length(); i < n; i++) {
                    final String key = names.getString(i);
                    final JSONObject value = chunks.getJSONObject(key);
                    m.put(key, value);
                }
            }
        } catch (final JSONException ex) {
            LCTX.e("JSON parsing error: " + ex.getMessage());
            return false;
        }

        for (final Map.Entry<String, JSONObject> e : m.entrySet()) {
            final IBackupAgent agent = agents.get(e.getKey());
            if (agent != null) {
                agent.restore(e.getValue());
            } else {
                LCTX.w("No agent found: " + e.getKey());
            }
        }

        return true;
    }

    public static int getMaxNumberOfAutoBackups() {
        return maxNumberOfAutoBackups;
    }

    public static void setMaxNumberOfAutoBackups(int maxNumberOfAutoBackups) {
        BackupManager.maxNumberOfAutoBackups = maxNumberOfAutoBackups;
    }
}
