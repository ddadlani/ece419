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
                        FileHandler fh = new FileHandler();
                        
                        while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                                /* create a packet to send reply back to client */
                                BrokerPacket packetToClient = new BrokerPacket();
                                if(packetFromClient.symbol == null) {
                                	packetToClient.type = BrokerPacket.BROKER_NULL;
                                	packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                                	toClient.writeObject(packetToClient);
                                	continue;
                                }
                                
                                /* process symbol */
                                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
                                        String space = " ";
                                        packetToClient.quote = fh.findQuote(packetFromClient.symbol, space);
                                        System.out.println("From Client: " + packetFromClient.symbol + "\nTo Client: " + packetToClient.quote);
                                        if (packetToClient.quote == 0L)
                                        	packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                                        else
                                        	packetToClient.type = BrokerPacket.BROKER_QUOTE;
                                        /* send reply back to client */
                                        toClient.writeObject(packetToClient);
                                        
                                        /* wait for next packet */
                                        continue;
                                }
                                
                                else if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                                	packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                                	packetToClient.quote = fh.findQuote(packetFromClient.symbol," ");
                                	if (packetToClient.quote != 0L) {
                                		packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
    		                        }
                                	else {
                                		fh.addSymbol(packetFromClient.symbol, packetFromClient.quote);
                                	}
                                	toClient.writeObject(packetToClient);
                          		continue;
                                }
                                
                                else if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                                	packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                                	packetToClient.quote = fh.findQuote(packetFromClient.symbol," ");
                                	if (packetToClient.quote == 0L) {
                                		packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                                	}
                                	else {
                                		fh.removeSymbol(packetFromClient.symbol);
                                		
                                	}
                                	toClient.writeObject(packetToClient);
                                	continue;
                                }
                                
                                else if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                                	packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                                	packetToClient.quote = fh.findQuote(packetFromClient.symbol," ");
                                	if (packetToClient.quote == 0L) {
                                		packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                                	}
                                	else {
                                		fh.removeSymbol(packetFromClient.symbol);
                                		fh.addSymbol(packetFromClient.symbol, packetFromClient.quote);
                                		
                                	}
                                	toClient.writeObject(packetToClient);
                                	continue;
                                
                                }
                                
                                /* Sending an BROKER_NULL || BROKER_BYE means quit */
                                else if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
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
                        fh.close();
                        

                } catch (IOException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                } catch (ClassNotFoundException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                }
        }

}
