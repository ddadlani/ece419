import java.io.Serializable;


public class ClientPacket extends Packet implements Serializable {

	public String hash;
	public String word;
	public int msgType;
	public boolean finish;
	
	
	public ClientPacket()
	{
		msgType = Packet.NULL_VALUE;
		finish = false;
	}
	
}
