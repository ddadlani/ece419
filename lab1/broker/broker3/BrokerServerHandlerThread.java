import java.net.*;
import java.io.*;

public class BrokerServerHandlerThread extends Thread {
	private Socket socket = null;
	private String exchange = null;
	private BrokerPacket otherbrokerloc;
	private String lookuphost;
	private Integer lookupport;
	public BrokerServerHandlerThread(Socket socket, String exchange, BrokerPacket otherbrokerloc, String lookuphost, Integer lookupport) {
		super("BrokerServerHandlerThread");
		this.socket = socket;
		this.exchange = exchange;
		this.otherbrokerloc = otherbrokerloc;
		this.lookuphost = lookuphost;
		this.lookupport = lookupport;
		
		//System.out.println("Created new Thread to handle client");
	}

	public void run() {

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(
					socket.getInputStream());
			BrokerPacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(
					socket.getOutputStream());
			FileHandler fh = new FileHandler(exchange);

			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				if (packetFromClient.symbol == null) {
					packetToClient.type = BrokerPacket.BROKER_NULL;
					packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
					toClient.writeObject(packetToClient);
					continue;
				}
				
				if (packetFromClient.quote != null) {
					if ((packetFromClient.quote > 300) || (packetFromClient.quote < 1)) {
						packetToClient.type = BrokerPacket.BROKER_NULL;
						packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
						toClient.writeObject(packetToClient);
						continue;
					}
				}

				/* process symbol */
				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					packetToClient.type = BrokerPacket.BROKER_QUOTE;
					String space = " ";
					packetToClient.quote = fh.findQuote(packetFromClient.symbol, space);
					
					if (packetToClient.quote == 0L)
					{
						if (otherbrokerloc == null)
						{	//Lookup other broker again
							String otherbrokername;
							if (exchange.equals("nasdaq"))
								otherbrokername = "tse";
							else
								otherbrokername = "nasdaq";
							otherbrokerloc = lookupExchange(otherbrokername, lookuphost, lookupport);
							if (otherbrokerloc == null)
								packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
						}
						if (otherbrokerloc != null)
						{
							//symbol not found
							//forwarding symbol to other broker
							packetToClient.quote = forwardToBroker(otherbrokerloc.locations[0].broker_host, otherbrokerloc.locations[0].broker_port, packetFromClient.symbol);
							
							if (packetToClient.quote == 0L)
								packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
							else
								packetToClient.type = BrokerPacket.BROKER_QUOTE;
						}
					}
					else
						packetToClient.type = BrokerPacket.BROKER_QUOTE;
					/* send reply back to client */
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				else if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					packetToClient.quote = fh.findQuote(packetFromClient.symbol, " ");
					if (packetToClient.quote != 0L) {
						packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
					} else {
						fh.addSymbol(packetFromClient.symbol, -1L);
					}
					toClient.writeObject(packetToClient);
					continue;
				}

