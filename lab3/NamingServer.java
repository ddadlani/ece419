import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class NamingServer {
	/** 
	 * HashMap players: Stores a list of all 
	 * the players by name and address. Does 
	 * not allow more than one address to be
	 * associated with one name.
	 */
	private static HashMap<String, Address> players;
	
	public NamingServer() {
		players = new HashMap<String, Address>();
	}
    
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
                if(args.length == 1) {
                        serverSocket = new ServerSocket(Integer.parseInt(args[0]));
                } else {
                        System.err.println("ERROR: Invalid arguments! Usage: ./mazeserver.sh <portnumber>");
                        System.exit(-1);
                }
        } catch (EOFException eofe) {
        	System.err.println("Connection error");
        } catch (SocketException se ) {
        	 System.err.println("Connection error");
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
        	new NamingServerHandlerThread(serverSocket.accept(), players).run();  
        }

        serverSocket.close();
    }

}
