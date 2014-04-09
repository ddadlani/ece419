import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class JobTrackerHandlerThread extends Thread implements Runnable{

	Socket clientSocket;
	public JobTrackerHandlerThread(Socket socket_) {
		this.clientSocket = socket_;
	}

	public void run() {

	try {
				System.out.println("ENTERED THREAD");
				ObjectInputStream in = null;
				ObjectOutputStream out = null;
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				
				
				ClientPacket packetFromClient = null;

				System.out.println("Receiving packet from Client...");

				while (( packetFromClient = (ClientPacket) in.readObject()) != null) {
					ClientPacket packetToClient = new ClientPacket();
					if (packetFromClient.msgType == ClientPacket.QUERY) {
								System.out.println("QUERY Packet recv for hash " + packetFromClient.hash);
								packetToClient.msgType = ClientPacket.REPLY;
								packetToClient.finish = false;
								/*
								packetToClient.finish = true;
								packetToClient.word = "HEY!";
								*/
								System.out.println("Writing FINISH FALSE packet to Client...");
								out.writeObject(packetToClient);
					}
					else if (packetFromClient.msgType == ClientPacket.FIND_HASH) {
								System.out.println("FIND_HASH Packet recv for hash " + packetFromClient.hash);
								packetToClient.msgType = ClientPacket.ACK;
								System.out.println("Writing ACK packet to Client...");
								out.writeObject(packetToClient);
					}
					else
					{

						// Should not get here
						System.err.println("ERROR: Client thread sent something other than a QUERY?");
						System.exit(0);
					}
						
				}
					// By now the work is done. Send ACK?
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("Client exited");
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

