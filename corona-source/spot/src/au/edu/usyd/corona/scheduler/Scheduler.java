package au.edu.usyd.corona.scheduler;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import au.edu.usyd.corona.collections.PriorityBlockingQueue;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.NetworkListener;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class manages the execution and scheduling of tasks depending on their
 * given scheduled time. This thread sleeps while waiting for the time that the
 * next task is to be executed. It also sleeps while there are no tasks left to
 * be executed at any point in time.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public class Scheduler implements Runnable, NetworkListener {
	protected final PriorityBlockingQueue tasksQueue; // The priority queue structure to store the tasks
	protected final Hashtable tasksTable; // A hash table to keep track of tasks
	protected final QueryToTaskTable queriesTable; // Hash table mapping queryID to a hashtable of tasks related to that query
	protected final Vector sleepable; // A vector of Sleepable objects.  They are notified of the Schedulers intention to go to sleep or wake up so they can react appropriately
	
	protected static Scheduler instance;
	protected static Thread thread;
	
	/**
	 * Constructor for the scheduler
	 * 
	 */
	protected Scheduler() {
		tasksQueue = new PriorityBlockingQueue();
		tasksTable = new Hashtable();
		queriesTable = new QueryToTaskTable();
		sleepable = new Vector();
		Network.getInstance().registerListener(this);
		
	}
	
	/**
	 * Initializes the scheduler's heap size with the maximum number of items
	 * that it can store at one point in time. This needs to be called before any
	 * references to the singleton instance is obtained
	 * 
	 */
	public static void initialize() {
		if (instance == null) {
			(thread = new Thread(instance = new Scheduler(), "Scheduler")).start();
			Logger.logDebug("Scheduler initialized");
		}
	}
	
	/**
	 * Returns the singleton instance of the scheduler
	 * 
	 * @return singleton instance
	 */
	public static Scheduler getInstance() {
		initialize();
		return instance;
	}
	
	/**
	 * Returns the time difference in milliseconds between the given time and the
	 * TimeKeeper's current time
	 * 
	 * @param time time to calculate distance from
	 * @return (time - Current Time)
	 */
	private synchronized static long calculateTimeDelta(long time) {
		if (Network.NETWORK_MODE == Network.MODE_UNITTEST)
			return time - System.currentTimeMillis();
		else
			return time - TimeSync.getInstance().getTime();
	}
	
	/**
	 * Adds a SchedulableTask to the scheduler
	 * 
	 * @param task the Task to add to the scheduler
	 */
	public synchronized void addTask(SchedulableTask task) {
		// makes sure its not a duplicate recieve
		if (tasksTable.containsKey(task.getTaskId()))
			return;
		_addTask(task);
	}
	
	/**
	 * Call this from an addTask method only once its guarenteed that the task is
	 * not already in the scheduler
	 * 
	 * @param task
	 */
	protected synchronized final void _addTask(SchedulableTask task) {
		task.setStatus(TaskDetails.STATUS_SUBMITTED);
		tasksTable.put(task.getTaskId(), task);
		queriesTable.addTask(task);
		tasksQueue.add(task);
		synchronized (this) {
			notifyAll();
		}
	}
	
	/**
	 * Kill a query. This is achieved by removing all Tasks related to the query
	 * from the scheduler
	 * 
	 * @param queryID The ID of the query to kill
	 */
	public synchronized void killQuery(int queryID) {
		if (queriesTable.containsQuery(queryID)) {
			Enumeration killTasks = queriesTable.getTasks(queryID);
			while (killTasks.hasMoreElements()) {
				SchedulableTask kill = (SchedulableTask) killTasks.nextElement();
				kill.setStatus(TaskDetails.STATUS_KILLED);
				removeTask(kill);
			}
		}
	}
	
	/**
	 * Remove a task from the Scheduler
	 * 
	 * @param task The Task to remove from the Scheduler
	 */
	protected synchronized void removeTask(SchedulableTask task) {
		tasksQueue.remove(task);
		tasksTable.remove(task.getTaskId());
		queriesTable.removeTask(task.getTaskId());
		task.deconstruct();
		if (task.getStatus() != TaskDetails.STATUS_KILLED)
			task.setStatus(TaskDetails.STATUS_COMPLETE);
	}
	
	/**
	 * Reschedule the given task in the Scheduler
	 * 
	 * @param task The Task to reschedule
	 */
	protected synchronized void reschedule(SchedulableTask task) {
		if (!tasksTable.containsKey(task.getTaskId()))
			return;
		task.reschedule();
		tasksQueue.add(task);
		synchronized (this) {
			notifyAll();
		}
	}
	
	/**
	 * Returns true if the Scheduler contains a task with the given ID
	 * 
	 * @param taskID The ID of the Task to check for
	 * @return True if the task is contained in the Scheduler
	 */
	public synchronized boolean containsTask(TaskID taskID) {
		return tasksTable.containsKey(taskID);
	}
	
	/**
	 * Returns true of the Scheduler contains a task related to a query with the
	 * given ID
	 * 
	 * @param queryID the ID of the query to find related tasks of
	 * @return true of the Scheduler contains a task of the given query ID
	 */
	public synchronized boolean containsQuery(int queryID) {
		return queriesTable.containsQuery(queryID);
	}
	
	/**
	 * Retrieve a Task with a given ID
	 * 
	 * @param taskID The ID of the Task to retrieve
	 * @return The task with the given ID or null if it does not exist
	 * @throws TaskNotFoundException If the task is not found in the scheduler
	 */
	public synchronized SchedulableTask getTask(TaskID taskID) throws TaskNotFoundException {
		SchedulableTask res = (SchedulableTask) tasksTable.get(taskID);
		if (res == null)
			throw new TaskNotFoundException("Task " + taskID + " not found in Scheduler.");
		return res;
	}
	
	/**
	 * This method is executed when the scheduling of tasks is to begin. It
	 * creates an eternal loop, and then performs the appropriate tasks based on
	 * what tasks are currently in the scheduler.
	 */
	public void run() {
		SchedulableTask currentTask = null;
		while (true) {
			// inner loop to catch notifyAll() calls on the wait() when a new task
			// is added
			while (true) {
				toSleep();
				currentTask = (SchedulableTask) tasksQueue.peekBlocking();
				wakeUp();
				
				// Sleep thread until next task is due
				final long timeDelta = calculateTimeDelta(currentTask.getExecutionTime());
				if (timeDelta > 0) {
					
					synchronized (this) {
						toSleep();
						try {
							wait(timeDelta);
						}
						catch (InterruptedException e) {
						}
						wakeUp();
					}
				}
				else
					break;
			}
			
			// actually remove the next task from the heap
			currentTask = (SchedulableTask) tasksQueue.poll();
			
			// If the task has been killed, don't execute it
			if (tasksTable.get(currentTask.getTaskId()) == null) {
				continue;
			}
			
			// performs all the relational algebra such as merging in a new thread
			new TaskExecutor(currentTask).start();
		}
	}
	
	/**
	 * Wakeup all registered Sleepables
	 */
	private void wakeUp() {
		for (Enumeration e = sleepable.elements(); e.hasMoreElements();) {
			((Sleepable) e.nextElement()).wakeUp();
		}
		
	}
	
	/**
	 * Sleep all registered Sleepables
	 */
	private void toSleep() {
		for (Enumeration e = sleepable.elements(); e.hasMoreElements();) {
			((Sleepable) e.nextElement()).toSleep();
		}
	}
	
	/**
	 * Add a Sleepable which will be notified of the Schedulers intentions to
	 * sleep
	 * 
	 * @param s The Sleepable to register
	 */
	public synchronized void addSleepable(Sleepable s) {
		sleepable.addElement(s);
	}
	
	/**
	 * Returns the number of Tasks in the Scheduler
	 * 
	 * @return The number of Tasks in the Scheduler
	 */
	public int size() {
		return tasksTable.size();
	}
	
	public void receive(byte[] payload, long source) {
		try {
			addTask(SchedulableTask.decode(payload));
		}
		catch (IOException e) {
			Logger.logError("Could not decode task: " + e);
		}
	}
	
	/**
	 * This class maps a query id to Task's that are related to that query. This
	 * allows all tasks related to a given query to be accessed.
	 * 
	 */
	protected class QueryToTaskTable {
		private final Hashtable tasks; // {query id => {task id => task}}
		
		public QueryToTaskTable() {
			tasks = new Hashtable();
		}
		
		/**
		 * Add a task to the table
		 * 
		 * @param t Task to add to the table
		 */
		public void addTask(SchedulableTask t) {
			if (Network.getInstance().getMode() == Network.MODE_SPOT)
				t.nodeInit();
			Integer queryID = new Integer(t.getTaskId().getQueryID());
			if (tasks.containsKey(queryID))
				((Hashtable) tasks.get(queryID)).put(t.getTaskId(), t);
			else {
				Hashtable newHash = new Hashtable();
				newHash.put(t.getTaskId(), t);
				tasks.put(queryID, newHash);
			}
		}
		
		/**
		 * Remove a task from the table
		 * 
		 * @param tID TaskID of the task to remove
		 */
		private void removeTask(TaskID tID) {
			Integer queryID = new Integer(tID.getQueryID());
			if (tasks.containsKey(queryID)) {
				Hashtable currentQuery = ((Hashtable) tasks.get(queryID));
				currentQuery.remove(tID);
				if (currentQuery.isEmpty())
					tasks.remove(queryID);
			}
		}
		
		/**
		 * Remove a query (and all related tasks) from the table
		 * 
		 * @param queryID The ID of the query to remove
		 */
		public void removeQuery(int queryID) {
			tasks.remove(new Integer(queryID));
		}
		
		/**
		 * Retrieve all tasks related to a given query
		 * 
		 * @param queryID The id to retrieve tasks associated with
		 * @return an Enumeration of tasks associated with the given query ID
		 */
		public Enumeration getTasks(int queryID) {
			return ((Hashtable) tasks.get(new Integer(queryID))).elements();
		}
		
		/**
		 * Returns whether the table contains any tasks with the given query ID
		 * 
		 * @param queryID The ID of the query
		 * @return Whether there are tasks that have the given query ID
		 */
		public boolean containsQuery(int queryID) {
			return tasks.containsKey(new Integer(queryID));
		}
	}
	
	/**
	 * This class represents a thread that is spawned to execute the Task. This
	 * allows multiple Tasks to be executing simultaneously.
	 * 
	 */
	protected class TaskExecutor extends Thread {
		private final SchedulableTask task;
		
		public TaskExecutor(SchedulableTask task) {
			super("Executor (" + task.getTaskId().getQueryID() + ": " + task.getClass().getName() + ")");
			this.task = task;
		}
		
		public void run() {
			try {
				// executes the task
				task.execute();
			}
			catch (Throwable e) {
				e.printStackTrace();
				SPOTTools.reportError(e);
			}
			
			// reschedule the task if it needs to
			if (task.needsRescheduling())
				reschedule(task);
			else
				removeTask(task);
		}
	}
	
}
