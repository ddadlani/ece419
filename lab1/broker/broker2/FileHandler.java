import java.net.*;
import java.io.*;
import java.util.*;

public class FileHandler {
        public String filename;
        public File f;
        private FileReader fr;
        private FileWriter fw;
	public Stock stock;
	private BufferedReader br;
	private BufferedWriter bw;
        
        public FileHandler () {
                this.filename = new String("/homes/d/dadlanid/ece419/lab1/broker/broker1/nasdaq");
                try {
                        f = new File(filename);
                        fr = new FileReader(f);
                        fw = new FileWriter(f);
                } catch (FileNotFoundException fnf) {
                        System.err.println("ERROR: File not found!!");
                        System.exit(1);
                }
                
                
        }
        
        public Long findQuote(String symbol, String delimiter) {
                	Stock stock = this.findSymbol(symbol, delimiter);
                        return stock.getQuote();
        }
        
        public Integer addSymbol(String symbol, Long quote) throws IOException {
        	bw = new BufferedWriter(fw);
                String new_entry = symbol + " " + quote + "\n";
                System.out.print("Adding " + new_entry + "to file.\n");
                try {
             		bw.write(new_entry);
                } catch (IOException e) {
                       	System.err.println("ERROR: Could not write to file!");
                      	System.exit(1);
                }
                try {
                        bw.close();
                } catch (IOException e) {
                        System.err.println("ERROR: BW couldn't close.");
                        System.exit(1);
                }
       	
                return 0;                      
        }
        
        public removeSymbol(String symbol) {
        	String tempfilename = "temp.txt";
        	try {
        		File tempfile = new File(tempfilename);
        		Filewriter tempfr = new Filewriter(tempfile);
        		
        		br = new BufferedReader(f);
        		bw = new BufferedWriter(tempfr);
        		
        		String currentRecord;
        		String[] currentline = new String[2];
        		
        		while((currentRecord = br.readLine()) != null) {
        			currentline = currentRecord.split(delimiter);
        			
        			if (!(symbol.equals(currentline[0]))) {
                			String new_record = currentRecord + "\n";
					bw.write(new_record);
        			}
        		}
        		
                       	bw.close();
                       	br.close();
                	
       		} catch (IOException e) {
       			System.err.println("ERROR with removal.");
                        System.exit(1);
                }
       		
       	}
        
        private findSymbol(String symbol) throws IOException {
        	Stock stock = new Stock();
                stock.setQuote(0L);
                
                String currentRecord;
                br = new BufferedReader(fr);
                
                while((currentRecord = br.readLine()) != null) {
                        String[] row_array = new String[2];
                        row_array = currentRecord.split(delimiter);

			if (symbol.equals(row_array[0]))
			{
				stock.setQuote(Long.parseLong(row_array[1], 10));
				stock.setSymbol(row_array[0]);
				break;
			}
                }
                try {
                        br.close();
                } catch (IOException e) {
                        System.err.println("ERROR: BR couldn't close.");
                        System.exit(1);
                }
        }
        
        
        //@Override
       // public String toString() {
      // return name;
     // }
}
