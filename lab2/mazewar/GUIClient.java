import java.net.*;
import java.io.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

	// public Queue<MazePacket> receive_queue;
	public String serverHost = null;
	public int serverPort = 0;
	public Socket SenderSocket = null;
	public ObjectOutputStream out = null;
	public ObjectInputStream in = null;

	/**
	 * Create a GUI controlled {@link LocalClient}.
	 */
	public GUIClient(String name, String hostname, int port) {
		super(name);

		/* variables for hostname/port */
		this.serverHost = hostname;
		this.serverPort = port;

	}

	/**
	 * Handle a key press.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyPressed(KeyEvent e) {

		try {
			SenderSocket = new Socket(serverHost, serverPort);
			// new ClientListenerThread(ReceiverSocket.accept(),
			// receive_queue).start();
			// new ClientExecutionThread(receive_queue).start();
			out = new ObjectOutputStream(SenderSocket.getOutputStream());
			/* make a new request packet */
			MazePacket packetToServer = new MazePacket();
			Address client_addr = new Address();
			client_addr.name = this.getName();
			client_addr.hostname = InetAddress.getLocalHost().getHostName();
			packetToServer.setclientInfo(client_addr);

			// If the user pressed Q, invoke the cleanup code and quit.
			if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {

				packetToServer.setmsgType(MazePacket.MAZE_DISCONNECT);
				// SEND DATA NEEDED TO DISCONNECT: Address, location?
				out.writeObject(packetToServer);

				// Up-arrow moves forward.
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {

				packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
				//ClientEvent c = ClientEvent.moveForward;
				packetToServer.setevent(MazePacket.MOVE_FORWARD);
				out.writeObject(packetToServer);

				// Down-arrow moves backward.
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {

				packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
				//ClientEvent c = ClientEvent.moveBackward;
				packetToServer.setevent(MazePacket.MOVE_BACKWARD);
				out.writeObject(packetToServer);

				// Left-arrow turns left.
			} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {

				packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
				//ClientEvent c = ClientEvent.turnLeft;
				packetToServer.setevent(MazePacket.TURN_LEFT);
				out.writeObject(packetToServer);

				// Right-arrow turns right.
			} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {

				packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
				//ClientEvent c = ClientEvent.turnRight;
				packetToServer.setevent(MazePacket.TURN_RIGHT);
				out.writeObject(packetToServer);

				// Spacebar fires.
			} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {

				packetToServer.setmsgType(MazePacket.MAZE_REQUEST);
				//ClientEvent c = ClientEvent.fire;
				packetToServer.setevent(MazePacket.FIRE);
				out.writeObject(packetToServer);

			}
			in = new ObjectInputStream(SenderSocket.getInputStream());
			MazePacket ack = (MazePacket) in.readObject();
			
			if (ack.geterrorCode() != 0) {
				System.err.println("ERROR: Invalid packet sent");
			}
			else ;

			out.close();
			SenderSocket.close();

		} catch(ConnectException ce) {
			System.err.println("ERROR: Socket could not connect.");
			System.exit(1);
		} catch (UnknownHostException err) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (ClassNotFoundException cnf) {
			System.err.println("ERROR: GUIClient class not found");
			cnf.printStackTrace();
			System.exit(1);
		} catch (IOException err) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			err.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Handle a key release. Not needed by {@link GUIClient}.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * Handle a key being typed. Not needed by {@link GUIClient}.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyTyped(KeyEvent e) {
	}

}
