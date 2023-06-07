package au.edu.usyd.corona.server.session.notifier;


import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import au.edu.usyd.corona.scheduler.Scheduler;

/**
 * This class implements the Singleton pattern to handle notifiers. It
 * automatically removes notifiers when they are invalid.
 * 
 * @author Raymes Khoury
 */
public class NotifierManager extends Thread {
	private static final Logger logger = Logger.getLogger(NotifierManager.class.getCanonicalName());
	
	public static final int USER_NOTIFIER = -1;
	public static final int QUERY_NOTIFIER = -2;
	
	public static final long UPDATE_PERIOD = 500;
	
	private static NotifierManager instance = null;
	
	private final Map<NotifierID, Set<RemoteNotifierInterface>> notifiers;
	private final Map<NotifierID, Set<RemoteNotifierInterface>> toBeUpdated;
	
	/**
	 * @return the singleton instance of this NotifierManager
	 */
	public static NotifierManager getInstance() {
		if (instance == null)
			instance = new NotifierManager();
		return instance;
	}
	
	private NotifierManager() {
		notifiers = new HashMap<NotifierID, Set<RemoteNotifierInterface>>();
		toBeUpdated = new HashMap<NotifierID, Set<RemoteNotifierInterface>>();
	}
	
	/**
	 * Add the given notifier, associated with a given table
	 * 
	 * @param notifierID The id of the notifier
	 * @param notifier The actual notifier to associate to the given table
	 */
	public synchronized void add(NotifierID notifierID, RemoteNotifierInterface notifier) {
		logger.fine("Adding Notifier: " + notifierID);
		if (!notifiers.containsKey(notifierID))
			notifiers.put(notifierID, new HashSet<RemoteNotifierInterface>());
		logger.fine("Adding notifier for table: " + notifierID.getId());
		notifiers.get(notifierID).add(notifier);
	}
	
	/**
	 * Update all notifiers that are associated with the given NotifierID
	 * 
	 * @param notifierID The id of the notifier to update
	 */
	public synchronized void updateAll(NotifierID notifierID) {
		Set<RemoteNotifierInterface> current = notifiers.get(notifierID);
		if (current == null)
			return;
		
		if (!toBeUpdated.containsKey(notifierID))
			toBeUpdated.put(notifierID, new HashSet<RemoteNotifierInterface>());
		toBeUpdated.get(notifierID).addAll(current);
	}
	
	/**
	 * Remove the given notifier
	 * 
	 * @param notifierID The ID of the event associated with the Notifier
	 * @param notifier The Notifier to remove
	 */
	public synchronized void remove(NotifierID notifierID, RemoteNotifierInterface notifier) {
		Set<RemoteNotifierInterface> set = notifiers.get(notifierID);
		if (set == null)
			return;
		Iterator<RemoteNotifierInterface> iter = set.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(notifier)) {
				logger.fine("Removing notifier with ID: " + notifierID.getId() + " reference: " + notifier);
				iter.remove();
			}
		}
	}
	
	/**
	 * Remove stale notifiers
	 */
	public void reap() {
		// Avoid deadlock when checking task status in scheduler
		synchronized (Scheduler.getInstance()) {
			synchronized (this) {
				Iterator<NotifierID> iter = notifiers.keySet().iterator();
				while (iter.hasNext()) {
					NotifierID current = iter.next();
					if (current.getType() == NotifierID.NotifierType.QUERIES_TABLE_NOTIFIER || current.getType() == NotifierID.NotifierType.USERS_TABLE_NOTIFIER)
						continue;
					if (!Scheduler.getInstance().containsQuery(current.getId())) {
						logger.fine("Removing notifier: " + current.getId());
						iter.remove();
					}
				}
			}
		}
		
	}
	
	/**
	 * Update all notifiers that have been requested an update.
	 */
	private synchronized void updateAll() {
		for (NotifierID nid : toBeUpdated.keySet()) {
			logger.fine("Updating notifier id: " + nid);
			Set<RemoteNotifierInterface> x = toBeUpdated.get(nid);
			for (RemoteNotifierInterface y : x)
				new UpdateThread(nid, y).start();
			x.clear();
		}
		toBeUpdated.clear();
	}
	
	/**
	 * Periodically update notifiers and removes those which are stale. This
	 * prevents update spamming.
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(UPDATE_PERIOD);
			}
			catch (InterruptedException e) {
			}
			reap();
			updateAll();
		}
	}
	
	/**
	 * This class represents a Thread that executes the
	 * RemoteNotifierInterface.update() method. Executing in a separate Thread
	 * prevents the delay from the clients execution of the update() method. If
	 * this threads execution is blocked, it will be terminated when the client
	 * disconnects, anyway.
	 * 
	 * @author Raymes Khoury
	 */
	private class UpdateThread extends Thread {
		private final NotifierID nID;
		private final RemoteNotifierInterface n;
		
		public UpdateThread(NotifierID nID, RemoteNotifierInterface n) {
			this.nID = nID;
			this.n = n;
		}
		
		@Override
		public void run() {
			try {
				n.update(nID);
			}
			catch (RemoteException e) {
				NotifierManager.getInstance().remove(nID, n);
			}
		}
		
	}
	
}
