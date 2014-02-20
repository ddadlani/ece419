import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MazeServer {
	public Integer clientID;
	public Integer sequenceNum;
    public Queue<MazePacket> queue;
    public ArrayList<Address> addressBook;
    
    public MazeServer() {
    	this.clientID = 0;
    	this.sequenceNum = 0;
    	this.queue = new LinkedList<MazePacket>();
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
            new MazeServerListenerThread(serverSocket.accept(), maze.clientID, maze.queue).start();
            synchronized(maze)	{
    			maze.clientID++;
    			maze.sequenceNum++;
    		}    
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
