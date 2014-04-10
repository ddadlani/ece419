
import java.io.Serializable;

public class Task implements Serializable{

	public String hash;
	public String word;
	public int first_partition;
	public int last_partition;
	public int result;

	/*
	 * Constants
	 */
	public static final int IN_PROGRESS = 2;
	public static final int NOT_STARTED = -1;
	public static final int NOT_FOUND = 0;
	public static final int FOUND = 1;

	public Task(String hash_, int start_p, int end_p)
	{
		hash = hash_;
		first_partition = start_p;
		last_partition = end_p;
		result = Task.NOT_STARTED;
		word = null;
	}

	public Task(Task t) {
		hash = t.hash;
		word = t.word;
		first_partition = t.first_partition;
		last_partition = t.last_partition;
		result = t.result;
	}
}
