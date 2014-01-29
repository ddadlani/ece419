import java.net.*;
import java.io.*;

public class BrokerLookupServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
                if(args.length == 1) {
                        serverSocket = new ServerSocket(Integer.parseInt(args[0]));
                } else {
                        System.err.println("ERROR: Invalid arguments! Usage: ./lookup.sh <portnumber>");
                        System.exit(-1);
                }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
        	System.out.println("Lookup server accepting connections on port number " + args[0] + ".");
                new LookupServerHandlerThread(serverSocket.accept()).start();
        }

        serverSocket.close();
        
    }
}
