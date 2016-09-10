import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer extends Thread {

	private int listenPort;
	UARTThread uartThread;
	
	private List<Connection> connections = new ArrayList<>();
	
	public TCPServer (UARTThread uartThread, int listenPort) {
		this.uartThread = uartThread;
		this.listenPort = listenPort;
	}
	
	public void run() {

		try {
			ServerSocket listenSocket = new ServerSocket(listenPort);
			System.out.println("TCP port listening.on " + listenPort);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				Connection c = new Connection(uartThread, clientSocket);
				connections.add(c);
			}
		} catch (IOException e) {
			System.out.println("Listen :" + e.getMessage());
		}

	}
	
	public void sendToAll (String message) throws IOException{
		for (Connection connection : connections) {
			connection.sendMessage(message+"\n");
		}
	}
}
