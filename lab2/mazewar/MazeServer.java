import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MazeServer {
	public Integer clientID;
	public Integer sequenceNum;
	public Boolean fatalError;
    public Queue<MazePacket> requestQueue;
    public ArrayList<Address> addressBook;
    
    public MazeServer() {
    	this.clientID = 0;
    	this.sequenceNum = 0;
    	this.fatalError = false;
    	this.requestQueue = new LinkedList<MazePacket>();
    	this.addressBook = new ArrayList<Address>();
    }
    
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
        MazeServer maze = new MazeServer();
        try {
                if(args.length == 1) {
                        serverSocket = new ServerSocket(Integer.parseInt(args[0]));
                } else {
                        System.err.println("ERROR: Invalid arguments! Usage: ./server.sh <portnumber>");
                        System.exit(-1);
                }
        } catch (EOFException eofe) {
        	//try to handle both of these the same way?
        	//remove their entry from the addressbook if this happens
        	System.out.println("Player closed connection. Terminating the handler thread");
        	System.out.println("Connection error");
        }
          catch (SocketException se ) {
        	  System.out.println("Player closed connection. Terminating the handler thread");
        	  System.out.println("Connection error");
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
            new MazeServerHandlerThread(serverSocket.accept(), maze).start();  
        }

        serverSocket.close();
    }

	public static Integer searchInAddressBook(final Integer clientID, final ArrayList<Address> addressBook) {
		for(Integer i = 0; i < addressBook.size(); i++) {
			if (addressBook.get(i).id.equals(clientID))
				return i;
			else;
		}	
		return -1;
	}
}
