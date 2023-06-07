package au.edu.usyd.corona.middleLayer;


import au.edu.usyd.corona.collections.PriorityBlockingQueue;
import au.edu.usyd.corona.util.Logger;

/**
 * This class represents a RoutingScheduler which is used to schedule
 * RoutingAction's at given points in time. Calling getNextAction will block
 * until the next action is available and then return that action.
 * 
 * @author Raymes Khoury
 */
class RoutingScheduler {
	protected final PriorityBlockingQueue actionQueue; // The priority queue structure to store the tasks
	
	public RoutingScheduler() {
		actionQueue = new PriorityBlockingQueue();
	}
	
	private long calculateTimeDelta(long time) {
		return time - System.currentTimeMillis();
	}
	
	/**
	 * Add an action to the sheduler
	 * 
	 * @param action The RoutingAction to add
	 */
	void addAction(RoutingAction action) {
		if (action.getType() == Network.ACTION_REGULAR_HEARTBEAT)
			Logger.logDebug("[RS] adding heartbeat " + action + " at " + System.currentTimeMillis());
		actionQueue.add(action);
		synchronized (this) {
			notifyAll();
		}
	}
	
	/**
	 * Get the next RoutingAction. This method blocks until an action is
	 * available.
	 * 
	 * @return the next action in the scheduler
	 */
	RoutingAction getNextAction() {
		RoutingAction currentAction = null;
		while (true) {
			currentAction = (RoutingAction) actionQueue.peekBlocking();
			
			// Sleep thread until next task is due
			final long timeDelta = calculateTimeDelta(currentAction.getTimeExecution());
			if (timeDelta > 0) {
				synchronized (this) {
					try {
						wait(timeDelta);
					}
					catch (InterruptedException e) {
					}
				}
			}
			else
				break;
		}
		
		// actually remove the action from the heap
		RoutingAction res = (RoutingAction) actionQueue.poll();
		return res;
	}
	
}
