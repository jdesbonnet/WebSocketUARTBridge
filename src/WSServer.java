import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * Server that acts as a WebSocket gateway between a WebSocket
 * and UART. Facilitates writing browsers UIs for lightweight embedded
 * applications. This is line orientated: UART->WebSocket messages
 * are triggered on receiving a CR. WebSocket->UART messages are 
 * suffixed with a CR. Also assuming a simple ASCII character set and
 * unprintable characters are not transmitted. 
 * 
 * Uses Java Simple Serial Connector to interface with serial port devices.
 * https://github.com/scream3r/java-simple-serial-connector/
 * 
 * WebSocket library: http://java-websocket.org/
 * https://github.com/TooTallNate/Java-WebSocket
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class WSServer extends WebSocketServer {
	
	private UARTThread uartThread;
	
	public WSServer(UARTThread uartThread, int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.uartThread = uartThread;
	}

	public WSServer(InetSocketAddress address) {
		super(address);
	}

	
	/**
	 * Handle incoming message from WebSocket which is a line of text
	 * to be transmitted to the UART. A CR is added to the received
	 * message.
	 */
	@Override
	public void onMessage(WebSocket conn, String message) {
		System.err.println(conn + ": " + message);
		uartThread.sendMessageToUART(message);
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		System.err.println("received fragment: " + fragment);
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
		System.err.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected" );
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		System.err.println( conn + " connection closed" );
	}
	
}
