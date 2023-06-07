package au.edu.usyd.corona.scheduler;


/**
 * This exception is thrown if a Task is not found in the scheduler when
 * requested.
 * 
 * @author Raymes Khoury
 */
public class TaskNotFoundException extends Exception {
	
	public TaskNotFoundException(String string) {
		super(string);
	}
}
