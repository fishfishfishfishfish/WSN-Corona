package au.edu.usyd.corona.scheduler;


/**
 * This interface is implemented by any class that wishes to perform some action
 * (such as going to sleep) when the Scheduler sleeps. For example, the
 * {@link au.edu.usyd.corona.sensing.CPUUsageMonitor} should only be active when
 * the Scheduler is active as it uses a significant number of CPU cycles to
 * determine the usage.
 * 
 * @see Scheduler
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public interface Sleepable {
	
	/**
	 * Called when the Scheduler goes to sleep
	 */
	public void toSleep();
	
	/**
	 * Called when the Scheduler awakens
	 */
	public void wakeUp();
	
}
