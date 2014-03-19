import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;

public class ClientListenHandlerThread {
	Socket socket;
	SortedMap<Double, MazePacket> localQueue;
	Integer numRemotes;
	Integer pid;
	String name;
	Address address;

	public ClientListenHandlerThread(Socket socket_, Mazewar mazewar_) {
		synchronized (mazewar_) {
			this.socket = socket_;
			this.localQueue = mazewar_.moveQueue;

			// May be needed for heart beats? Not sure
			this.numRemotes = mazewar_.numRemotes;
			this.pid = mazewar_.pid;
			this.name = mazewar_.getName();
			this.address = mazewar_.clientAddr;
		}
	}

	public void run() {
		try {

			MazePacket packetFromClient;
			ObjectInputStream in = new ObjectInputStream(
					socket.getInputStream());

			if ((packetFromClient = (MazePacket) in.readObject()) != null) {

				switch (packetFromClient.getmsgType()) {

				// An ACK was received for some move
				case (MazePacket.ACK): {
					// Check that queue is properly initialized
					if ((localQueue != null) && (localQueue.size() != 0)) {
						synchronized (localQueue) {
							// Look up relevant move in localQueue using Lamport
							// clock value

							MazePacket gotNewAck = localQueue
									.get(packetFromClient.getlamportClock());

							if (gotNewAck == null) {
								// Packet with this Lamport clock not found
								// DO WE HOLD THIS PACKET? IN CASE MOVE ARRIVES
								// LATER?? NO Fifo assumption doesn't hold, we
								// don't need to worry.
								System.err
										.println("ERROR: MazePacket with Lamport Clock "
												+ packetFromClient
														.getlamportClock()
												+ " was not found.");
								System.exit(1);
							}
							gotNewAck.incrementAcks();
							// If received ack for connect, and I am the one
							// connecting, then add player name in ack to
							// remotes so that can add GUI Client later
							if (packetFromClient.getevent() == MazePacket.CONNECT) {
								// ADD name of clients in remote
								Iterator<Address> itr = gotNewAck.remotes
										.iterator();
								while (itr.hasNext()) {
									Address addr = itr.next();
									if (addr.address_equals(packetFromClient
											.getclientInfo())) {
										addr.name = packetFromClient.getName();

										// SET POSITION AND ORIENTATION
										break;
									}
								}
							}
							//gotNewAck = null;
						}
					} else {
						// Move queue is null or empty
						System.err
								.println("ERROR: Move queue is null but we got an ACK? For what??");
						System.exit(1);
					}
					break;
				}

				// In either of these cases, all we do is broadcast the ack and
				// queue the move
				// for connect ack, send back position, orientation and name of
				// player
				case (MazePacket.CONNECTION_REQUEST):
				case (MazePacket.MOVE_REQUEST):
				case (MazePacket.DISCONNECT_REQUEST): {
					MazePacket Ack = new MazePacket();
					Ack.setmsgType(MazePacket.ACK);
					Ack.setevent(packetFromClient.getevent());
					Ack.setlamportClock(packetFromClient.getlamportClock());
					Ack.setclientInfo(this.address);
					if (packetFromClient.getevent() == MazePacket.CONNECT) {
						Ack.setclientID(this.pid);
						Ack.setName(this.name);
						// Handle Position and orientation
					} else {
						Ack.setclientID(packetFromClient.getclientID());
						Ack.setName(packetFromClient.getName());
					}
					broadcastPacket(Ack, packetFromClient.remotes);
					localQueue.put(packetFromClient.getlamportClock(),
							packetFromClient);
					break;
				}

				// TODO: case (MazePacket.HEARTBEAT) -- count heart beats
				// received for each player
				default:
					// A wrong packet was received
					System.err
							.println("ERROR: A packet with invalid type was received.");
					break;
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

	public void broadcastPacket(MazePacket outPacket,
			ArrayList<Address> addressBook) {
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
