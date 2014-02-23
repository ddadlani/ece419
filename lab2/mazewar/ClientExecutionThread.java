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
		GUIClient localClient = null;
		RemoteClient remoteClient = null;
        	while(true)
        	{
			move = queue.poll();
			if (move != null)
			{
				//Check if local
				try {
					if (move.getclientInfo().hostname == InetAddress.getLocalHost().getHostName())
					{
						local = true;
						localClient = new GUIClient(move.getclientInfo().name); //WEIRD MAYBE!
					}
				
					else
					{
						remoteClient = new RemoteClient(move.getclientInfo().name);
					}
				}catch(UnknownHostException e)
				{
				}
				if (move.getmsgType() == MazePacket.MAZE_REPLY)
				{
					if(local)
					{
						if (move.getevent() == ClientEvent.moveForward)
							maze.moveClientForward(localClient);
						else if (move.getevent() == ClientEvent.moveBackward)
							maze.moveClientBackward(localClient);
						/*else if (move.getevent() == ClientEvent.turnLeft)
						{
							ClientEvent ce = ClientEvent.turnLeft;
							maze.clientUpdate(localClient, ce);
						}
						else if (move.getevent() == ClientEvent.turnRight)
						{
							ClientEvent ce = ClientEvent.turnRight;
							maze.clientUpdate(localClient, ce);
						} */
						else if (move.getevent() == ClientEvent.fire)
							maze.clientFire(localClient);
					}
					else
					{
						if (move.getevent() == ClientEvent.moveForward)
							maze.moveClientForward(remoteClient);
						else if (move.getevent() == ClientEvent.moveBackward)
							maze.moveClientBackward(remoteClient);
						/*else if (move.getevent() == ClientEvent.turnLeft)
						{
							ClientEvent ce = ClientEvent.turnLeft;
							maze.clientUpdate(remoteClient, ce);           
						}
						else if (move.getevent() == ClientEvent.turnRight)
						{
							ClientEvent ce = ClientEvent.turnRight;
							maze.clientUpdate(remoteClient, ce);
						} */
						else if (move.getevent() == ClientEvent.fire)
							maze.clientFire(remoteClient);
					}
				}
				
				
			
				else if (move.getmsgType() == MazePacket.MAZE_DISCONNECT)
				{
					//CHECK IF LOCAL
					if (local)
						Mazewar.quit();
					//REMOVE REMOTE CLIENT
					else 
						maze.removeClient(remoteClient);
				}
				else if (move.getmsgType() == MazePacket.NEW_REMOTE_CONNECTION)
				{
					//ADD REMOTE CLIENT
					maze.addClient(remoteClient);
				}
			}
        	}
        }
}
