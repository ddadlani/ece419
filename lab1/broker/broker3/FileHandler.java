import java.io.*;

public class FileHandler {
	public String filename;
	public File f;
	private FileReader fr;

	public Stock stock;
	private BufferedReader br;
	private BufferedWriter bw;

	public FileHandler() {
		this.filename = new String("nasdaq");
	}

	public FileHandler(String filename) {
		this.filename = filename;
	}
	
	public Long findQuote(String symbol, String delimiter) {
		Stock stock = findSymbol(symbol, delimiter);
		return stock.getQuote();
	}

	public void addSymbol(String symbol, Long quote) throws IOException {

		try {
			FileWriter fw = new FileWriter(filename, true);
			bw = new BufferedWriter(fw);
			
			String new_record = symbol + " " + quote;
			System.out.print("Adding " + new_record + " to file.\n");

			bw.write(new_record);
			bw.newLine();

			bw.close();

		} catch (FileNotFoundException fnf) {
			System.err.println("ERROR with file");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void removeSymbol(String symbol) {
		String tempfilename = "temp.txt";
		try {
			File tempfile = new File(tempfilename);
			FileWriter tempfr = new FileWriter(tempfile);
			bw = new BufferedWriter(tempfr);

			f = new File(filename);
			fr = new FileReader(f);
			br = new BufferedReader(fr);

			String currentRecord;
			String[] currentline = new String[2];

			while ((currentRecord = br.readLine()) != null) {
				System.out.println(currentRecord);
				currentline = currentRecord.split(" ");

				if (!(symbol.equals(currentline[0]))) {
					System.out.println("Writing " + currentRecord
							+ " to temp.txt");
					bw.write(currentRecord);
					bw.newLine();
				}
			}
			File nasdaq = new File(filename);
			boolean done = tempfile.renameTo(nasdaq);
			if (!done) {
				System.err.println("Could not rename file.");
			}

			bw.close();
			br.close();

		} catch (FileNotFoundException fnf) {
			System.err.println("Could not find file.");
		} catch (IOException e) {
			System.err.println("ERROR with removal.");
			System.exit(1);
		}

	}
	
	
	// Overload removeSymbol to be called by lookup server
	public void removeSymbol(String symbol, String filename_) {
		String tempfilename = "temp2.txt";
		try {
			File tempfile = new File(tempfilename);
			FileWriter tempfr = new FileWriter(tempfile);
			bw = new BufferedWriter(tempfr);

			f = new File(filename);
			fr = new FileReader(f);
			br = new BufferedReader(fr);

			String currentRecord;
			String[] currentline = new String[2];

			while ((currentRecord = br.readLine()) != null) {
				System.out.println(currentRecord);
				currentline = currentRecord.split(" ");

				if (!(symbol.equals(currentline[0]))) {
					System.out.println("Writing " + currentRecord
							+ " to temp.txt");
					bw.write(currentRecord);
					bw.newLine();
				}
			}
			File lookupTable = new File(filename_);
			boolean done = tempfile.renameTo(lookupTable);
			if (!done) {
				System.err.println("Could not rename file.");
			}

			bw.close();
			br.close();

		} catch (FileNotFoundException fnf) {
			System.err.println("Could not find file.");
		} catch (IOException e) {
			System.err.println("ERROR with removal.");
			System.exit(1);
		}

	}

	private Stock findSymbol(String symbol, String delimiter) {
		Stock stock = new Stock();
		stock.setQuote(0L);

		String currentRecord;

		try {
			f = new File(filename);
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			while ((currentRecord = br.readLine()) != null) {
				System.out.println(currentRecord);
				String[] row_array = new String[2];
				row_array = currentRecord.split(delimiter);

				if (symbol.equals(row_array[0])) {
					System.out.println("" + row_array[0] + " found with quote " + Long.parseLong(row_array[1], 10) );
					stock.setQuote(Long.parseLong(row_array[1], 10));
					stock.setSymbol(row_array[0]);
					break;
				}
			}
			br.close();
		} catch (FileNotFoundException fnf) {
			System.err.println("Error closing file in findSymbol.");
		} catch (IOException e) {
			System.err.println("ERROR Reading file.");
			System.exit(1);
		}
		return stock;
	}
	
	public void registerBroker(String exchange, String host, Integer port) {
	
		String port_str = Integer.toString(port);
		try {
			FileWriter fw = new FileWriter(filename, true);
			bw = new BufferedWriter(fw);
			
			String new_record = exchange + " " + host + " " + port_str;
			System.out.print("Adding " + new_record + " to lookup register.\n");

			bw.write(new_record);
			bw.newLine();

			bw.close();

		} catch (FileNotFoundException fnf) {
			System.err.println("ERROR with file");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public BrokerPacket lookupBroker(String exchange) {
		BrokerPacket packet = new BrokerPacket();
		packet.exchange = exchange;
		packet.num_locations = 1;
		packet.type = BrokerPacket.LOOKUP_REPLY;
		boolean found = false;
		String currentRecord;
		System.out.println(exchange);
		
		try {
			f = new File(filename);
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			while ((currentRecord = br.readLine()) != null) {
				System.out.println(currentRecord);
				String[] row_array = new String[3];
				row_array = currentRecord.split(" ");
				System.out.println(row_array[0]);

				if (exchange.equals(row_array[0])) {
					Integer port = Integer.parseInt(row_array[2]);
					packet.locations = new BrokerLocation[packet.num_locations];
					packet.locations[0] = new BrokerLocation(row_array[1], port);
					found = true;
					break;
				}
			}
			br.close();
			if (!found)
				packet.error_code = BrokerPacket.ERROR_INVALID_EXCHANGE;
		} catch (FileNotFoundException fnf) {
			System.err.println("Error closing file in findSymbol.");
		} catch (IOException e) {
			System.err.println("ERROR Reading file.");
			System.exit(1);
		} catch (NullPointerException npe) {
			System.err.println("Segmentation fault.");
			npe.printStackTrace();
		}
		return packet;
	
	}
	
	public void clear(String filename_) {
		File tempfile = new File("temp.txt");
		f = new File(filename_);
		
		tempfile.renameTo(f);
		return;
		
	}
	
}
