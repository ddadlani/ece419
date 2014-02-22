import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MazeServer {
	public Integer clientID;
<<<<<<< HEAD
    public Queue<MazePacket> q;
=======
	public Integer sequenceNum;
    public Queue<MazePacket> requestQueue;
>>>>>>> b5872b37edc03194aa3b878d69837068a1f04dc5
    public ArrayList<Address> addressBook;
    
    public MazeServer() {
    	this.clientID = 0;
    	this.sequenceNum = 0;
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
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
<<<<<<< HEAD
        	//System.out.println("Broker server accepting connections on port number " + args[0] + ".");
    		synchronized(maze)
    		{
    			maze.clientID ++;
    		}
            new MazeServerListenerThread(serverSocket.accept(), maze.clientID, maze.q).start();
                
=======
            new MazeServerHandlerThread(serverSocket.accept(), maze).start();
            synchronized(maze)	{
    			maze.clientID++;
    			maze.sequenceNum++;
    		}    
>>>>>>> b5872b37edc03194aa3b878d69837068a1f04dc5
        }

        serverSocket.close();
    }
	
	public static Integer searchInAddressBook(final Integer clientID, final ArrayList<Address> addressBook) {
		for(Integer i = 0; i < addressBook.size(); i++) {
			if (addressBook.get(i).id == clientID)
				return i;
			else;
		}
		return -1;
	}
}
