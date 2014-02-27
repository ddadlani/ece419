import java.net.*;
import java.io.*;
import java.util.*;

public class ClientListenerThread extends Thread {
	private Socket socket = null;
	private Queue<MazePacket> queue;
	private Integer seqnumCounter;
	private Maze maze;
	private Map<Integer, String> map = null;

	public ClientListenerThread(Socket socket, Mazewar mazewar, Maze maze,
			Integer seqnumCounter) {
		super("ClientListenerThread");
		this.socket = socket;
		this.queue = mazewar.receive_queue;
		this.maze = maze;
		this.seqnumCounter = seqnumCounter;
		this.map = mazewar.clientIDs_sorted;
	}

	public void run() {
		ObjectInputStream in = null;
		try {
			/* variables for hostname/port */
			in = new ObjectInputStream(socket.getInputStream());
			MazePacket packetFromServer = new MazePacket();

			packetFromServer = (MazePacket) in.readObject();
			//System.out.println("IM LISTENING!");
			// NOT SORTING QUEUE(using priority queue) YET
			// just adding to queue if in sequence, otherwise dropping packet
			if (packetFromServer != null) {

				synchronized (this.queue) {
					queue.add(packetFromServer);
				}
			}

			/* EXECUTE! */

			MazePacket move = new MazePacket();
			boolean local = false;
			LocalClient localClient = null;
			RemoteClient remoteClient = null;
			synchronized (this.queue) {
				move = queue.poll();
			}
			if (move != null) {
				// Check if local
				//System.out.println("IM EXECUTING!");
				try {
					if (move.getclientInfo().hostname.equals(InetAddress
							.getLocalHost().getHostName())) {
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
							System.out
									.println("Can't find the local client in listener queue!");
						}

					}

					else {
						Iterator i = maze.getClients();
						boolean found = false;
						while (i.hasNext()) {
							Object o = i.next();
							assert (o instanceof Client);
							if (o instanceof RemoteClient) {
								remoteClient = (RemoteClient) o;
								Integer count;
								synchronized (map) {
								for (count = 0; count < map.size(); count++)
									if (remoteClient.getName().equals(map.get(count))) {
										found = true;
										break;
									}
								}
								if (found)
									break;
								else
									remoteClient = null;
							}
						}

						if (remoteClient == null) {
							System.out
									.println("Can't find the remote client in listener queue!");
						}
					}

				} catch (UnknownHostException e) {
				}

				if (move.getmsgType() == MazePacket.MAZE_REPLY) {
					if (local) {
						if (move.getevent() == MazePacket.MOVE_FORWARD)
							localClient.forward();
						// maze.moveClientForward(localClient);
						else if (move.getevent() == MazePacket.MOVE_BACKWARD)
							localClient.backup();
						// maze.moveClientBackward(localClient);
						else if (move.getevent() == MazePacket.TURN_LEFT)
							localClient.turnLeft();
						// ClientEvent ce = ClientEvent.turnLeft;
						// maze.clientUpdate(localClient, ce);
						else if (move.getevent() == MazePacket.TURN_RIGHT)
							localClient.turnRight();
						else if (move.getevent() == MazePacket.FIRE)
							localClient.fire();
					} else {
						if (move.getevent() == MazePacket.MOVE_FORWARD)
							remoteClient.forward();
						// maze.moveClientForward(remoteClient);
						else if (move.getevent() == MazePacket.MOVE_BACKWARD)
							remoteClient.backup();
						// maze.moveClientBackward(remoteClient);
						else if (move.getevent() == MazePacket.TURN_LEFT)
							remoteClient.turnLeft();
						// ClientEvent ce = ClientEvent.turnLeft;
						// maze.clientUpdate(remoteClient, ce);
						else if (move.getevent() == MazePacket.TURN_RIGHT)
							remoteClient.turnRight();
						else if (move.getevent() == MazePacket.FIRE)
							remoteClient.fire();
					}
				}

				else if (move.getmsgType() == MazePacket.MAZE_DISCONNECT) {
					// CHECK IF LOCAL
					if (local)
						Mazewar.quit();
					// NEED TO ADD SOCKET CLOSING STUFF!
					// REMOVE REMOTE CLIENT
					else
						maze.removeClient(remoteClient);
				}
				/*
				 * else if (move.getmsgType() ==
				 * MazePacket.NEW_REMOTE_CONNECTION) { //ADD REMOTE CLIENT
				 * remoteClient = new RemoteClient(move.getclientInfo().name);
				 * maze.addClient(remoteClient); }
				 */
			}

			in.close();
			// socket.shutdownInput();

		} catch (EOFException eof) {
			System.err.println("No reply received EOF");
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		} catch (ClassNotFoundException e) {
		}
	}
}