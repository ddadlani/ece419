import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;


import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class JobTrackerHandlerThread extends Thread implements Runnable{

	public static final int numPartitions = 266;
	Socket clientSocket;
	String jobPath = "/jobs";
	String workersPath = "/workers";
	Watcher watcher;
	ZkConnector zkc;

	public JobTrackerHandlerThread(Socket socket_, ZkConnector zkc_, Watcher watcher_) {
		this.clientSocket = socket_;
		this.zkc = zkc_;
		this.watcher = watcher_;
	}

	public void run() {

	try {
				System.out.println("ENTERED THREAD");
				ObjectInputStream in = null;
				ObjectOutputStream out = null;
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());

				Serializer s = new Serializer();
				ZooKeeper zk = zkc.getZooKeeper();
				
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
								List<String> jobs = null;
								List<String> workers = null;
								try {
								workers = zk.getChildren(workersPath, watcher);
								} catch (KeeperException k) {
									System.out.println("Keeper Exception at getChildren");
								} catch (InterruptedException e) {
									e.printStackTrace();
								} 

								int num_workers = workers.size();
								int partitions_per_worker = numPartitions/num_workers;
								
								int partitions_first_worker = 0; 
								partitions_first_worker = partitions_first_worker + (numPartitions -(partitions_per_worker * num_workers));

								try {
								jobs = zk.getChildren(jobPath, false);
								} catch (KeeperException k) {
									System.out.println("Keeper Exception at getChildren");
								} catch (InterruptedException e) {
									e.printStackTrace();
								} 

								for (int i = 0; i < jobs.size() ; i++)
								{
									int start = i * partitions_per_worker;

									String jobNodePath = jobs.get(i);
									String jobfullPath = "/jobs/" + jobNodePath;
									System.out.println(jobfullPath);
									byte[] jobNode_in_b = zkc.getData(jobfullPath, null);
									System.out.println("I'M HERE AFTER GETTING DATA");
									JobNode jobNode = (JobNode) s.deserialize(jobNode_in_b);
									System.out.println("I'M HERE AFTER DESIARILIZING DATA");
									System.out.println(jobfullPath);
									int end;
									if (i == 0)
										end = start + partitions_first_worker - 1;
									else
										end = start + partitions_per_worker - 1;

									System.out.println("start: " + start + "end: " + end + "worker path: " + jobNode.workerPath);
									jobNode.addTask(packetFromClient.hash, start, end);
									Task task = jobNode.getFirstIncompleteTask();
									System.out.println("start: " + task.first_partition + "end: " + task.last_partition);
									byte[] jobNode_out_b = s.serialize((Object)jobNode);
									
									try {
										zkc.setData(jobfullPath, jobNode_out_b);
									} catch (InterruptedException e) {
										e.printStackTrace();
									} 

								}

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

/*
JobTrackerHandlerThread:
1. Set watch on /workers when getting Children, find number of workers and split partitions evenly. 
Set partition start and end and hash for each JobNode for each FIND_HASH from Client.
2. for QUERY, gets task with given hash from each JobNode, if any one Task is in progress, 
	returns in progress. 
	If any one found, returns found with word. 
	If ALL tasks not found, returns not found. 
3. If Worker (child) dies (get Children watch), copy workerJobs to another worker, ideally to the last jobNode 
	(last index of getChildren of /jobs) 
4. If Worker (child) is created, recompute number of partitions per task for next job.
*/

