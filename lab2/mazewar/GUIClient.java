import java.net.*;
import java.io.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {


	//public Queue<MazePacket> receive_queue;
		public Socket SenderSocket = null;
		public ObjectOutputStream out = null;

        /**
         * Create a GUI controlled {@link LocalClient}.  
         */
        public GUIClient(String name, Socket socket, ObjectOutputStream out) {
                super(name);
                this.SenderSocket = socket;
                this.out = out;
                /* variables for hostname/port */
               
        }
        
        /**
         * Handle a key press.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyPressed(KeyEvent e) {
        
        	
                try {
                	
		        //new ClientListenerThread(ReceiverSocket.accept(), receive_queue).start();  
		        //new ClientExecutionThread(receive_queue).start();  	

                /* make a new request packet */
                
                //Socket SenderSocket = null;

		        MazePacket packetToServer = new MazePacket();
		        Address client_addr = new Address();
		        client_addr.name = this.getName();
		        client_addr.hostname = InetAddress.getLocalHost().getHostName();
		        packetToServer.setclientInfo(client_addr);
		     
		        // If the user pressed Q, invoke the cleanup code and quit. 
		        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {

		        	packetToServer.setmsgType(MazePacket.MAZE_DISCONNECT);
		        	//SEND DATA NEEDED TO DISCONNECT: Address, location? 
		        	out.writeObject(packetToServer);

		        // Up-arrow moves forward.
		        } else if(e.getKeyCode() == KeyEvent.VK_UP) {

		        	packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
		        	ClientEvent c = ClientEvent.moveForward;
		        	packetToServer.setevent(c);
		        	out.writeObject(packetToServer);


		        // Down-arrow moves backward.
		        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {

		        	packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
		        	ClientEvent c = ClientEvent.moveBackward;
		        	packetToServer.setevent(c);
		        	out.writeObject(packetToServer);


		        // Left-arrow turns left.
		        } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {

		        	packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
		        	ClientEvent c = ClientEvent.turnLeft;
		        	packetToServer.setevent(c);
		        	out.writeObject(packetToServer);      


		        // Right-arrow turns right.
		        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {

		        	packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
		        	ClientEvent c = ClientEvent.turnRight;
		        	packetToServer.setevent(c);
		        	out.writeObject(packetToServer);


		        // Spacebar fires.
		        } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {

		        	packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
		        	ClientEvent c = ClientEvent.fire;
		        	packetToServer.setevent(c);
		        	out.writeObject(packetToServer);

		        }

		        //out.close();
		        //SenderSocket.close();    
                
                } catch (UnknownHostException err) {
                        System.err.println("ERROR: Don't know where to connect!!");
                        System.exit(1);
                } catch (IOException err) {
                	System.err.println("ERROR: Couldn't get I/O for the connection.");
                	err.printStackTrace();
                	System.exit(1);
                }
     
        }
        
        /**
         * Handle a key release. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyReleased(KeyEvent e) {
        }
        
        /**
         * Handle a key being typed. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyTyped(KeyEvent e) {
        }

}
