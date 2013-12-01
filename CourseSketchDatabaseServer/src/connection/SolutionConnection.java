package connection;

import java.net.URI;

import multiConnection.MultiInternalConnectionServer;
import multiConnection.WrapperConnection;
import org.java_websocket.drafts.Draft;

/** This example demonstrates how to create a websocket connection to a server. Only the most important callbacks are overloaded. */
public class SolutionConnection extends WrapperConnection {

	public SolutionConnection( URI serverUri , Draft draft , MultiInternalConnectionServer parent) {
		this( serverUri, draft );
	}
	
	public SolutionConnection( URI serverUri , Draft draft ) {
		super( serverUri, draft );
	}

	public SolutionConnection( URI serverURI ) {
		super( serverURI );
	}
}