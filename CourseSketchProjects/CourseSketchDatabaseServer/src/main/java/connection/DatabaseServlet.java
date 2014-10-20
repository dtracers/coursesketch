package connection;

import interfaces.IMultiConnectionManager;
import coursesketch.jetty.multiconnection.ServerWebSocket;
import coursesketch.jetty.multiconnection.GeneralConnectionServlet;

/**
 * A database specific servlet that creates a new Database server and Database
 * Connection Managers.
 *
 * @author gigemjt
 *
 */
public class DatabaseServlet extends GeneralConnectionServlet {

    /**
     * Constructor for DatabaseServlet.
     *
     * @param timeoutTime
     *            The time before a stale connection times out.
     * @param secure
     *            True if the connection should use SSL
     * @param connectLocally
     *            True if the connection is a local connection.
     */
    public DatabaseServlet(final long timeoutTime, final boolean secure, final boolean connectLocally) {
        super(timeoutTime, secure, connectLocally);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ServerWebSocket createServerSocket() {
        return new DatabaseServerWebSocket(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final IMultiConnectionManager createConnectionManager(final boolean connectLocally, final boolean secure) {
        return new DatabaseConnectionManager(getServer(), connectLocally, secure);
    }
}
