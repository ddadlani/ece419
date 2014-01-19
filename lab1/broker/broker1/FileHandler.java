import java.net.*;
import java.io.*;
import java.util.*;

public class FileHandler {
        public String filename;
        //public ArrayList<Stock> rows;
        public File f;
        public FileReader fr;
	public Stock stock;
        
        public FileHandler () {
                this.filename = new String("/homes/d/dadlanid/ece419/lab1/broker/broker1/nasdaq");
        }
        
        public Long findQuote(String symbol, String delimiter) throws FileNotFoundException,
                                        IOException {
		// ArrayList<Stock> rows = new ArrayList<Stock>();
                Stock stock = new Stock();
                stock.setQuote(0L);
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
                        
                        //System.out.println(row_array[0]);
                        //System.out.println(row_array[1]);
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
                        System.err.println("ERROR: Couldn't close.");
                        System.exit(1);
                }
                         return stock.getQuote();
        }
        
        
        //@Override
       // public String toString() {
      // return name;
     // }
}
