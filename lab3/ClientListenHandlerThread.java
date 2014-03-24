import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;

public class ClientListenHandlerThread extends Thread {
	Socket socket;
	Address address;
	Mazewar mazewar;
	SortedMap<Double, Integer> ackQueue;

	/**
	 * Creates a new thread to handle the request from another client
	 * 
	 * @param socket_
	 *            The socket connecting to the other client
	 * @param mazewar_
	 *            Mazewar game
	 * @param ackQueue_
	 *            The queue of ACKs being collected to account for lack of FIFO
	 *            order
	 */
	public ClientListenHandlerThread(Socket socket_, Mazewar mazewar_, SortedMap<Double, Integer> ackQueue_) {
		synchronized (mazewar_) {
			this.socket = socket_;
			this.address = new Address(mazewar_.clientAddr);
			this.mazewar = mazewar_;
			this.ackQueue = ackQueue_;
		}
	}

	public void run() {
		try {
			MazePacket packetFromClient;
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			if ((packetFromClient = (MazePacket) in.readObject()) != null) {
				synchronized (mazewar.moveQueue) {
					synchronized (ackQueue) {
						switch (packetFromClient.getmsgType()) {

						// An ACK was received for some move
						case (MazePacket.ACK): {
							// Look up relevant move in mazewar.moveQueue using
							// Lamport clock value
							MazePacket gotNewAck = null;

							System.out.println("ACK from " + packetFromClient.getlamportClock() + " in " + address.name
									+ " for " + packetFromClient.getevent());
							gotNewAck = mazewar.moveQueue.get(packetFromClient.getlamportClock());

							if (gotNewAck != null) {
								gotNewAck.incrementAcks();

							} else if (gotNewAck == null) {
								// Packet with this Lamport clock not found.
								// Add to queue to wait for actual request
								System.out.println("MazePacket with Lamport Clock "
										+ packetFromClient.getlamportClock()
										+ " was not found. Adding ACK to ackQueue.");
								Integer currentAcks;
								// synchronized (ackQueue) {
								currentAcks = ackQueue.get(packetFromClient.getlamportClock());
								// }
								if (currentAcks == null) {
									currentAcks = new Integer(1);

								} else {
									currentAcks++;

								}
								// synchronized (ackQueue) {
								// put will remove the previous value associated
								// with this Lamport clock
								ackQueue.put(packetFromClient.getlamportClock(), currentAcks);
								// }
							}

							// If received ACK for own connection, add player
							// name in ACK to
							// remotes so that we can add GUIClient later
							//if ((packetFromClient.getevent() == MazePacket.CONNECT)
							//		&& (((packetFromClient.getlamportClock() * 10) % 10) == address.id)) {
								// Add synchronization info for this
								// remoteclient
							break;
						}
							
						case (MazePacket.POSITION): {
							System.out.println("Received POS from " + packetFromClient.getlamportClock());
							// Increment number of ACKs
							double key = mazewar.moveQueue.firstKey();
							MazePacket connectFirst  = mazewar.moveQueue.get(key);
							connectFirst.incrementAcks();
							
							// Set the position and orientation and score in remote addresses
							Address pktAddr = packetFromClient.getclientInfo();
							synchronized (mazewar.remotes_addrbook) {
								Iterator<Address> i = mazewar.remotes_addrbook.iterator();
								while (i.hasNext()) {
									Address remoteAddr = i.next();
									if (remoteAddr.equals(pktAddr)) {
										remoteAddr.name = pktAddr.name;
										remoteAddr.id = pktAddr.id;
										remoteAddr.position = pktAddr.position;
										remoteAddr.orientation = pktAddr.orientation;
										remoteAddr.score = pktAddr.score;
									}
								}
							}
							break;
						}

						// for connect ACK, send back position, orientation
						// name and address of player sending the ack
						case (MazePacket.CONNECTION_REQUEST): {
							System.out.println("Connect request from " + packetFromClient.getlamportClock() + " in "
									+ address.name);
							// Check if any ACKs have been received for this
							// move
							Double moveLClock = new Double(packetFromClient.getlamportClock());
							Integer moveAcks = ackQueue.get(moveLClock);
							if (moveAcks != null) {
								// Add any received ACKs to the packet and
								// remove from queue
								packetFromClient.setnumAcks(moveAcks.intValue());
								ackQueue.remove(moveLClock);
							}
							// Update Lamport clock
							synchronized (mazewar) {
								if (mazewar.lClock < packetFromClient.getlamportClock()) {
									mazewar.lClock = packetFromClient.getlamportClock();
									mazewar.lClock -= (packetFromClient.getclientID() / 10);
									mazewar.lClock += (address.id / 10);
								}
								mazewar.lClock++;
							}
							MazePacket Ack = new MazePacket(packetFromClient);
							Ack.setmsgType(MazePacket.ACK);
							// Send own client details so new connection can add
							// to their address book
							Ack.setclientID(address.id);
							Ack.setName(address.name);
							Ack.setclientInfo(address);
							// Handle Position and orientation
							if (!(packetFromClient.getclientInfo().equals(address))) {
								// received connect_req from another person
								// add address of new member
								synchronized (mazewar.remotes_addrbook) {
									mazewar.remotes_addrbook.clear();
									mazewar.remotes_addrbook.addAll(packetFromClient.remotes);
								}
								synchronized (mazewar) {
									mazewar.numPlayers++;
								}

							}

							// synchronized (mazewar.moveQueue) {
							System.out.println("Local queue size before adding = " + mazewar.moveQueue.size());
							mazewar.moveQueue.put(packetFromClient.getlamportClock(), packetFromClient);
							System.out.println("Local queue size after adding = " + mazewar.moveQueue.size());
							// }
							//if (!(packetFromClient.getclientInfo().equals(address))) {
							System.out.println("Broadcasting ACK for connect req from "	+ packetFromClient.getlamportClock());
								broadcastPacket(Ack, mazewar.remotes_addrbook);
							//}

							break;
						}
						// In either of these cases, all we do is broadcast the
						// ACK and queue the move
						case (MazePacket.MOVE_REQUEST):
							System.out.println("Move request from " + packetFromClient.getlamportClock() + " in "
									+ address.name);

						case (MazePacket.DISCONNECT_REQUEST): {
							// Check if any ACKs have been received for this
							// move
							Double moveLClock = new Double(packetFromClient.getlamportClock());
							Integer moveAcks = ackQueue.get(moveLClock);
							if (moveAcks != null) {
								// Add any received ACKs to the packet and
								// remove from queue
								packetFromClient.setnumAcks(moveAcks.intValue());
								ackQueue.remove(moveLClock);
							}
							// Update Lamport Clock
							synchronized (mazewar) {
								if (mazewar.lClock < packetFromClient.getlamportClock()) {
									mazewar.lClock = packetFromClient.getlamportClock();
									mazewar.lClock -= (packetFromClient.getclientID() / 10);
									mazewar.lClock += (address.id / 10);
								}
								mazewar.lClock++;
							}
							MazePacket Ack = new MazePacket(packetFromClient);
							Ack.setmsgType(MazePacket.ACK);

							// synchronized (mazewar.moveQueue) {
							System.out.println("Local queue size before adding = " + mazewar.moveQueue.size());
							mazewar.moveQueue.put(packetFromClient.getlamportClock(), packetFromClient);
							System.out.println("Local queue size after adding = " + mazewar.moveQueue.size());
							// }
							System.out.println("Broadcasting ACK for connect req from "
									+ packetFromClient.getlamportClock());
							broadcastPacket(Ack, mazewar.remotes_addrbook);

							break;
						}

						default:
							// A wrong packet was received
							System.err.println("ERROR: A packet with invalid type was received.");
							break;
						}
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
