import java.net.*;
import java.io.*;

public class OnlineBroker {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
	String broker_name = null;
        try {
        	
                if(args.length == 4) {
                	String lookup_host = args[0];
			Integer lookup_port = Integer.parseInt(args[1]);
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
				
			BrokerPacket otherbrokerloc = new BrokerPacket();
			otherbrokerloc = registerBroker(broker_name, otherbroker_name, lookup_host, lookup_port, local_port);
			if (otherbrokerloc != null)
				connectBrokers(otherbrokerloc.locations[0].broker_host, otherbrokerloc.locations[0].broker_port);
                } else {
                        System.err.println("ERROR: Invalid arguments! Usage: ./server.sh <lookup_hostname> <lookup_portnumber> <portnumber> <broker_name>");
                        System.exit(-1);
                }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
        	System.out.println("Broker server accepting connections on port number " + args[0] + ".");
                new BrokerServerHandlerThread(serverSocket.accept(), broker_name).start();
        }

        serverSocket.close();
    }
    
    public static BrokerPacket registerBroker(String brokername, String otherbroker_name, String lookuphost, Integer lookupport, Integer port)
    {

		Socket LookupSocket = null;
	        ObjectOutputStream out = null;
	        ObjectInputStream in = null;
	        BrokerPacket packetFromServer = new BrokerPacket();

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
		
		        if (packetFromServer.type != BrokerPacket.LOOKUP_REPLY)
		        {
		        	System.exit(-1);
		        	System.err.println("Server already open from another place");
			}
			else
		        	System.out.println("Server registered!");
		        
		        /*lookup other broker location*/
		        packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
		        packetToServer.exchange = otherbroker_name;
		        out.writeObject(packetToServer);
		        
		        /* print server reply */
		        packetFromServer = (BrokerPacket) in.readObject();
		        if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE)
		       	{
		        	System.out.println("Other server not registered yet.");
		        	return null;
			}
		        /* tell server that i'm quitting */
		        packetToServer.type = BrokerPacket.BROKER_BYE;
		        packetToServer.symbol = "Bye!";
		        out.writeObject(packetToServer);
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
		
		return packetFromServer; 
		        
    }
    
    public static void connectBrokers(String otherbrokerhost, Integer otherbrokerport) 
    {
    
    	    	/* connect to other broker */
		Socket BrokersSocket = null;
	        ObjectOutputStream out = null;
	        ObjectInputStream in = null;

	        try {
	                BrokersSocket = new Socket(otherbrokerhost, otherbrokerport);
	
	                out = new ObjectOutputStream(BrokersSocket.getOutputStream());
	                in = new ObjectInputStream(BrokersSocket.getInputStream());
		       

	        } catch (UnknownHostException e) {
	                System.err.println("ERROR: Don't know where to connect!!");
	                System.exit(1);
	        } catch (IOException e) {
	                System.err.println("ERROR: Couldn't get I/O for the connection.");
	                System.exit(1);
	        } 
		        
    }
}
