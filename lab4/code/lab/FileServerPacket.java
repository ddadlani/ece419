import java.util.ArrayList;


public class FileServerPacket extends Packet {

	public FileServerPacket() {
		type = NULL_VALUE;
		error = NULL_VALUE;
		value = NULL_VALUE;
		partition = null;
	}
	public Integer type;
	public Integer error;
	public Integer value;
	public ArrayList<String> partition;
	
}
