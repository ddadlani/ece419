import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Provides a naming server to store the locations of all connected Mazewar
 * clients
 */
public class NamingServerHandlerThread extends Thread{

	private Socket socket;
	private NamingServer nServer;


	public NamingServerHandlerThread(Socket socket, NamingServer nServer_) {
		this.socket = socket;
		this.nServer = nServer_;
	}

	public void run() {
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			MazePacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			MazePacket packetToClient;

			int error = 0;
			packetToClient = new MazePacket();

			if ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
				// If it's not a request for the naming server, why are we
				// getting this?
				if (packetFromClient.getmsgType() != MazePacket.LOOKUP_REQUEST)
					error = MazePacket.ERROR_INVALID_TYPE;

				switch (packetFromClient.getevent()) {
				case (MazePacket.CONNECT): {

					// Add player to playerList
					error = addPlayer(packetFromClient.getclientInfo());
					if (error != 0) {
						System.err.println("New player could not be added to database.");
					}
					if (error == 0) {
						// Set remotes and assign client ID
						// playerList contains the new connection's info as well
						packetToClient.remotes = nServer.playerList;
						packetToClient.setevent(MazePacket.CONNECT);
						synchronized (nServer) {
							packetToClient.setclientID(nServer.clientID);
							nServer.clientID = nServer.clientID + 1;
						}
					} else {
						packetToClient.seterrorCode(error);
						error = 0;
					}
					break;
				}
				case MazePacket.DISCONNECT: {
					// Remove player from playerList
					error = removePlayer(packetFromClient.getclientInfo());
					if (error != 0) {
						System.err.println("ERROR: Player could not be removed.");
					}
					break;
				}
				default:
					error = MazePacket.ERROR_INVALID_TYPE;
				}
			}

			/*
			 * Send a packet back to client with error details or connected
			 * players' information
			 */
			if (packetFromClient == null) {
				error = MazePacket.ERROR_NULL_POINTER_SENT;
			}

			if (error != 0) {
				packetToClient.seterrorCode(error);
				error = 0;
			}
			packetToClient.setmsgType(MazePacket.ACK);

			toClient.writeObject(packetToClient);

			/*
			 * if ((packetFromClient = (MazePacket) fromClient.readObject()) !=
			 * null) { if (packetFromClient.getmsgType() != MazePacket.ACK)
			 * System.err.println
			 * ("ERROR: Expecting ACK. Wrong message type received."); } else {
			 * System
			 * .err.println("ERROR: Expecting ACK. Null packet received."); }
			 */

			/*
			 * Close everything and clean up
			 */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (NullPointerException n) {
			n.printStackTrace();
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// DOES THIS USAGE OF SYNCHRONIZED CAUSE DEADLOCKS??

	/**
	 * Adds a player to the HashMap using its address
	 * 
	 * @param address
	 *            Information of type Address of new player
	 * @return Returns false if the given name already exists or an error
	 *         occurred while adding player info
	 */
	public int addPlayer(Address address) {
		if (address == null)
			return MazePacket.ERROR_NULL_POINTER_SENT;

		synchronized (nServer.playerList) {
			if (nServer.playerList.contains(address))
				return MazePacket.ERROR_PLAYER_EXISTS;

			if (nServer.playerList.add(address) == false)
				return MazePacket.ERROR_COULD_NOT_ADD;
		}
		return 0;
	}

	/**
	 * Removes a player from the database
	 * 
	 * @param name
	 *            The name of the player to remove
	 * @return Returns false if player <name> is not found in the database
	 */
	public int removePlayer(Address address) {
		if (address == null)
			return MazePacket.ERROR_NULL_POINTER_SENT;

		synchronized (nServer.playerList) {
			if (nServer.playerList.remove(address) == false) {
				return MazePacket.ERROR_PLAYER_DOES_NOT_EXIST;
			}
		}
		return 0;
	}

	/**
	 * Function to get all current player information
	 * 
	 * @return Returns all the connected players' info in a Collection<Address>
	 */
	public Object[] getAllPlayers() {
		synchronized (nServer.playerList) {
			if (nServer.playerList == null)
				return null;
			else
				return nServer.playerList.toArray();
		}
	}

}
