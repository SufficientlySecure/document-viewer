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

import android.annotation.TargetApi;
import android.net.http.AndroidHttpClient;
import android.webkit.URLUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.emdev.BaseDroidApp;
import org.emdev.common.archives.zip.ZipArchive;
import org.emdev.common.archives.zip.ZipArchiveEntry;
import org.emdev.common.cache.CacheManager;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.progress.UIFileCopying;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;
import org.emdev.utils.base64.Base64;

@TargetApi(8)
public class OPDSClient {

    private static final LogContext LCTX = LogManager.root().lctx("OPDS");

    private final AndroidHttpClient client;
    private final IEntryBuilder builder;

    private final Map<String, Map<String, Header>> hostParameters = new HashMap<String, Map<String, Header>>();

    public OPDSClient(final IEntryBuilder builder) {
        this.client = AndroidHttpClient.newInstance(BaseDroidApp.APP_PACKAGE + " " + BaseDroidApp.APP_VERSION);
        this.builder = builder;
    }

    @Override
    protected void finalize() {
        close();
    }

    public void close() {
        client.close();
    }

    public Feed loadFeed(final Feed feed, final IProgressIndicator progress) throws OPDSException {
        if (feed.link == null) {
            return feed;
        }

        final Header h1 = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml");
        final Header h2 = new BasicHeader("Accept-Charset", "UTF-8");

        try {
            final AtomicReference<URI> uri = createURI(feed.parent, feed.link.uri);
            final HttpResponse resp = connect(uri, h1, h2);
            final StatusLine statusLine = resp.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            if (statusCode == 401) {
                LCTX.i("Authorization required: " + statusLine);
                final Header[] allHeaders = resp.getAllHeaders();
                for (final Header header : allHeaders) {
                    LCTX.d("" + header.getName() + "=" + header.getValue());
                }
                String method = "Basic";
                final Header authHeader = resp.getFirstHeader("WWW-Authenticate");
                if (authHeader != null) {
                    method = LengthUtils.safeString(authHeader.getValue(), method).split(" ")[0];
                }
                throw new AuthorizationRequiredException(uri.get().getHost(), method);
            }

            if (statusCode != 200) {
                LCTX.e("Content cannot be retrived: " + statusLine);
                throw new HttpRequestFailed(statusCode, statusLine.getReasonPhrase());
            }

            progress.setProgressDialogMessage(R.string.opds_loading_catalog);

            final HttpEntity entity = resp.getEntity();

            final OPDSContentHandler h = new OPDSContentHandler(feed, builder);
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

            h.parse(buf.toString());

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

    private AtomicReference<URI> createURI(final Feed parent, String uri) throws URISyntaxException {
        URI reqUri = new URI(uri);
        if (reqUri.getHost() == null) {
            for (Feed p = parent; p != null; p = p.parent) {
                final URI parentURI = new URI(p.link.uri);
                if (parentURI.getHost() != null) {
                    reqUri = new URI(parentURI.getScheme(), parentURI.getHost(), reqUri.getPath(), reqUri.getFragment());
                    uri = reqUri.toASCIIString();
                    break;
                }
            }
        }
        return new AtomicReference<URI>(reqUri);
    }

    public File loadFile(final Feed parent, final Link link) {
        try {
            final AtomicReference<URI> uriRef = createURI(parent, link.uri);
            final HttpResponse resp = connect(uriRef);
            final HttpEntity entity = resp.getEntity();
            return CacheManager.createTempFile(entity.getContent(), ".opds");
        } catch (final Throwable th) {
            LCTX.e("Error on OPDS catalog access: ", th);
        }
        return null;
    }

    public File downloadBook(final Book book, final BookDownloadLink link, final IProgressIndicator progress)
            throws IOException {
        try {
            final AtomicReference<URI> uri = createURI(book.parent, link.uri);
            final HttpResponse resp = connect(uri);
            final StatusLine statusLine = resp.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            if (statusCode != 200) {
                LCTX.e("Content cannot be retrived: " + statusLine);
                return null;
            }

            final HttpEntity entity = resp.getEntity();
            final String contentDisposition = getHeaderValue(resp, "Content-Disposition");
            final String mimeType = getHeaderValue(entity.getContentType());
            final long contentLength = entity.getContentLength();

            LCTX.d("Content-Disposition: " + contentDisposition);
            LCTX.d("Content-Type: " + mimeType);
            LCTX.d("Content-Length: " + contentLength);

            final String guessFileName = URLUtil.guessFileName(uri.get().toString(), contentDisposition.replace("attachement", "attachment"), mimeType);

            LCTX.d("File name: " + guessFileName);

            // create a new file, specifying the path, and the filename which we want to save the file as.
            final File downloadDir = new File(OpdsSettings.current().downloadDir);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            final File file = new File(downloadDir, guessFileName);

            final boolean exists = file.exists() && file.length() == contentLength;

            // this will be used to write the downloaded data into the file we created
            try {
                if (!exists) {
                    final UIFileCopying worker = new UIFileCopying(R.string.opds_loading_book, 64 * 1024, progress);
                    final BufferedInputStream input = new BufferedInputStream(entity.getContent(), 64 * 1024);
                    final BufferedOutputStream fileOutput = new BufferedOutputStream(new FileOutputStream(file), 256 * 1024);
                    worker.copy(contentLength, input, fileOutput);
                }
                if (OpdsSettings.current().unpackArchives && link.isZipped && !link.bookType.isZipSupported()) {
                    return unpack(file, progress);
                }
            } catch (final ClosedByInterruptException ex) {
                try {
                    file.delete();
                } catch (final Exception ex1) {
                }
            }

            return file;
        } catch (final IOException ex) {
            LCTX.e("Error on downloading book: " + ex.getMessage());
            throw ex;
        } catch (final Throwable th) {
            LCTX.e("Error on downloading book: ", th);
        }
        return null;
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
                        LCTX.d("Unpacked file name: " + entryFile.getAbsolutePath());

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
                LCTX.e("Error on unpacking book: ", ex);
                try {
                    archive.close();
                } catch (final Exception ex1) {
                }
            }
        } catch (final Exception ex) {
            LCTX.e("Error on unpacking book: ", ex);
        }
        return file;
    }

