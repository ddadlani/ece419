import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class NamingServer {
	/** 
	 * HashMap players: Stores a list of all 
	 * the players by name and address. Does 
	 * not allow more than one address to be
	 * associated with one name.
	 */
	private static ArrayList<Address> playerList;
	private static int serverPort = 6000;
	
	public NamingServer() {
		playerList = new ArrayList<Address>();
	}
    
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
        	serverSocket = new ServerSocket(serverPort);	

        } catch (EOFException eofe) {
        	System.err.println("Connection error");
        } catch (SocketException se ) {
        	 System.err.println("Connection error");
        } catch (IOException e) {
        	System.err.println("Default port number " + serverPort + " unavailable. Please enter new port number: ");
        	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        	serverPort = Integer.valueOf(in.readLine());
    		serverSocket = new ServerSocket(serverPort);	
        }

        while (listening) {
        	new NamingServerHandlerThread(serverSocket.accept(), playerList).run();  
        }

        serverSocket.close();
    }

}
