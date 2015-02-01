package connection;

import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import coursesketch.server.interfaces.MultiConnectionManager;
import utilities.ConnectionException;

/**
 * A manager for holding all of the connections that were created.
 *
 * @author gigemjt
 */
public class AnswerConnectionManager extends MultiConnectionManager {
    /**
     * IP address.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String SUBMISSION_ADDRESS = "192.168.56.202";

    /**
     * Port number.
     */
    private static final int SUBMISSION_PORT = 8883;

    /**
     * Creates a default {@link MultiConnectionManager}.
     *
     * @param parent  The server that is using this object.
     * @param isLocal True if the connection should be for a local server instead of
     *                 a remote server.
     * @param isSecure  True if the connections should be secure.
     */
    public AnswerConnectionManager(final AbstractServerWebSocketHandler parent,
            final boolean isLocal, final boolean isSecure) {
        super(parent, isLocal, isSecure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void connectServers(final AbstractServerWebSocketHandler parent) {
        try {
            createAndAddConnection(parent, isConnectionLocal(), SUBMISSION_ADDRESS,
                    SUBMISSION_PORT, this.isSecure(), SubmissionClientWebSocket.class);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }
}
