import java.net.*;
import java.util.SortedMap;
import java.util.TreeMap;
import java.io.*;

public class ClientListenerThread extends Thread {
	private ServerSocket listenSocket = null;
	private Mazewar mazewar;
	private SortedMap<Double, Integer> ackQueue;
	
	public ClientListenerThread(Mazewar mazewar_, ServerSocket listenSocket_) {
		super("ClientListenerThread");
		
		// Pass server socket in from Mazewar
		this.listenSocket = listenSocket_;
		this.mazewar = mazewar_;
		this.ackQueue = new TreeMap<Double, Integer>();
	}

	public void run() {
		Boolean listening = true;
 
		// Enter continual listening loop
		 while (listening) {
				try {
					// create new thread to handle each incoming request
					new Thread(new ClientListenHandlerThread(listenSocket.accept(), mazewar, ackQueue)).start();
				} catch (IOException e) {
					System.err.println("ERROR: Could not accept incoming connection on listenSocket.");
					e.printStackTrace();
				}
		 }

	}
}