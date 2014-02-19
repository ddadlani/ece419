import java.net.*;
import java.util.Queue;
import java.io.*;

public class MazeServerListenerThread extends Thread {
	private Socket socket = null;
	private int clientID;
	private Queue<MazePacket> q;

	public MazeServerListenerThread(Socket socket, Integer clientID,
			Queue<MazePacket> q) {
		super("MazeServerHandlerThread");
		this.socket = socket;
		this.clientID = clientID;
		this.q = q;
		// System.out.println("Created new Thread to handle client");
	}

	public void run() {

		boolean gotByePacket = false;

		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(
					socket.getInputStream());
			MazePacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(
					socket.getOutputStream());

			while ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {

				/* process symbol */
				if (packetFromClient.getmsgType() == MazePacket.CONNECTION_REQUEST) {
					/* create a packet to send clientID back to client */
					MazePacket packetToClient = new MazePacket(); // clientID,
																	// event,
																	// seqNum,
																	// msgType
					//packetToClient.setclientInfo(clientID);
					packetToClient.setmsgType(MazePacket.CONNECTION_REPLY);
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				if (packetFromClient.getmsgType() == MazePacket.MAZE_REQUEST) {
					/* create a packet to send latest move back to client */
					// ClientEvent
					MazePacket packetToClient;
					
					// !! Change this to a lock, maybe? Decide later
					synchronized (this) {
						q.add(packetFromClient);
						packetToClient = q.remove();
					}
			
					// !! Send to all clients, not just one
					packetToClient.setmsgType(MazePacket.MAZE_REPLY);
					toClient.writeObject(packetToClient);
				}

				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown MAZE_* packet!!");
				System.exit(-1);
			}

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (EOFException e) {

		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}

}
