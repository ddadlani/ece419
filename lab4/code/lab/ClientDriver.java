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
import org.apache.zookeeper.Watcher.Event.EventType;

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
import java.util.ArrayList;

import java.util.List;
import java.io.IOException;

public class ClientDriver {

    public Watcher watcher;
    public ZkConnector zkc;
    public String jtPath = "/jobtracker";
    public String hostname_jt;
    public int port_jt;
    public Socket CSocket;
    public ObjectOutputStream out;
    public ObjectInputStream in;
    public int i;
    public ArrayList <CountDownLatch> nodeCreatedSignal; 
    public boolean primary_failed;

    public ClientDriver(String hosts) {

        primary_failed = false;
        hostname_jt = null;
        port_jt = -1;
        CSocket = null;
        out = null;
        in = null;
        zkc = new ZkConnector();
        i = 0;
        nodeCreatedSignal = new ArrayList <CountDownLatch>();
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

        if (c.setWatch() == null)  
            c.jtcreate_wait();
           
        //set hostname and port
        byte[] jtAddr_bytes = c.zkc.getData(c.jtPath, c.watcher); //SETS WATCH
        String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
        c.hostname_jt = hostname_port[1];
        c.port_jt = Integer.parseInt(hostname_port[0]);
        System.out.println("Jobtracker exists! hostname: " + c.hostname_jt + " port: " + c.port_jt);

        //Create socket
        try {
            c.CSocket = new Socket(c.hostname_jt, c.port_jt);
            c.out = new ObjectOutputStream(c.CSocket.getOutputStream());
            c.in = new ObjectInputStream(c.CSocket.getInputStream());
        } catch (IOException e)
        {
            System.out.println("IO EXCEPTION");
            System.exit(0);
        }
            
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String input;

        //User interface
        System.out.println("> What do you want to do? find_password (F) / Query progress (Q) / exit(x) ?");
        System.out.print("> ");
        
        ClientPacket toPacket = null;
        ClientPacket fromPacket = null;

        try { 

            while ((input = stdIn.readLine()) != null
                        && input.toLowerCase().indexOf("x") == -1) {

                /*byte[] jtAddr_bytes = zk.getData(jtPath, true);
                String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
                String hostname_jt = hostname_port[0];
                int port_jt = Integer.parseInt(hostname_port[1]);
                */   
                toPacket = new ClientPacket();
                fromPacket = new ClientPacket();

                if (input.equals("Q"))
                {
                    toPacket.msgType = ClientPacket.QUERY;
                    System.out.println("> Input hash to query.");
                    System.out.print("> ");
                    input = stdIn.readLine();
                    toPacket.hash = input; 
                    System.out.println("Sending Q packet to JT...");

                    if (c.primary_failed)
                        Thread.sleep(5000);
                    c.out.writeObject(toPacket);
                    
                    //Receive result :finish = false OR (finish = true and word)
                    System.out.println("Receiving REPLY packet from JT...");

                    if (c.primary_failed)
                        Thread.sleep(5000);
                    fromPacket = (ClientPacket) c.in.readObject();

                    System.out.println("PACKET RECEIVED, type: " + fromPacket.msgType);

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
                
                else if (input.equals("F")) 
                {
                    System.out.println("> Input hash to find password.");
                    System.out.print("> ");
                    input = stdIn.readLine();
                    //Send hash
                    toPacket.msgType = ClientPacket.FIND_HASH;
                    toPacket.hash = input;
                    System.out.println("Sending F packet to JT...");

                    if (c.primary_failed)
                        Thread.sleep(5000);
                    c.out.writeObject(toPacket);
                    
                    //Receive ack saying got sent
                    System.out.println("Receiving ACK packet from JT...");

                    if (c.primary_failed)
                        Thread.sleep(5000);
                    fromPacket = (ClientPacket) c.in.readObject();

                    if (fromPacket.msgType == ClientPacket.ACK)
                        System.out.println("Your request has been sent.");
                    else
                    {
                        System.out.println("Wrong packet type received, needed Ack");
                        System.exit(-1);
                    }
                }
                System.out.println("> What do you want to do? find_password (F) / Query progress (Q) / exit(x) ?");
                System.out.print("> ");
                 
   
            }       

            stdIn.close();

            
        } catch(Exception e) {
            System.out.println(e.getMessage());
        } 
       
    }

    
    public static String[] parseAddressFromBytes(byte[] unparsed)
    {
        String Addr = null;
        try {
            Addr = new String(unparsed, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] hostname_port = Addr.split(" ");
        return hostname_port;
    }

    //jtcreate_waits till node is created
    public void jtcreate_wait() {
        System.out.println("waiting for jobtracker node to be created...");
        nodeCreatedSignal.add(new CountDownLatch(1));
        try{       
                nodeCreatedSignal.get(i).await();
                i++;
                System.out.println("AFTER wait IS OVER " + i);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    //jtcreate_waits till JT exists and gets its hostname and port
    /*private void checkpath(boolean primary_failed) {
        
        Stat stat = null;
        System.out.println("Checking if exists and setting watch");
        stat = zkc.exists(jtPath, watcher);

        //if it exists, change the contents, otherwise we set another watch for when it exists
        

        if (stat == null) {
            System.out.println("jtcreate_waiting for jobtracker node to be created...");
            nodeCreatedSignal.add(new CountDownLatch(1));
            try{       
                    nodeCreatedSignal.get(i).ajtcreate_wait();
                    i++;
                    System.out.println("AFTER jtcreate_wait IS OVER " + i);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
        //set hostname and port
        byte[] jtAddr_bytes = zkc.getData(jtPath, watcher);
        String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
        hostname_jt = hostname_port[1];
        port_jt = Integer.parseInt(hostname_port[0]);
        System.out.println("Jobtracker exists! hostname: " + hostname_jt + " port: " + port_jt);

        if (stat == null && primary_failed)
        {
            //Came from node deleted, need to reopen socket and object streams
            try {
            c.CSocket = new Socket(c.hostname_jt, c.port_jt);
            c.out = new ObjectOutputStream(c.CSocket.getOutputStream());
            c.in = new ObjectInputStream(c.CSocket.getInputStream());  
            } catch (IOException io) {}
            primary_failed = false;
        }
    }
    */

    public Stat setWatch() {
        return (zkc.exists(jtPath, watcher));
    }
    
    private void handleEvent(WatchedEvent event) {
        // check for event type NodeCreated or NodeDataChanged
        
        if (event.getType().equals(EventType.NodeCreated))
        {
            System.out.println("Node got created after deletion...");
            //boolean exists = this.checkpath();
            //if (exists)
            //{
                System.out.println("BEFORE COUNTING DOWN " + i);
                if (!primary_failed)
                    nodeCreatedSignal.get(i).countDown();
                System.out.println(primary_failed);
                if (primary_failed)
                {
                    //set hostname and port
                    byte[] jtAddr_bytes = zkc.getData(jtPath, watcher);
                    String[] hostname_port = parseAddressFromBytes(jtAddr_bytes);
                    hostname_jt = hostname_port[1];
                    port_jt = Integer.parseInt(hostname_port[0]);
                    System.out.println("Jobtracker exists! hostname: " + hostname_jt + " port: " + port_jt);
                    try {
                        this.CSocket = new Socket(hostname_jt, port_jt);
                        out = new ObjectOutputStream(this.CSocket.getOutputStream());
                        in = new ObjectInputStream(this.CSocket.getInputStream());
                    } catch (IOException e)
                    {
                        System.out.println("IO EXCEPTION");
                        System.exit(0);
                    }
                }
                primary_failed = false;
                // verify if this is the defined znode
                //System.out.println("New Socket connection port: " + port_jt + " and hostname: " + hostname_jt );  
            //}
        }
        if (event.getType().equals(EventType.NodeDeleted))
        {
            System.out.println("NODE DELETED EVENT TYPE");
            try {
                out.close();
                in.close();
                CSocket.close();
            } catch (IOException e)
            {
                System.out.println("IO EXCEPTION");
                System.exit(0);
            }
            primary_failed = true;
            setWatch();
        }
        if (event.getType().equals(EventType.NodeDataChanged))
        {
            System.out.println("Why did anyone change data (address of JT)??");
            System.exit(-1);
        }
    }

}


