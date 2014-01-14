package connection;

import handlers.DataRequestHandler;
import handlers.DataInsertHandler;
import handlers.UpdateHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import multiConnection.ConnectionException;
import multiConnection.MultiConnectionManager;
import multiConnection.MultiInternalConnectionServer;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;

import protobuf.srl.query.Data.DataRequest;
import protobuf.srl.query.Data.DataResult;
import protobuf.srl.query.Data.ItemQuery;
import protobuf.srl.query.Data.ItemRequest;
import protobuf.srl.query.Data.ItemResult;
import protobuf.srl.request.Message.Request;
import protobuf.srl.request.Message.Request.MessageType;
import protobuf.srl.school.School.SrlAssignment;
import protobuf.srl.school.School.SrlBankProblem;
import protobuf.srl.school.School.SrlCourse;
import protobuf.srl.school.School.SrlProblem;
import protobuf.srl.school.School.SrlSchool;

import com.google.protobuf.InvalidProtocolBufferException;

import database.auth.AuthenticationException;
import database.institution.Institution;

/**
 * A simple WebSocketServer implementation.
 *
 * Contains simple proxy information that is sent to other servers.
 */
public class DatabaseServer extends MultiInternalConnectionServer {

	private boolean connectLocal = MultiConnectionManager.CONNECT_REMOTE;
	UpdateHandler updateHandler = new UpdateHandler();
	MultiConnectionManager internalConnections = new MultiConnectionManager(this);

	public DatabaseServer( int port ) throws UnknownHostException {
		this( new InetSocketAddress( port ) );
	}

	public DatabaseServer( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer buffer) {
		System.out.println("Receiving message...");
		Request req = Decoder.parseRequest(buffer);

		if (req == null) {
			System.out.println("protobuf error");
			//this.
			// we need to somehow send an error to the client here
			return;
		}
		if (req.getRequestType() == Request.MessageType.SUBMISSION) {
			System.out.println("Submitting submission id");
			Institution.mongoInsertSubmission(req);
		} else if (req.getRequestType() == Request.MessageType.DATA_REQUEST) {
			DataRequestHandler.handleRequest(req, conn);
		} else if (req.getRequestType() == Request.MessageType.DATA_INSERT) {
			DataInsertHandler.handleData(req, conn);
		}
		System.out.println("Finished looking at query");
	}

	public void onFragment( WebSocket conn, Framedata fragment ) {
		//System.out.println( "received fragment: " + fragment );
	}

	public void reconnect() {
		internalConnections.dropAllConnection(true, false);
		try {
			internalConnections.createAndAddConnection(this, connectLocal, "srl02.tamu.edu", 8883, SolutionConnection.class);
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
	}
	public static void main( String[] args ) throws InterruptedException , IOException {
		System.out.println("Database Server: Version 1.0.2.mouse");
		WebSocketImpl.DEBUG = false;
		int port = 8885; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}
		DatabaseServer s = new DatabaseServer( port );
		s.start();
		s.reconnect();
		System.out.println( "Database Server started on port: " + s.getPort() );

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			if( in.equals( "exit" ) ) {
				s.stop();
				break;
			} else if( in.equals( "restart" ) ) {
				s.stop();
				s.start();
				break;
			} else if( in.equals("reconnect")) {
				s.reconnect();
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
}