				else if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					packetToClient.quote = fh.findQuote(packetFromClient.symbol, " ");
					if (packetToClient.quote == 0L) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
					} else {
						fh.removeSymbol(packetFromClient.symbol);

					}
					toClient.writeObject(packetToClient);
					continue;
				}

				else if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					packetToClient.quote = fh.findQuote(packetFromClient.symbol, " ");
					if (packetToClient.quote == 0L) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
					} else {
						fh.removeSymbol(packetFromClient.symbol);
						fh.addSymbol(packetFromClient.symbol, packetFromClient.quote);
					}
					toClient.writeObject(packetToClient);
					continue;

				}
				
				else if (packetFromClient.type == BrokerPacket.BROKER_FORWARD) {
				
					packetToClient.type = BrokerPacket.BROKER_QUOTE;
					String space = " ";
					//System.out.println("Entered FORWARD for server: " + this.exchange + "with symbol " + packetFromClient.symbol);
					packetToClient.quote = fh.findQuote(packetFromClient.symbol, space);
					//System.out.println("At the non local server: " + this.exchange + "found quote: " + packetToClient.quote);
					if (otherbrokerloc == null)
					{	//Lookup other broker again
						//System.out.println("other broker location not found in lookup table.");
						String otherbrokername;
						if (exchange.equals("nasdaq"))
							otherbrokername = "tse";
						else
							otherbrokername = "nasdaq";
						otherbrokerloc = lookupExchange(otherbrokername, lookuphost, lookupport);
						if (otherbrokerloc == null)
						{	
							//connect back to other server
							forwardbackToBroker(otherbrokerloc.locations[0].broker_host, otherbrokerloc.locations[0].broker_port, packetToClient.quote);
						}
					}
					if (otherbrokerloc != null)
					{
						//symbol not found
						//forwarding symbol to other broker
											
						if (packetToClient.quote == 0L)
							packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
						else
							packetToClient.type = BrokerPacket.BROKER_QUOTE;
					}
					
					else
						packetToClient.type = BrokerPacket.BROKER_QUOTE;
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					break;
				
				}

				/* Sending an BROKER_NULL || BROKER_BYE means quit */
				else if (packetFromClient.type == BrokerPacket.BROKER_NULL
						|| packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					packetToClient.symbol = "Bye!";
					toClient.writeObject(packetToClient);
					break;
				}

				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown BROKER_* packet!!");
				System.exit(-1);
			}

			/* cleanup when client exits */
			if (packetFromClient.type != BrokerPacket.BROKER_FORWARD)
			{
				fromClient.close();
				toClient.close();
				socket.close();
			}

		} catch (EOFException e) {
			socket = null;
		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}
	
	public static Long forwardToBroker(String otherbrokerhost, Integer otherbrokerport, String symbol) 
	{
	    
    	    	/* connect to other broker */
		Socket BrokersSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		BrokerPacket packetToServer = new BrokerPacket();
		BrokerPacket packetFromServer = new BrokerPacket();
		try {
		        BrokersSocket = new Socket(otherbrokerhost, otherbrokerport);

		        out = new ObjectOutputStream(BrokersSocket.getOutputStream());
		        in = new ObjectInputStream(BrokersSocket.getInputStream());
		        packetToServer.type = BrokerPacket.BROKER_FORWARD;
		        packetToServer.symbol = symbol;
		        
		        
		        out.writeObject(packetToServer);

		        /* print server reply */
		        
		        packetFromServer = (BrokerPacket) in.readObject();
		
		        if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
		        {
		        	//System.out.println("Got a forwarded quote: " + packetFromServer.quote);
		        	return packetFromServer.quote;
		        	
		        }
		        else
		        	return 0L;

		} catch (UnknownHostException e) {
		        System.err.println("ERROR: Unknown Host Exception: " + e.getMessage());
		        System.exit(1);
		} catch (IOException e) {
		        //System.err.println("ERROR: General I/O exception: " + e.getMessage());
  			//e.printStackTrace();
		        //System.exit(1);

			out = null;
			in = null;
			BrokersSocket = null;
			
		} catch (ClassNotFoundException cnf) {
			System.err.println("ERROR: Class not found: " + cnf.getMessage());
		}    
		return 0L;
				
	 }
	 
	 public static void forwardbackToBroker(String otherbrokerhost, Integer otherbrokerport, Long quote) 
	{
	    
    	    	/* connect to other broker */
		Socket BrokersSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		BrokerPacket packetToServer = new BrokerPacket();

		try {
		        BrokersSocket = new Socket(otherbrokerhost, otherbrokerport);

		        out = new ObjectOutputStream(BrokersSocket.getOutputStream());
		        in = new ObjectInputStream(BrokersSocket.getInputStream());
		        if (quote != 0L)
		        	packetToServer.type = BrokerPacket.BROKER_QUOTE;
		        else
		        	packetToServer.type = BrokerPacket.ERROR_INVALID_SYMBOL;
		        packetToServer.quote = quote;
		        out.writeObject(packetToServer);

		} catch (UnknownHostException e) {
		        System.err.println("ERROR: Don't know where to connect!!");
		        System.exit(1);
		} catch (IOException e) {
		        /*System.err.println("ERROR: Couldn't get I/O for the connection.");
			  System.exit(1);*/
			out = null;
			in = null;
			BrokersSocket = null;
		} 
				
	 }
	 
	 private static void disconnectFromServer(ObjectOutputStream out, ObjectInputStream in) throws IOException {
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		packetToServer.symbol = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();

		out = null;
		in = null;
	}
	
	 private static BrokerPacket lookupExchange(String localserver, String hostname, int port) {

		Socket LookupSocket = null;
		ObjectOutputStream outLookup = null;
		ObjectInputStream inLookup = null;

		try {
			LookupSocket = new Socket(hostname, port);
			outLookup = new ObjectOutputStream(LookupSocket.getOutputStream());
			inLookup = new ObjectInputStream(LookupSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BrokerPacket packetToServer = new BrokerPacket();
		BrokerPacket packetFromServer;

		packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
		packetToServer.exchange = localserver;

		try {
			outLookup.writeObject(packetToServer);
			packetFromServer = (BrokerPacket) inLookup.readObject();

			if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE) {
				//System.err.println("The broker server " + localserver + " was not found. Terminating.");
				disconnectFromServer(outLookup, inLookup);
			} else if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY) {
				disconnectFromServer(outLookup, inLookup);
				return packetFromServer;
			} else {
				System.err.println("An unknown error occurred.");
				disconnectFromServer(outLookup, inLookup);
				System.exit(1);
			}

		} catch (IOException e) {
			System.err.println("No reply received. Lookup Server shut down.");
		} catch (ClassNotFoundException cnf) {
			cnf.printStackTrace();
		} catch (NullPointerException np) {
			System.err.println("No reply received. Lookup Server shut down.");
		}
		return null;
	}

}
