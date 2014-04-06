import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.io.IOException;

public class ClientDriver {

	public Watcher watcher;
	public ZkConnector zkc;
	public String jtPath = "/jobtracker";
	public String hostname_jt;
	public int port_jt;
	
	public ClientDriver(String hosts) {
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
	public static void main (String args[])
	{
	
		
		//Lookup primary job tracker
		
		if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. ClientDriver zkServer:clientPort");
            return;
        }
		
		ClientDriver c = new ClientDriver(args[0]);
        
        try {
        	//get hostname and port of JT
        	c.checkpath();        	
        	
        	//Create connection with jttracker
        	Socket ClientSocket = null;
        	ObjectOutputStream out = null;
            ObjectInputStream in = null;

            ClientSocket = new Socket(c.hostname_jt, c.port_jt);

            out = new ObjectOutputStream(ClientSocket.getOutputStream());
            in = new ObjectInputStream(ClientSocket.getInputStream());

        
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String input;

    		//User interface
    		System.out.println("> Enter hash/Query progress(Q)/exit(x).");
    		System.out.print("> ");
    		
    		ClientPacket toPacket = null;
    		ClientPacket fromPacket = null;
    		
    		while ((input = stdIn.readLine()) != null
                        && input.toLowerCase().indexOf("x") == -1) {

        		/*byte[] jtAddr_bytes = zk.getData(jtPath, true);
            	String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
            	String hostname_jt = hostname_port[0];
            	int port_jt = Integer.parseInt(hostname_port[1]);
            	*/
                        
    			if (input.equals("Q"))
    			{
    				//Return progress/result
    				toPacket.msgType = ClientPacket.QUERY;
    				out.writeObject(toPacket);
    				
    				//Receive result :finish = false OR (finish = true and word)
    				fromPacket = (ClientPacket) in.readObject();
    				if (fromPacket.msgType == ClientPacket.REPLY)
    				{	
    					System.out.println("Result = " + fromPacket.finish);
    					if (fromPacket.finish)
    						System.out.println("Word = " + fromPacket.word);
    				}
    				else
    				{
    					System.out.println("Wrong packet type received, needed Reply");
    					System.exit(-1);
    				}
    			}
    			
    			else 
    			{
    				//Send hash
    				toPacket.msgType = ClientPacket.FIND_HASH;
    				toPacket.hash = input;
    				out.writeObject(toPacket);
    				
    				//Receive ack saying got sent
    				fromPacket = (ClientPacket) in.readObject();
    				if (fromPacket.msgType == ClientPacket.ACK)
    					System.out.println("Your request has been sent.");
    				else
    				{
    					System.out.println("Wrong packet type received, needed Ack");
    					System.exit(-1);
    				}
    			}
    			
    			System.out.print("> ");
    		 
   
    	}       


        out.close();
        in.close();
        stdIn.close();
        ClientSocket.close();
        		
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

                            
        System.out.println("Waiting for " + c.jtPath + " to be created ...");

        System.out.println("DONE");
		
		

		
	}
	
	public static String[] parseAddressFromBytes(byte[] unparsed)
	{
		String Addr = null;
		try {
			Addr = new String(unparsed, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String[] hostname_port = Addr.split(" ");
    	return hostname_port;
	}
	
    //waits till JT exists and gets its hostname and port
    private void checkpath() {
    	
    	Stat stat = null;
    	do {
	        stat = zkc.exists(jtPath, watcher);
	        try {
                if (stat == null)
				    Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	} while (stat == null);
    	
    	byte[] jtAddr_bytes = zkc.getData(jtPath);
    	String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
    	hostname_jt = hostname_port[0];
    	port_jt = Integer.parseInt(hostname_port[1]);
    }
    
    private void handleEvent(WatchedEvent event) {
    	// check for event type NodeCreated or NodeDataChanged
        
        
        checkpath();
        ClientSocket = new Socket(c.hostname_jt, c.port_jt);

        out = new ObjectOutputStream(ClientSocket.getOutputStream());
        in = new ObjectInputStream(ClientSocket.getInputStream());
        
        // verify if this is the defined znode
        System.out.println("New Socket connection port: " + port_jt + " and hostname: " + hostname_jt );	
    }
}


