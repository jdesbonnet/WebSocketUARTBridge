import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * Server that acts as a websocket gateway between a WebSocket
 * and UART. Facilitates writing browers UIs for lightweight embedded
 * applications.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class WebSocketUARTBridge extends WebSocketServer {

	private FileWriter serialOut;
	
	public WebSocketUARTBridge(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public WebSocketUARTBridge(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println(conn + ": " + message);
		try {
			serialOut.write(message);
			serialOut.write("\r");
			serialOut.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		System.out.println("received fragment: " + fragment);
	}

	public static void main(String[] args) throws InterruptedException,
			IOException {
		WebSocketImpl.DEBUG = true;
		
		int port = 8887;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
		}
		
		
		WebSocketUARTBridge server = new WebSocketUARTBridge(port);
		server.start();
		System.out.println("WebSocketUARTBridge started on port: " + server.getPort());

		server.serialOut = new FileWriter((args[1]));

		BufferedReader r = new BufferedReader(new FileReader(args[1]));
		String line;
		while ( (line=r.readLine()) != null) {
			server.sendToAll(line);
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
	public void sendToAll(String text) {
		Collection<WebSocket> con = connections();
		synchronized (con) {
			for (WebSocket c : con) {
				c.send(text);
			}
		}
	}


	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected" );
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		System.out.println( conn + " connection closed" );
	}
	
}
