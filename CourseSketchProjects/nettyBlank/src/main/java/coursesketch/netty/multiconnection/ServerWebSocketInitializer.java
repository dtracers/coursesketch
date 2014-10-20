package coursesketch.netty.multiconnection;

import interfaces.IServerWebSocketHandler;
import interfaces.ISocketInitializer;
import interfaces.MultiConnectionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

/**
 * Created by gigemjt on 10/19/14.
 */
public class ServerWebSocketInitializer extends ChannelInitializer<SocketChannel> implements ISocketInitializer {
    private SslContext sslContext;

    private SocketWrapper singleWrapper;

    /**
     * The server that the servlet is connected to.
     */
    private final IServerWebSocketHandler connectionServer;

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
    public ServerWebSocketInitializer(final long iTimeoutTime, final boolean iSecure, final boolean connectLocally) {
        this.timeoutTime = iTimeoutTime;
        this.secure = iSecure;
        connectionServer = createServerSocket();
        manager = createConnectionManager(connectLocally, secure);
    }

    /**
     * Stops the socket, and the server and drops all connections.
     */
    @Override
    public void stop() {

    }

    /**
     * This is called when the reconnect command is executed.
     * <p/>
     * By default this drops all connections and then calls
     *
     * @see MultiConnectionManager#connectServers(interfaces.IServerWebSocketHandler)
     */
    @Override
    public void reconnect() {

    }

    /**
     * @return The current number of current connections.
     */
    @Override
    public final int getCurrentConnectionNumber() {
        return connectionServer.getCurrentConnectionNumber();
    }

    /**
     * Override this method to create a subclass of the MultiConnectionManager.
     *
     * @param connectLocally True if the connection is acting as if it is on a local computer (used for testing)
     * @param iSecure        True if the connection is using SSL.
     * @return An instance of the {@link interfaces.MultiConnectionManager}
     */
    @Override
    public MultiConnectionManager createConnectionManager(final boolean connectLocally, final boolean iSecure) {
        return new MultiConnectionManager(connectionServer, connectLocally, iSecure);
    }

    /**
     * Override this method to create a subclass of GeneralConnectionServer.
     *
     * @return An instance of the {@link interfaces.IServerWebSocketHandler}
     */
    @Override
    public IServerWebSocketHandler createServerSocket() {
        return new ServerWebSocketHandler(this);
    }

    final void setSslContext(final SslContext iSslContext) {
        this.sslContext = iSslContext;
    }

    /**
     * This method will be called once the {@link io.netty.channel.Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link io.netty.channel.Channel}.
     *
     * @param ch the {@link io.netty.channel.Channel} which was registered.
     * @throws Exception is thrown if an error occurs. In that case the {@link io.netty.channel.Channel} will be closed.
     */
    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null) {
            pipeline.addFirst("ssl", sslContext.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        // TODO: change this to the double locking check thingy
        if (singleWrapper == null) {
            singleWrapper = new SocketWrapper(createServerSocket(), this.secure);
        }
        pipeline.addLast(singleWrapper);
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
    protected final IServerWebSocketHandler getServer() {
        return connectionServer;
    }
}
