import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;

public class ClientDriver {


	public void main (String args[])
	{
	
		//Lookup primary job tracker
		String jtPath = "/jobtracker";
		if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. B zkServer:clientPort");
            return;
        }
    
        ZkConnector zkc = new ZkConnector();
        try {
            zkc.connect(args[0]);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }

        ZooKeeper zk = zkc.getZooKeeper();
        
        try {
        	do {
        		Stat stat = zk.exists(jtPath, true);
        	}
        	while (stat == null)
        	
        	//extract IP address data from zk somehow using zk.dataPath
        		
        } catch(KeeperException e) {
            System.out.println(e.code());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
                            
        System.out.println("Waiting for " + jtPath + " to be created ...");

        System.out.println("DONE");
		
		
		//User interface
		System.out.println("Client Driver Started. Type x to exit");
		Scanner input = new Scanner(System.in);
		do {
			System.out.print("Job/Query? (J/Q): ");
			String input = input.next();
			if (input.equals("J"))
			{
				System.out.print("Enter hash: ");
				input = input.next();
				String hash = input;
				//Return result
			}
			else if (input.equals("Q"))
			{
				//Return progress
			}
			
		} while (!input.equals("x"))
		
	}

}
