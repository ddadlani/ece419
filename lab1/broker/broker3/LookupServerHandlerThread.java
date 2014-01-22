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

			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				
				if(packetFromClient.type == BrokerPacket.LOOKUP_REGISTER)
                                {
                                	//write to file
                                	packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                                	fh.registerBroker(packetFromClient.exchange, packetFromClient.locations[0].broker_host, packetFromClient.locations[0].broker_port);
                                	toClient.writeObject(packetToClient);
					continue;
                                	
                                }
                                else if(packetFromClient.type == BrokerPacket.LOOKUP_REQUEST)
                                {	
                                	//read from file
                                	//if find failed then return ERROR_INVALID_EXCHANGE
                                	packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                                	packetToClient = fh.lookupBroker(packetFromClient.exchange);
                                	toClient.writeObject(packetToClient);
					continue;
                                }
                                else 
                                {
                                	System.err.println("ERROR: Unknown BROKER_* packet!!");
                                	System.exit(-1);
                                }		 
			}

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
				e.printStackTrace();
		}
	}

}
