package org.emdev.utils.archives.rar;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.common.log.LogContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

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
                LCTX.d("Unrar executing: ");
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
        return Runtime.getRuntime().exec(cmd);
    }

    private static boolean copy(final File unrar) {
        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(EBookDroidApp.context.getResources().getAssets().open("unrar/unrar"));
            out = Channels.newChannel(new FileOutputStream(unrar));
            final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
            while (in.read(buf) > 0) {
                buf.flip();
                out.write(buf);
                buf.flip();
            }
            return true;
        } catch (final IOException ex) {
            LCTX.e("Unrar executable cannot be copied from assets: " + ex.getMessage());
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        }

    }

    private static boolean chmod(final File unrar) {
        final String[] cmd = { "chmod", "777", unrar.getAbsolutePath() };
        try {
            final Process p = Runtime.getRuntime().exec(cmd);
            int res = p.waitFor();
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
