package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.scheduler.Sleepable;
import au.edu.usyd.corona.util.Logger;

/**
 * This class provides a mechanism to monitor how much the CPU is loaded at any
 * given point in time. It periodically checks the load on the CPU and when
 * asked for the current load, returns an average of the last N recordings
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class CPUUsageMonitor implements Sleepable, Runnable {
	/** the number of i++ counts to keep */
	public static final int N_COUNTS = 5;
	/** the number of i++ loops used to provide an average max calibration value */
	public static final int CALIBRATION_COUNT = 10;
	
	public static final int INTERVAL_SLEEP = 100;
	
	private static CPUUsageMonitor instance; // singleton instance
	
	private final int maxCountsIn25ms;
	private final int counts[]; // storage for the last {@link #N_COUNTS} counts
	private int index = 0; // current index into counts storage array
	private boolean doCounting = false; // whether or not we should still be counting
	private final Object mutex = new Object();
	
	private final LoopBreaker breaker;
	
	private boolean asleep;
	private long startSleepTime;
	
	private CPUUsageMonitor() {
		asleep = true;
		
		breaker = new LoopBreaker();
		breaker.start();
		
		int sum = 0;
		for (int i = 0; i != CALIBRATION_COUNT; i++) {
			doCounting = true;
			synchronized (breaker) {
				breaker.notifyAll();
			}
			while (doCounting) {
				sum++;
				Thread.yield();
			}
		}
		maxCountsIn25ms = sum / CALIBRATION_COUNT;
		Logger.logDebug("CPU usage maximum counter calibrated to " + maxCountsIn25ms);
		
		counts = new int[N_COUNTS];
	}
	
	/**
	 * Initializes the CPU Usage monitor if it is not already initialized. If so,
	 * this method does nothing
	 */
	public static void initialize() {
		// creates a new instance
		instance = new CPUUsageMonitor();
		new Thread(instance, "CPUMonitor").start();
		
		// registers with the scheduler to make it sleep when the scheduler sleeps
		Scheduler.getInstance().addSleepable(instance);
	}
	
	public static CPUUsageMonitor getInstance() {
		return instance;
	}
	
	/**
	 * Invokes periodically an i++ counter in order to provide an estimate as to
	 * the CPU usage at the current point in time
	 */
	public void run() {
		int count;
		while (true) {
			if (asleep) {
				startSleepTime = System.currentTimeMillis();
				synchronized (this) {
					try {
						wait();
					}
					catch (InterruptedException e) {
					}
				}
			}
			
			count = 0;
			doCounting = true;
			synchronized (breaker) {
				breaker.notifyAll();
			}
			while (doCounting) {
				count++;
				Thread.yield();
			}
			
			synchronized (mutex) {
				counts[index] = count;
				index = (index + 1) % N_COUNTS;
			}
			try {
				Thread.sleep(INTERVAL_SLEEP);
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * This method will return the CPU usage of the system at the current point
	 * in time as an integer percentage (range [0,100])
	 * 
	 * @return approximate CPU usage percentage
	 */
	public int getUsage() {
		int sum = 0, n, denom = 0;
		synchronized (mutex) {
			for (int i = 0; i < N_COUNTS; i++) {
				n = Math.max(0, 100 - (counts[(index - 1 - i + N_COUNTS) % N_COUNTS] * 100) / maxCountsIn25ms);
				sum += n * (N_COUNTS - i);
				denom += N_COUNTS - i;
			}
		}
		return sum / denom;
	}
	
	/**
	 * This helper class acts as the alarm for the 25ms period of incrementing
	 * 
	 * @author Tim Dawborn
	 */
	private class LoopBreaker extends Thread {
		public void run() {
			while (true) {
				try {
					Thread.sleep(25);
				}
				catch (InterruptedException e) {
				}
				synchronized (this) {
					try {
						doCounting = false;
						wait();
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void toSleep() {
		asleep = true;
		startSleepTime = System.currentTimeMillis();
	}
	
	public void wakeUp() {
		// make the state not asleep and zero the CPU usage counts
		asleep = false;
		long delta = (System.currentTimeMillis() - startSleepTime);
		int n = (int) (delta / INTERVAL_SLEEP);
		if (n > N_COUNTS) // ensure n is a valid value 
			n = N_COUNTS;
		else if (n < 0)
			n = 0;
		
		// Blank out old results (except the first one)
		synchronized (mutex) {
			for (int i = 1; i < n; i++)
				counts[(index - i - 1 + N_COUNTS) % N_COUNTS] = maxCountsIn25ms;
		}
		
		// wake ourself up
		synchronized (this) {
			notifyAll();
		}
	}
}
