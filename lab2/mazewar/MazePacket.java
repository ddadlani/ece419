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
	public static int NEW_REMOTE_CONNECTION = 105;
	public static int MAZE_DISCONNECT = 106;
	
	/**
	 * Message recipient constants 
	 */
	public static int REMOTE_CLIENT = 201;
	public static int LOCAL_CLIENT = 202;
	
	/**
	 * Fields in the packet 
	 */
	private Address clientInfo;
	private ClientEvent event;
	private Integer seqNum;
	private Integer msgType;
	private Integer recipient;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
		this.clientInfo = null;
		this.event = null;
		this.seqNum = MAZE_NULL;
		this.msgType = MAZE_NULL;
		this.recipient = MAZE_NULL;
	}
	
	/**
	 * Getter functions
	 * Used to get various parts of the MazePacket.
	 * @return Returns the value of the required field
	 */
	
	public Address getclientInfo() {
		return this.clientInfo;
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
	
	public Integer getrecipient() {
		return this.recipient;
	}
	
	
	/**
	 * Setter functions
	 * Used to set particular values of the MazePacket
	 * @param Takes in the value of the required field
	 */
	public void setclientInfo(Address info) {
		this.clientInfo = info;
	}
	
	public void setevent(ClientEvent event_) {
		this.event = event_;
	}
	
	public void setseqNum(Integer seq_) {
		this.seqNum = seq_;
	}
	
	public void setmsgType(Integer msgType) {
		this.msgType = msgType;
	}
	
	public void setRecipient(Integer recipient_) {
		this.recipient = recipient_;
	}
}