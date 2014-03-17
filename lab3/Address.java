import java.io.*;

public class Address implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* Identification parameters */
	public String name;
	//public Integer id;
	
	/* Connection parameters */
	public String hostname;
	public Integer port;
	
	/* Position parameters */
	public Point position;
	public Direction orientation;
	
	
	/* Constructor */
	public Address() {
	//	this.name = null;
	//	this.id = 0;
		this.hostname = null;
		this.port = 0;
	}
	
	/* Copy constructor */
	public Address(Address toCopy) {
	//	this.name = toCopy.name;
	//	this.id = toCopy.id;
		this.hostname = toCopy.hostname;
		this.port = toCopy.port;
		this.position = toCopy.position;
		this.orientation = toCopy.orientation;
	}

	
	public boolean equals(Address other) {
		if ((other == null)||(this == null))
			return false;
		
		else if((this.name == other.name)) 
			return true;
		//   (this.id == other.id) &&
		else if((this.hostname == other.hostname) &&
		   (this.port == other.port)) {
			return true;
		}
		return false;	
	}
}
