import java.io.*;
import java.net.*;

public class BrokerClient {

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, EOFException {

		Socket LookupSocket = null;
		ObjectOutputStream outLookup = null;
		ObjectInputStream inLookup = null;
		BrokerLocation lookup = null;
		
		Socket BrokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		String local = null;

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

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		String userInput;

		System.out.println("Enter command, symbol or x for exit:");
		System.out.print("> ");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("x") == -1) {
			while (local == null) {
				if (!(userInput.toLowerCase().equals("local tse")) && !(userInput.toLowerCase().equals("local nasdaq"))) {
					System.out.println("Error: Please declare a valid local exchange server.");
					System.out.println("Enter command, symbol or x for exit:");
					System.out.print("> ");
					continue;
				}
				if (userInput.toLowerCase().equals("local tse")) {
					local = new String("tse");
					lookup = lookupExchange(local,outLookup, inLookup);
					if (lookup == null) {
						System.out.println("Exchange server lookup failed. Please try again later.");
						local = null;
						continue;
					}
				} else if (userInput.toLowerCase().equals("local nasdaq")) {
					local = new String("nasdaq");
					lookup = lookupExchange(local, outLookup, inLookup);
					if (lookup == null) {
						System.out.println("Exchange server lookup failed. Please try again later.");
						local = null;
						continue;
					}
				}
			}
			
			/* connect to broker */
			BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);

			out = new ObjectOutputStream(BrokerSocket.getOutputStream());
			in = new ObjectInputStream(BrokerSocket.getInputStream());
			
			/* accept next user input */
			System.out.println("Enter command, symbol or x for exit:");
			System.out.print("> ");
			userInput = stdIn.readLine();
			if(userInput.length() > 5) {
				if (userInput.toLowerCase().equals("local tse")) {
					local = new String("tse");
					lookup = lookupExchange(local, outLookup, inLookup);
					if (lookup == null)
						System.out.println("Exchange server lookup failed. Please try again later.");
				} else if (userInput.toLowerCase().equals("local nasdaq")) {
					local = new String("nasdaq");
					lookup = lookupExchange(local, outLookup, inLookup);
					if (lookup == null) 
						System.out.println("Exchange server lookup failed. Please try again later");
				}
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

			/*
			 * if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL)
			 * System.out.println("Symbol not found");
			 */
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
