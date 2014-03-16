/**
 * A packet to enable transfer of information between the server and 
 * the clients.
 * @author dadlanid
 *
 */
 
import java.io.*;


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
	public static final int LOOKUP_REQUEST = 102;
	public static final int MAZE_REQUEST = 103;
	public static final int DISCONNECT_REQUEST = 104;
	public static final int HEARTBEAT = 105;
	
	/**
	 * Event constants
	 */
	public static final int CONNECT = 0;
	public static final int MOVE_FORWARD = 1;
	public static final int MOVE_BACKWARD = 2;
	public static final int TURN_LEFT = 3;
	public static final int TURN_RIGHT = 4;
	public static final int FIRE = 5;
	public static final int DISCONNECT = 6;
	
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
	//private String name;
	private Integer clientID;
	private Integer event;
	private Double lClock;
	private Integer msgType;
	private Integer errorCode;
	public Address[] remotes;
	private Integer numAcks;
	//public Collection<Address> addresses;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
	//	this.name = null;
		this.clientID = MAZE_NULL;
		this.clientInfo = null;
		this.event = MAZE_NULL;
		this.lClock = 0.0;
		this.msgType = MAZE_NULL;
		this.errorCode = MAZE_NULL;
		this.remotes = null;
		this.numAcks = MAZE_NULL;
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
	
	public Double getlamportClock() {
		return this.lClock;
	}
	
	public Integer getmsgType() {
		return this.msgType;
	}
	
	public Integer geterrorCode() {
		return this.errorCode;
	}
	
	public Integer getclientID() {
		return this.clientID;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Integer getnumAcks() {
		return this.numAcks;
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
	
	public void setlamportClock(Double clock_) {
		this.lClock = clock_;
	}
	
	public void setmsgType(Integer msgType) {
		this.msgType = msgType;
	}
	
	public void seterrorCode(Integer error_) {
		this.errorCode = error_;
	}
	
	public void setclientID(Integer cid_) {
		this.clientID = cid_;
	}
	
	public void setName(String name_) {
		this.name = name_;
	}
	
	public void getnumAcks(Integer numAcks_) {
		this.numAcks = numAcks_;
	}
	
}
