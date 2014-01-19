import java.net.*;
import java.io.*;

public class BrokerServerHandlerThread extends Thread {
        private Socket socket = null;

        public BrokerServerHandlerThread(Socket socket) {
                super("BrokerServerHandlerThread");
                this.socket = socket;
                System.out.println("Created new Thread to handle client");
        }

        public void run() {

                boolean gotByePacket = false;
                
                try {
                        /* stream to read from client */
                        ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                        BrokerPacket packetFromClient;
                        
                        /* stream to write back to client */
                        ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
                        
                        
                        while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                                /* create a packet to send reply back to client */
                                BrokerPacket packetToClient = new BrokerPacket();
                                packetToClient.type = BrokerPacket.BROKER_QUOTE;
                                
                                /* process symbol */
                                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                                        //packetToClient.symbol = packetFromClient.symbol;
                                        FileHandler fh = new FileHandler();
                                        String space = " ";
                                        packetToClient.quote = fh.findQuote(packetFromClient.symbol, space);
                                        System.out.println("From Client: " + packetFromClient.symbol + "\nTo Client: " + packetToClient.quote);
                                        if (packetToClient.quote == 0L)
						packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
					else
						packetToClient.type = BrokerPacket.BROKER_QUOTE;
                                        /* send reply back to client */
                                        toClient.writeObject(packetToClient);
                                        
                                        /* wait for next packet */
                                        continue;
                                }
                                
                                /* Sending an BROKER_NULL || BROKER_BYE means quit */
                                if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
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
                        fromClient.close();
                        toClient.close();
                        socket.close();

                } catch (IOException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                } catch (ClassNotFoundException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                }
        }

}
