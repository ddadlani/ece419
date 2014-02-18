import java.net.*;
import java.util.Queue;
import java.io.*;

public class MazeServerHandlerThread extends Thread {
        private Socket socket = null;
        private int clientID;
        private Queue<MovePacket> q;
        
        public MazeServerHandlerThread(Socket socket, Integer clientID, Queue<MovePacket> q) {
                super("MazeServerHandlerThread");
                this.socket = socket;
                this.clientID = clientID;
                this.q = q;
               // System.out.println("Created new Thread to handle client");
        }

        public void run() {

                boolean gotByePacket = false;
                
                try {
                        /* stream to read from client */
                        ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                        MovePacket packetFromClient;
                        
                        /* stream to write back to client */
                        ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
                        
                        
                        while (( packetFromClient = (MovePacket) fromClient.readObject()) != null) {
        
                                
                                /* process symbol */
 
                                //System.out.println("From Client: " + packetFromClient.symbol + "\nTo Client: " + packetToClient.quote);
                                if(packetFromClient.getmsgType() == MovePacket.CONNECTION_REQUEST) {
                                        /* create a packet to send clientID back to client */
                                    	MovePacket packetToClient = new MovePacket(); //clientID, event, seqNum, msgType
                                    	packetToClient.setclientID(clientID);
                                    	packetToClient.setmsgType(MovePacket.CONNECTION_REPLY);
                                        toClient.writeObject(packetToClient);
                                        
                                        /* wait for next packet */
                                        continue;
                                }
                                
                        		if(packetFromClient.getmsgType() == MovePacket.MOVE_REQUEST) {
                        				/* create a packet to send latest move back to client */
                        				//ClientEvent 
                        				MovePacket packetToClient;
                        				synchronized(this)
                        				{
                        					q.add(packetFromClient);
                        					packetToClient = q.remove();
                        				}
                        				packetToClient.setmsgType(MovePacket.MOVE_REPLY);
                        				toClient.writeObject(packetToClient);
                        		}	
                              
                                
                                /* if code comes here, there is an error in the packet */
                                System.err.println("ERROR: Unknown BROKER_* packet!!");
                                System.exit(-1);
                        }
                        
                        /* cleanup when client exits */
                        fromClient.close();
                        toClient.close();
                        socket.close();

                } catch (EOFException e) {
                	
                } catch (IOException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                } catch (ClassNotFoundException e) {
                        if(!gotByePacket)
                                e.printStackTrace();
                }
        }

}
