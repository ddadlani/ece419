import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
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

	/**
	 * Relative path names for the File Server and the workers
	 */
	String fspath = "/primaryFS";
	String mypath = "/workers/worker";

	/**
	 * Host name and port of the File Server
	 */
	String fsAddress;

	/**
	 * Worker's own sequential path
	 */
	String myID;

	/**
	 * Watcher for the file server
	 */
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
	ArrayList<ArrayList<String>> dataPartitions;

	/**
	 * Default constructor
	 */
	public Worker(String zkAddress_) {
		this.zkAddress = zkAddress_;
		this.fsAddress = null;

		this.firstPartition = 0;
		this.lastPartition = 3;

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
			while (checkpath() == false) {
				// Sleep while primary File Server does not exist
				Thread.sleep(500);
			}
		} catch (InterruptedException ie) {
		}

		// Create a sequential-ephemeral node for this worker
		Stat stat = zkc.exists("/workers", null);
		if (stat == null) {
			String name = zkc.create("/workers", null, CreateMode.PERSISTENT);
			assert(name != null);
		}
		this.myID = zkc.create(mypath, null, CreateMode.EPHEMERAL_SEQUENTIAL);
		if (myID == null) {
			System.err.println("ERROR: Something went wrong with node creation. Terminating.");
			System.exit(1);
		} //else {
		  //	System.out.println("My path = "+ myID);
		  //}

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Worker zkServer:clientPort");
			return;
		}

		// Create worker and connect to ZooKeeper
		Worker worker = new Worker(args[0]);

		// Retrieve partitions from FileServer
		//System.out.println("fsAddress = " + worker.fsAddress);
		String[] host_port = worker.fsAddress.split(" ");
		assert (host_port.length == 2);
		Integer num = worker.lastPartition - worker.firstPartition + 1;

		worker.dataPartitions = new ArrayList<ArrayList<String>>(num);
		//System.out.println("Attempting to connect to FileServer");
		//System.out.println("Address " + host_port[1] + " port " + host_port[0]);
		try {

			Socket socket = new Socket(host_port[1],
					Integer.parseInt(host_port[0]));

			//System.out.println("Created socket");
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			//System.out.println("Created objout");
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			
			

			//System.out.println("About to send to fileServer");
			for (Integer i = worker.firstPartition; i <= worker.lastPartition; i++) {
				FileServerPacket outPacket = new FileServerPacket();
				outPacket.type = FileServerPacket.QUERY;
				outPacket.value = new Integer(i);
				out.writeObject(outPacket);
				//System.out.println("Just sent packet");
				FileServerPacket inPacket = (FileServerPacket) in.readObject();
				worker.dataPartitions.add(inPacket.partition);
				//System.out.println("First word in partition: "
				//		+ worker.dataPartitions.get(i - worker.firstPartition)
				//				.get(0));
				//System.out.println("Last word in partition: "
				//		+ worker.dataPartitions.get(i - worker.firstPartition)
				//				.get(worker.dataPartitions.get(i - worker.firstPartition).size() -1));
			}

			FileServerPacket donePacket = new FileServerPacket();
			donePacket.type = FileServerPacket.DONE;
			out.writeObject(donePacket);

			// Wait for ACK
			FileServerPacket ackPacket = (FileServerPacket) in.readObject();
			assert (ackPacket.type == FileServerPacket.ACK);

			//System.out.println("Done. Returning");
			return;
		} catch (IOException e) {
			System.out.println("ERROR: A connection error occurred.");
			e.printStackTrace();
			System.exit(1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
	}

	public static String getHash(String word) {
		String hash = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
			hash = hashint.toString(16);
			while (hash.length() < 32)
				hash = "0" + hash;
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
				//System.out.println("New FileServer located at " + this.fsAddress);
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
			return true;
		}
		this.fsAddress = null;
		return false;
	}
}