import java.net.*;
import java.io.*;
import java.util.*;

public class Address implements Serializable {
	/* Identification parameters */
	public String name;
	public Integer id;
	
	/* Connection parameters */
	public String hostname;
	public Integer port;
	
	
	/* Constructor */
	public Address() {
		this.name = null;
		this.id = 0;
		this.hostname = null;
		this.port = 0;
	}
	
	/* Copy constructor */
	public Address(Address toCopy) {
		this.name = toCopy.name;
		this.id = toCopy.id;
		this.hostname = toCopy.hostname;
		this.port = toCopy.port;
	}
}
