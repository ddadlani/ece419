import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.SortedMap;

public class ClientListenHandlerThread {
	Socket socket;
	SortedMap<Double, MazePacket> localQueue;
	Integer numRemotes;

	public ClientListenHandlerThread(Socket socket_, Mazewar mazewar_) {
		synchronized (mazewar_) {
			this.socket = socket_;
			this.localQueue = mazewar_.moveQueue;
			
			// May be needed for heart beats? Not sure
			this.numRemotes = mazewar_.numRemotes;
		}
	}

	public void run() {
		try {
		MazePacket packetFromClient;
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		
			if ((packetFromClient = (MazePacket) in.readObject()) != null) {
				
				switch (packetFromClient.getmsgType()) {
				
				// An ACK was received for some move
				case (MazePacket.ACK): {
					// Check that queue is properly initialized
					if ((localQueue != null) && (localQueue.size() != 0)) {						
						synchronized (localQueue){
							// Look up relevant move in localQueue using Lamport clock value
							MazePacket gotNewAck = localQueue.get(packetFromClient.getlamportClock()); 
							
							if (gotNewAck == null) {
								// Packet with this Lamport clock not found
								// DO WE HOLD THIS PACKET? IN CASE MOVE ARRIVES LATER??
								System.err.println("ERROR: MazePacket with Lamport Clock " + packetFromClient.getlamportClock() + " was not found.");
								System.exit(1);
							}
							gotNewAck.incrementAcks();
							gotNewAck = null;
						}
					} else {
						// Move queue is null or empty
						System.err.println("ERROR: Move queue is null but we got an ACK? For what??");
						System.exit(1);
					}
					break;
				}
				
				// In either of these cases, all we do is queue the move
				case (MazePacket.CONNECTION_REQUEST):
				case (MazePacket.MOVE_REQUEST):
				case (MazePacket.DISCONNECT_REQUEST): {
					localQueue.put(packetFromClient.getlamportClock(), packetFromClient);
					break;
				}
				
				// TODO: case (MazePacket.HEARTBEAT) -- count heart beats received for each player
				default:
					// A wrong packet was received
					System.err.println("ERROR: A packet with invalid type was received.");
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
}
