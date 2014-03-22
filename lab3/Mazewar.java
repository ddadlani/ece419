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

import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;

/**
 * The entry point and glue code for the game. It also contains some helpful
 * global utility methods.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

	// LAB2
	// public Queue<MazePacket> receive_queue = null;
	// public Integer seqnumCounter;
	// public Map<Integer,String> clientIDs_sorted = null;

	// LAB3
	public ArrayList<Address> remotes_addrbook;
	public int pid;
	public double lClock;
	public int numPlayers;
	public SortedMap<Double, MazePacket> moveQueue;
	public Address clientAddr;
	public Integer listenPort;
	/**
	 * The default width of the {@link Maze}.
	 */
	public final int mazeWidth = 20;

	/**
	 * The default height of the {@link Maze}.
	 */
	public final int mazeHeight = 10;

	/**
	 * The default random seed for the {@link Maze}. All implementations of the
	 * same protocol must use the same seed value, or your mazes will be
	 * different.
	 */
	public final int mazeSeed = 42;

	/**
	 * The {@link Maze} that the game uses.
	 */
	public Maze maze = null;

	/**
	 * The {@link GUIClient} for the game.
	 */
	public GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	public OverheadMazePanel overheadPanel = null;

	/**
	 * The table the displays the scores.
	 */
	public JTable scoreTable = null;

	/**
	 * Create the textpane statically so that we can write to it globally using
	 * the static consolePrint methods
	 */
	public static final JTextPane console = new JTextPane();

	/**
	 * Write a message to the console followed by a newline.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrintLn(String msg) {
		console.setText(console.getText() + msg + "\n");
	}

	/**
	 * Write a message to the console.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrint(String msg) {
		console.setText(console.getText() + msg);
	}

	/**
	 * Clear the console.
	 */
	public static synchronized void clearConsole() {
		console.setText("");
	}

	/**
	 * Static method for performing cleanup before exiting the game.
	 */
	public static void quit() {
		// Put any network clean-up code you might have here.
		// (inform other implementations on the network that you have
		// left, etc.)

		System.exit(0);
	}

	/**
	 * The place where all the pieces are put together.
	 */
	public Mazewar(int listenPort, String serverHost, Integer serverPort) {
		
		super("ECE419 Mazewar");
		consolePrintLn("ECE419 Mazewar started!");

		//Lab 3 Initializations 
		remotes_addrbook = new ArrayList<Address> ();
		moveQueue = new TreeMap<Double, MazePacket>();
		clientAddr = new Address();
		
		// Create the maze
		maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
		assert (maze != null);

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
/*		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		maze.addMazeListener(scoreModel);*/

		// Throw up a dialog to get the GUIClient name.
		String name = JOptionPane.showInputDialog("Enter your name");
		// String no_players =
		// JOptionPane.showInputDialog("Enter the number of players:");
		if ((name == null) || (name.length() == 0)) {
			System.err
					.println("ERROR: Need a name and the number of players to start. Shutting down Mazewar.");
			Mazewar.quit();
		}

		Socket MazeSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;


		try {
			// MazeSocket = socket used only for connection information (naming
			// server)
			MazeSocket = new Socket(serverHost, serverPort);

			out = new ObjectOutputStream(MazeSocket.getOutputStream());
			in = new ObjectInputStream(MazeSocket.getInputStream());

			// make a new request packet
			MazePacket packetToServer = new MazePacket();
			clientAddr.name = name;
			clientAddr.hostname = InetAddress.getLocalHost().getHostName();
			clientAddr.port = listenPort;

			packetToServer.setmsgType(MazePacket.LOOKUP_REQUEST);
			packetToServer.setclientInfo(clientAddr);
			packetToServer.setevent(MazePacket.CONNECT);

			out.writeObject(packetToServer);

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		// process server reply
		MazePacket packetFromServer = new MazePacket();

		try {
			do {
				packetFromServer = (MazePacket) in.readObject();

				// Error packet
				if (packetFromServer.geterrorCode() == MazePacket.ERROR_INVALID_TYPE) {
					System.err.println("SENT INVALID TYPE");
					System.exit(-1);
				}
				if (packetFromServer.getmsgType() == MazePacket.ERROR_PLAYER_EXISTS) {
					System.err.println("SENT CONNECTION REQUEST PACKET TWICE");
					System.exit(-1);
				}
				if ((packetFromServer.getmsgType() == MazePacket.ACK)
						&& (packetFromServer.getevent() == MazePacket.CONNECT)) {

					// RECEIVE NUMBER AND LOCATION OF REMOTE CLIENTS, ADD THEM
					// INTO REMOTES (includes yourself)
					numPlayers = packetFromServer.remotes.size();
					remotes_addrbook = packetFromServer.remotes;
					pid = packetFromServer.getclientID();
					clientAddr.id = pid;
					lClock = (double) pid / 10.0; // Initialize lClock
				}

			} while (packetFromServer.getmsgType() != MazePacket.ACK); // Do we need the do-while loop?

			out.close();
			in.close();
			MazeSocket.close();

		} catch (EOFException eof) {
			System.err.println("No reply received EOF");
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			e.printStackTrace();
			System.exit(1);
		} catch (ClassNotFoundException e) {

		} catch (NullPointerException np) {
			System.err.println("ERROR: Null pointer accessed.");
			np.printStackTrace();
		}

		// Create the panel that will display the maze.
		/*overheadPanel = new OverheadMazePanel(maze, guiClient);
		assert (overheadPanel != null);
		maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		console.setEditable(false);
		console.setFocusable(false);
		console.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(console);
		assert (consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Console"));

		// Create the score table
		scoreTable = new JTable(scoreModel);
		assert (scoreTable != null);
		scoreTable.setFocusable(false);
		scoreTable.setRowSelectionAllowed(false);

		// Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
		assert (scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Scores"));

		// Create the layout manager
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(layout);

		// Define the constraints on the components.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(overheadPanel, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();*/
	}

	/**
	 * Entry point for the game.
	 * 
	 * @param args
	 *            Command-line arguments.
	 */
	public static void main(String args[]) {
		String hostname = null;
		Integer hostport = null;

		if (args.length == 2) {
			hostname = args[0];
			hostport = Integer.parseInt(args[1]);
		} else {
			System.err
					.println("ERROR: Invalid arguments! Usage: ./run.sh <server_host> <server_port>");
			System.exit(-1);
		}

		// Start the listener socket. This will be passed into the listener
		// thread
		try {
			ServerSocket listenSocket = new ServerSocket(0);
			int listenPort = listenSocket.getLocalPort();

			Mazewar mazewar = new Mazewar(listenPort, hostname, hostport);
			new Thread(new ClientListenerThread(mazewar, listenSocket)).start();
			new Thread(new ClientExecutionThread(mazewar, mazewar.maze, hostname,
					hostport)).start();

			// Send Connection requests to all clients including itself

			// increment clock before sending
			synchronized (mazewar) {
				mazewar.lClock++;
			}
			
			MazePacket toPlayers = new MazePacket();
			toPlayers.setclientInfo(mazewar.clientAddr);
			toPlayers.setmsgType(MazePacket.CONNECTION_REQUEST);
			toPlayers.setevent(MazePacket.CONNECT);
			toPlayers.remotes = mazewar.remotes_addrbook;
			toPlayers.setlamportClock(mazewar.lClock);
			toPlayers.setName(mazewar.clientAddr.name);

			System.out.println("Broadcasting connect req from " + toPlayers.getlamportClock());

			mazewar.broadcastPacket(toPlayers, toPlayers.remotes);

		} catch (EOFException eofe) {
			System.err.println("Connection error");
			eofe.printStackTrace();
			System.exit(1);
		} catch (SocketException se) {
			System.err.println("Connection error");
			se.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public void broadcastPacket(MazePacket outPacket, ArrayList<Address> addressBook) {
		Socket clientsocket = null;
		ObjectOutputStream out = null;

		// If nothing has been added to the address book yet, nothing to do
		if ((addressBook != null) && (addressBook.isEmpty())) {
			return;
		}
		try {
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
