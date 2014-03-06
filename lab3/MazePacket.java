/**
 * A packet to enable transfer of information between the server and 
 * the clients.
 * @author dadlanid
 *
 */
 
import java.io.*;
import java.util.Collection;


public class MazePacket implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 *  Message type constants 
	 */
	public static final int MAZE_NULL = 0;
	public static final int ACK = 100;
	public static final int CONNECTION_REQUEST = 101;
	public static final int CONNECTION_REPLY = 102;
	public static final int MAZE_REQUEST = 103;
	public static final int MAZE_REPLY = 104;
	public static final int NEW_PLAYER = 105;
	public static final int REMOVE_PLAYER = 106;
	public static final int DISCONNECT_REQUEST = 107;
	
	/**
	 * Event constants
	 */
	 public static final int MOVE_FORWARD = 0;
     public static final int MOVE_BACKWARD = 1;
     public static final int TURN_LEFT = 2;
     public static final int TURN_RIGHT = 3;
     public static final int FIRE = 4;
	/**
	 * Message error constants 
	 */
	public static final int ERROR_INVALID_TYPE = 201;
	public static final int ERROR_PLAYER_EXISTS = 202;
	public static final int ERROR_COULD_NOT_ADD = 203;
	public static final int ERROR_PLAYER_DOES_NOT_EXIST = 204;
	public static final int ERROR_COULD_NOT_REMOVE_PLAYER = 206;
	public static final int ERROR_NULL_POINTER_SENT = 207;
	
	/**
	 * Fields in the packet 
	 */
	private Address clientInfo;
	private String name;
	private Integer event;
	private Integer seqNum;
	private Integer msgType;
	private Integer errorCode;
	public Collection<Address> remotes;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
		this.clientInfo = null;
		this.event = MAZE_NULL;
		this.seqNum = MAZE_NULL;
		this.msgType = MAZE_NULL;
		this.errorCode = MAZE_NULL;
		this.remotes = null;
	}
	
	/**
	 * Getter functions
	 * Used to get various parts of the MazePacket.
	 * @return Returns the value of the required field
	 */
	
	public Address getclientInfo() {
		return this.clientInfo;
	}
	
	public Integer getevent() {
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
	
	public String getName() {
		return this.name;
	}
	
	
	/**
	 * Setter functions
	 * Used to set particular values of the MazePacket
	 * @param Takes in the value of the required field
	 */
	public void setclientInfo(Address info) {
		this.clientInfo = info;
	}
	
	public void setevent(Integer event_) {
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
	
	public void setName(String name_) {
		this.name = name_;
	}
	
}
