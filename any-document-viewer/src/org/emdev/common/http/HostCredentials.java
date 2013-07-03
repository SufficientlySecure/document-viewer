package org.emdev.common.http;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.base64.Base64;

public class HostCredentials extends UsernamePasswordCredentials {

    public final String host;
    private final AtomicReference<State> state = new AtomicReference<HostCredentials.State>(State.CREATED);

    public HostCredentials(final String host, final String userName, final String password) {
        super(userName, LengthUtils.safeString(password));
        this.host = host;
    }

    public State getState() {
        return state.get();
    }

    public void setState(final State newState) {
        state.set(newState);
    }

    public boolean setState(final State oldState, final State newState) {
        return state.compareAndSet(oldState, newState);
    }

    public Header basicAuthorization() {
        final String raw = getUserName() + ":" + getPassword();
        final String encoded = Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP);
        return new BasicHeader("Authorization", "Basic " + encoded);
    }

    public static enum State {
        CREATED, AUTH_ASKED, AUTH_ENTERED, AUTH_SENT, AUTH_FAILED;
    }
}
