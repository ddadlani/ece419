import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MazeServerHandlerThread extends Thread{

	private Socket socket = null;
	private MazeServer mazeData;

	public MazeServerHandlerThread(Socket socket, MazeServer maze) {
		super("MazeServerHandlerThread");
		this.socket = socket;
		this.mazeData = maze;
	}

	public void run() {
		System.out.println("Entered MazeServerHandlerThread");
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			MazePacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			MazePacket packetToClient;	// Remember to initialize this before using it!

			/* Next packet in queue */
			MazePacket nextInLine = null;
			Boolean queued = true;

			if ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {

				/* process symbol */
				Integer type = packetFromClient.getmsgType();
				// Check if type is incorrect. If it is, do not update sequence number.
				if ((type != MazePacket.CONNECTION_REQUEST) && (type != MazePacket.MAZE_REQUEST) && (type != MazePacket.MAZE_DISCONNECT)) {
					packetToClient = new MazePacket();
					packetToClient.seterrorCode(MazePacket.ERROR_INVALID_TYPE);
					toClient.writeObject(packetToClient);
					System.err.println("Error: Invalid packet type received from " + packetToClient.getclientInfo().name
									+ ". Dropping packet.");
					return;
				}
				// type must be valid here
				else {
					// Inform the client that you got the packet
					MazePacket ack = new MazePacket();
					toClient.writeObject(ack);
					synchronized (mazeData.sequenceNum) {
						synchronized(mazeData.requestQueue) {
							System.out.println("Entered Queuing server code (synchronized) seqnum: " + mazeData.sequenceNum);
							// Update sequence number
							packetFromClient.setseqNum(mazeData.sequenceNum);
							System.out.println("Entered Queuing server code (synchronized)");
							mazeData.sequenceNum++;
							System.out.println("Entered Queuing server code (synchronized)");
							// Update request queue
							queued = mazeData.requestQueue.add(packetFromClient);
							System.out.println("Added packet to queue (synchronized)");
							nextInLine = mazeData.requestQueue.poll(); // remove() doesn't tell you if the requestQueue is empty. poll does. 
							//System.out.println("Dequeued packet hostname: " + nextInLine.getclientInfo().hostname);
						}
					}
					if (queued != true) {
						System.err.println("Request could not be queued. Aborting.");
						System.exit(-1);
					}

					/* if (nextInLine == null)
						throw new NullPointerException(); */
				}

				switch (nextInLine.getmsgType()) {
					case MazePacket.CONNECTION_REQUEST: {
						Integer numRemotes;
						System.out.println("Entered CONNECTION REQUEST processing");
						// Assign client ID
						synchronized (mazeData.clientID) {
							nextInLine.setclientID(mazeData.clientID);
							// Update global client ID
							mazeData.clientID++;

							synchronized(mazeData.addressBook) {
								// Ask all clients to create a new remote client
								nextInLine.setmsgType(MazePacket.NEW_REMOTE_CONNECTION);
								broadcastPacket(nextInLine, mazeData.addressBook);

								// Gather remote client data for the new connection
								numRemotes = mazeData.addressBook.size();
								nextInLine.remotes = new Address[numRemotes];						
								for (int i = 0; i < numRemotes; i++) {
									nextInLine.remotes[i] = new Address(mazeData.addressBook.get(i));
								}
								// Add new connection data to the addressBook
								mazeData.addressBook.add(nextInLine.getclientInfo());	
							}			// addressBook released here
						}			// client ID released here

						packetToClient = nextInLine;
						packetToClient.setmsgType(MazePacket.CONNECTION_REPLY);
						toClient.writeObject(packetToClient);
						packetToClient = null;
						break;
					}

					case MazePacket.MAZE_REQUEST: {
						packetToClient = nextInLine;
						// Set message type
						packetToClient.setmsgType(MazePacket.MAZE_REPLY);
						// Broadcast move to all players
						synchronized (mazeData.addressBook) {
							broadcastPacket(packetToClient, mazeData.addressBook);
						}
						break;
					}

					case MazePacket.MAZE_DISCONNECT: {
						synchronized (mazeData.addressBook) {
							packetToClient = nextInLine;
							// Broadcast to all players to remove this remoteclient
							broadcastPacket(packetToClient, mazeData.addressBook);
							// Delete client's data from addressbook (synchronized)
							Integer index = MazeServer.searchInAddressBook(nextInLine.getclientID(), mazeData.addressBook);
							mazeData.addressBook.remove(index);
						}	// addressBook released here

						break;
					}
					default: {
						// Should not get here
						System.err.println("Error: Unknown packet in requestQueue, generated by player " + nextInLine.getclientInfo().name
									+ ". Dropping packet.");
						System.exit(1); 
					}
				}		// end of switch statement
			}		// end of while loop

			/* cleanup when client exits */
			// Closing the streams closes the socket as well. 
			// We only want to close the inputstream and outputstream
			fromClient.close();
			toClient.close();
			fromClient = null;
			toClient = null;

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

	private void broadcastPacket(MazePacket outPacket, ArrayList<Address> addressBook) {
		Socket clientsocket = null;
			ObjectOutputStream out = null;

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
