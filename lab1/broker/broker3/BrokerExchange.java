import java.io.*;
import java.net.*;

public class BrokerExchange {
        public static void main(String[] args) throws IOException,
                        ClassNotFoundException, EOFException {

                Socket BrokerExchangeSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                
                /* variables for hostname/port */
                String hostname = "localhost";
                int port = 4444;
                String local = null;

                if(args.length == 3 ) {
	                hostname = args[0];
                        port = Integer.parseInt(args[1]);
                        local = args[2];
                } else {
                        System.err.println("ERROR: Invalid arguments!");
                        System.exit(1);
                }

		BrokerLocation lookup = lookupExchange(local, hostname, port);
		if (lookup == null)
		{
			System.out.println("Exchange server is not connected");
			System.exit(1);
		}

		BufferedReader stdIn = null;
		String userInput;
	       	try {
		BrokerExchangeSocket = new Socket(lookup.broker_host, lookup.broker_port);
		
		out = new ObjectOutputStream(BrokerExchangeSocket.getOutputStream());
		in = new ObjectInputStream(BrokerExchangeSocket.getInputStream());
		
                stdIn = new BufferedReader(new InputStreamReader(System.in));

		}
		catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!");
			System.exit(1);
		}
		catch (IOException e) {
		System.err.println("ERROR: Couldn't connect to server. Terminating.");
		System.exit(1);
		}
	
                System.out.print("Enter command or quit for exit:\n> ");
                while ((userInput = stdIn.readLine().toLowerCase()) != null && userInput.indexOf("x") == -1) {
                        
                        /* make a new request packet */
                        BrokerPacket packetToServer = new BrokerPacket();
                        
                        /* parse input for exchange command */
                        String[] row_array = new String[3];
                        String add_cmd = "add";
                        String update_cmd = "update";
                        String remove_cmd = "remove";
                        boolean cont = true;
                        row_array = userInput.split(" ");
                        
                        //ADD
                        if ((row_array.length == 2) && add_cmd.equals(row_array[0]) && row_array[1].matches("[a-zA-Z]+"))
                        {
                        	//System.out.println("ADDED!");
                        	packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                        	packetToServer.symbol = row_array[1];
                        	cont = false;
                        }
                        
                        //UPDATE
                        else if ((row_array.length == 3) && update_cmd.equals(row_array[0]) && row_array[1].matches("[a-zA-Z]+"))
                        {
                        	try  {  
                        		Long quote = Long.parseLong(row_array[2]); 
                        		//System.out.println("UPDATED!");
                        		packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                        		packetToServer.symbol = row_array[1];
                        		packetToServer.quote = quote; 
                        		cont = false;
                        	}  
                        	catch(NumberFormatException nfe)  {  
                        		System.out.println("Not a long!");
                        		cont = true;
                        	} 
                        	
                        }         
                        
                        //REMOVE                        
                        else if ((row_array.length == 2) && remove_cmd.equals(row_array[0]) && row_array[1].matches("[a-zA-Z]+"))
                        {
                        	//System.out.println("REMOVED!");
                        	packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                        	packetToServer.symbol = row_array[1];
                        	cont = false;
                        }         
                        
                        if (cont)
                        {
                        	System.out.println("Invalid command. Try again.");
                        	System.out.print("> ");
                        	continue;
                        }
                        
			out.writeObject(packetToServer);
                        /* print server reply */
                        BrokerPacket packetFromServer = new BrokerPacket();
                        try{
                        	packetFromServer = (BrokerPacket) in.readObject();
                        } catch (EOFException eof)  {
                                System.err.println("No reply received EOF");
                        }
                        if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL)
                        	System.out.println(packetToServer.symbol + " invalid.");
				
                        else if (packetFromServer.error_code == BrokerPacket.ERROR_OUT_OF_RANGE)
                        	System.out.println(packetToServer.symbol + " out of range.");
				
                        else if (packetFromServer.error_code == BrokerPacket.ERROR_SYMBOL_EXISTS)
                        	System.out.println(packetToServer.symbol + " exists.");
                       
                        else if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY)  {
                        	
                        	if (add_cmd.equals(row_array[0]))
                        		System.out.println(packetToServer.symbol + " added.");
                        	else if (update_cmd.equals(row_array[0]))
                        		System.out.println(packetToServer.symbol + " updated to " + packetToServer.quote + ".");
                        	else
                        		System.out.println(packetToServer.symbol + " removed.");
                        }

                        /* re-print console prompt */
                        System.out.print("> ");
                }

                /* tell server that i'm quitting */
                disconnectFromServer(out, in);
                stdIn.close();
                BrokerExchangeSocket.close();
        }
        	
        private static BrokerLocation lookupExchange(String localserver, String hostname, int port) {

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
				System.err.println("The broker server " + localserver + " was not found. Terminating.");
				disconnectFromServer(outLookup, inLookup);
				System.exit(1);
			} else if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY) {
				disconnectFromServer(outLookup, inLookup);
				return packetFromServer.locations[0];
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
}
