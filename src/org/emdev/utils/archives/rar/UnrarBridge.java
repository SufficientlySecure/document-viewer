package org.emdev.utils.archives.rar;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.common.log.LogContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.emdev.utils.FileUtils;

public class UnrarBridge {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Unrar");
    private static File unrar;

    public static boolean init() {
        unrar = new File(EBookDroidApp.context.getFilesDir(), "unrar");
        if (unrar != null && unrar.isFile()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Unrar executable found");
            }
            return true;
        }

        if (copy(unrar) && chmod(unrar)) {
            return true;
        }

        unrar = null;
        return false;
    }

    public static Process exec(final String... args) throws IOException {
        if (init()) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Unrar executing: " + Arrays.toString(args));
            }
            return execImpl(args);
        }

        throw new IOException("No unrar executable installed");
    }

    private static Process execImpl(final String... args) throws IOException {
        final String[] cmd = new String[1 + args.length];
        cmd[0] = unrar.getAbsolutePath();

        if (args.length > 0) {
            System.arraycopy(args, 0, cmd, 1, args.length);
        }
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        return pb.start();
    }

    private static boolean copy(final File unrar) {
        try {
            final InputStream source = EBookDroidApp.context.getResources().getAssets().open("unrar/unrar");
            final FileOutputStream target = new FileOutputStream(unrar);
            FileUtils.copy(source, target);
            return true;
        } catch (final IOException ex) {
            LCTX.e("Unrar executable cannot be copied from assets: " + ex.getMessage());
            return false;
        }
    }

    private static boolean chmod(final File unrar) {
        final String[] cmd = { "chmod", "777", unrar.getAbsolutePath() };
        try {
            final Process p = Runtime.getRuntime().exec(cmd);
            final int res = p.waitFor();
            return 0 == res;
        } catch (final IOException ex) {
            LCTX.e("Unrar executable cannot be copied from assets: " + ex.getMessage());
            return false;
        } catch (final InterruptedException ex) {
            Thread.interrupted();
            return false;
        }
    }

}
