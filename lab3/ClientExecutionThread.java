import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;

class ClientExecutionThread extends Thread {
	private Maze maze;
	private Mazewar mazewar;
	private String lookupHostName;
	private Integer lookupPort;
	private ScoreTableModel scoreModel;

	public ClientExecutionThread(Mazewar mazewar, Maze maze, String hostname, Integer port, ScoreTableModel scoremodel) {
		this.maze = maze;
		this.mazewar = mazewar;
		this.lookupHostName = hostname;
		this.lookupPort = port;
		this.scoreModel = scoremodel;
		// ScoreModel initialization now done in Mazewar constructor
		assert (scoreModel != null);
	}

	public void run() {
		boolean created = false;
		while (true) {
			MazePacket move = new MazePacket();
			boolean local = false;
			LocalClient localClient = null;
			RemoteClient remoteClient = null;

			// Check if ACKs have been received for the move at the top
			if (mazewar.moveQueue != null && mazewar.moveQueue.size() > 0) {
				System.out.println("In CEX, Local queue size  = " + mazewar.moveQueue.size());

				do {
					int count = 10000;
					while (count > 0)
						count--;
					synchronized (mazewar.moveQueue) {
						double lclock = mazewar.moveQueue.firstKey();
						move = mazewar.moveQueue.get(lclock);
					}

				} while (move.getnumAcks() < mazewar.numPlayers);

				// Execute move that was at head of queue
				if (move != null) {
					// Connection Request packet
					if (move.getmsgType() == MazePacket.CONNECTION_REQUEST) {

						// This is our connection move
						if (move.getclientInfo().equals(mazewar.clientAddr)) {
							// Wait for other position packets
							do {
								double lclock = mazewar.moveQueue.firstKey();
								move = mazewar.moveQueue.get(lclock);
							} while (move.getnumposAcks() < (mazewar.numPlayers - 1));

							synchronized (mazewar) {
								System.out.println("Received all n-1 POS acks from remotes");
								// Add all remote players
								Iterator<Address> i = mazewar.remotes_addrbook.iterator();
								while (i.hasNext()) {
									Address remoteAddr = i.next();

									// Don't add yourself as remote
									if (!(remoteAddr.equals(mazewar.clientAddr))) {
										remoteClient = new RemoteClient(remoteAddr.name);
										maze.addGivenClient(remoteClient, remoteAddr.position, remoteAddr.orientation);
										scoreModel.setScore(remoteClient, remoteAddr.score);
									}
								}

								// Create yourself

								mazewar.guiClient = new GUIClient(move.getclientInfo().name, move.getclientInfo(), lookupHostName,
										lookupPort, mazewar);

								maze.addClient(mazewar.guiClient);
								mazewar.addKeyListener(mazewar.guiClient);
							}
							// Update your position and direction in your
							// clientAddr
							synchronized (mazewar.clientAddr) {
								mazewar.clientAddr.position = new Point(mazewar.guiClient.getPoint());
								mazewar.clientAddr.orientation = mazewar.guiClient.getOrientation().createNewDirection();
								mazewar.clientAddr.score = 0;
								System.out.println("Your position X = " + mazewar.clientAddr.position.getX());
								System.out.println("Your position Y = " + mazewar.clientAddr.position.getY());
							}

							// Now send your ACK so everyone (including you) can
							// add you to the game
							MazePacket Ack = new MazePacket();
							Ack.setmsgType(MazePacket.POSITION);
							Ack.setName(mazewar.clientAddr.name);
							Ack.setclientID(mazewar.clientAddr.id);
							Ack.setclientInfo(mazewar.clientAddr);

							broadcastPacket(Ack, mazewar.remotes_addrbook);
							System.out.println("Broadcasted new client pos info");
							// wait for ack from yourself
							do {
								double lclock = mazewar.moveQueue.firstKey();
								move = mazewar.moveQueue.get(lclock);
							} while (move.getnumposAcks() < (mazewar.numPlayers));

							System.out.println("Received POS ack from myself");
							Create_game(scoreModel);

						} else {
							// Setting position and orientation and score for
							// new client
							mazewar.clientAddr.position = new Point(mazewar.guiClient.getPoint());
							mazewar.clientAddr.orientation = mazewar.guiClient.getOrientation().createNewDirection();
							mazewar.clientAddr.score = mazewar.scoreModel.getScore(mazewar.guiClient);

							// Send these to the new client
							MazePacket positionAck = new MazePacket();
							positionAck.setmsgType(MazePacket.POSITION);
							positionAck.setclientID(mazewar.clientAddr.id);
							positionAck.setName(mazewar.clientAddr.name);
							positionAck.setclientInfo(mazewar.clientAddr);

							try {
								Socket sendPos = new Socket(move.getclientInfo().hostname, move.getclientInfo().port);
								ObjectOutputStream out = new ObjectOutputStream(sendPos.getOutputStream());
								out.writeObject(positionAck);
							} catch (IOException ioe) {
								System.err.println("ERROR: Could not send position to new client.");
								ioe.printStackTrace();
								System.exit(1);
							}
							System.out.println("Sent my POS ack to new client. Pos ack should be 0: " + move.getnumposAcks());
							do {
								double lclock = mazewar.moveQueue.firstKey();
								move = mazewar.moveQueue.get(lclock);
							} while (move.getnumposAcks() < 1);

							System.out.println("Received POS ack from new client");
							// Another player is joining the game
							synchronized (mazewar.remotes_addrbook) {
								System.out.println("Acquired remotes_addrbook in client exec while adding new client as remote");
								Iterator<Address> i = mazewar.remotes_addrbook.iterator();
								while (i.hasNext()) {
									Address remoteAddr = i.next();
									if (remoteAddr.equals(move.getclientInfo())) {
										System.out.println("Adding remote client at posX = " + remoteAddr.position.getX() + " "
												+ remoteAddr.position.getY());
										remoteClient = new RemoteClient(remoteAddr.name);
										// Add client to maze at given position
										// and orientation
										maze.addGivenClient(remoteClient, remoteAddr.position, remoteAddr.orientation);
										// Update score of remote client
										scoreModel.setScore(remoteClient, move.getclientInfo().score);
									}
								}
							}

						}

					} else {
						// Other than connection request packet

						if (move.getclientInfo().equals(mazewar.clientAddr)) {
							local = true;
							Iterator i = maze.getClients();

							// Find the local client

							while (i.hasNext()) {
								Object o = i.next();
								assert (o instanceof Client);
								if (o instanceof LocalClient) {
									localClient = (LocalClient) o;
									break;
								}
							}

							if (localClient == null) {
								System.out.println("Can't find the local client in listener queue!");
							}

						} else {

							Iterator i = maze.getClients();
							boolean found = false;

							// Find which of the remote client is this packet
							// from
							while (i.hasNext()) {
								Object o = i.next();
								assert (o instanceof Client);
								if (o instanceof RemoteClient) {

									remoteClient = (RemoteClient) o;
									if (move.getName().equals(remoteClient.getName()))
										found = true;
								}
								if (found)
									break;
								else
									remoteClient = null;
							}
						}

						if ((remoteClient == null) && (!local)) {
							System.err.println("ERROR: Can't find the remote client in listener queue of local machine!");
							System.exit(1);
						}

						if (move.getmsgType() == MazePacket.MOVE_REQUEST) {
							if (local) {
								if (move.getevent() == MazePacket.MOVE_FORWARD)
									localClient.forward();
								else if (move.getevent() == MazePacket.MOVE_BACKWARD)
									localClient.backup();
								else if (move.getevent() == MazePacket.TURN_LEFT)
									localClient.turnLeft();
								else if (move.getevent() == MazePacket.TURN_RIGHT)
									localClient.turnRight();
								else if (move.getevent() == MazePacket.FIRE)
									localClient.fire();
							} else {
								if (move.getevent() == MazePacket.MOVE_FORWARD)
									remoteClient.forward();
								else if (move.getevent() == MazePacket.MOVE_BACKWARD)
									remoteClient.backup();
								else if (move.getevent() == MazePacket.TURN_LEFT)
									remoteClient.turnLeft();
								else if (move.getevent() == MazePacket.TURN_RIGHT)
									remoteClient.turnRight();
								else if (move.getevent() == MazePacket.FIRE)
									remoteClient.fire();
							}
						}
						// }

						else if (move.getmsgType() == MazePacket.DISCONNECT_REQUEST) {

							if (local) {
								Mazewar.quit();
							} else {
								synchronized (mazewar) {
									mazewar.numPlayers--;
									mazewar.remotes_addrbook.remove(move.getclientInfo());
								}
								maze.removeClient(remoteClient);
							}
						}
					}
					// We can now proceed with the game
					synchronized (mazewar.moveQueue) {
						// remove head of the queue
						mazewar.moveQueue.remove(mazewar.moveQueue.firstKey());
					}
				}
			}
		}
	}

