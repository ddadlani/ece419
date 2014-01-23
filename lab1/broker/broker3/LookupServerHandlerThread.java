import java.net.*;
import java.io.*;

public class LookupServerHandlerThread extends Thread {
	private Socket socket = null;

	public LookupServerHandlerThread(Socket socket) {
		super("LookupServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		
			try {
				/* stream to read from client */
				ObjectInputStream fromClient = new ObjectInputStream(
						socket.getInputStream());
				BrokerPacket packetFromClient;

				/* stream to write back to client */
				ObjectOutputStream toClient = new ObjectOutputStream(
						socket.getOutputStream());
				FileHandler fh = new FileHandler("lookupTable");

				while ((packetFromClient = (BrokerPacket) fromClient
						.readObject()) != null) {
					/* create a packet to send reply back to client */
					BrokerPacket packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.LOOKUP_REPLY;

					if (packetFromClient.type == BrokerPacket.LOOKUP_REGISTER) {
						// Check if there is already an entry
						packetToClient = fh
								.lookupBroker(packetFromClient.exchange);
						if (packetToClient.error_code != BrokerPacket.ERROR_INVALID_EXCHANGE) {
							packetToClient.error_code = BrokerPacket.ERROR_INVALID_EXCHANGE;
							toClient.writeObject(packetToClient);
							continue;
						}
						// write to file
						
						fh.registerBroker(packetFromClient.exchange, packetFromClient.locations[0].broker_host,
								packetFromClient.locations[0].broker_port);
						packetToClient.error_code = BrokerPacket.BROKER_NULL;
						toClient.writeObject(packetToClient);
						continue;

					} else if (packetFromClient.type == BrokerPacket.LOOKUP_REQUEST) {
						// read from file
						// if find failed then return ERROR_INVALID_EXCHANGE
						packetToClient.type = BrokerPacket.LOOKUP_REPLY;
						packetToClient = fh
								.lookupBroker(packetFromClient.exchange);
						toClient.writeObject(packetToClient);
						continue;
					} else if (packetFromClient.type == BrokerPacket.BROKER_BYE) {
						// client exiting
						break;
					} else {
						System.err.println("ERROR: Unknown BROKER_* packet!!");
						System.exit(-1);
					}
				}

				/* cleanup when client exits */
				close(toClient, fromClient);

			} catch (EOFException e) {
				System.err.println("Client closed connection.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		
	}

	protected void close(ObjectOutputStream toClient,
			ObjectInputStream fromClient) throws IOException {
		fromClient.close();
		toClient.close();
		socket.close();
	}

}
