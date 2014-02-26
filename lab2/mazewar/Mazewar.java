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
import javax.swing.JScrollPane;
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
	public Queue<MazePacket> receive_queue = null;
	public Integer seqnumCounter;
	

	/**
	 * The default width of the {@link Maze}.
	 */
	private final int mazeWidth = 20;

	/**
	 * The default height of the {@link Maze}.
	 */
	private final int mazeHeight = 10;

	/**
	 * The default random seed for the {@link Maze}. All implementations of the
	 * same protocol must use the same seed value, or your mazes will be
	 * different.
	 */
	private final int mazeSeed = 42;

	/**
	 * The {@link Maze} that the game uses.
	 */
	private Maze maze = null;

	/**
	 * The {@link GUIClient} for the game.
	 */
	private GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

	/**
	 * The table the displays the scores.
	 */
	private JTable scoreTable = null;

	/**
	 * Create the textpane statically so that we can write to it globally using
	 * the static consolePrint methods
	 */
	private static final JTextPane console = new JTextPane();

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
	 *  First two local, second two for server
	 */
	public Mazewar(ServerSocket ReceiverSocket, int listenPort, String serverHost, Integer serverPort) {
		super("ECE419 Mazewar");
		consolePrintLn("ECE419 Mazewar started!");

		// Lab2
		this.receive_queue = new LinkedList<MazePacket>();
		this.seqnumCounter = 1;
		// Create the maze
		maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
		assert (maze != null);

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		maze.addMazeListener(scoreModel);

		// Throw up a dialog to get the GUIClient name.
		String name = JOptionPane.showInputDialog("Enter your name");
		String no_players = JOptionPane.showInputDialog("Enter the number of players:");
		if((name == null) || (name.length() == 0) || (no_players == null)) {
			System.err.println("ERROR: Need a name and the number of players to start. Shutting down Mazewar.");
			Mazewar.quit();
		}
		int num_players = Integer.parseInt(no_players);

		// You may want to put your network initialization code somewhere in
		// here.

		Socket MazeSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;


		try {
			/* variables for hostname/port */
		
			/**
			 * MazeSocket = socket used only for connection information 
			 */
			MazeSocket = new Socket(serverHost, serverPort);

			out = new ObjectOutputStream(MazeSocket.getOutputStream());
			in = new ObjectInputStream(MazeSocket.getInputStream());

			/* make a new request packet */
			MazePacket packetToServer = new MazePacket();
			Address client_addr = new Address();
			client_addr.hostname = InetAddress.getLocalHost().getHostName();
			System.out.println("" + client_addr.hostname);
			client_addr.port = listenPort; // ??
			client_addr.name = name;

			packetToServer.setmsgType(MazePacket.CONNECTION_REQUEST);
			packetToServer.setclientInfo(client_addr);

			out.writeObject(packetToServer);

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		/* process server reply */
		MazePacket packetFromServer = new MazePacket();
		try {
			do {
				packetFromServer = (MazePacket) in.readObject();
				System.out.println("Received Packet from Server: msgType: "
						+ packetFromServer.getmsgType());
				if (packetFromServer.getmsgType() == MazePacket.CONNECTION_REPLY) {
					System.out.println("Connection Made");
					// RECEIVE NUMBER AND LOCATION OF REMOTE CLIENTS, ADD THEM
					// INTO GAME
					int numRemotes = packetFromServer.remotes.length;
					for (int i = 0; i < numRemotes; i++) {
						maze.addClient(new RemoteClient(packetFromServer.remotes[i].name));
					}
				}
				if (packetFromServer.geterrorCode() == MazePacket.ERROR_INVALID_TYPE) {
					System.err.println("SENT INVALID TYPE");
					System.exit(-1);
				}
			} while (packetFromServer.getmsgType() != MazePacket.CONNECTION_REPLY);

			out.close();
			in.close();
			// MazeSocket.close();

		} catch (EOFException eof) {
			System.err.println("No reply received EOF");
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			e.printStackTrace();
			System.exit(1);
		} catch (ClassNotFoundException e) {

		}
		MazePacket remoteRequestPacket = null;
		//Case where all players haven't yet connected
		if (packetFromServer.remotes.length != (num_players - 1))
		{
			//wait for others to connect
			
			try {
				assert(ReceiverSocket != null);
				Socket socket = null; 
				ObjectInputStream input = null;
				
				while(true) {
					socket = ReceiverSocket.accept();
					input = new ObjectInputStream(socket.getInputStream());
					remoteRequestPacket = (MazePacket) input.readObject();
					
					if (remoteRequestPacket != null) {
						if (remoteRequestPacket.getmsgType().equals(MazePacket.NEW_REMOTE_CONNECTION) && (remoteRequestPacket.remotes.length == (num_players - 1)))
							break;
					}
					input.close();
					socket.close();
				} 
			
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		else
		{
			remoteRequestPacket = packetFromServer;
		}
		//int[] client_ids = new int[num_players];
		//client_ids[0] = packetFromServer.getclientID();
		//HashMap<Integer, Integer> client_ids = new HashMap<Integer, Integer>();
	    Map<Integer,String> clientIDs_sorted = new TreeMap<Integer,String>();
	    // Put some values in it
	    int i;
	    clientIDs_sorted.put(packetFromServer.getclientID(),packetFromServer.getclientInfo().name);
		for (i = 0; i < (num_players-1); i++)
		{
			clientIDs_sorted.put(remoteRequestPacket.remotes[i].id, remoteRequestPacket.remotes[i].name );
		}
	    // Iterate through it and it'll be in order!
	    for(Map.Entry<Integer,String> entry : clientIDs_sorted.entrySet()) {
	    	
	    	String name_of_player = entry.getValue();
	    	if (name_of_player.equals(packetFromServer.getclientInfo().name))
	    	{
	    		guiClient = new GUIClient(name_of_player, serverHost, serverPort);
	    		maze.addClient(guiClient);
	    	}	
	    	else
	    	{
	    		RemoteClient rc = new RemoteClient(name_of_player);
	    		maze.addClient(rc);
	    	}
	    		
	    } 
			
		
		// Create the GUIClient and connect it to the KeyListener queue
		// just removed MazeSocket from arguments
		guiClient = new GUIClient(name, serverHost, serverPort);
		maze.addClient(guiClient);
		this.addKeyListener(guiClient);

		// Use braces to force constructors not to be called at the beginning of
		// the constructor.
		// Uncomment below code to create remote clients
		/*
		 * { maze.addClient(new RobotClient("Norby")); maze.addClient(new
		 * RobotClient("Robbie")); maze.addClient(new RobotClient("Clango"));
		 * maze.addClient(new RobotClient("Marvin")); }
		 */

		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(maze, guiClient);
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
		this.requestFocusInWindow();
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
		try {
			
			if(args.length == 2) {
                hostname = args[0];
                hostport = Integer.parseInt(args[1]);
			} else {
                System.err.println("ERROR: Invalid arguments! Usage: ./run.sh <server_host> <server_port>");
                System.exit(-1);
			}
		
			// sending the serversocket into the Mazewar constructor to add to the
			// client info object
			ServerSocket ReceiverSocket = new ServerSocket(0);
			int listenPort = ReceiverSocket.getLocalPort();

			/* Create the GUI */
			Mazewar mazewar = new Mazewar(ReceiverSocket, listenPort, hostname, hostport);

			// Maze maze = null;
			// mazewar.maze = maze;
			boolean listening = true;
			try {
				while(listening)
					new ClientListenerThread(ReceiverSocket.accept(), mazewar, mazewar.maze, mazewar.seqnumCounter).start();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			ReceiverSocket.close();
			// ClientListenerThread Lt = new ClientListenerThread
			// (ReceiverSocket.accept(), mazewar);
			// new Thread(Lt).start();
			// new ClientExecutionThread(mazewar, maze).start();
			// ReceiverSocket.close();
		} catch (IOException ioe) {
			System.err.println("Error: ReceiverSocket error.");
		}
	}

}
