import java.io.IOException;
import java.net.ServerSocket;

import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

public class FileServer {

	String myPath = "/primary";
	ZkConnector zkc;
	Watcher watcher;
	ServerSocket listenSocket;

	public static void main(String[] args) {

		if (args.length != 1) {
			System.out
					.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Test zkServer:clientPort");
			return;
		}
		// TAKE IN FILE NAME TOO?
		FileServer fs = new FileServer(args[0]);

		/*
		 * System.out.println("Sleeping..."); try { Thread.sleep(5000); } catch
		 * (Exception e) {}
		 */
		boolean primary = fs.checkpath();

		if (primary) {

			// System.out.println("Sleeping...");
			// while (true) {
			// try{ Thread.sleep(5000); } catch (Exception e) {}
			// }

			// Now that you are primary, do FileServer stuff

			// Get the partition id from workers, send back partitions to
			// workers

		}
	}

	public FileServer(String hosts) {
		// Start listening port

		try {
			listenSocket = new ServerSocket(0);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Load the file into memory?

		// Election of primary
		zkc = new ZkConnector();
		try {
			zkc.connect(hosts);
		} catch (Exception e) {
			System.out.println("Zookeeper connect " + e.getMessage());
		}

		watcher = new Watcher() { // Anonymous Watcher
			@Override
			public void process(WatchedEvent event) {
				handleEvent(event);

			}
		};

	}

	private boolean checkpath() {
		Stat stat = zkc.exists(myPath, watcher);
		if (stat == null) { // znode doesn't exist; let's try creating it
			System.out.println("Creating " + myPath);
			Code ret = zkc.create(myPath, // Path of znode
					null, // Data not needed.
					CreateMode.EPHEMERAL // Znode type, set to EPHEMERAL.
					);
			if (ret == Code.OK) {
				System.out.println("the boss!");
				String listenPort = String.valueOf(listenSocket.getLocalPort());
				String listenAddress = listenSocket.getInetAddress().getHostName();
				String addr = listenPort + " " + listenAddress;
				byte[] data = addr.getBytes();
				try {
					zkc.setData(myPath, data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}

	private void handleEvent(WatchedEvent event) {
		String path = event.getPath();
		EventType type = event.getType();
		if (path.equalsIgnoreCase(myPath)) {
			if (type == EventType.NodeDeleted) {
				System.out.println(myPath + " deleted! Let's go!");
				checkpath(); // try to become the boss
			}
			if (type == EventType.NodeCreated) {
				System.out.println(myPath + " created!");
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}
				checkpath(); // re-enable the watch
			}
		}
	}
}