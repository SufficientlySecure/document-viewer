package org.ebookdroid.opds;

import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.settings.OpdsSettings;
import org.ebookdroid.opds.exceptions.AuthorizationRequiredException;
import org.ebookdroid.opds.exceptions.HttpRequestFailed;
import org.ebookdroid.opds.exceptions.OPDSException;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;
import org.ebookdroid.opds.xml.OPDSContentHandler;

import android.annotation.TargetApi;
import android.webkit.URLUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.emdev.BaseDroidApp;
import org.emdev.common.archives.zip.ZipArchive;
import org.emdev.common.archives.zip.ZipArchiveEntry;
import org.emdev.common.cache.CacheManager;
import org.emdev.common.http.BaseHttpClient;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.progress.UIFileCopying;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

@TargetApi(8)
public class OPDSClient extends BaseHttpClient {

    private static final LogContext LCTX = LogManager.root().lctx("OPDS");

    private final IEntryBuilder builder;

    public OPDSClient(final IEntryBuilder builder) {
        super(BaseDroidApp.APP_PACKAGE + " " + BaseDroidApp.APP_VERSION_NAME);
        this.builder = builder;
    }

    @Override
    protected void finalize() {
        close();
    }

    public Feed loadFeed(final Feed feed, final IProgressIndicator progress) throws OPDSException {
        if (feed.link == null) {
            return feed;
        }

        final Header h1 = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml");
        final Header h2 = new BasicHeader("Accept-Charset", "UTF-8");

        try {
            final AtomicReference<URI> uri = createURI(feed.parent, feed.link.uri);
            if (feed.parent == null && LengthUtils.isNotEmpty(feed.login)) {
                setAuthorization(uri.get().getHost(), feed.login, feed.password);
            }
            final HttpResponse resp = connect(uri, h1, h2);
            final StatusLine statusLine = resp.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            if (statusCode != 200) {
                LCTX.e("Content cannot be retrived: " + statusLine);
                throw new HttpRequestFailed(statusLine);
            }

            progress.setProgressDialogMessage(R.string.opds_loading_catalog);

            final HttpEntity entity = resp.getEntity();

            final Header enc = entity.getContentEncoding();
            final String encoding = LengthUtils.safeString(enc != null ? enc.getValue() : "", "UTF-8");

            final BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent(), encoding),
                    MathUtils.adjust((int) entity.getContentLength(), 4 * 1024, 64 * 1024));

            final StringBuilder buf = new StringBuilder();
            for (String s = in.readLine(); s != null; s = in.readLine()) {
                buf.append(s).append("\n");
            }
            in.close();

            // System.out.println(buf);

            // Workaround for Calibre (tested with 0.8.38)
            if (buf.indexOf("<title>..:: calibre library ::.. </title>") >= 0) {
                LCTX.w("The feed link is calibre root URI: " + feed.link.uri);
                feed.link = new Link(feed.link.uri + "/opds");
                return loadFeed(feed, progress);
            }

