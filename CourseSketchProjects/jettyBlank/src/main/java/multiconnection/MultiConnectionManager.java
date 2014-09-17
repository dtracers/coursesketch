package multiconnection;

import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import connection.ConnectionException;
import protobuf.srl.request.Message.Request;

/**
 * A manager for holding all of the connections that were created.
 * @author gigemjt
 *
 */
public class MultiConnectionManager {

    /**
     * This value signifies that the server will connect to a local host.
     */
    public static final boolean CONNECT_LOCALLY = true;

    /**
     * This value signifies that the server will connect to a remote host.
     */
    public static final boolean CONNECT_REMOTE = false;

    /**
     * Determines whether the server is being connected locally.
     *
     * Can be overridden.
     */
    protected boolean connectLocally = CONNECT_LOCALLY;

    /**
     * Determines whether the server will be connecting securely.
     *
     * Can be overridden.
     */
    protected boolean secure = false;

    /**
     * A map that contains a list of connections that are differentiated by a specific class.
     */
    HashMap<Class<?>, ArrayList<ConnectionWrapper>> connections = new HashMap<Class<?>, ArrayList<ConnectionWrapper>>();

    /**
     * The server that using this {@link MultiConnectionManager}.
     */
    protected GeneralConnectionServer parent; // TODO: CHANGE THIS

    /**
     * Creates a default {@link MultiConnectionManager}.
     *
     * @param parent
     *            The server that is using this object.
     * @param isLocal
     *            True if the connection should be for a local server instead of
     *            a remote server.
     * @param secure
     *            True if the connections should be secure.
     */
    public MultiConnectionManager(final GeneralConnectionServer parent, final boolean isLocal, final boolean secure) {
        this.parent = parent;
        this.connectLocally = isLocal;
        this.secure = secure;
    }

