package connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import protobuf.srl.request.Message.LoginInformation;
import protobuf.srl.request.Message.Request;
import protobuf.srl.request.Message.Request.MessageType;

/**
 * A simple WebSocketServer implementation.
 *
 * Contains simple proxy information that is sent to other servers.
 */
public class ProxyServer extends WebSocketServer {

	public static final int MAX_CONNECTIONS = 20;
	public static final int STATE_SERVER_FULL = 4001;
	public static final int STATE_INVALID_LOGIN = 4002;
	public static final int MAX_LOGIN_TRIES = 5;
	public static final String FULL_SERVER_MESSAGE = "Sorry the server is full";
	public static final String INVALID_LOGIN_MESSAGE = "Too many incorrect login attempts.\nClosing connection.";
	
	// Id Maps
	HashMap<WebSocket, ConnectionState> connectionToId = new HashMap<WebSocket, ConnectionState>();
	HashMap<ConnectionState, WebSocket> idToConnection = new HashMap<ConnectionState, WebSocket>();

	static int numberOfConnections = Integer.MIN_VALUE;
	public ProxyServer( int port ) throws UnknownHostException {
		this( new InetSocketAddress( port ) );
	}

	public ProxyServer( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		if (connectionToId.size() >= MAX_CONNECTIONS) {
			// Return negatative state.
			conn.close(STATE_SERVER_FULL, FULL_SERVER_MESSAGE);
			System.out.println("FULL SERVER"); // send message to someone?
			return;
		}
		ConnectionState id = getUniqueId();
		connectionToId.put(conn, id);
		idToConnection.put(id, conn);
		System.out.println("ID ASSIGNED");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote ) {
		System.out.println( conn + " has left the room!");
		idToConnection.remove(connectionToId.remove(conn));
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer buffer) {
		Request req = Decoder.prarseRequest(buffer);
		ConnectionState state = connectionToId.get(conn);
		if (req == null) {
			System.out.println("protobuf error");
			//this.
			// we need to somehow send an error to the client here
			return;
		}
		if (!state.isLoggedIn()) {
			if (LoginChecker.checkLogin(req)) {
				state.logIn();
				// Create the Request to respond.
				conn.send(LoginChecker.createResponse(req, true).toByteArray());
			} else {
				state.addTry();
				if (state.getTries() > MAX_LOGIN_TRIES) {
					conn.close(STATE_INVALID_LOGIN, INVALID_LOGIN_MESSAGE);
				}
				conn.send(LoginChecker.createResponse(req, false).toByteArray());
			}
		} else {
			if (state.getTries() > MAX_LOGIN_TRIES) {
				conn.close(STATE_INVALID_LOGIN, INVALID_LOGIN_MESSAGE);
				return;
			}
			// Parse message.
			conn.send(buffer);
			return;
		}
	}

	public void onFragment( WebSocket conn, Framedata fragment ) {
		System.out.println( "received fragment: " + fragment );
	}

	/**
	 * Returns a number that should be unique.
	 */
	public ConnectionState getUniqueId() {
		// TODO: Assign ID using a linked list so they can be used multiple times.  O(1) when used as a Queue
		return new ConnectionState(numberOfConnections++);
	}
	
	public static void main( String[] args ) throws InterruptedException , IOException {
		WebSocketImpl.DEBUG = true;
		int port = 8887; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}
		ProxyServer s = new ProxyServer( port );
		s.start();
		System.out.println( "ChatServer started on port: " + s.getPort() );

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			s.sendToAll( in );
			if( in.equals( "exit" ) ) {
				s.stop();
				break;
			} else if( in.equals( "restart" ) ) {
				s.stop();
				s.start();
				break;
			}
		}
	}
	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
		}
	}
}