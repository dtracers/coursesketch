package coursesketch.server.base;

import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import coursesketch.server.interfaces.ISocketInitializer;
import coursesketch.server.interfaces.MultiConnectionManager;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

/**
 * The default servlet it creates a single websocket instance that is then used
 * on all messages.
 *
 * To create a custom management of the connections use this version
 *
 * @author gigemjt
 */
@WebServlet(name = "Course Sketch WebSocket Servlet", urlPatterns = { "/websocket" })
public class ServerWebSocketInitializer extends WebSocketServlet implements ISocketInitializer {

    /**
     * The server that the servlet is connected to.
     */
    private final AbstractServerWebSocketHandler connectionServer;

    /**
     * The {@link MultiConnectionManager} that is used by the servlet to recieve
     * connections.
     */
    private final MultiConnectionManager manager;

    /**
     * The amount of time it takes before a connection times out.
     */
    private final long timeoutTime;

    /**
     * True if the server is allowing secure connections.
     */
    private final boolean secure;

    /**
     * Creates a GeneralConnectionServlet.
     * @param iTimeoutTime The time it takes before a connection times out.
     * @param iSecure True if the connection is allowing SSL connections.
     * @param connectLocally True if the server is connecting locally.
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public ServerWebSocketInitializer(final long iTimeoutTime, final boolean iSecure, final boolean connectLocally) {
        this.timeoutTime = iTimeoutTime;
        this.secure = iSecure;
        System.out.println("Creating a new connectionServer");
        connectionServer = createServerSocket();
        System.out.println("Creating a new connectionManager");
        manager = createConnectionManager(connectLocally, secure);
    }

    /**
     * If you want a different policy then you can override the configure
     * method.
     * @param factory The factory that is being configured.
     */
    @Override
    public final void configure(final WebSocketServletFactory factory) {
        System.out.println("Configuring servlet");
        if (timeoutTime > 0) {
            System.out.println("Adding a timeout to the socket: " + timeoutTime);
            factory.getPolicy().setIdleTimeout(timeoutTime);
        }
        factory.setCreator(new SocketCreator());
    }

    /**
     * A custom web socket creator that checks to make sure that the.
     *
     * @author gigemjt
     */
    private class SocketCreator implements WebSocketCreator {

        /**
         * Creates the new websocket. If the socket needs to be secure and it is
         * not secure then the socket creation fails and Null is returned.
         *
         * @param req The servlet upgrade request.
         * @param resp The servlet upgrade response.
         * @return the {@link ServerWebSocketHandler} that handles the websocket communication.
         */
        @Override
        public final Object createWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            System.out.println("Recieved Upgrade request");
            if (secure && !req.isSecure()) {
                System.out.println("Refusing an insecure connection");
                return null;
            }
            System.out.println("Returning a websocket with name " + connectionServer.getName());
            return connectionServer;
        }

    }

    /**
     * Stops the socket, and the server and drops all connections.
     */
    @Override
    public final void stop() {
        System.out.println("Stopping socket");
        connectionServer.stop();
        if (manager != null) {
            manager.dropAllConnection(true, false);
        }
    }

    /**
     * Override this method to create a subclass of GeneralConnectionServer.
     *
     * @return An instance of the {@link ServerWebSocketHandler}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public AbstractServerWebSocketHandler createServerSocket() {
        return new ServerWebSocketHandler(this);
    }

    /**
     * Override this method to create a subclass of the MultiConnectionManager.
     *
     * @param connectLocally True if the connection is acting as if it is on a local computer (used for testing)
     * @param iSecure True if the connection is using SSL.
     * @return An instance of the {@link MultiConnectionManager}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public MultiConnectionManager createConnectionManager(final boolean connectLocally, final boolean iSecure) {
        return new MultiConnectionManager(connectionServer, connectLocally, iSecure);
    }

    /**
     * This is called when the reconnect command is executed.
     *
     * By default this drops all connections and then calls
     *
     * @see coursesketch.server.interfaces.MultiConnectionManager#connectServers(coursesketch.server.interfaces.AbstractServerWebSocketHandler)
     */
    @Override
    public final void reconnect() {
        System.out.println("Reconnecting");
        if (manager != null) {
            manager.dropAllConnection(true, false);
            manager.connectServers(connectionServer);
        }
        onReconnect();
    }

    /**
     * @return The current number of current connections.
     */
    @Override
    public final int getCurrentConnectionNumber() {
        return connectionServer.getCurrentConnectionNumber();
    }

    /**
     * Called after reconnecting the connections.
     */
    protected void onReconnect() { }

    /**
     * @return the multiConnectionManager.  This is only used within this package.
     */
    /* package-private */ final MultiConnectionManager getManager() {
        return manager;
    }

    /**
     * @return the GeneralConnectionServer.
     */
    protected final AbstractServerWebSocketHandler getServer() {
        return connectionServer;
    }
}