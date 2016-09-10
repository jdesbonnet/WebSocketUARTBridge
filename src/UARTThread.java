import java.io.IOException;

import org.java_websocket.server.WebSocketServer;

import jssc.SerialPort;
import jssc.SerialPortException;

public class UARTThread extends Thread {

	private SerialPort uart;
	private WSServer webSocketServer;
	private TCPServer tcpServer;
	
	public UARTThread (SerialPort uart) {
		this.uart = uart;
	}
	
	public void run () {
		try {
		byte[] buf = new byte[1024];
		byte c;
		int i=0;
		while (true) {
			c = uart.readBytes(1)[0];
			if (c == '\r') {
				buf[i] = 0;
				String msg = new String(buf,0,i);
				System.out.println(msg);
				// Send to WebSocket listeners
				//webSocketServer.sendToAll(new String(buf,0,i));
				tcpServer.sendToAll(msg);
				webSocketServer.sendToAll(msg);
				i = 0;
			}
			// Ignore non-printable chars (including LF)
			if ( c >= 32) {
				buf[i++] = c;
			}
		}
		} catch (IOException | SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessageToUART (String message) {
		try {
			System.out.println("Sending to UART: \"" + message + "\"");
			this.uart.writeString(message + "\r");
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setTcpServer (TCPServer tcpServer) {
		this.tcpServer = tcpServer;
	}
	public void setWebSocketServer (WSServer webSocketServer) {
		this.webSocketServer = webSocketServer;
	}
}
