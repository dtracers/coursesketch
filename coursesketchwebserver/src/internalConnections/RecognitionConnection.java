package internalConnections;

import java.net.URI;

import multiConnection.MultiInternalConnectionServer;
import multiConnection.WrapperConnection;
import org.java_websocket.drafts.Draft;


/** This example demonstrates how to create a websocket connection to a server. Only the most important callbacks are overloaded. */
public class RecognitionConnection extends WrapperConnection {

	public RecognitionConnection( URI serverUri , Draft draft , MultiInternalConnectionServer parent) {
		this( serverUri, draft );
	}
	
	public RecognitionConnection( URI serverUri , Draft draft ) {
		super( serverUri, draft );
	}

	public RecognitionConnection( URI serverURI ) {
		super( serverURI );
	}
}