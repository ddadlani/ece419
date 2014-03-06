import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

/**
 * Provides a naming server to store the locations of all connected Mazewar
 * clients
 */
public class NamingServerHandlerThread {

	private Socket socket = null;
	private HashMap<String, Address> players;

	public NamingServerHandlerThread(Socket socket,
			HashMap<String, Address> players) {
		this.socket = socket;
		this.players = players;
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

			if ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
				switch (packetFromClient.getmsgType()) {
				case (MazePacket.NEW_PLAYER): {
					error = addPlayer(packetFromClient.getName(), packetFromClient.getclientInfo());
					if (error != 0) {
						System.err.println("New player could not be added to database.");
					}
					break;
				}
				case MazePacket.REMOVE_PLAYER: {
					error = removePlayer(packetFromClient.getName());
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
			 * Send a packet back to client with error details
			 * or connected players' information
			 */
			if (packetFromClient == null) {
				error = MazePacket.ERROR_NULL_POINTER_SENT;
			}

			packetToClient = new MazePacket();
			packetToClient.setmsgType(MazePacket.NEW_PLAYER);
			if (error == 0) {
				packetToClient.remotes = getAllPlayers();
			} else {
				packetToClient.seterrorCode(error);
				error = 0;
			}
			
			toClient.writeObject(packetToClient);
			
			if ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
				if (packetFromClient.getmsgType() != MazePacket.ACK) 
					System.err.println ("ERROR: Expecting ACK. Wrong message type received.");
			} else {
				System.err.println("ERROR: Expecting ACK. Null packet received.");
			}
			
			
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
	public int addPlayer(String name, Address address) {
		if (address == null)
			return MazePacket.ERROR_NULL_POINTER_SENT;

		synchronized(players) {
			if ((players.containsKey(name) == true))
				return MazePacket.ERROR_PLAYER_EXISTS;

			if (players.put(name, address) != null) {
				return MazePacket.ERROR_COULD_NOT_ADD;
			}
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
	public int removePlayer(String name) {
		if (name == null)
			return MazePacket.ERROR_NULL_POINTER_SENT;
		
		synchronized (players) {
			if (players.remove(name) == null) {
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
	public Collection<Address> getAllPlayers() {
		synchronized (players) {
			if (players == null)
				return null;
			else
				return players.values();
		}
	}

}
