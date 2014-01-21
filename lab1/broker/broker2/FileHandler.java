import java.io.*;

public class FileHandler {
	public String filename;
	public File f;
	private FileReader fr;
    
	public Stock stock;
	private BufferedReader br;
	private BufferedWriter bw;
        
        public FileHandler () {
                this.filename = new String("nasdaq");
                try {
                        f = new File(filename);
                        fr = new FileReader(f);
                        br = new BufferedReader(fr);
                } catch (FileNotFoundException fnf) {
                        System.err.println("ERROR: File not found!!");
                        System.exit(1);
                }                 
                
        }
        
        public Long findQuote(String symbol, String delimiter) {
                	Stock stock = findSymbol(symbol, delimiter);
                        return stock.getQuote();
        }
        
        public Integer addSymbol(String symbol, Long quote) throws IOException {
        	
        	try {
        		//File tempfile = new File(tempfilename);
        		FileWriter fw = new FileWriter(filename, true);
        		bw = new BufferedWriter(fw);
        	
        		String new_entry = symbol + " " + quote + "\n";
        		System.out.print("Adding " + new_entry + "to file.\n");
        	
        		bw.write(new_entry);
                	
               
                bw.close();
               
        	} catch (FileNotFoundException fnf) {
        		System.err.println("ERROR with file");
        		System.exit(1);
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        	return 0;                      
        }
        
        public void removeSymbol(String symbol) {
        	String tempfilename = "temp.txt";
        	try {
        		File tempfile = new File(tempfilename);
        		FileWriter tempfr = new FileWriter(tempfile);
        		
        		
        		bw = new BufferedWriter(tempfr);
        		
        		String currentRecord;
        		String[] currentline = new String[2];
        		
        		while((currentRecord = br.readLine()) != null) {
        			System.out.println(currentRecord);
        			currentline = currentRecord.split(" ");
        			
        			if (!(symbol.equals(currentline[0]))) {
                			String new_record = currentRecord + "\n";
                			System.out.println("Writing " + currentRecord + " to temp.txt");
                			bw.write(new_record);
        			}
        		}
        		File nasdaq = new File(filename);
                boolean done = tempfile.renameTo(nasdaq);
                if (!done) {
                  	System.err.println("Could not rename file.");
                }
        		
                bw.close();
                bw = null;
                               	
       		} catch (IOException e) {
       			System.err.println("ERROR with removal.");
                        System.exit(1);
                }
       		
       	}
        
        private Stock findSymbol(String symbol, String delimiter) {
        	Stock stock = new Stock();
                stock.setQuote(0L);
                
                String currentRecord;
                br = new BufferedReader(fr);
                try {
                	while((currentRecord = br.readLine()) != null) {
                		System.out.println(currentRecord);
                        	String[] row_array = new String[2];
                        	row_array = currentRecord.split(delimiter);

                        	if (symbol.equals(row_array[0]))
                        	{
                        		stock.setQuote(Long.parseLong(row_array[1], 10));
                        		stock.setSymbol(row_array[0]);
                        		break;
                        	}
                	}
                
                } catch (IOException e) {
                        System.err.println("ERROR Reading file.");
                        System.exit(1);
                }
                return stock;
        }
        
        public void close() {
        	try {
        		br.close();
        	} catch (IOException e) {
        		System.out.println("Error closing file.");
        		System.exit(1);
        	} finally {
        		fr = null;
        		br = null;
        		f = null;
        	}
        }
        

}
