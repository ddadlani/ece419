import java.net.*;
import java.util.ArrayList;
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
	public String lookupHostName = null;
	public Integer lookupPort = 0;
	public ObjectOutputStream out = null;
	public ObjectInputStream in = null;
	//public ArrayList<Address> broadcastAddrBook = null;
	public Address localAddr = null;
	//public double lClock = 0.0;
	public Mazewar mazewar;

	/**
	 * Create a GUI controlled {@link LocalClient}.
	 */
	public GUIClient(String name,
			Address localAddr, String lookupHostName, Integer lookupPort,
			Mazewar mazewar) {

		super(name);
		/* variables for hostname/port */
		this.lookupHostName = lookupHostName;
		this.lookupPort = lookupPort;
		//this.broadcastAddrBook = remotes;
		this.localAddr = localAddr;
		this.mazewar = mazewar;
	}

	/**
	 * Handle a key press.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyPressed(KeyEvent e) {

		/* make a new request packet */
		MazePacket moveToBroadcast = new MazePacket();

		localAddr.name = this.getName();
		localAddr.position = this.getPoint();
		localAddr.orientation = this.getOrientation();
		localAddr.score = mazewar.scoreModel.getScore(this);
		moveToBroadcast.setclientInfo(localAddr);
		moveToBroadcast.setName(localAddr.name);
		mazewar.lClock++;
		moveToBroadcast.setlamportClock(mazewar.lClock);
		// If the user pressed Q, invoke the cleanup code and quit.
		if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {

			moveToBroadcast.setmsgType(MazePacket.DISCONNECT_REQUEST);
			moveToBroadcast.setevent(MazePacket.DISCONNECT);

			// DISCONNECT FROM NAMING SERVER

			Socket LookupSocket = null;
			ObjectOutputStream outstream = null;
			ObjectInputStream instream = null;
			// Send disconnect packet
			try {
				// LookupSocket = socket used only for connection information
				// (naming server)
				LookupSocket = new Socket(this.lookupHostName, this.lookupPort);

				outstream = new ObjectOutputStream(
						LookupSocket.getOutputStream());
				instream = new ObjectInputStream(LookupSocket.getInputStream());

				// make a new request packet
				MazePacket packetToServer = new MazePacket();

				packetToServer.setmsgType(MazePacket.DISCONNECT_REQUEST);
				packetToServer.setlamportClock(mazewar.lClock);
				packetToServer.setclientInfo(localAddr);
				packetToServer.setevent(MazePacket.DISCONNECT);

				outstream.writeObject(packetToServer);

			} catch (UnknownHostException err) {
				System.err.println("ERROR: Don't know where to connect!!");
				System.exit(1);
			} catch (IOException err) {
				err.printStackTrace();
				System.err
						.println("ERROR: Couldn't get I/O for the connection.");
				System.exit(1);
			}

			// process lookup server ack
			MazePacket packetFromServer = new MazePacket();

			try {
				do {
					packetFromServer = (MazePacket) instream.readObject();

					// Error packet
					if (packetFromServer.geterrorCode() == MazePacket.ERROR_INVALID_TYPE) {
						System.err.println("SENT INVALID TYPE");
						System.exit(-1);
					}
					if (packetFromServer.getmsgType() == MazePacket.ERROR_PLAYER_DOES_NOT_EXIST) {
						System.err.println("PLAYER DOES NOT EXIST");
						System.exit(-1);
					}
					if ((packetFromServer.getmsgType() == MazePacket.ACK)
							&& (packetFromServer.getevent() == MazePacket.DISCONNECT)) {
						//System.out.println("DISCONNECTED FROM LOOKUP SERVER");
					}

				} while (packetFromServer.getmsgType() != MazePacket.ACK); 


				outstream.close();
				instream.close();
				LookupSocket.close();

			} catch (EOFException eof) {
				System.err.println("No reply received EOF");
			} catch (IOException error) {
				System.err
						.println("ERROR: Couldn't get I/O for the connection.");
				error.printStackTrace();
				System.exit(1);
			} catch (ClassNotFoundException error) {

			} catch (NullPointerException np) {
				System.err.println("ERROR: Null pointer accessed.");
				np.printStackTrace();
			}
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);

			// Up-arrow moves forward.
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {

			moveToBroadcast.setmsgType(MazePacket.MOVE_REQUEST);
			moveToBroadcast.setevent(MazePacket.MOVE_FORWARD);
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);
			// Down-arrow moves backward.
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {

			moveToBroadcast.setmsgType(MazePacket.MOVE_REQUEST);
			moveToBroadcast.setevent(MazePacket.MOVE_BACKWARD);
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);
			// Left-arrow turns left.
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {

			moveToBroadcast.setmsgType(MazePacket.MOVE_REQUEST);
			moveToBroadcast.setevent(MazePacket.TURN_LEFT);
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);
			// Right-arrow turns right.
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {

			moveToBroadcast.setmsgType(MazePacket.MOVE_REQUEST);
			moveToBroadcast.setevent(MazePacket.TURN_RIGHT);
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);
			// Spacebar fires.
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {

			moveToBroadcast.setmsgType(MazePacket.MOVE_REQUEST);
			moveToBroadcast.setevent(MazePacket.FIRE);
			broadcastPacket(moveToBroadcast, mazewar.remotes_addrbook);

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

	public void broadcastPacket(MazePacket outPacket,
			ArrayList<Address> addressBook) {
		Socket clientsocket = null;
		ObjectOutputStream out = null;

		// If nothing has been added to the address book yet, nothing to do
		if ((addressBook != null) && (addressBook.isEmpty())) {
			return;
		}
		try {
			//System.out.println("Broadcasting packet " + outPacket.getlamportClock());
			for (int i = 0; i < addressBook.size(); i++) {
				clientsocket = new Socket(addressBook.get(i).hostname,
						addressBook.get(i).port);
				out = new ObjectOutputStream(clientsocket.getOutputStream());
				out.writeObject(outPacket);
				out.close();
				clientsocket.close();
			}
		} catch (NullPointerException npe) {
			System.err
					.println("Error: A null pointer was accessed in broadcastPacket.");
			npe.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error: IOException thrown in broadcastPacket.");
			e.printStackTrace();
		}
	}

}
