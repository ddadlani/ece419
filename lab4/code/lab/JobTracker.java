import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import java.io.IOException;



public class JobTracker {
    
    String myPath = "/jobtracker";
    String jobs_root = "/jobs";
    String workers_root = "/workers";
    ZkConnector zkc;
    Watcher watcher;
    ServerSocket listenSocket;
    boolean is_p;
    int num_workers;

    public static void main(String[] args) {
      
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            return;
        }

        JobTracker jt = new JobTracker(args[0]);   

        boolean primary = jt.checkpath();
        jt.is_p = primary;
        
        try {
            while (!jt.is_p) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {}

        if (jt.zkc.exists(jt.jobs_root, null) == null)
            jt.zkc.create(jt.jobs_root, null, CreateMode.PERSISTENT);
            

        while(jt.is_p) {
            //Listening for connections from ClientDriver
            System.out.println("Listening for connections from ClientDriver");
            try {
                new Thread (new JobTrackerHandlerThread(jt.listenSocket.accept(), jt.zkc, jt.watcher, jt)).start();
            } catch (IOException e) {
                //System.err.println("ERROR: Could not accept connection from client driver thread.");
                //e.printStackTrace();
                //System.exit(1);
                //
            }
        }

        
        
    }

    public JobTracker(String hosts) {
        num_workers = 0;
        is_p = false;
        System.out.println("Entered ClientDriver constructor");
        // Start listening port
        try {
            listenSocket = new ServerSocket(0);
        } catch (IOException e1) {
            System.err.println("ERROR: Could not create server socket.");
            e1.printStackTrace();
            System.exit(1);
        }

        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
 
        watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handleEvent(event);
                        
                            } };
    }
    
    private boolean checkpath() {
        System.out.println("Entered checkpath()");
        Stat stat = zkc.exists(myPath, watcher);
        if (stat == null) { // znode doesn't exist; let's try creating it
            System.out.println("Creating " + myPath);
            String listenPort = String.valueOf(listenSocket.getLocalPort());
            String listenAddress = listenSocket.getInetAddress().getHostAddress();
            System.out.println("listenAddress = " + listenAddress + "listenPort = " + listenPort);
            String addr = listenPort + " " + listenAddress;

            String ret = zkc.create(myPath, // Path of znode
                    addr, // IP addr and port
                    CreateMode.EPHEMERAL // Znode type, set to EPHEMERAL.
                    );
            if (ret != null) {
                System.out.println("Primary JobTracker");
                this.is_p = true;
                return true;
            }
            else { 
                    System.out.println("CODE NOT OK!!"); 
                    System.exit(0); 
            }
        }
        return false;
    }

    private void handleDeletion(ZooKeeper zk, List<String> workers)
    {
        List<String> jobs = null;
        try {
            jobs = zk.getChildren(jobs_root, null);
        } catch (KeeperException k) {
            System.out.println("Keeper Exception at getChildren");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        /*for (int i = 0; i < workers.size(); i++) {
            for (int j = 0; j < jobs.size(); j++) {
                if(workers.get(i).equals(jobs.get(j)))
                    break;
            }
        }*/
        Serializer s = new Serializer();
        Collection<String> unassigned_jobs = new HashSet<String>();
        Collection<String> assigned_jobs = new HashSet<String>( jobs );
        unassigned_jobs.addAll(jobs);
        unassigned_jobs.addAll(workers);
        assigned_jobs.retainAll(workers);
        unassigned_jobs.removeAll(assigned_jobs);

        Iterator<String> itr = unassigned_jobs.iterator();

        while (itr.hasNext()){
            String temp = itr.next();
            System.out.println(temp);
        }

        Iterator<String> i = unassigned_jobs.iterator();
        int index = 0;
        while(i.hasNext())
        {

            String u_job = i.next();
            int worker_index = workers.size()-1-index;

            String u_jobs_path = jobs_root + "/" + u_job;
            String jobs_path = jobs_root + "/" + workers.get(worker_index);

            byte[] tasks_b_in = zkc.getData(jobs_path, null);
            byte[] unassigned_tasks_b = zkc.getData(u_jobs_path, null);

            JobNode tasks = null;
            JobNode unassigned = null;
            try {
                tasks = (JobNode) s.deserialize(tasks_b_in);
                unassigned = (JobNode) s.deserialize(unassigned_tasks_b);
            } catch (IOException io) {}
            catch (ClassNotFoundException c) {}

            for (int j = 0; j < unassigned.workerJobs.size(); j ++) {
                tasks.reassignTask(unassigned.workerJobs.get(j)); //new Task??
            }

            byte[] tasks_b = null;
            try {
                    tasks_b = s.serialize((Object)tasks);
            }catch (IOException io) {}


            try {
                zkc.setData(jobs_path,tasks_b);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException io) {}
            index ++;
        }


    }

    private void handleEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(myPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(myPath + " deleted! Let's go!");       
                checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(myPath + " created!");       
                /*try{ Thread.sleep(5000); } catch (Exception e) {}*/
                checkpath(); // re-enable the watch
            }
        }
        else if (path.equals(workers_root))
        {
            System.out.println("I'M HERE IN THE HANDLER!");
            if (type == EventType.NodeChildrenChanged) {

                ZooKeeper zookeeper = zkc.getZooKeeper();
                //re-enable watch
                List<String> workers = null;
                try {
                    workers = zookeeper.getChildren(workers_root, watcher);
                } catch (KeeperException k) {
                    System.out.println("Keeper Exception at getChildren");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 

                if (num_workers == workers.size())
                    System.out.println("Same number of workers?");
                else if (num_workers > workers.size())
                {
                    System.out.println("Worker deleted");
                    handleDeletion(zookeeper,workers);
                }
                else
                {
                    System.out.println("Worker added");
                }       

                num_workers = workers.size();
            }
        }

    }

}
