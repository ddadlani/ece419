/**
 * A packet to enable transfer of information between the server and 
 * the clients.
 * @author dadlanid
 *
 */
public class MovePacket {
	
	/* Message type Constants */
	public static int MOVE_NULL = 0;
	public static int CONNECTION_REQUEST = 1;
	public static int CONNECTION_REPLY = 2;
	public static int MOVE_REQUEST = 3;
	public static int MOVE_REPLY = 4;
	
	/* Fields in the packet */
	@SuppressWarnings("unused")
	private Integer clientID;
	@SuppressWarnings("unused")
	private ClientEvent event;
	@SuppressWarnings("unused")
	private Integer seqNum;
	@SuppressWarnings("unused")
	private Integer msgType;
	
	/* Constructor */
	public MovePacket() {
		this.clientID = MOVE_NULL;
		this.event = null;
		this.seqNum = MOVE_NULL;
		this.msgType = MOVE_NULL;
	}
	
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
	
	public void setclientID(Integer cid) {
		this.clientID = cid;
	}
	
	public void setmsgType(Integer msgType) {
		this.msgType = msgType;
	}
	
}