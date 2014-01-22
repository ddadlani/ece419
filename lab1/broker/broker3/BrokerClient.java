import java.io.*;
import java.net.*;

public class BrokerClient {

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, EOFException {

		// initialize variables
		Socket LookupSocket = null;
		ObjectOutputStream outLookup = null;
		ObjectInputStream inLookup = null;
		BrokerLocation lookup = null;
		
		Socket BrokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		String local = null;

		// Connect to naming server
		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;

			if (args.length == 2) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err
						.println("ERROR: Invalid arguments! Usage: ./client.sh <hostname> <portnumber>");
				System.exit(-1);
			}
			LookupSocket = new Socket(hostname, port);

			outLookup = new ObjectOutputStream(LookupSocket.getOutputStream());
			inLookup = new ObjectInputStream(LookupSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		// Set up user input from stdin
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		String userInput;

		System.out.println("Enter command, symbol or x for exit:");
		System.out.print("> ");
		
		// Client input handler loop
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
			
			// If local server has not yet been declared
			while (local == null) {
				/* tse */
				if (userInput.toLowerCase().equals("local tse")) {
					// Look up connection params for tse
					local = new String("tse");
					lookup = lookupExchange(local,outLookup, inLookup);
					
					// Lookup failed
					if (lookup == null) {
						System.out.println("Exchange server lookup failed. Please try again later.");
						local = null;
						continue;
					}
				}
				/*nasdaq */
				else if (userInput.toLowerCase().equals("local nasdaq")) {
					local = new String("nasdaq");
					lookup = lookupExchange(local, outLookup, inLookup);
					if (lookup == null) {
						System.out.println("Exchange server lookup failed. Please try again later.");
						local = null;
						continue;
					}
				}
				// Invalid server
				else {
					System.out.println("Error: Please declare a valid local exchange server.");
					System.out.print("> ");
					continue;
				}
			}
			
			/* connect to broker */
			if (BrokerSocket == null) {
				BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);

				out = new ObjectOutputStream(BrokerSocket.getOutputStream());
				in = new ObjectInputStream(BrokerSocket.getInputStream());
			}
			
			if ((BrokerSocket == null) || (out == null) || (in == null)) {
				System.err.println("Could not connect to broker server. Terminating.\n");
				System.exit(1);
			}
			else {
				System.out.println(local + " as local.");
			}
			
			/* accept next user input */
			System.out.print("> ");
			userInput = stdIn.readLine();
						
			// if trying to change local broker to tse
			while (userInput.toLowerCase().equals("local tse")) {
				local = new String("tse");
				lookup = lookupExchange(local, outLookup, inLookup);
				if (lookup == null)
					System.out.println("Exchange server lookup failed. Please try again later.");
				else {
					// connect to broker
					BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);
					out = new ObjectOutputStream(BrokerSocket.getOutputStream());
					in = new ObjectInputStream(BrokerSocket.getInputStream());
					
					System.out.println("tse as local.");
				}
				/* accept next user input */
				System.out.print("> ");
				userInput = stdIn.readLine();
			}
			
			// if trying to change local broker to nasdaq
			while (userInput.toLowerCase().equals("local nasdaq")) {
				local = new String("nasdaq");
				lookup = lookupExchange(local, outLookup, inLookup);
				if (lookup == null) 
					System.out.println("Exchange server lookup failed. Please try again later");
				else {
					BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);
					out = new ObjectOutputStream(BrokerSocket.getOutputStream());
					in = new ObjectInputStream(BrokerSocket.getInputStream());

					System.out.println("nasdaq as local.");
				}
				/* accept next user input */
				System.out.print("> ");
				userInput = stdIn.readLine();
			}
			
			
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			packetToServer.symbol = userInput.toLowerCase();
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer = new BrokerPacket();
			try {
				packetFromServer = (BrokerPacket) in.readObject();
			} catch (EOFException eof) {
				System.err.println("No reply received EOF");
			}
			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
				System.out.println("Quote from broker: "
						+ packetFromServer.quote);

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
		BrokerSocket.close();
	}

	private static BrokerLocation lookupExchange(String localserver, ObjectOutputStream out, ObjectInputStream in) {
		BrokerPacket packetToServer = new BrokerPacket();
		BrokerPacket packetFromServer;
		
		packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
		packetToServer.symbol = localserver;
		try {
			out.writeObject(packetToServer);
			packetFromServer = (BrokerPacket) in.readObject();
				
			if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE) {
				System.err.println("The exchange server was not found. Please select another exchange server.");
				return null;
			}
			else if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY) {
				return packetFromServer.locations[0];
			}
			else {
				System.err.println("An unknown error occurred.");
				return null;
			}
		
		} catch (IOException e) {
			System.err.println("No reply received EOF");
		} catch (ClassNotFoundException cnf) {
			cnf.printStackTrace();
		} catch (NullPointerException np) {
			System.err.println("No reply received EOF");
		}
		return null;
	}
}
