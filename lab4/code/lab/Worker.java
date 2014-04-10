import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

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
	String jobpath = "/jobs";


	/**
	 * Host name and port of the File Server
	 */
	String fsAddress;

	/**
	 * Worker's own sequential path
	 */
	String myID;

	/**
	 * Worker's job path
	 */
	String myJobID;


	/**
	 * Watcher for the file server
	 */
	Watcher watcher;

	/**
	 * The zkConnector we will use to interact with the ZooKeeper
	 */
	ZkConnector zkc;

	/**
	 * The set of dictionary partitions we have to sift through
	 */
	//ArrayList<ArrayList<String>> dataPartitions;

	/**
	 * A queue of all the received tasks
	 */
	Queue<Task> jobQueue;

	int numPartitions;
	boolean connected;
	Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;

	/**
	 * Default constructor
	 */
	public Worker(String zkAddress) {

		this.fsAddress = null;

		// Connect to ZooKeeper
		this.zkc = new ZkConnector();
		try {
			zkc.connect(zkAddress);
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
				System.out.println("Created watcher");
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
		} else {
		  	//System.out.println("My path = "+ myID);
		  	String[] splitID = myID.split("/");
		  	myID = new String(splitID[2]);
		  	//System.out.println("My ID " + myID);
		}

		// Create a persistent node with your name under /jobs
		stat = null;
		stat = zkc.exists(jobpath, null);
		if (stat == null) {
			String name = zkc.create(jobpath, null, CreateMode.PERSISTENT);
			if (name != null) {
				System.out.println("jobpath created");
			} else {
				System.out.println("???");
			}
		}
		String str = new String(jobpath);
		myJobID = jobpath + "/" + myID;
		System.out.println("my job id " + myJobID);
		JobNode jn = new JobNode();
		//byte[] stuff = Serializer.serialize(jn);
		str = zkc.create(myJobID, jn, CreateMode.PERSISTENT);
		if (str == null) {
			System.err.println("ERROR: Something went wrong with job node creation. Terminating.");
			System.exit(1);
		} else {
			// Set a watch
		  	byte[] data = zkc.getData(myJobID, watcher);
		  	if (data == null) {
		  		System.out.println("Waiting for data");
		  	} 
		}

		jobQueue = new LinkedList<Task>();
		//dataPartitions = new ArrayList<ArrayList<String>>(numPartitions);
		connected = false;

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Worker zkServer:clientPort");
			return;
		}

		// Create worker and connect to ZooKeeper
		Worker worker = new Worker(args[0]);

		// Continuous loop of doing tasks one at a time
		while(true) {
			Task curTask = null;
			try {
				// Dequeue one task
				while ((curTask = worker.jobQueue.poll()) == null) {
					System.out.println("Sleeping...");
					Thread.sleep(500);
				} 
			} catch (InterruptedException ie) {}

			// curTask should not be null here. Gather required parameters
			Integer firstPartition = new Integer(curTask.first_partition);
			Integer lastPartition = new Integer(curTask.last_partition);

			Integer num = lastPartition - firstPartition + 1;

			String taskHash = curTask.hash;

			// Retrieve partitions required for that task
			ArrayList<ArrayList<String>> dataPartitions = worker.getPartitions(firstPartition, lastPartition, num);
			
			// Process the job
			String wordHash = null;
			String cracked = null;
			boolean found = false;

			// Iterate through ArrayList of ArrayLists
			for (Integer i = 0; i < num; i++) {
				ArrayList curPartition = dataPartitions.get(i);
				// Iterate through each individual partition
				for (Integer j = 0; j < curPartition.size(); j++) {
					if ((i == num - 1) && (j == 744))
						break;
					//System.out.println("Current iteration value: "+ i + " j: " + j + " " + curPartition.get(j));
					wordHash = getHash((String) curPartition.get(j));
					if (taskHash.equals(wordHash)) {
						cracked = (String) curPartition.get(j);
						found = true;
						break;
					}
					if (found)
						break;
				}
			}

			// Store the result back in the node
			Task doneTask = new Task(curTask);
			if (found) {
				doneTask.word = cracked;
				doneTask.result = Task.FOUND;
			} else {
				doneTask.result = Task.NOT_FOUND;
			}
			try {
				byte[] putBack = worker.zkc.getData(worker.myJobID, worker.watcher);
				Object o = Serializer.deserialize(putBack);	
				JobNode jn = (JobNode) o;
				
				boolean removed = jn.workerJobs.remove(curTask);
				if (!removed) {
					System.out.println("Job wasnt found, what?");
				}
				jn.workerJobs.add(doneTask);
				System.out.println("done task added to arraylist");
				putBack = null;
				putBack = Serializer.serialize(jn);
				if (putBack != null) {
					worker.zkc.setData(worker.myJobID, putBack);
				} else {
					System.out.println("putback was null?");
				}
			} catch (IOException e) {
				System.err.println("ERROR: Could not serialize/deserialize");
				e.printStackTrace();
				System.exit(1);
			} catch (ClassNotFoundException cne) {
				System.err.println("ERROR: Class not found");
				cne.printStackTrace();
				System.exit(1);
			} catch (InterruptedException ie) {
				System.err.println("Interrupted exception thrown");
				ie.printStackTrace();
				System.exit(1);
			}


		}

	}


	/**
	 * Function to connect to the fileserver
	 */
	private boolean connectToFileServer() {
		System.out.println("fsAddress = " + this.fsAddress);
		String[] host_port = this.fsAddress.split(" ");
		assert (host_port.length == 2);
		
		System.out.println("Attempting to connect to FileServer");
		System.out.println("Address " + host_port[1] + " port " + host_port[0]);

		this.out = null;
		this.in = null;
		this.socket = null;
		try {

			this.socket = new Socket(host_port[1], Integer.parseInt(host_port[0]));

			System.out.println("Created socket");
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			System.out.println("Created objout");
			this.in = new ObjectInputStream(this.socket.getInputStream());

			return true;
		} catch (IOException e) {
			System.out.println("ERROR: A connection error occurred.");
			return false;
		}
	}


	/**
	 * Function to get partitions from the fileserver
	 */
	private ArrayList<ArrayList<String>> getPartitions(Integer firstPartition, Integer lastPartition, int num) {

		// Partitions to send back to the worker thread
		ArrayList<ArrayList<String>> dataPartitions = new ArrayList<ArrayList<String>>(num);
		
		// Connect to file server
		connectToFileServer();

		System.out.println("About to send to fileServer");
			for (Integer i = firstPartition; i <= lastPartition; i++) {
				try {
					// Send request packet
					FileServerPacket outPacket = new FileServerPacket();
					outPacket.type = FileServerPacket.QUERY;
					outPacket.value = new Integer(i);				
					this.out.writeObject(outPacket);
					//System.out.println("Just sent packet");
				
					// Receive reply packet with information
					FileServerPacket inPacket = (FileServerPacket) this.in.readObject();
					dataPartitions.add(inPacket.partition);

					//System.out.println("First word in partition: "
					//	+ dataPartitions.get(i).get(0));
					//System.out.println("Last word in partition: "
					//	+ dataPartitions.get(i).get(dataPartitions.get(i).size() -1));

				} catch (IOException e) {
					// File server died, connection unexpectedly lost
					System.out.println("FileServer unavailable. Waiting for FileServer to come online...");
					// Repeat last working partition
					i--;
					
					// Sleep to allow time for the backup file server to create a new znode
					try {
						Thread.sleep(8000);
					} catch (InterruptedException ie) {}

					// Check if we are connected
					this.connected = connectToFileServer();
					if (!this.connected) {
						System.err.println("ERROR: Could not connect to FileServer. Try again later");
						System.exit(1);
					}	

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return null;
				} 
			}

			// All necessary data received. Send DONE packet
			try {
				FileServerPacket donePacket = new FileServerPacket();
				donePacket.type = FileServerPacket.DONE;
				this.out.writeObject(donePacket);

				// Wait for ACK
				FileServerPacket ackPacket = (FileServerPacket) this.in.readObject();
				assert (ackPacket.type == FileServerPacket.ACK);

				System.out.println("Done. Returning");
				
			} catch (IOException e) {
				System.out.println("ERROR: A connection error occurred.");
				e.printStackTrace();
				System.exit(1);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		
			return dataPartitions;
	}

	/**
	 * Function to get the hash encoding of a given string
	 */
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

	/**
	 * Interrupt and watch-event handlers
	 */
	private void handleEvent(WatchedEvent event) {
		String path = event.getPath();
		EventType type = event.getType();
		if (path.equalsIgnoreCase(fspath)) {
			if (type == EventType.NodeDeleted) {
				System.out.println("FileServer dead. Attempting to connect to backup");
				checkpath();
			} else if (type == EventType.NodeCreated) {
				System.out.println(fspath + " created!");
				checkpath();
			} else {
				System.out.println("An unexpected event received");
			}
		} else if(path.equalsIgnoreCase(myJobID)) {
			if (type == EventType.NodeDataChanged) {
				System.out.println("Data has been changed");
				byte[] data = zkc.getData(myJobID, watcher);
				try {
					Object o = Serializer.deserialize(data);
					JobNode jn = (JobNode) o;
					Task newTask = jn.getFirstIncompleteTask();
					jobQueue.add(newTask);
				} catch (Exception e) {
					System.out.println("Exception thrown in handleEvent");
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				System.out.println("An unexpected event received here");
			}
		}
	}

	private boolean checkpath() {
		Stat stat = zkc.exists(fspath, watcher);
		if (stat != null) {
			try {
				byte[] data = zkc.getData(fspath, watcher);
				// Record the new primary file server information
				Object o = Serializer.deserialize(data);
				this.fsAddress = (String) o;
				System.out.println("New FileServer located at " + this.fsAddress);
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException cne) {
				cne.printStackTrace();
			}
			return true;
		}
		this.fsAddress = null;
		return false;
	}


}