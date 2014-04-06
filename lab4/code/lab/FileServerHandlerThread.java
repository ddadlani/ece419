import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;


public class FileServerHandlerThread extends Thread implements Runnable {
	
	Socket workerSocket;
	ArrayList <ArrayList<String>> dataPartitions;
	
	
	public FileServerHandlerThread(Socket socket_, ArrayList<ArrayList<String>> data_){
		super("FileServerHandlerThread");
		this.workerSocket = socket_;
		this.dataPartitions = data_;
	}
	
	public void run() {
		
		try {
			ObjectInputStream in = null;
			in = new ObjectInputStream(workerSocket.getInputStream());
		
			Object o = in.readObject();
			assert(o instanceof FileServerPacket);
			FileServerPacket packetFromClient = (FileServerPacket) o;
			
			if (packetFromClient.type != FileServerPacket.QUERY) {
				// Should not get here
				System.err.println("ERROR: Worker thread sent something other than a QUERY?");
				return;
			} else {
				boolean done = false;
				ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
				do {
					if (packetFromClient.type == FileServerPacket.DONE) {
						done = true;
					}
					// Send out the partition
					Integer partitionID = packetFromClient.value;
					FileServerPacket packetToClient = new FileServerPacket();
					packetToClient.type = FileServerPacket.REPLY;
					
					synchronized (dataPartitions) {
						packetToClient.partition = dataPartitions.get(partitionID);
					}					
					out.writeObject(packetToClient);
					packetFromClient = null;
					
					// Wait for next packet
					o = in.readObject();
					assert(o instanceof FileServerPacket);
					packetFromClient = (FileServerPacket) o;
					
				} while(!done);
				
				// By now the work is done. Send ACK?
			}
			workerSocket.close();
		} catch (IOException e) {
			System.err.println("ERROR: Connection from worker thread lost.");
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
}
