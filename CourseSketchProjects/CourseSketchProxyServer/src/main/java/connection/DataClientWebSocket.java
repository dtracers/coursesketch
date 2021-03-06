package connection;

import coursesketch.server.base.ClientWebSocket;
import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import coursesketch.server.interfaces.MultiConnectionState;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.srl.request.Message.Request;
import utilities.ConnectionException;
import utilities.LoggingConstants;
import utilities.TimeManager;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * This example demonstrates how to create a websocket connection to a server.
 * Only the most important callbacks are overloaded.
 */
@WebSocket(maxBinaryMessageSize = AbstractServerWebSocketHandler.MAX_MESSAGE_SIZE)
public final class DataClientWebSocket extends ClientWebSocket {

    /**
     * Declaration and Definition of Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataClientWebSocket.class);

    /**
     * Creates a new connection for the Answer checker server.
     *
     * @param destination
     *            The location of the database server.
     * @param parent
     *            The proxy server instance.
     */
    public DataClientWebSocket(final URI destination, final AbstractServerWebSocketHandler parent) {
        super(destination, parent);
    }

    /**
     * Accepts messages and sends the request to the correct server and holds
     * minimum client state.
     *
     * Also removes all identification that should not be sent to the client.
     *
     * @param buffer
     *            The message that is received by this object.
     */
    @Override
    public void onMessage(final ByteBuffer buffer) {
        final Request req = AbstractServerWebSocketHandler.Decoder.parseRequest(buffer);

        if (req.getRequestType() == Request.MessageType.TIME) {

            final Request rsp = TimeManager.decodeRequest(req);
            if (rsp != null) {
                try {
                    this.getParentManager().send(rsp, req.getSessionInfo(), DataClientWebSocket.class);
                } catch (ConnectionException e) {
                    LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
                }
            }
        } else {
            final MultiConnectionState state = getStateFromId(req.getSessionInfo());

            final Request request = AbstractServerWebSocketHandler.Decoder.parseRequest(buffer);
            // Strips away identification.
            final Request result = ProxyConnectionManager.createClientRequest(request);
            this.getParentServer().send(getConnectionFromState(state), result);
        }
    }
}
