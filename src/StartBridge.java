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
 * TODO: thread safety.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class StartBridge {

	private static void usage() {
		System.out.println("Parameters: <websocket-port> <uart-device>");
		
		// List serial ports
		System.err.println ("Available serial ports:");
		String[] portNames = SerialPortList.getPortNames();
		for(int i = 0; i < portNames.length; i++){
			System.err.println(portNames[i]);
		}
		
		
	}
	

	public static void main(String[] args) throws InterruptedException,
			IOException,SerialPortException {
		
		if (args.length < 2) {
			usage();
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		String serialPortDevice = args[1];
		
		
		// Open serial port, 9600bps, 8 data bits, 1 stop bit, no parity
		SerialPort uart = new SerialPort(serialPortDevice);
		uart.openPort();
		uart.setParams(9600, 8, 1, 0);
		
		UARTThread uartThread = new UARTThread(uart);

		
		WSServer webSocketServer = new WSServer(uartThread, port);
		
		TCPServer tcpServer = new TCPServer(uartThread, port+1);
		
		uartThread.setWebSocketServer(webSocketServer);
		uartThread.setTcpServer(tcpServer);

		
		webSocketServer.start();
		tcpServer.start();
		uartThread.start();
		
		
		System.err.println("WebSocketUARTBridge started on port: " + webSocketServer.getPort());

		
		
	}

	
	
}