            final OPDSContentHandler h = new OPDSContentHandler(feed, builder);
            h.parse(buf.toString());

        } catch (final InterruptedIOException ex) {
            LCTX.e("Error on OPDS catalog access: " + ex.getMessage());
            throw new OPDSException(ex);
        } catch (final OPDSException ex) {
            LCTX.e("Error on OPDS catalog access: " + ex.getMessage());
            throw ex;
        } catch (final Throwable th) {
            LCTX.e("Error on OPDS catalog access: ", th);
            throw new OPDSException(th);
        }

        feed.loadedAt = System.currentTimeMillis();
        return feed;
    }

    public File downloadBook(final Book book, final BookDownloadLink link, final IProgressIndicator progress)
            throws OPDSException {
        try {
            final AtomicReference<URI> uri = createURI(book.parent, link.uri);
            final HttpResponse resp = connect(uri);
            final StatusLine statusLine = resp.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            if (statusCode != 200) {
                LCTX.e(Thread.currentThread().getName() + ": Content cannot be retrived: " + statusLine);
                throw new HttpRequestFailed(statusLine);
            }

            final HttpEntity entity = resp.getEntity();
            final String contentDisposition = getHeaderValue(resp, "Content-Disposition");
            final String mimeType = getHeaderValue(entity.getContentType());
            final long contentLength = entity.getContentLength();

            LCTX.d(Thread.currentThread().getName() + ": Content-Disposition: " + contentDisposition);
            LCTX.d(Thread.currentThread().getName() + ": Content-Type: " + mimeType);
            LCTX.d(Thread.currentThread().getName() + ": Content-Length: " + contentLength);

            final String guessFileName = URLUtil.guessFileName(uri.get().toString(),
                    contentDisposition.replace("attachement", "attachment"), mimeType);

            LCTX.d(Thread.currentThread().getName() + ": File name: " + guessFileName);

            // create a new file, specifying the path, and the filename which we want to save the file as.
            final File downloadDir = new File(OpdsSettings.current().downloadDir);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            final File file = new File(downloadDir, guessFileName);
            final File tmpFile = new File(downloadDir, guessFileName + ".part");

            final boolean exists = file.exists() && file.length() == contentLength;

            // this will be used to write the downloaded data into the file we created
            try {
                if (!exists) {
                    final UIFileCopying worker = new UIFileCopying(R.string.opds_loading_book, 64 * 1024, progress);
                    final BufferedInputStream input = new BufferedInputStream(entity.getContent(), 64 * 1024);
                    final BufferedOutputStream tmpFileOutput = new BufferedOutputStream(new FileOutputStream(tmpFile),
                            256 * 1024);
                    worker.copy(contentLength, input, tmpFileOutput);
                    tmpFile.renameTo(file);
                }
                if (OpdsSettings.current().unpackArchives && link.isZipped && !link.bookType.isZipSupported()) {
                    return unpack(file, progress);
                }
            } catch (final ClosedByInterruptException ex) {
                try {
                    file.delete();
                    tmpFile.delete();
                } catch (final Exception ex1) {
                }
            }

            return file;
        } catch (final OPDSException ex) {
            throw ex;
        } catch (final IOException ex) {
            LCTX.e(Thread.currentThread().getName() + ": Error on downloading book: " + ex.getMessage());
            throw new OPDSException(ex);
        } catch (final Throwable th) {
            LCTX.e(Thread.currentThread().getName() + ": Error on downloading book: ", th);
            throw new OPDSException(th);
        }
    }

    public File loadFile(final Feed parent, final Link link) {
        try {
            final AtomicReference<URI> uriRef = createURI(parent, link.uri);
            final HttpResponse resp = connect(uriRef);
            final HttpEntity entity = resp.getEntity();
            return CacheManager.createTempFile(entity.getContent(), ".opds");
        } catch (final ClosedByInterruptException ex) {
            Thread.interrupted();
            LCTX.w(Thread.currentThread().getName() + ": Thumbnail loading interrupted");
        } catch (final InterruptedIOException ex) {
            Thread.interrupted();
            LCTX.w(Thread.currentThread().getName() + ": Thumbnail loading interrupted");
        } catch (final AuthorizationRequiredException ex) {
            // No thumbnails without authentication
        } catch (final OPDSException ex) {
            LCTX.e(Thread.currentThread().getName() + ": Error on OPDS thumbnail loading: ", ex.getCause());
        } catch (final Throwable th) {
            LCTX.e(Thread.currentThread().getName() + ": Error on OPDS catalog access: ", th);
        }
        return null;
    }

    protected AtomicReference<URI> createURI(final Feed parent, String uri) throws URISyntaxException {
        URI reqUri = new URI(uri);
        if (reqUri.getHost() == null) {
            for (Feed p = parent; p != null; p = p.parent) {
                final URI parentURI = new URI(p.link.uri);
                if (parentURI.isAbsolute()) {
                    reqUri = parentURI.resolve(reqUri);
                    uri = reqUri.toASCIIString();
                    break;
                }
            }
        }
        return new AtomicReference<URI>(reqUri);
    }

    protected File unpack(final File file, final IProgressIndicator progress) {
        try {
            final ZipArchive archive = new ZipArchive(file);
            try {
                final Enumeration<ZipArchiveEntry> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    final ZipArchiveEntry entry = entries.nextElement();
                    final CodecType codecType = CodecType.getByUri(entry.getName());
                    if (codecType != null) {

                        final File entryFile = new File(file.getParentFile(), entry.getName());
                        LCTX.d(Thread.currentThread().getName() + ": Unpacked file name: " + entryFile.getAbsolutePath());

                        final int bufsize = 256 * 1024;
                        final UIFileCopying worker = new UIFileCopying(R.string.opds_unpacking_book, bufsize, progress);

                        final BufferedInputStream in = new BufferedInputStream(entry.open(), bufsize);
                        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(entryFile),
                                bufsize);
                        worker.copy(entry.getSize(), in, out);

                        release(file, archive);
                        return entryFile;
                    }
                }
            } catch (final ClosedByInterruptException ex) {
                release(file, archive);
            } catch (final Exception ex) {
                LCTX.e(Thread.currentThread().getName() + ": Error on unpacking book: ", ex);
                try {
                    archive.close();
                } catch (final Exception ex1) {
                }
            }
        } catch (final Exception ex) {
            LCTX.e(Thread.currentThread().getName() + ": Error on unpacking book: ", ex);
        }
        return file;
    }

    protected void release(final File file, final ZipArchive archive) {
        try {
            archive.close();
        } catch (final Exception ex1) {
        }
        if (OpdsSettings.current().deleteArchives) {
            try {
                file.delete();
            } catch (final Exception ex) {
            }
        }
    }
}
