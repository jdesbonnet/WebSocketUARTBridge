import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

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
 *
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class WebSocketUARTBridge extends WebSocketServer {

	private SerialPort serialPort;
	
	public WebSocketUARTBridge(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public WebSocketUARTBridge(InetSocketAddress address) {
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
		try {
			serialPort.writeString(message + "\r");
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		System.err.println("received fragment: " + fragment);
	}

	public static void main(String[] args) throws InterruptedException,
			IOException,SerialPortException {
		
		WebSocketImpl.DEBUG = true;
		
		// List serial ports
		System.err.println ("Available serial ports:");
		String[] portNames = SerialPortList.getPortNames();
		for(int i = 0; i < portNames.length; i++){
	            System.err.println(portNames[i]);
		}
		
		String serialPortDevice = args[1];
		

		int port = 8887;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
		}
		
		
		WebSocketUARTBridge server = new WebSocketUARTBridge(port);
		
		// Open serial port, 9600bps, 8 data bits, 1 stop bit, no parity
		server.serialPort = new SerialPort(serialPortDevice);
		server.serialPort.openPort();
		server.serialPort.setParams(9600, 8, 1, 0);
		
		
		server.start();
		System.err.println("WebSocketUARTBridge started on port: " + server.getPort());

		
		byte[] buf = new byte[1024];
		byte c;
		int i=0;
		while (true) {
			c = server.serialPort.readBytes(1)[0];
			if (c == '\r') {
				buf[i] = 0;
				server.sendToAll(new String(buf,0,i));
				i = 0;
			}
			// Ignore non-printable chars (including LF)
			if ( c >= 32) {
				buf[i++] = c;
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
