import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.ArrayList;

import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

public class FileServer {
	public static final int numPartitions = 266;
	
	public static final int partitionSize = 1000;

	String datafile;
	String myPath = "/primaryFS";
	ZkConnector zkc;
	Watcher watcher;
	ServerSocket listenSocket;
	ArrayList <ArrayList<String>> dataPartitions;
	boolean primary;

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. FileServer zkServer:clientPort dictionary-file-name");
			return;
		}
		// TAKE IN FILE NAME TOO?
		FileServer fs = new FileServer(args[0], args[1]);

		fs.primary = fs.checkpath();

		try {
			while (!(fs.primary)) {
				//System.out.println("Sleeping...");
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {

		}
		//assert(primary == true);
		
		while (fs.primary) {
			// Listen for connections
			//System.out.println("Listening");
			try {
				new Thread(new FileServerHandlerThread(fs.listenSocket.accept(), fs.dataPartitions)).start();
			} catch (IOException e) {
				System.err.println("ERROR: Could not accept connection from worker thread.");
				e.printStackTrace();
				System.exit(1);
			}
			
		}

	}

	public FileServer(String hosts, String filename) {
		//System.out.println("Entered FileServer constructor");
		// Start listening port
		try {
			listenSocket = new ServerSocket(0);
		} catch (IOException e1) {
			System.err.println("ERROR: Could not create server socket.");
			e1.printStackTrace();
			System.exit(1);
		}

		// Load the file into memory
		//System.out.println("Searching for data file...");
		this.datafile = new String(filename);
		FileReader fr = null;
		BufferedReader getData = null;
		ArrayList<String> temp = null;
		try {
			fr = new FileReader(datafile);
			getData = new BufferedReader(fr);
			//System.out.println("Loading data file into memory...");
			dataPartitions = new ArrayList<ArrayList<String>>(numPartitions);
			for (int partition = 0; partition < numPartitions; partition++) {
				temp = new ArrayList<String>(partitionSize);
				for (int index = 0; index < partitionSize; index++) {
					temp.add(getData.readLine());
				}
				dataPartitions.add(temp);
				temp = null;
			}
		} catch (FileNotFoundException fnf) {
			System.err.println("ERROR: Could not find data file " + datafile);
			fnf.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			if (temp.size() == 744) {/* do nothing */}
			else {
				//System.err.println("ERROR: getData threw IOException before eof. Terminating.");
				System.exit(1);
			}
		}
		
		// Election of primary
		primary = false;
		//System.out.println("Checking to see if primary exists");
		zkc = new ZkConnector();
		try {
			zkc.connect(hosts);
		} catch (Exception e) {
			//System.out.println("Zookeeper connect in FileServer " + e.getMessage());
		}

		watcher = new Watcher() { // Anonymous Watcher
			@Override
			public void process(WatchedEvent event) {
				handleEvent(event);

			}
		};

	}

	private boolean checkpath() {
		//System.out.println("Entered checkpath()");
		
		Stat stat = zkc.exists(myPath, watcher);
		if (stat == null) { // znode doesn't exist; let's try creating it
			//System.out.println("Creating " + myPath);
			//System.out.println("Primary FileServer");

			String listenPort = String.valueOf(listenSocket.getLocalPort());
			String listenAddress = listenSocket.getInetAddress().getHostAddress();
			
			//System.out.println("listenAddress = " + listenAddress);
			//System.out.println("listenPort = " + listenPort);
			
			String addr = listenPort + " " + listenAddress;
			String name = zkc.create(myPath, addr, CreateMode.EPHEMERAL); // Znode type, set to EPHEMERAL.
			primary = true;
		}
		return primary;
	}

	private void handleEvent(WatchedEvent event) {
		String path = event.getPath();
		EventType type = event.getType();
		if (path.equalsIgnoreCase(myPath)) {
			if (type == EventType.NodeDeleted) {
				//System.out.println(myPath + " deleted! Let's go!");
				//System.out.println("Primary before1: " + primary);
				this.primary = checkpath(); // try to become the boss
				//System.out.println("Primary after1: " + primary);
			}
			if ((type == EventType.NodeCreated)&&(!primary)) {
				//System.out.println(myPath + " created!");
				/*try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}*/
				//System.out.println("Primary before2: " + primary);
				this.primary = checkpath(); // re-enable the watch
				//System.out.println("Primary after2: " + primary);
			}
		}
	}
}