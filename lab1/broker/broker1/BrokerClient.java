import java.io.*;
import java.net.*;

public class BrokerClient {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException, EOFException {

		Socket BrokerSocket = null;
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
			BrokerSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(BrokerSocket.getOutputStream());
			in = new ObjectInputStream(BrokerSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("Enter queries or x for exit:\n> ");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("x") == -1) {
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			packetToServer.symbol = userInput;
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			try{
			packetFromServer = (BrokerPacket) in.readObject();
			} catch (EOFException eof)
			{
				System.err.println("No reply received EOF");
			}
			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
				System.out.println("Quote from broker: " + packetFromServer.quote);

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
}