    private void release(final File file, final ZipArchive archive) {
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

    public void setAuthorization(final String host, final String method, final String username, final String password)
            throws OPDSException {
        Map<String, Header> params = hostParameters.get(host);
        if (params == null) {
            params = new HashMap<String, Header>();
            hostParameters.put(host, params);
        }
        if ("Basic".equalsIgnoreCase(method)) {
            params.put(
                    "Authorization",
                    new BasicHeader("Authorization", method + " "
                            + Base64.encodeToString((username + ":" + password).getBytes(), 0)));
        } else {
            throw new OPDSException("Required authorization method not supported: " + method);
        }
    }

    protected HttpResponse connect(final AtomicReference<URI> uriRef, final Header... headers) throws IOException,
            URISyntaxException {
        URI uri = uriRef.get();
        HttpGet req = createRequest(uri, headers);

        LCTX.d("Connecting to: " + req.getURI());
//        for (final Header h : req.getAllHeaders()) {
//            LCTX.d("Connecting to: " + "\t" + h.getName() + "=" + h.getValue());
//        }

        HttpResponse resp = client.execute(req);
        StatusLine statusLine = resp.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        LCTX.d("Status: " + statusLine);
        int redirectCount = 5;
        while (redirectCount > 0 && (statusCode == 301 || statusCode == 302)) {
            redirectCount--;

            final String location = getHeaderValue(resp, "Location");
            if (LengthUtils.isEmpty(location)) {
                break;
            }

            uri = new URI(location);
            uriRef.set(uri);
            LCTX.d("Location: " + uri);

            req = createRequest(uri, headers);
            LCTX.d("Connecting to: " + req.getURI());
            resp = client.execute(req);
            statusLine = resp.getStatusLine();
            statusCode = statusLine.getStatusCode();
            LCTX.d("Status: " + statusLine);
        }

        return resp;
    }

    protected HttpGet createRequest(final URI uri, final Header... headers) {
        final HttpGet req = new HttpGet(uri);
        final String host = uri.getHost();
        req.setHeader("Host", host);
        req.setHeader("User-Agent", "EBookDroid OPDS browser");

        if (LengthUtils.isNotEmpty(headers)) {
            for (final Header h : headers) {
                req.setHeader(h);
            }
        }
        final Map<String, Header> params = hostParameters.get(host);
        if (LengthUtils.isNotEmpty(params)) {
            for (final Header h : params.values()) {
                req.setHeader(h);
            }
        }
        return req;
    }

    protected static String getHeaderValue(final HttpResponse resp, final String name) {
        final Header header = resp.getFirstHeader(name);
        return header != null ? header.getValue() : null;
    }

    protected static String getHeaderValue(final Header header) {
        return header != null ? header.getValue() : null;
    }

}
