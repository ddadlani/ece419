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
	public ArrayList<Address> playerList;
	private int serverPort;
	public int clientID;
	public NamingServer() {
		playerList = new ArrayList<Address>();
		serverPort = 6000;
		clientID = 0;
	}
    
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
        NamingServer nServer = new NamingServer();
        //playerList = new ArrayList<Address>();
        //serverPort = 6000;
        //clientID = 0;
        
        try {
        	serverSocket = new ServerSocket(nServer.serverPort);	

        } catch (EOFException eofe) {
        	System.err.println("Connection error");
        } catch (SocketException se ) {
        	 System.err.println("Connection error");
        } catch (IOException e) {
        	System.err.println("Default port number " + nServer.serverPort + " unavailable. Please enter new port number: ");
        	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        	nServer.serverPort = Integer.valueOf(in.readLine());
    		serverSocket = new ServerSocket(nServer.serverPort);	
        }
        

        while (listening) {
        	new Thread(new NamingServerHandlerThread(serverSocket.accept(), nServer)).start();  
        }

        serverSocket.close();
    }

}
