import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.io.IOException;

public class JobTracker {
    
    String myPath = "/jobtracker";
    ZkConnector zkc;
    Watcher watcher;

    public static void main(String[] args) {
      
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            return;
        }

        JobTracker jt = new JobTracker(args[0]);   

        boolean primary = jt.checkpath();

        if (!primary) {
            try {
                while (true) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {}
        }
        assert(primary == true);
        
        while (primary) {
            //Listening for connections from ClientDriver
            System.out.println("Listening for connections from ClientDriver");
            /*try {
                new Thread(new JobTrackerHandlerThread(fs.listenSocket.accept(), fs.dataPartitions)).start();
            } catch (IOException e) {
                System.err.println("ERROR: Could not accept connection from client driver thread.");
                e.printStackTrace();
                System.exit(1);
            }*/

        }
        
    }

    public JobTracker(String hosts) {

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
            System.out.println("listenAddress = " + listenAddress);
            String addr = listenPort + " " + listenAddress;
            byte[] data = null;
            try {
                data = addr.getBytes("UTF-8"); 
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } 

            Code ret = zkc.create(myPath, // Path of znode
                    data, // IP addr and port
                    CreateMode.EPHEMERAL // Znode type, set to EPHEMERAL.
                    );
            if (ret == Code.OK) {
                System.out.println("Primary JobTracker");
                return true;
            }
            else
                return false;
        }
        return false;
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
    }

}
