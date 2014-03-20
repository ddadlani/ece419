import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;

public class ClientListenHandlerThread extends Thread{
	Socket socket;
	SortedMap<Double, MazePacket> localQueue;
	ArrayList<Address> remote_addresses;
	// Integer numRemotes;
	// Integer pid;
	Double lamportClock;
	// String name;
	Address address;

	public ClientListenHandlerThread(Socket socket_, Mazewar mazewar_) {
		synchronized (mazewar_) {
			this.socket = socket_;
			this.localQueue = mazewar_.moveQueue;
			this.remote_addresses = mazewar_.remotes_addrbook;
			// May be needed for heart beats? Not sure
			// this.numRemotes = mazewar_.numRemotes;
			// this.pid = mazewar_.pid;
			this.lamportClock = mazewar_.lClock;
			this.address = new Address(mazewar_.clientAddr);
			//this.address.name = mazewar_.clientAddr.name;
			//this.address.id = mazewar_.pid;
		}
	}

	public void run() {
		try {
			MazePacket packetFromClient;
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			if ((packetFromClient = (MazePacket) in.readObject()) != null) {
				synchronized (localQueue) {
					switch (packetFromClient.getmsgType()) {

					// An ACK was received for some move
					case (MazePacket.ACK): {
						// Check that queue is properly initialized
						if ((localQueue != null) && (localQueue.size() != 0)) {
							// Look up relevant move in localQueue using
							// Lamport
							// clock value
							MazePacket gotNewAck = localQueue.get(packetFromClient.getlamportClock());

							if (gotNewAck == null) {
								// Packet with this Lamport clock not found
								System.err.println("ERROR: MazePacket with Lamport Clock "
										+ packetFromClient.getlamportClock() + " was not found.");
								System.exit(1);
							}
							gotNewAck.incrementAcks();
							// If received ACK for connect, and I am the one
							// connecting, then add player name in ACK to remotes so that can add GUI
							// Client later
							if (packetFromClient.getevent() == MazePacket.CONNECT) {
								// ADD name of clients in remote
								synchronized (remote_addresses) {
									Iterator<Address> itr = remote_addresses.iterator();
									while (itr.hasNext()) {
										Address addr = itr.next();
										if (addr.equals(packetFromClient.getclientInfo())) {
											addr.name = packetFromClient.getName();
											addr.id = packetFromClient.getclientID();
											// TODO: Set position and
											// orientation
											break;
										}
									}
								}
							}

						} else {
							// Move queue is null or empty
							System.err.println("ERROR: Move queue is null but we got an ACK? For what??");
							System.exit(1);
						}
						break;
					}

						// In either of these cases, all we do is broadcast the
						// ACK
						// and queue the move
						// for connect ACK, send back position, orientation and
						// name
						// of player sending the ack
					case (MazePacket.CONNECTION_REQUEST): {
						// Update Lamport clock
						synchronized (lamportClock) {
							if (lamportClock < packetFromClient.getlamportClock()) {
								lamportClock = packetFromClient.getlamportClock();
								lamportClock -= (packetFromClient.getclientID()/10);
								lamportClock += (address.id/10);
							}
							lamportClock++;
						}
						MazePacket Ack = new MazePacket(packetFromClient);
						Ack.setmsgType(MazePacket.ACK);
						// Send own client details so new connection can add to
						// their address book
						Ack.setclientID(address.id);
						Ack.setName(address.name);
						// Handle Position and orientation
						broadcastPacket(Ack, remote_addresses);
						localQueue.put(packetFromClient.getlamportClock(), packetFromClient);
						break;

					}
					case (MazePacket.MOVE_REQUEST):
					case (MazePacket.DISCONNECT_REQUEST): {
						// Update Lamport Clock
						synchronized (lamportClock) {
							if (lamportClock < packetFromClient.getlamportClock()) {
								lamportClock = packetFromClient.getlamportClock();
								lamportClock -= (packetFromClient.getclientID()/10);
								lamportClock += (address.id/10);
							}
							lamportClock++;
						}
						MazePacket Ack = new MazePacket(packetFromClient);
						Ack.setmsgType(MazePacket.ACK);

						broadcastPacket(Ack, remote_addresses);
						localQueue.put(packetFromClient.getlamportClock(), packetFromClient);
						break;
					}

						// TODO: case (MazePacket.HEARTBEAT) -- count heart
						// beats
						// received for each player
					default:
						// A wrong packet was received
						System.err.println("ERROR: A packet with invalid type was received.");
						break;
					}
				}
			} else {
				System.err.println("ERROR: Null packet received.");
			}

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

	/**
	 * Atomically executes operations
	 * 
	 * @param outPacket
	 *            Packet to broadcast
	 * @param addressBook
	 *            List of remote addresses
	 */

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
