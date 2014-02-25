import java.net.*;
import java.util.*;


public class ClientExecutionThread extends Client implements Runnable {
	private Queue<MazePacket> queue;
	private Maze maze;
	public ClientExecutionThread(Mazewar mazewar, Maze maze) {
                super("ClientExecutionThread");
                this.queue = mazewar.receive_queue;
                this.maze = maze;
        }

        public void run() {
        	
        MazePacket move = new MazePacket();
		boolean local = false;
		LocalClient localClient = null;
		RemoteClient remoteClient = null;
        	while(true)
        	{
				move = queue.poll();
				if (move != null)
				{
					//Check if local
					System.out.println("IM EXECUTING!");
					try {
						if (move.getclientInfo().hostname.equals(InetAddress.getLocalHost().getHostName()))
						{
							local = true;
							Iterator i = maze.getClients();
							while (i.hasNext()) {
			                        Object o = i.next();
			                        assert(o instanceof Client);
			                        if (o instanceof LocalClient)
			                        {
			                        	localClient = (LocalClient)o;
			                        	break;
			                        }
			                } 
							
							if (localClient == null)
				            {
								System.out.println("Can't find the local client in listener queue!");
				            }
	
						}
	
						else
						{
							Iterator i = maze.getClients();
							boolean found = false;
							while (i.hasNext()) {
			                        Object o = i.next();
			                        assert(o instanceof Client);
			                        if (o instanceof RemoteClient)
			                        {
			                        	remoteClient = (RemoteClient)o;
			                        	Integer count;
			                        	for (count = 0; count < move.remotes.length; count ++)
			                        		if (remoteClient.getName().equals(move.remotes[count].name))
			                        		{	
			                        			found = true;
			                        			break;
			                        		}
			                        	if(found)
			                        		break;
			                        	else
			                        		remoteClient = null;
			                        }
							}						
							
							if (remoteClient == null)
				            {
								System.out.println("Can't find the remote client in listener queue!");
				            }
						}
						
					} catch(UnknownHostException e)
					{
					}
					
					if (move.getmsgType() == MazePacket.MAZE_REPLY)
					{
						if(local)
						{
							if (move.getevent()==(ClientEvent.moveForward))
								localClient.forward();
								//maze.moveClientForward(localClient);
							else if (move.getevent()==(ClientEvent.moveBackward))
								localClient.backup();
								//maze.moveClientBackward(localClient);
							else if (move.getevent()==(ClientEvent.turnLeft))
								localClient.turnLeft();
							//	ClientEvent ce = ClientEvent.turnLeft;
							//	maze.clientUpdate(localClient, ce);
							else if (move.getevent()==(ClientEvent.turnRight))
								localClient.turnRight();
							//	ClientEvent ce = ClientEvent.turnRight;
							//	maze.clientUpdate(localClient, ce);
							else if (move.getevent()==(ClientEvent.fire))
								localClient.fire();
						}
						else
						{
							if (move.getevent()==(ClientEvent.moveForward))
								remoteClient.forward();
								//maze.moveClientForward(localClient);
							else if (move.getevent()==(ClientEvent.moveBackward))
								remoteClient.backup();
								//maze.moveClientBackward(localClient);
							else if (move.getevent()==(ClientEvent.turnLeft))
								remoteClient.turnLeft();
							//	ClientEvent ce = ClientEvent.turnLeft;
							//	maze.clientUpdate(localClient, ce);
							else if (move.getevent()==(ClientEvent.turnRight))
								remoteClient.turnRight();
							//	ClientEvent ce = ClientEvent.turnRight;
							//	maze.clientUpdate(localClient, ce);
							else if (move.getevent()==(ClientEvent.fire))
								remoteClient.fire();
						}
					}
	
					else if (move.getmsgType() == MazePacket.MAZE_DISCONNECT)
					{
						//CHECK IF LOCAL
						if (local)
							Mazewar.quit();
							//NEED TO ADD SOCKET CLOSING STUFF!
						//REMOVE REMOTE CLIENT
						else 
							maze.removeClient(remoteClient);
					}
					else if (move.getmsgType() == MazePacket.NEW_REMOTE_CONNECTION)
					{
						//ADD REMOTE CLIENT
						remoteClient = new RemoteClient(move.getclientInfo().name);
						maze.addClient(remoteClient);
					}
				}
        	}
        }
}
