import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Worker {
	public Worker() {
		
	}
	
	public static void main (String[] args) {
		// connect to zookeeper
		// get fileserver address
		// put a watch on fileserver so can ask for backup if fileserver dies
		// create a sequential ephemeral node at path /workers/worker for worker
		// look in jobs/ list it or sth to find the same x as you have in workerx, put a watch on that
		// put a watch on 
	}
	
	public static String getHash(String word) {
        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
        	// ignore
	    }
	        return hash;
	    }
}