import java.io.*;
import java.net.*;

public class BrokerExchange {
        public static void main(String[] args) throws IOException,
                        ClassNotFoundException, EOFException {

                Socket BrokerExchangeSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;

                try {
                        /* variables for hostname/port */
                        String hostname = "localhost";
                        int port = 4444;
                        
                        if(args.length == 2 ) {
                                hostname = args[0];
                                port = Integer.parseInt(args[1]);
                        } else {
                                System.err.println("ERROR: Invalid arguments!");
                                System.exit(-1);
                        }
                        BrokerExchangeSocket = new Socket(hostname, port);

                        out = new ObjectOutputStream(BrokerExchangeSocket.getOutputStream());
                        in = new ObjectInputStream(BrokerExchangeSocket.getInputStream());

                } catch (UnknownHostException e) {
                        System.err.println("ERROR: Don't know where to connect!!");
                        System.exit(1);
                } catch (IOException e) {
                        System.err.println("ERROR: Couldn't get I/O for the connection.");
                        System.exit(1);
                }

                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                String userInput;

                System.out.print("Enter command or quit for exit:\n> ");
                while ((userInput = stdIn.readLine().toLowerCase()) != null
                                && userInput.indexOf("x") == -1) {
                        
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
                        		//System.out.println("Not a long!");
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
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.BROKER_BYE;
                packetToServer.symbol = "Bye!";
                out.writeObject(packetToServer);

                out.close();
                in.close();
                stdIn.close();
                BrokerExchangeSocket.close();
        }
}
