import java.io.Serializable;
import java.util.ArrayList;

public class JobNode implements Serializable{

	public ArrayList <Task> workerJobs;

	public JobNode () {
		workerJobs = new ArrayList <Task> ();
	}

	//JT Handler calls to add 
	public void addTask (String hash_, int start_p, int end_p) {
		workerJobs.add(new Task(hash_, start_p, end_p));
	}

	//JT Handler calls when reassigning tasks
	public void reassignTask(Task t) {
		workerJobs.add(new Task(t));
	}

	//Worker calls to get latest task that needs to be processed
	public Task getFirstIncompleteTask () {
		for (int i = 0; i < workerJobs.size(); i++)
		{
			Task t = new Task(workerJobs.get(i));
			if (t.result == Task.NOT_STARTED)
				return t;
		}	
		return null;
	} 

	//JobTracker calls to get tasks with given hash when Queried about Progress.
	public ArrayList<Task> getTasks (String hash_) {

		ArrayList<Task> t = new ArrayList<Task> ();
		for (int i = 0; i < workerJobs.size(); i++)
		{
			Task task = new Task(workerJobs.get(i));
			if (task.hash.equals(hash_))
				t.add(task);
		}	
		return t;
	}

	//worker calls to find index of first task with given hash
	public int getFirstTaskIndex (String hash_) {
		for (int i = 0; i < workerJobs.size(); i++)
		{
			Task t = new Task(workerJobs.get(i));
			if (t.hash.equals(hash_))
				return i;
		}	
		return -1;
		
	}

}