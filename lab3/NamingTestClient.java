
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;

public class NamingTestClient {
	public static void main(String[] args){
		Socket socket = null;
		ObjectInputStream in;
		ObjectOutputStream out;
		try {
			socket = new Socket("localhost", 6000);
		//	in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
		
			
			MazePacket test = new MazePacket();
			test.setmsgType(MazePacket.LOOKUP_REQUEST);
			test.setevent(MazePacket.CONNECT);
			test.setName("rand");
		
			Address info = new Address();
			info.hostname = "localhost";
			info.name = "rand";
			info.port = socket.getLocalPort();
		
			test.setclientInfo(info);
		
			out.writeObject(test);
			
			MazePacket recvtest;
			in = new ObjectInputStream(socket.getInputStream());
			if ((recvtest = (MazePacket) in.readObject()) != null) {
				System.out.println("Message type: " + recvtest.getmsgType());
				System.out.println("Event type: " + recvtest.getevent());
				
				if (recvtest.remotes == null) {
					System.err.println("Remotes are null");
				}
				else {
					Iterator<Address> i = recvtest.remotes.iterator();
					while (i.hasNext()) {
						Address next = i.next();
						System.out.println("Remote: Name =  " + next.name);
						System.out.println("Hostname = " + next.hostname + " Port = " + next.port);
					}
				}
			}
			
			//test.setmsgType(MazePacket.ACK);
			//out.writeObject(test);
			//try {
			//	Thread.sleep(100);
			//} catch (InterruptedException e) {
			//	e.printStackTrace();
			//}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ClassNotFoundException cnfe) {
			System.err.println("Class not found.");
		}
		
	}
}
