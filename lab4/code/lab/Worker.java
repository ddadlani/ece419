import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;



public class Worker {

	String fspath = "/primaryFS";
	String mypath = "/workers/worker";
	String fsAddress;

	Watcher watcher;
	/**
	 * The address of the ZooKeeper in the form zkHostname:zkPort
	 */
	String zkAddress;

	/**
	 * The zkConnector we will use to interact with the ZooKeeper
	 */
	ZkConnector zkc;
	/**
	 * The first and last partition numbers we need to work with
	 */ 
	Integer firstPartition;
	Integer lastPartition;

	/**
	 * The password we are trying to hack
	 */
	String passwordHash;

	/**
	 * The set of dictionary partitions we have to sift through
	 */
	ArrayList <ArrayList<String>> dataPartitions;

	/**
	 * Default constructor
	 */
	public Worker(String zkAddress_) {
		this.zkAddress = zkAddress_;
		this.fsAddress = null;

		this.firstPartition = 2;
		this.lastPartition = 5;
		
		this.passwordHash = null;
		this.dataPartitions = null;

		// Connect to ZooKeeper
		this.zkc = new ZkConnector();
		try {
			zkc.connect(this.zkAddress);
		} catch (InterruptedException ie) {
			System.err.println("ERROR: Could not connect to ZooKeeper");
			ie.getMessage();
			ie.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("ERROR: Could not connect to ZooKeeper");
			ioe.getMessage();
			ioe.printStackTrace();
		}

		// Set a watch on FileServer
		this.watcher = new Watcher() { // Anonymous Watcher
			@Override
			public void process(WatchedEvent event) {
				handleEvent(event);
			}
		};

		// Read in FileServer information
		try {
			while(checkpath() == false) {
				// Sleep while primary File Server does not exist
				Thread.sleep(500);
			}
		} catch (InterruptedException ie) {}

		// Create a sequential-ephemeral node for this worker
		Code ret = zkc.create(mypath, null, CreateMode.EPHEMERAL_SEQUENTIAL);
		if (ret != Code.OK) {
			System.err.println("ERROR: Something went wrong with node creation. Terminating.");
			System.exit(1);
		}


	}
	
	public static void main (String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Worker zkServer:clientPort");
		}

		// Create worker and connect to ZooKeeper
		Worker worker = new Worker(args[0]);


		// get fileserver address
		// put a watch on fileserver so can ask for backup if fileserver dies
		// create a sequential ephemeral node at path /workers/worker for worker
		// look in jobs/ list it or sth to find the same x as you have in workerx, put a watch on that
		// put a watch on 
	}
	
	public static String getHash(String word) {
        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
        	// ignore
	    }
	        return hash;
	}

	private void handleEvent(WatchedEvent event) {
		String path = event.getPath();
		EventType type = event.getType();
		if (path.equalsIgnoreCase(fspath)) {
			if (type == EventType.NodeDeleted) {
				System.out.println("FileServer dead. Attempting to connect to backup");
				checkpath(); 
			}
			if (type == EventType.NodeCreated) {
				System.out.println(fspath + " created!");
				checkpath();
			}
		}
	}

	private boolean checkpath() {
		Stat stat = zkc.exists(fspath, watcher);
		if (stat != null) {
			try {
				byte[] data = zkc.getData(fspath, watcher);
				// Record the new primary file server information
				this.fsAddress = new String(data, "UTF-8");
				System.out.println("New FileServer located at " + this.fsAddress);
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
			return true;
		}
		this.fsAddress = null;
		return false;
	}
}