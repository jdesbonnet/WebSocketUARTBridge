import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class Connection extends Thread {
	DataInputStream input;

	DataOutputStream output;

	Socket clientSocket;
	private UARTThread uartThread;

	public Connection(UARTThread uartThread, Socket aClientSocket) {

		this.uartThread = uartThread;
		
		try {
			clientSocket = aClientSocket;
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
		} catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}
	
	public void sendMessage (String message) throws IOException {
		output.writeBytes(message);
	}

	public void run() {
		byte[] buf = new byte[80];
		int i=0;
		try {
			while (true) {
				buf[i] = input.readByte();
				if (i == buf.length || buf[i]=='\r') {
					// send line to UART
					String message = new String(buf,0,i);
					uartThread.sendMessageToUART(message);
					i = 0;
					continue;
				}
				i++;
			}
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO:" + e.getMessage());
		}

		finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				/* close failed */
			}
		}

	}

}
