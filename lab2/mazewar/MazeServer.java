import java.net.*;
import java.io.*;
import java.util.*;

public class MazeServer {
	public Integer clientID;
    public Queue<MovePacket> q;
    
    public MazeServer() {
    	this.clientID = 0;
    	this.q = new LinkedList<MovePacket>();
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
        	//System.out.println("Broker server accepting connections on port number " + args[0] + ".");
    		synchronized(maze)
    		{
    			maze.clientID ++;
    		}
            new MazeServerHandlerThread(serverSocket.accept(), maze.clientID, maze.q).start();
                
        }

        serverSocket.close();
    }
}
