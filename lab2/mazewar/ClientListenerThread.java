import java.net.*;
import java.io.*;
import java.util.*;


public class ClientListenerThread extends Thread {
	private Socket socket = null;
	private Queue<MazePacket> queue;
	private Integer seqnumCounter;

	public ClientListenerThread(Socket socket, Mazewar mazewar) {
                super("ClientListenerThread");
                this.socket = socket;
                this.queue = mazewar.receive_queue;
                seqnumCounter = 1;
        }

        public void run() {
        	ObjectInputStream in = null;
        	try {
                        /* variables for hostname/port */
                        //String hostname = InetAddress.getLocalHost().getHostName();
                        //int port = 3344;
                        //socket = new Socket(hostname, port);
                        in = new ObjectInputStream(socket.getInputStream());
                        MazePacket packetFromServer = new MazePacket();
                        while (true) {
                        	packetFromServer = (MazePacket) in.readObject();
                        	System.out.println("IM LISTENING!");
                        	//NOT SORTING QUEUE(using priority queue) YET
                        	//just adding to queue if in sequence, otherwise dropping packet 
                        	if (packetFromServer != null)
                		{
		        		if ((packetFromServer.getseqNum()).equals(seqnumCounter))
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
                        	
                        }
                    } catch (EOFException eof)
		      {
		              System.err.println("No reply received EOF");
		      } catch (IOException e) {
		    	  System.err.println("ERROR: Couldn't get I/O for the connection.");
		    	  e.printStackTrace();
		    	  System.exit(1);
		      } catch (ClassNotFoundException e) {
		      }
        }
}