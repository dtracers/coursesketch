package internalConnection;

import java.net.URI;
import java.nio.ByteBuffer;

import coursesketch.jetty.multiconnection.ConnectionWrapper;
import coursesketch.jetty.multiconnection.ServerWebSocket;

import interfaces.IServerWebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import protobuf.srl.request.Message.Request;
import protobuf.srl.request.Message.Request.MessageType;
// import protobuf.srl.submission.Submission.SrlExperiment;
// import protobuf.srl.submission.Submission.SrlSolution;

// simport com.google.protobuf.InvalidProtocolBufferException;

/**
 * This example demonstrates how to create a websocket connection to a server.
 * Only the most important callbacks are overloaded.
 */
@WebSocket(maxBinaryMessageSize = Integer.MAX_VALUE)
public class SubmissionConnection extends ConnectionWrapper {

    public SubmissionConnection(final URI destination,
            final ServerWebSocket parentServer) {
        super(destination, parentServer);
    }

    @Override
    public final void onMessage(final ByteBuffer buffer) {
        final Request req = IServerWebSocket.Decoder.parseRequest(buffer); // this
                                                                            // contains
                                                                            // the
                                                                            // solution
        System.out.println(req.getSessionInfo());
        final String[] sessionInfo = req.getSessionInfo().split("\\+");
        System.out.println(sessionInfo[1]);
        final AnswerConnectionState state = (AnswerConnectionState) getStateFromId(sessionInfo[1]);
        System.out.println(state);
        if (req.getRequestType() == MessageType.DATA_REQUEST) {
            // SrlExperiment expr = state.getExperiment(sessionInfo[1]);
            // SrlSolution sol = null;
            /*
             * try { sol = SrlSolution.parseFrom(req.getOtherData()); } catch
             * (InvalidProtocolBufferException e) { e.printStackTrace(); }
             */
            // FIXME: implement comparison.
            // this could take a very very long time!

            // we need to this at least
            final Request.Builder builder = Request.newBuilder(req);
            builder.setSessionInfo(sessionInfo[0]);
            ServerWebSocket.send(getConnectionFromState(state),
                    builder.build());
        } else if (req.getRequestType() == MessageType.SUBMISSION) {
            // pass up the Id to the client
            final Request.Builder builder = Request.newBuilder(req);
            builder.setSessionInfo(sessionInfo[0]);
            final Session connection = getConnectionFromState(state);
            if (connection == null) {
                System.err.println("SOCKET IS NULL");
            }
            ServerWebSocket.send(getConnectionFromState(state),
                    builder.build());
        }
    }
}
