package coursesketch.server.frontend;

import coursesketch.server.interfaces.SocketSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.srl.request.Message;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Created by gigemjt on 10/19/14.
 */
public final class NettySession implements SocketSession {

    /**
     * Declaration/Definition of Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NettySession.class);

    /**
     * The context of the socket.
     */
    private final ChannelHandlerContext session;

    /**
     * Creates a wrapper around the channel context.
     * @param context The context of the session (the socket itself).
     */
    public NettySession(final ChannelHandlerContext context) {
        this.session = context;
    }

    /**
     * Get the address of the remote side.
     *
     * @return the remote side address
     */
    @Override
    public String getRemoteAddress() {
        return session.channel().remoteAddress().toString();
    }

    /**
     * Request a close of the current conversation with a normal status code and no reason phrase.
     * <p/>
     * This will enqueue a graceful close to the remote endpoint.
     *
     * @see #close(int, String)
     */
    @Override
    public void close() {
        session.channel().close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Void> send(final Message.Request req) {
        return send(ByteBuffer.wrap(req.toByteArray()));
    }

    /**
     * Initiates the asynchronous transmission of a binary message. This method returns before the message is transmitted.
     * Developers may use the returned Future object to track progress of the transmission.
     *
     * @param buffer
     *         the data being sent
     * @return the Future object representing the send operation.
     */
    @Override
    public Future<Void> send(final ByteBuffer buffer) {
        LOG.debug("BYTES {}", Arrays.toString(buffer.array()));
        LOG.debug("Sending");
        LOG.debug("local address {}", session.channel().localAddress());
        LOG.debug("remote address {}", session.channel().remoteAddress());
        final BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer(buffer));
        LOG.debug("Frame: {}", frame);
        LOG.debug("FRAME BYTES {}", Arrays.toString(frame.content().array()));
        final ChannelFuture future = session.channel().write(frame);
        session.channel().flush();
        future.addListener(new GenericFutureListener<DefaultChannelPromise>() {
            /**
             * {@inheritDoc}
             **/
            @SuppressWarnings("PMD.CommentRequired")
            @Override public void operationComplete(final DefaultChannelPromise future) {
                LOG.debug("Message sent successfully");
            }
        });
        LOG.info("MESSAGE FINISHED!");
        return future;
    }

    /**
     * Send a websocket Close frame, with status code.
     * <p/>
     * This will enqueue a graceful close to the remote endpoint.
     *
     * @param statusCode
     *         the status code
     * @param reason
     *         the (optional) reason. (can be null for no reason)
     * @see #close()
     */
    @Override
    public void close(final int statusCode, final String reason) {
        session.channel().close();
    }

    /**
     * @param other
     *         a different JettySession.
     * @return true if the {@link org.eclipse.jetty.websocket.api.Session} are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof NettySession) {
            return session.equals(((NettySession) other).session);
        }
        return false;
    }

    /**
     * @return the hash code of the {@link org.eclipse.jetty.websocket.api.Session}.
     */
    @Override
    public int hashCode() {
        return session.hashCode();
    }
}
