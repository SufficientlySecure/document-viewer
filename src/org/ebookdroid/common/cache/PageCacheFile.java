package org.ebookdroid.common.cache;

import org.ebookdroid.common.cache.DocumentCacheFile.DocumentInfo;
import org.ebookdroid.common.cache.DocumentCacheFile.PageInfo;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.emdev.common.log.LogContext;

@Deprecated
public class PageCacheFile extends File {

    private static final long serialVersionUID = 6836895806027391288L;

    private static final LogContext LCTX = CacheManager.LCTX;

    PageCacheFile(final File dir, final String name) {
        super(dir, name);
    }

    public DocumentInfo load() {
        try {
            LCTX.d("Loading pages cache...");
            final DataInputStream in = new DataInputStream(new FileInputStream(this));
            try {
                final DocumentInfo info = new DocumentInfo();
                info.docPageCount = in.readInt();
                info.viewPageCount = -1;

                for (int i = 0; i < info.docPageCount; i++) {
                    final PageInfo pi = new PageInfo(i);
                    pi.info = new CodecPageInfo(in.readInt(), in.readInt());
                    info.docPages.append(i, pi);
                    if (pi.info.width == -1 || pi.info.height == -1) {
                        return null;
                    }
                }
                LCTX.d("Loading pages cache finished");
                return info;
            } catch (final EOFException ex) {
                LCTX.e("Loading pages cache failed: " + ex.getMessage());
            } catch (final IOException ex) {
                LCTX.e("Loading pages cache failed: " + ex.getMessage());
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final FileNotFoundException ex) {
            LCTX.e("Loading pages cache failed: " + ex.getMessage());
        }
        return null;
    }
}
