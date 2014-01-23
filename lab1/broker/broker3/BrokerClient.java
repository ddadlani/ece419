import java.io.*;
import java.net.*;

public class BrokerClient {

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, EOFException {

		// initialize variables

		BrokerLocation lookup = null;

		Socket BrokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		String local = null;
		String hostname = null;
		int port = 0;
		// Connect to naming server

		/* variables for hostname/port */
		hostname = "localhost";
		port = 4444;

		if (args.length == 2) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
		} else {
			System.err
					.println("ERROR: Invalid arguments! Usage: ./client.sh <hostname> <portnumber>");
			System.exit(-1);
		}

		// Set up user input from stdin
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		String userInput;

		System.out.println("Enter command, symbol or x for exit:");
		System.out.print("> ");
		userInput = stdIn.readLine();

		// If local server has not yet been declared
		while (local == null) {
			if (userInput.toLowerCase().equals("x")) {
				/* Quit */
				stdIn.close();
				System.exit(0);
			}
			/* tse */
			else if (userInput.toLowerCase().equals("local tse")) {
				// Look up connection params for tse
				local = new String("tse");
				lookup = lookupExchange(local, hostname, port);

				// Lookup failed
				if (lookup == null) {
					userInput = null;
					System.out.println("Broker Server lookup failed. Please try again later.");
					local = null;
				}
				continue;
			}
			/* nasdaq */
			else if (userInput.toLowerCase().equals("local nasdaq")) {
				local = new String("nasdaq");
				lookup = lookupExchange(local, hostname, port);

				// Lookup failed
				if (lookup == null) {
					userInput = null;
					System.out.println("Broker Server lookup failed. Please try again later.");
					local = null;
				}
				continue;
			}
			// Invalid server
			else {
				userInput = null;
				System.out
						.println("Error: Please declare a valid local exchange server.");
				System.out.print("> ");
			}

			userInput = stdIn.readLine();

		}

		/* connect to broker */
		try {
			if (BrokerSocket == null) {
				BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);
				out = new ObjectOutputStream(BrokerSocket.getOutputStream());
				in = new ObjectInputStream(BrokerSocket.getInputStream());
			}
		} catch (ConnectException ce) {
			System.err
					.println("Could not connect to broker server. Terminating.\n");
			System.exit(1);
		}

		System.out.println(local + " as local.");

		/* accept next user input */
		System.out.print("> ");
		userInput = stdIn.readLine();

		
		// Client input handler loop
		while (userInput != null) {
			if (userInput.toLowerCase().equals("x"))
				break;
			
			// if trying to change local broker to tse
			else if (userInput.toLowerCase().equals("local tse")) {
				local = new String("tse");
				lookup = lookupExchange(local, hostname, port);
				
				// Lookup failed
				if (lookup == null)
					System.out.println("Broker Server lookup failed. Please try again later.");
				else {
					// disconnect from old broker
					disconnectFromServer(out, in);
					
					// Connect to new broker
					BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);
					out = new ObjectOutputStream(BrokerSocket.getOutputStream());
					in = new ObjectInputStream(BrokerSocket.getInputStream());

					System.out.println("tse as local.");
				}
			}

			// if trying to change local broker to nasdaq
			else if (userInput.toLowerCase().equals("local nasdaq")) {
				local = new String("nasdaq");
				lookup = lookupExchange(local, hostname, port);
				
				// Lookup failed
				if (lookup == null)
					System.out.println("Broker Server lookup failed. Please try again later");
				else {
					// disconnect from old broker
					disconnectFromServer(out, in);
					
					// Connect to new broker
					BrokerSocket = new Socket(lookup.broker_host, lookup.broker_port);
					out = new ObjectOutputStream(BrokerSocket.getOutputStream());
					in = new ObjectInputStream(BrokerSocket.getInputStream());

					System.out.println("nasdaq as local.");
				}
			}

			else {
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
					System.err.println("No reply received. Broker server shut down.");
				}
				if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
					System.out.println("Quote from broker: " + packetFromServer.quote);
			}
			
			/* accept next user input */
			System.out.print("> ");
			userInput = stdIn.readLine();
		}

		/* tell server that i'm quitting */
		disconnectFromServer(out, in);
		stdIn.close();
		BrokerSocket.close();
	}

	private static BrokerLocation lookupExchange(String localserver,
			String hostname, int port) {

		Socket LookupSocket = null;
		ObjectOutputStream outLookup = null;
		ObjectInputStream inLookup = null;

		try {
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

		BrokerPacket packetToServer = new BrokerPacket();
		BrokerPacket packetFromServer;

		packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
		packetToServer.exchange = localserver;

		try {
			outLookup.writeObject(packetToServer);
			packetFromServer = (BrokerPacket) inLookup.readObject();

			if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_EXCHANGE) {
				System.err
						.println("The exchange server was not found. Please select another exchange server.");
				disconnectFromServer(outLookup, inLookup);
				return null;
			} else if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY) {
				disconnectFromServer(outLookup, inLookup);
				return packetFromServer.locations[0];
			} else {
				System.err.println("An unknown error occurred.");
				disconnectFromServer(outLookup, inLookup);
				return null;
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

	private static void disconnectFromServer(ObjectOutputStream out,
			ObjectInputStream in) throws IOException {
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
