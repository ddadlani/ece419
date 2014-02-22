import java.net.*;
import java.io.*;
import java.util.*;

/*
Copyright (C) 2004 Geoffrey Alan Washburn
      
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
      
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
      
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

	
	public Queue<MazePacket> receive_queue;  
	public ServerSocket ReceiverSocket;
	
        /**
         * Create a GUI controlled {@link LocalClient}.  
         */
        public GUIClient(String name) {
                super(name);
                
        }
        
        /**
         * Handle a key press.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyPressed(KeyEvent e) {
        
        	
                try {
                
                	ReceiverSocket = null;
		        new ClientListenerThread(ReceiverSocket.accept(), receive_queue).start();  
		        new ClientExecutionThread(receive_queue).start();  	
			
			/* make a new request packet */
			
			Socket SenderSocket = null;
		        ObjectOutputStream out = null;
                        /* variables for hostname/port */
                        String hostname = "ug160.eecg.utoronto.ca";
                        int port = 4444;
                        
                        SenderSocket = new Socket(hostname, port);

                        out = new ObjectOutputStream(SenderSocket.getOutputStream());
                        
                        
		        MazePacket packetToServer = new MazePacket();
		        
		        // If the user pressed Q, invoke the cleanup code and quit. 
		        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
		        
		        	packetToServer.setmsgType(MazePacket.MAZE_DISCONNECT);
		        	//SEND DATA NEEDED TO DISCONNECT: Address, location? 
		        	out.writeObject(packetToServer);
<<<<<<< HEAD

=======
>>>>>>> b5872b37edc03194aa3b878d69837068a1f04dc5
		                
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

		        out.close();
		        SenderSocket.close();    
                
                } catch (UnknownHostException err) {
                        System.err.println("ERROR: Don't know where to connect!!");
                        System.exit(1);
                } catch (IOException err) {
                        System.err.println("ERROR: Couldn't get I/O for the connection.");
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
