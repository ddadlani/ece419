import java.net.*;
import java.util.*;

class ClientExecutionThread extends Thread {
	private SortedMap<Double, MazePacket> localQueue;
	private Integer numRemotes;
	private Maze maze;
	private Mazewar mazewar;
	private String lookupHostName;
	private Integer lookupPort;

	public ClientExecutionThread(SortedMap<Double, MazePacket> localQueue,
			Integer numRemotes, Mazewar mazewar, Maze maze, String hostname,
			Integer port) {
		this.localQueue = localQueue;
		this.numRemotes = numRemotes;
		this.maze = maze;
		this.mazewar = mazewar;
		this.lookupHostName = hostname;
		this.lookupPort = port;
	}

	public void run() {

		MazePacket move;
		boolean local = false;
		LocalClient localClient = null;
		RemoteClient remoteClient = null;

		// Check if ACKs have been received for the move at the top
		do {
			synchronized (this.localQueue) {
				Double lclock = localQueue.firstKey();
				move = localQueue.get(lclock);
			}
		} while (!(move.getnumAcks()).equals(numRemotes));

		// Move at head of queue is valid
		if (move != null) {
			try {
				if (move.getmsgType() == MazePacket.CONNECTION_REQUEST) {

					if (move.getclientInfo().hostname.equals(InetAddress.getLocalHost().getHostName())) {
						// Our own connection has been approved
						mazewar.guiClient = new GUIClient(move.getclientInfo().name, lookupHostName,lookupPort);
						maze.addClient(mazewar.guiClient);
						mazewar.addKeyListener(mazewar.guiClient);
					} else {
						// Another player is joining the game
						remoteClient = new RemoteClient(move.getclientInfo().name);
						maze.addClient(remoteClient);
					}
				}

				else {
					if (move.getclientInfo().hostname.equals(InetAddress.getLocalHost().getHostName())) {
						local = true;
						Iterator i = maze.getClients();
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
						while (i.hasNext()) {
							Object o = i.next();
							assert (o instanceof Client);
							if (o instanceof RemoteClient) {

								remoteClient = (RemoteClient) o;
								if (move.getName().equals(remoteClient.getName()))
									found = true;
								// Integer count;
								// synchronized (map) {
								// for (count = 0; count < map.size(); count++)
								// if
								// (remoteClient.getName().equals(map.get(count)))
								// {
								// found = true;
								// break;
								// }
							}
							if (found)
								break;
							else
								remoteClient = null;
						}
					}

					if (remoteClient == null) {
						System.out
								.println("Can't find the remote client in listener queue of local machine!");
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
							// MAYBE NOT?
							Mazewar.quit();
						} else
							maze.removeClient(remoteClient);
					}
				}
			} catch (UnknownHostException e) {
			}
		}
	}

}