    /**
     * Creates a connection given the different information.
     *
     * @param serv
     *            The server that is connected to this connection manager.
     * @param isLocal
     *            If the connection that is being created is local or remote.
     * @param remoteAdress
     *            The location to connect to if it is connecting remotely.
     * @param port
     *            The port that this connection is created at. (Has to be unique
     *            to this computer)
     * @param isSecure
     *            True if using SSL false otherwise.
     * @param connectionType
     *            The class that will be made (should be a subclass of
     *            ConnectionWrapper)
     * @return a completed {@link ConnectionWrapper}.
     * @throws ConnectionException
     *             If a connection has failed to be made.
     */
    public static ConnectionWrapper createConnection(final GeneralConnectionServer serv, final boolean isLocal, final String remoteAdress,
            final int port, final boolean isSecure, final Class<? extends ConnectionWrapper> connectionType) throws ConnectionException {
        ConnectionWrapper c = null;
        if (serv == null) {
            throw new ConnectionException("Can't create connection with a null parent server");
        }
        if (remoteAdress == null && !isLocal) {
            throw new ConnectionException("Attempting to connect to null address");
        }

        String start = isSecure ? "wss://" : "ws://";

        String location = start + (isLocal ? "localhost:" + port : "" + remoteAdress + ":" + port);
        System.out.println("Creating a client connecting to: " + location);
        try {
            @SuppressWarnings("rawtypes")
            final Constructor construct = connectionType.getConstructor(URI.class, GeneralConnectionServer.class);
            c = (ConnectionWrapper) construct.newInstance(new URI(location), serv);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (c != null) {
            try {
                c.connect();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        // In case of error do this!
        // c.setParent(serv);
        if (c == null) {
            throw new ConnectionException("failed to create ConnectionWrapper");
        }
        return c;
    }

    /**
     * Sends a request with the id and the connection at the given index.
     *
     * @param req
     *            The request to send.
     * @param sessionID
     *            The session Id of the request.
     * @param connectionType
     *            The type of connection being given
     * @throws ConnectionException thrown if a connection failed to be found.
     */
    public void send(final Request req, final String sessionID, final Class<? extends ConnectionWrapper> connectionType) throws ConnectionException {
        // Attach the existing request with the UserID
        final Request packagedRequest = GeneralConnectionServer.Encoder.requestIDBuilder(req, sessionID);
        try {
            getBestConnection(connectionType).send(packagedRequest.toByteArray());
        } catch (NullPointerException e) {
            System.out.println("Failed to get a local connection");
            throw new ConnectionException(e.getLocalizedMessage());
        }
    }

    /**
     * Creates and then adds a connection to the {@link MultiConnectionManager}.
     *
     * @param serv
     *            The server that is connected to this connection manager.
     * @param isLocal
     *            If the connection that is being created is local or remote.
     * @param remoteAdress
     *            The location to connect to if it is connecting remotely.
     * @param port
     *            The port that this connection is created at. (Has to be unique
     *            to this computer)
     * @param isSecure
     *            True if using SSL false otherwise.
     * @param connectionType
     *            The class that will be made (should be a subclass of
     *            ConnectionWrapper)
     * @throws ConnectionException
     *             If a connection has failed to be made.
     * @see #createConnection(GeneralConnectionServer, boolean, String, int,
     *      Class)
     * @see #addConnection(ConnectionWrapper, Class)
     */
    public final void createAndAddConnection(final GeneralConnectionServer serv, final boolean isLocal, final String remoteAdress, final int port,
            final boolean isSecure, final Class<? extends ConnectionWrapper> connectionType) throws ConnectionException {
        final ConnectionWrapper connection = createConnection(serv, isLocal, remoteAdress, port, isSecure, connectionType);
        addConnection(connection, connectionType);
    }

    /**
     * Allows a server to set an action to occur when a socket is no longer able
     * to send messages.
     *
     * @param listen
     *            the source object will be a list of request and will also
     *            contain a string specifying the type of connection.
     * @param connectionType
     *            The type to bind the action to.
     *
     */
    public final void setFailedSocketListener(final ActionListener listen, final Class<? extends ConnectionWrapper> connectionType) {
        ArrayList<ConnectionWrapper> cons = connections.get(connectionType);
        if (cons == null) {
            throw new NullPointerException("ConnectionType: " + connectionType.getName() + " does not exist in this manager");
        }
        for (ConnectionWrapper con : cons) {
            con.setFailedSocketListener(listen);
        }
    }

    /**
     * Drops all of the connections then adds them all back.
     */
    protected void reconnect() {
        this.dropAllConnection(true, false);
        this.connectServers(parent);
    }

    /**
     * Does nothing by default. Can be overwritten to make life easier.
     *
     * @param parentServer ignored by this implementation. Override to change functionality.
     */
    // @SuppressWarnings("unused")
    public void connectServers(final GeneralConnectionServer parentServer) {
    }

    /**
     * Adds a connection to a list with the given connection Type.
     *
     * @param connection
     *            The connection to be added.
     * @param connectionType
     *            The type to differentiate connections by.
     * @throws {@link NullPointerException} If connection is null or
     *         connectLocally is null.
     */
    public final void addConnection(final ConnectionWrapper connection, final Class<? extends ConnectionWrapper> connectionType) {
        if (connection == null) {
            throw new NullPointerException("can not add null connection");
        }

        if (connectionType == null) {
            throw new NullPointerException("can not add connection to null type");
        }

        connection.parentManager = this;

        ArrayList<ConnectionWrapper> cons = connections.get(connectionType);
        if (cons == null) {
            cons = new ArrayList<ConnectionWrapper>();
            cons.add(connection);
            connections.put(connectionType, cons);
            System.out.println("creating a new connectionList for: " + connectionType + " with list: " + connections.get(connectionType));
        } else {
            cons.add(connection);
        }
    }

    /**
     * Returns a connection that we believe to be the best connection at this
     * time.
     *
     * @param connectionType The type of connection being requested.
     * @return A valid connection.
     */
    public ConnectionWrapper getBestConnection(final Class<? extends ConnectionWrapper> connectionType) {
        ArrayList<ConnectionWrapper> cons = connections.get(connectionType);
        if (cons == null) {
            throw new NullPointerException("ConnectionType: " + connectionType.getName() + " does not exist in this manager");
        }
        System.out.println("getting Connection: " + connectionType.getSimpleName());
        return cons.get(0); // lame best connection.
    }

    /**
     * Closes all connections and removes them from storage.
     *
     * @param clearTypes
     *            if true then the mapping will be completely cleared.
     * @param debugPrint
     *            If true then the uri is printed as the connection is closed.
     */
    public final void dropAllConnection(final boolean clearTypes, final boolean debugPrint) {
        synchronized (connections) {
            // <? extends ConnectionWrapper> // for safe keeping
            for (Class<?> conKey : connections.keySet()) {
                for (ConnectionWrapper connection : connections.get(conKey)) {
                    if (debugPrint) {
                        System.out.println(connection.getURI());
                    }
                    connection.close();
                }
                connections.get(conKey).clear();
            }
            if (clearTypes) {
                connections.clear();
            }
        }
    }
}