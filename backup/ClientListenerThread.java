import java.net.*;
import java.io.*;
import java.util.*;


public class ClientListenerThread extends Thread {
	private Socket socket = null;
	private Queue<MazePacket> queue;
	private Integer seqnumCounter;
	
	public ClientListenerThread(Socket socket, Queue<MazePacket> queue) {
                super("ClientListenerThread");
                this.socket = socket;
                this.queue = queue;
                seqnumCounter = 0;
        }

        public void run() {
        	ObjectInputStream in = null;
        	try {
                        /* variables for hostname/port */
                        String hostname = "ug160.eecg.utoronto.ca";
                        int port = 4444;
                        
                        socket = new Socket(hostname, port);
                        in = new ObjectInputStream(socket.getInputStream());
                        MazePacket packetFromServer = new MazePacket();
                        while (true) {
                        	packetFromServer = (MazePacket) in.readObject();
                        	
                        	//NOT SORTING QUEUE(using priority queue) YET
                        	//just adding to queue if in sequence, otherwise dropping packet 
                		if (packetFromServer.getseqNum() == seqnumCounter)
                		{
                			queue.add(packetFromServer);
                			seqnumCounter ++;
                		}
                		else
                		{
                		//HANDLE OUT OF ORDER PACKET
                			System.out.println("Out of order packet received");
                		}
                        	
                        }
                    } catch (EOFException eof)
		      {
		              System.err.println("No reply received EOF");
		      } catch (IOException e) {
		              System.err.println("ERROR: Couldn't get I/O for the connection.");
		              System.exit(1);
		      } catch (ClassNotFoundException e) {
		      }
        }
}
