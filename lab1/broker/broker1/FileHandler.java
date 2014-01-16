import java.net.*;
import java.io.*;
import java.util.*;

public class FileHandler {
	public String filename;
	public ArrayList<Stock> rows;
	public File f;
	public FileReader fr;
	
	public FileHandler () {
		this.filename = new String("/homes/d/dadlanid/ece419/lab1/broker/broker1/nasdaq");
	}
	
	public ArrayList<Stock> parseDelimitedFile(String delimiter) throws FileNotFoundException,
					IOException {
		ArrayList<Stock> rows = new ArrayList<Stock>();
		
		try {
			f = new File(filename);
			fr = new FileReader(f);
		} catch (FileNotFoundException fnf) {
			System.err.println("ERROR: File not found!!");
			System.exit(1);
		}
		BufferedReader br = new BufferedReader(fr);
		String currentRecord;
		while((currentRecord = br.readLine()) != null) {
			String[] row_array = new String[2];
			row_array = currentRecord.split(delimiter);
			Stock new_entry = new Stock(
			rows.add(row_array[0]);
			System.out.println(row_array[0]);
			rows.add(row_array[1]);
			System.out.println(row_array[1]);
		}
		try {
			br.close();
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't close.");
			System.exit(1);
		}
		  	return rows;
	}
	
	//@Override
       // public String toString() {
      //      return name;
     //   }
}
