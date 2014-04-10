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
	JobTracker jt;

	public JobTrackerHandlerThread(Socket socket_, ZkConnector zkc_, Watcher watcher_, JobTracker jt_) {
		this.clientSocket = socket_;
		this.zkc = zkc_;
		this.watcher = watcher_;
		this.jt = jt_;
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

								List<String> jobs = null;
								List<String> workers = null;
								try {
								workers = zk.getChildren(workersPath, watcher);
								jobs = zk.getChildren(jobPath, null);
								} catch (KeeperException k) {
									System.out.println("Keeper Exception at getChildren");
								} catch (InterruptedException e) {
									e.printStackTrace();
								} 

								synchronized(jt) {
									jt.num_workers = workers.size();
								}
								int num_jobs = jobs.size();
								boolean not_found = true;
								System.out.println("numworkers: " + jt.num_workers + "numjobs: " + num_jobs);

								for (int i = 0; i < num_jobs ; i++) {
									String jobNodePath = jobs.get(i);
									String jobfullPath = jobPath + "/" + jobNodePath;
									byte[] job_node_b = zkc.getData(jobfullPath, null);
									JobNode job_node = (JobNode) s.deserialize(job_node_b);
									ArrayList <Task> tasks = job_node.getTasks(packetFromClient.hash);


									for(int j = 0; j < tasks.size(); j++) {
										if ((tasks == null) || (tasks.size() == 0))
										{
											System.out.println("Task not assigned to this node: " + jobfullPath );
											continue;
										}
										else if ((tasks.get(j).result == Task.IN_PROGRESS) || (tasks.get(j).result == Task.NOT_STARTED))
										{
											packetToClient.msgType = ClientPacket.REPLY;
											packetToClient.finish = false;
											not_found = false;
											break;
										}
										else if (tasks.get(j).result == Task.FOUND)
										{
											packetToClient.msgType = ClientPacket.REPLY;
											packetToClient.finish = true;
											packetToClient.found = true;
											packetToClient.word = tasks.get(j).word;
											not_found = false;
											break;
										}
									}
									if (!not_found)
											break;
								}

								if (not_found)
								{
									packetToClient.msgType = ClientPacket.REPLY;
									packetToClient.finish = true;
									packetToClient.found = false;
								}

								System.out.println("Writing Query reply packet to Client...");
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
								synchronized(jt) {
									jt.num_workers = workers.size();
								}	
								System.out.println("numworkers: " + jt.num_workers);
								int partitions_per_worker = numPartitions/jt.num_workers;
								
								int partitions_first_worker = 0; 
								partitions_first_worker = partitions_per_worker + (numPartitions -(partitions_per_worker * jt.num_workers));
								try {
									jobs = zk.getChildren(jobPath, null);
								} catch (KeeperException k) {
									System.out.println("Keeper Exception at getChildren");
								} catch (InterruptedException e) {
									e.printStackTrace();
								} 

								int start = 0;
								int end = 0;
								for (int i = 0; i < jobs.size() ; i++)
								{
									String jobNodePath = jobs.get(i);
									String jobfullPath = jobPath + "/" + jobNodePath;
									System.out.println(jobfullPath);

				
									//WORKER WILL DO THIS, just for testing purposes
									/*JobNode Init = new JobNode();
									byte[] Init_b = s.serialize((Object)Init);
									try {
										zkc.setData(jobfullPath, Init_b);
									} catch (InterruptedException e) {
										e.printStackTrace();
									} 
									*/

									byte[] jobNode_in_b = zkc.getData(jobfullPath, null);
									JobNode jobNode;
									//if (jobNode_in_b != null)
									//{
									//	System.out.println("getData returned not null: " + jobNode_in_b);
									jobNode = (JobNode) s.deserialize(jobNode_in_b);
									//}
									//else
									//	jobNode = new JobNode();
									if (i == 0)
										end = start + partitions_first_worker - 1;
									else
										end = start + partitions_per_worker - 1;

									System.out.println("start: " + start + "end: " + end + "worker path: " + jobfullPath);
									jobNode.addTask(packetFromClient.hash, start, end);
									//Task task = jobNode.getFirstIncompleteTask();
									//System.out.println("start: " + task.first_partition + "end: " + task.last_partition);
									byte[] jobNode_out_b = s.serialize((Object)jobNode);
									
									try {
										zkc.setData(jobfullPath, jobNode_out_b);
										//TEST CODE
										/*byte[] Test_b = zkc.getData(jobfullPath, null);
										JobNode Test = (JobNode) s.deserialize(Test_b);
										Task test_task = Test.getFirstIncompleteTask();
										System.out.println("start: " + test_task.first_partition + "end: " + test_task.last_partition);
										*/
									} catch (InterruptedException e) {
										e.printStackTrace();
									} 
									start = end + 1;
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

