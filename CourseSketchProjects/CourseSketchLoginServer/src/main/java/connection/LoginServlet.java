package connection;

import interfaces.IMultiConnectionManager;
import coursesketch.jetty.multiconnection.ServerWebSocket;
import coursesketch.jetty.multiconnection.GeneralConnectionServlet;

/**
 * Creates a servlet specific to the login server.
 */
@SuppressWarnings("serial")
public final class LoginServlet extends GeneralConnectionServlet {

    /**
     * Creates a GeneralConnectionServlet.
     *
     * @param timeoutTime
     *            The time it takes before a connection times out.
     * @param secure
     *            True if the connection is allowing SSL connections.
     * @param connectLocally
     *            True if the server is connecting locally.
     */
    public LoginServlet(final long timeoutTime, final boolean secure, final boolean connectLocally) {
        super(timeoutTime, secure, connectLocally);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerWebSocket createServerSocket() {
        return new LoginServerWebSocket(this);
    }

    /**
     * We do not need to manage multiple connections so we might as well just
     * make it return null.
     *
     * @param connectLocally
     *            <code>true</code> if the connection manager should use local
     *            connections, <code>false</code> otherwise
     * @param secure
     *            <code>true</code> if the connections should be secured,
     *            <code>false</code> otherwise
     *
     * @return a new connection manager object
     */
    @Override
    protected IMultiConnectionManager createConnectionManager(final boolean connectLocally, final boolean secure) {
        return null;
    }
}
