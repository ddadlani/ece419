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
		
	}
	
}
