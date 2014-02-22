import java.net.*;
import java.io.*;
import java.util.*;


public class ClientExecutionThread extends Thread {
	private Queue<MazePacket> queue;
	
	public ClientExecutionThread(Queue<MazePacket> queue) {
                super("ClientExecutionThread");
                this.queue = queue;
        }

        public void run() {

        	MazePacket move = new MazePacket();
        	while(true)
        	{
			move = queue.remove();
			if (move.getmsgType() == MazePacket.MAZE_REPLY)
			{
				if (move.getevent() == ClientEvent.moveForward)
					forward();
				else if (move.getevent() == ClientEvent.moveBackward)
					backup();
				else if (move.getevent() == ClientEvent.turnLeft)
					turnLeft();
				else if (move.getevent() == ClientEvent.turnRight)
					turnRight();
				else if (move.getevent() == ClientEvent.fire)
					fire();
			}
				
			else if (packetFromServer.getmsgType() == MazePacket.MAZE_DISCONNECT)
			{
				//CHECK IF LOCAL
				if (packetFromServer.getclientInfo().hostname == InetAddress.getLocalHost().getHostName())
					Mazewar.quit();
				//REMOVE REMOTE CLIENT
				else {}
			}
			else if (packetFromServer.getmsgType() == MazePacket.NEW_REMOTE_CONNECTION)
			{
				//ADD REMOTE CLIENT
			}
        	}
        }
}