	public void Create_game(ScoreTableModel scoreModel) {

		// Create the panel that will display the maze.
		mazewar.overheadPanel = new OverheadMazePanel(maze, mazewar.guiClient);
		assert (mazewar.overheadPanel != null);
		maze.addMazeListener(mazewar.overheadPanel);

		// Don't allow editing the console from the GUI
		mazewar.console.setEditable(false);
		mazewar.console.setFocusable(false);
		mazewar.console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(mazewar.console);
		assert (consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

		// Create the score table
		mazewar.scoreTable = new JTable(scoreModel);
		assert (mazewar.scoreTable != null);
		mazewar.scoreTable.setFocusable(false);
		mazewar.scoreTable.setRowSelectionAllowed(false);

		// Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(mazewar.scoreTable);
		assert (scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

		// Create the layout manager
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		mazewar.getContentPane().setLayout(layout);

		// Define the constraints on the components.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(mazewar.overheadPanel, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		mazewar.getContentPane().add(mazewar.overheadPanel);
		mazewar.getContentPane().add(consoleScrollPane);
		mazewar.getContentPane().add(scoreScrollPane);

		// Pack everything neatly.
		mazewar.pack();

		// Let the magic begin.
		mazewar.setVisible(true);
		mazewar.overheadPanel.repaint();
		mazewar.requestFocusInWindow();
	}

	public void broadcastPacket(MazePacket outPacket, ArrayList<Address> addressBook) {
		Socket clientsocket = null;
		ObjectOutputStream out = null;
		synchronized (addressBook) {
			// If nothing has been added to the address book yet, nothing to do
			if ((addressBook != null) && (addressBook.isEmpty())) {
				return;
			}
			try {
				for (int i = 0; i < addressBook.size(); i++) {
					clientsocket = new Socket(addressBook.get(i).hostname, addressBook.get(i).port);
					out = new ObjectOutputStream(clientsocket.getOutputStream());
					out.writeObject(outPacket);
					out.close();
					clientsocket.close();
				}
			} catch (NullPointerException npe) {
				System.err.println("Error: A null pointer was accessed in broadcastPacket.");
				npe.printStackTrace();
			} catch (IOException e) {
				System.err.println("Error: IOException thrown in broadcastPacket.");
				e.printStackTrace();
			}
		}
	}
}