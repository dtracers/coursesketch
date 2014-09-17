package multiconnection;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * The default servlet it creates a single websocket instance that is then used
 * on all messages.
 * 
 * To create a custom managment of the connections use this version
 * 
 * @author gigemjt
 */
@SuppressWarnings("serial")
@WebServlet(name = "Course Sketch WebSocket Servlet", urlPatterns = { "/" })
public class GeneralConnectionServlet extends WebSocketServlet {

    /**
     * The server that the servlet is connected to.
     */
    protected GeneralConnectionServer connectionServer;

    /**
     * The {@link MultiConnectionManager} that is used by the servlet to recieve
     * connections.
     */
    protected MultiConnectionManager manager;
    private long timeoutTime = 0;
    private boolean secure;

    public GeneralConnectionServlet(long timeoutTime, boolean secure, boolean connectLocally) {
        this.timeoutTime = timeoutTime;
        this.secure = secure;
        System.out.println("Creating a new connectionServer");
        connectionServer = createServerSocket();
        System.out.println("Creating a new connectionManager");
        manager = createConnectionManager(connectLocally, secure);
    }

    /**
     * If you want a different policy then you can override the configure
     * method.
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
    class SocketCreator implements WebSocketCreator {

        /**
         * Creates the new websocket. If the socket needs to be secure and it is
         * not secure then the socket creation fails and Null is returned.
         */
        @Override
        public final Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
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
     * @return
     */
    protected GeneralConnectionServer createServerSocket() {
        return new GeneralConnectionServer(this);
    }

    /**
     * Override this method to create a subclass of the GeneralConnectionServer
     * 
     * @param connectLocally
     * @return
     */
    protected MultiConnectionManager createConnectionManager(boolean connectLocally, boolean secure) {
        return new MultiConnectionManager(connectionServer, connectLocally, secure);
    }

    /**
     * This is called when the reconnect command is executed.
     *
     * By default this drops all connections and then calls
     *
     * @see multiconnection.MultiConnectionManager#connectServers(GeneralConnectionServer)
     */
    public void reconnect() {
        System.out.println("Reconnecting");
        if (manager != null) {
            manager.dropAllConnection(true, false);
            manager.connectServers(connectionServer);
        }
    }

    public int getCurrentConnectionNumber() {
        return connectionServer.getCurrentConnectionNumber();
    }
}