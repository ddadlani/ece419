import java.net.*;
import java.io.*;

public class OnlineBroker {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
	String broker_name = null;
	String lookup_host = null;
	Integer lookup_port = 0;
	BrokerPacket otherbrokerloc = new BrokerPacket();
        try {
        	
                if(args.length == 4) {
                	lookup_host = args[0];
			lookup_port = Integer.parseInt(args[1]);
			Integer local_port = Integer.parseInt(args[2]);
                        serverSocket = new ServerSocket(local_port);
			broker_name = args[3];
			String otherbroker_name = null;
			if (broker_name.equals("nasdaq"))
				otherbroker_name = "tse";
			else if (broker_name.equals("tse"))
				otherbroker_name = "nasdaq";
			else
			{
				System.err.println("ERROR: No such broker exists!");
				System.exit(-1);
			}
				
			
			otherbrokerloc = registerBroker(broker_name, otherbroker_name, lookup_host, lookup_port, local_port);
                } else {
                        System.err.println("ERROR: Invalid arguments! Usage: ./server.sh <lookup_hostname> <lookup_portnumber> <portnumber> <broker_name>");
                        System.exit(-1);
                }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
        	//System.out.println("Broker server accepting connections on port number " + args[0] + ".");
                new BrokerServerHandlerThread(serverSocket.accept(), broker_name, otherbrokerloc, lookup_host, lookup_port).start();
        }

        serverSocket.close();
    }
    
    public static BrokerPacket registerBroker(String brokername,
			String otherbroker_name, String lookuphost, Integer lookupport,
			Integer port) {

		Socket LookupSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		BrokerPacket packetFromServer = new BrokerPacket();
		BrokerPacket packetFromServer2 = new BrokerPacket();
		try {
			/* register to naming service */
			LookupSocket = new Socket(lookuphost, lookupport);

			out = new ObjectOutputStream(LookupSocket.getOutputStream());
			in = new ObjectInputStream(LookupSocket.getInputStream());

			String localhost_ = InetAddress.getLocalHost().getHostName();
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.LOOKUP_REGISTER;
			packetToServer.exchange = brokername;
			packetToServer.num_locations = 1;
			packetToServer.locations = new BrokerLocation[packetToServer.num_locations];
			packetToServer.locations[0] = new BrokerLocation(localhost_, port);
			out.writeObject(packetToServer);

			/* print server reply */

			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE) {
				System.err.println("Server already open from another place. Terminating.");
				System.exit(-1);
			} //else
				//System.out.println("Server registered!");

			/* lookup other broker location */
			BrokerPacket packetToServer2 = new BrokerPacket();
			packetToServer2.type = BrokerPacket.LOOKUP_REQUEST;
			packetToServer2.exchange = otherbroker_name;
			out.writeObject(packetToServer2);

			/* print server reply */
			packetFromServer2 = (BrokerPacket) in.readObject();
			if (packetFromServer2.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE) {
				//System.out.println("Other server not registered yet.");
				return null;
			}
			/* tell server that i'm quitting */
			BrokerPacket packetToServer3 = new BrokerPacket();
			packetToServer3.type = BrokerPacket.BROKER_BYE;
			packetToServer3.symbol = "Bye!";
			out.writeObject(packetToServer3);
			out.close();
			in.close();
			LookupSocket.close();

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		} catch (ClassNotFoundException cnf) {
			System.err.println("ERROR: Class not found");
		}

		return packetFromServer2;

	}
    
    public static void deregisterBroker(String brokername, String lookuphost, Integer lookupport, Integer port) {

		Socket LookupSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		BrokerPacket packetFromServer = new BrokerPacket();
		try {
			/* remove yourself from naming service */
			LookupSocket = new Socket(lookuphost, lookupport);

			out = new ObjectOutputStream(LookupSocket.getOutputStream());
			in = new ObjectInputStream(LookupSocket.getInputStream());

			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.LOOKUP_REMOVE;
			packetToServer.exchange = brokername;
			packetToServer.num_locations = 1;
			out.writeObject(packetToServer);
			
			if (packetFromServer.error_code != BrokerPacket.BROKER_NULL) {
				System.err.println("ERROR: Something went wrong during deregistration.");
				System.exit(-1);
			} //else
				//System.out.println("Server deregistered!");
			
		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}
    }

    

}
