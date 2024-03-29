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
	public static final int MAZE_NULL = 0;
	public static final int CONNECTION_REQUEST = 100;
	public static final int CONNECTION_REPLY = 101;
	public static final int MAZE_REQUEST = 102;
	public static final int MAZE_REPLY = 103;
	public static final int NEW_REMOTE_CONNECTION = 104;
	public static final int MAZE_DISCONNECT = 105;
	
	/**
	 * Message error constants 
	 */
	public static final int ERROR_INVALID_TYPE = 201;
	
	/**
	 * Fields in the packet 
	 */
	private Address clientInfo;
	private ClientEvent event;
	private Integer seqNum;
	private Integer msgType;
	private Integer errorCode;
	public Address[] remotes;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
		this.clientInfo = null;
		this.event = null;
		this.seqNum = MAZE_NULL;
		this.msgType = MAZE_NULL;
		this.errorCode = MAZE_NULL;
		this.remotes = null;
	}
	
/*	public MazePacket(MazePacket toCopy) {
		this.clientInfo = new Address(toCopy.clientInfo);
		this.event = toCopy.event;
		this.seqNum = toCopy.seqNum;
		this.msgType = toCopy.msgType;
		this.errorCode = toCopy.errorCode;
		
		for(int i = 0; i < toCopy.remotes.size(); i++) {
			
		}
	} */
	
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
	
	public Integer geterrorCode() {
		return this.errorCode;
	}
	
	public Integer getclientID() {
		return this.clientInfo.id;
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
	
	public void seterrorCode(Integer error_) {
		this.errorCode = error_;
	}
	
	public void setclientID(Integer cid_) {
		this.clientInfo.id = cid_;
	}
	
}