/**
 * A packet to enable transfer of information between the server and 
 * the clients.
 * @author dadlanid
 *
 */
 
import java.io.*;
import java.util.ArrayList;


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
	public static final int MOVE_REQUEST = 103;
	public static final int DISCONNECT_REQUEST = 104;
	public static final int HEARTBEAT = 105;
	public static final int POSITION = 106;
	
	/**
	 * Event constants
	 */
	public static final int CONNECT = 201;
	public static final int MOVE_FORWARD = 202;
	public static final int MOVE_BACKWARD = 203;
	public static final int TURN_LEFT = 204;
	public static final int TURN_RIGHT = 205;
	public static final int FIRE = 206;
	public static final int DISCONNECT = 207;
	
	/**
	 * Message error constants 
	 */
	public static final int ERROR_INVALID_TYPE = 301;
	public static final int ERROR_PLAYER_EXISTS = 302;
	public static final int ERROR_COULD_NOT_ADD = 303;
	public static final int ERROR_PLAYER_DOES_NOT_EXIST = 304;
	public static final int ERROR_COULD_NOT_REMOVE_PLAYER = 306;
	public static final int ERROR_NULL_POINTER_SENT = 307;
	
	/**
	 * Fields in the packet 
	 */
	private Address clientInfo;
	private String name;
	private int clientID;
	private Integer event;
	private Double lClock;
	private Integer msgType;
	private Integer errorCode;
	public ArrayList<Address> remotes;
	private int numAcks;
	private int numposAcks;
	//private Integer score;
	//public Collection<Address> addresses;
	
	/**
	 * Default constructor
	 */
	public MazePacket() {
		this.name = null;
		this.clientID = MAZE_NULL;
		this.clientInfo = null;
		this.event = MAZE_NULL;
		this.lClock = 0.0;
		this.msgType = MAZE_NULL;
		this.errorCode = MAZE_NULL;
		this.remotes = new ArrayList<Address> ();
		this.numAcks = MAZE_NULL;
		this.numposAcks = MAZE_NULL;
		//this.score = 0;
	}
	
	public MazePacket(MazePacket toCopy) {
		this.name = toCopy.name;
		this.clientID = toCopy.getclientID();
		this.clientInfo = new Address(toCopy.clientInfo);
		this.event = toCopy.getevent();
		this.lClock = toCopy.getlamportClock();
		this.msgType = toCopy.getmsgType();
		this.errorCode = toCopy.geterrorCode();
		this.numAcks = toCopy.getnumAcks();
		this.numposAcks = toCopy.numposAcks;
		//this.score = toCopy.getScore();
		this.remotes = new ArrayList<Address> (); 
		// Deep copy of remotes;
		for(int i = 0; i < toCopy.remotes.size(); i++) {
			this.remotes.add(toCopy.remotes.get(i));
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if ((other == null)||(this == null)) {
			return false;
		}
		
		MazePacket otherPacket = (MazePacket) other;
		// If the Lamport clock is the same but the name is not, 
		// we have a problem with Lamport clocks
		if ((otherPacket.getlamportClock() == this.getlamportClock()) &&
				(otherPacket.getName().equals(this.getName()))){
			return true;
		}
		return false;
		
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
	
	public int getclientID() {
		return this.clientID;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getnumAcks() {
		return this.numAcks;
	}
	
	public int getnumposAcks(){
		return this.numposAcks;
	}
	
	//public Integer getScore() {
	//	return this.score;
	//}
	/**
	 * Setter functions
	 * Used to set particular values of the MazePacket
	 * @param Takes in the value of the required field
	 */
	public void setclientInfo(Address info) {
		this.clientInfo = new Address(info);
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
	
	public void setclientID(int cid_) {
		this.clientID = cid_;
	}
	
	public void setName(String name_) {
		this.name = name_;
	}
	
	public void setnumAcks(int numAcks_) {
		this.numAcks = numAcks_;
	}
	
	public void setnumposAcks(int numposAcks_) {
		this.numposAcks = numposAcks_;
	}
	
	public void incrementAcks() {
		this.numAcks++;
	}
	
	public void incrementposAcks() {
		this.numposAcks++;
	}
	//public void setScore(Integer score_) {
	//	this.score = score_;
	//}
	
}
