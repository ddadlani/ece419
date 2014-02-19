/**
 * A packet to enable transfer of information between the server and 
 * the clients.
 * @author dadlanid
 *
 */
public class MazePacket {
	
	/**
	 *  Message type constants 
	 */
	public static int MAZE_NULL = 100;
	public static int CONNECTION_REQUEST = 101;
	public static int CONNECTION_REPLY = 102;
	public static int MAZE_REQUEST = 103;
	public static int MAZE_REPLY = 104;
	
	/**
	 * Message recipient constants 
	 */
	public static int REMOTE_CLIENT = 201;
	public static int LOCAL_CLIENT = 201;
	
	/**
	 * Fields in the packet 
	 */
	private Integer clientID;
	private ClientEvent event;
	private Integer seqNum;
	private Integer msgType;
	private Integer recipient;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
		this.clientID = MAZE_NULL;
		this.event = null;
		this.seqNum = MAZE_NULL;
		this.msgType = MAZE_NULL;
		this.recipient = MAZE_NULL;
	}
	
	/**
	 * Used to get the client ID.
	 * @return Returns an {@link Integer} which is the client ID
	 */
	public Integer getclientID() {
		return this.clientID;
	}
	
	public ClientEvent getevent() {
		return this.event;
	}
	
	public Integer getseqNum() {
		return this.seqNum;
	}
	
	public Integer getmsgType() {
		return this.msgType;
	}
	
	public Integer getRecipient() {
		return this.recipient;
	}
	
	public void setclientID(Integer cid) {
		this.clientID = cid;
	}
	
	public void setmsgType(Integer msgType) {
		this.msgType = msgType;
	}
	
	public void setRecipient(Integer recipient_) {
		this.recipient = recipient_;
	}
}