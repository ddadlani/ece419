import java.io.Serializable;
import java.util.ArrayList;

public class JobNode implements Serializable{

	public ArrayList <Task> workerJobs;
	public String workerPath;

	public JobNode (String workerPath_) {
		workerJobs = new ArrayList <Task> ();
		workerPath = workerPath_;
	}

	//JT Handler calls to add 
	public void addTask (String hash_, int start_p, int end_p) {
		workerJobs.add(new Task(hash_, start_p, end_p));
	}

	//Worker calls to get latest task that needs to be processed
	public Task getFirstIncompleteTask () {
		for (int i = 0; i < workerJobs.size(); i++)
		{
			Task t = new Task(workerJobs.get(i));
			if (t.result == Task.IN_PROGRESS)
				return t;
		}	
		return null;
	} 

	//JobTracker calls to get task with given hash when Queried about Progress.
	public Task getTask (String hash_) {
		for (int i = 0; i < workerJobs.size(); i++)
		{
			Task t = new Task(workerJobs.get(i));
			if (t.hash.equals(hash_))
				return t;
		}	
		return null;
	}

}