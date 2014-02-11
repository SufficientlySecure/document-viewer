package org.emdev.common.http;

import org.ebookdroid.opds.exceptions.AuthorizationRequiredException;
import org.ebookdroid.opds.exceptions.OPDSException;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.emdev.common.http.HostCredentials.State;

public class BasicAuthenticator {

    private final Map<String, HostCredentials> credentials = new HashMap<String, HostCredentials>();

    public void setAuthorization(final String host, final String username, final String password) {
        final HostCredentials newCred = new HostCredentials(host, username, password);
        final HostCredentials oldCred = credentials.put(host, newCred);
        if (oldCred != null && oldCred.getState() == State.AUTH_ASKED) {
            newCred.setState(State.AUTH_ENTERED);
        }
    }

    public HostCredentials onPreAuthorization(final HttpGet req) {
        final String host = req.getURI().getHost();
        final HostCredentials cred = credentials.get(host);
        final State state = cred != null ? cred.getState() : null;

        if (state == State.AUTH_ENTERED || state == State.AUTH_SENT) {
            cred.setState(State.AUTH_ENTERED, State.AUTH_SENT);
            return cred;
        }

        return null;
    }

    public HostCredentials onAuthorizationAsked(final HttpGet req) throws OPDSException {
        final String host = req.getURI().getHost();
        HostCredentials cred = credentials.get(host);

        if (cred == null) {
            cred = new HostCredentials(host, "", "");
            cred.setState(State.AUTH_ASKED);
            credentials.put(host, cred);
            throw new AuthorizationRequiredException(host, "Basic");
        }

        if (req.getFirstHeader("Authorization") != null) {
            cred.setState(State.AUTH_SENT, State.AUTH_FAILED);
        }

        final State state = cred.getState();
        if (state == State.AUTH_ASKED || state == State.AUTH_FAILED) {
            throw new AuthorizationRequiredException(host, "Basic");
        }

        cred.setState(State.AUTH_SENT);
        return cred;
    }

}
