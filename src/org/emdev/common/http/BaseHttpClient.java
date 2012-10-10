package org.emdev.common.http;

import org.ebookdroid.opds.exceptions.OPDSException;

import android.annotation.TargetApi;
import android.net.http.AndroidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.LengthUtils;

@TargetApi(8)
public class BaseHttpClient {

    private static final LogContext LCTX = LogManager.root().lctx("HTTP");

    private final String userAgent;

    private final AndroidHttpClient client;

    private final BasicAuthenticator auth;

    public BaseHttpClient(final String userAgent) {
        this.userAgent = userAgent;
        this.client = AndroidHttpClient.newInstance(userAgent);
        this.auth = new BasicAuthenticator();
    }

    @Override
    protected void finalize() {
        close();
    }

    public void close() {
        client.close();
    }

    public void setAuthorization(final String host, final String username, final String password) {
        auth.setAuthorization(host, username, password);
    }

    protected HttpGet createRequest(final URI uri, final Header... headers) {
        final HttpGet req = new HttpGet(uri);
        final String host = uri.getHost();
        req.setHeader("Host", host);
        req.setHeader("User-Agent", userAgent);

        if (LengthUtils.isNotEmpty(headers)) {
            for (final Header h : headers) {
                req.setHeader(h);
            }
        }
        return req;
    }

    protected HttpResponse connect(final AtomicReference<URI> uriRef, final Header... headers) throws IOException,
            URISyntaxException, OPDSException {
        URI uri = uriRef.get();

        HttpGet req = createRequest(uri, headers);
        onPreAuthentication(req);
        LCTX.d(Thread.currentThread().getName() + ": Connecting to: " + req.getURI());

        HttpResponse resp = client.execute(req);
        StatusLine statusLine = resp.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        LCTX.d(Thread.currentThread().getName() + ": Status: " + statusLine);
        int redirectCount = 5;

        while (redirectCount > 0 && (statusCode == 401 || statusCode == 301 || statusCode == 302)) {
            if (statusCode == 401) {
                final HostCredentials creds = onAuthorizationAsked(req, resp);
                req = createRequest(uri, headers);
                req.setHeader(creds.basicAuthorization());
            } else {
                redirectCount--;
                final String location = getHeaderValue(resp, "Location");
                if (LengthUtils.isEmpty(location)) {
                    break;
                }
                uri = uri.resolve(new URI(location));

                uriRef.set(uri);
                LCTX.d(Thread.currentThread().getName() + ": Location: " + uri);

                req = createRequest(uri, headers);
                onPreAuthentication(req);
            }

            LCTX.d(Thread.currentThread().getName() + ": Connecting to: " + req.getURI());
            resp = client.execute(req);
            statusLine = resp.getStatusLine();
            statusCode = statusLine.getStatusCode();
            LCTX.d(Thread.currentThread().getName() + ": Status: " + statusLine);
        }

        return resp;
    }

    public void onPreAuthentication(final HttpGet req) {
        final HostCredentials cred = auth.onPreAuthorization(req);
        if (cred != null) {
            req.setHeader(cred.basicAuthorization());
        }
    }

    protected HostCredentials onAuthorizationAsked(final HttpGet req, final HttpResponse resp) throws OPDSException {
        String method = "Basic";
        final Header authHeader = resp.getFirstHeader("WWW-Authenticate");
        if (authHeader != null) {
            method = LengthUtils.safeString(authHeader.getValue(), method).split(" ")[0];
        }
        if (!"Basic".equalsIgnoreCase(method)) {
            throw new OPDSException("Required authorization method not supported: " + method);
        }

        return auth.onAuthorizationAsked(req);
    }

    protected static String getHeaderValue(final HttpResponse resp, final String name) {
        final Header header = resp.getFirstHeader(name);
        return header != null ? header.getValue() : "";
    }

    protected static String getHeaderValue(final Header header) {
        return header != null ? header.getValue() : "";
    }
}
