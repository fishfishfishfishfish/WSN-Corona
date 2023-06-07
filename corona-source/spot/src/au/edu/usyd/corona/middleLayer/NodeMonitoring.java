package au.edu.usyd.corona.middleLayer;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import au.edu.usyd.corona.util.Logger;

import com.sun.spot.util.IEEEAddress;

/**
 * This clustering is responsible for monitoring the status of other nodes, i.e.
 * whether this node can hear from that node and vice-versa. If a node can
 * no-longer be heard, or transmitted to, it is removed from this instance, and
 * all registered listeners are notified.
 * 
 * The checks for node existence are done in the following way:
 * 
 * (1) To ensure that we can receive from a given node, the RoutingManager will
 * send a heartbeat message every T / N seconds, where T is the period that we
 * check for a nodes existence. If N > 1 (which it should be) it gives the
 * opportunity for the node to miss a heartbeat or for the heartbeat not to be
 * received, without removing the node.
 * 
 * (2) To ensure a node can hear from us, when we are transmitting messages, if
 * we get a No-ACK exception, we register it with the NodeMonitor. A certain
 * number of NO-ACKs in a row will cause removal of the node.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 * @author Khaled Almi'ani
 */
class NodeMonitoring implements Runnable {
	// constants
	private static final int DEFAULT_SLEEP_PERIOD = 5 * 60 * 1000; // The default period for checking for node removal
	private static final int MAX_NUM_FAILS = 7; // The number of transmission failures before the node is considered lost
	private static final int NUM_HEARTBEATS_PER_CHECK = 3; // This is the number of heart beat messages we expect to receive from a node per SLEEP_PERIOD.  If we receive less than this number, it is ok, but we should receive at least 1 before considering the node dead.
	
	// singleton instance
	private static NodeMonitoring instance;
	
	// instance variables
	private final Hashtable nodes; // Long => Long (IEEE address => time stamp)
	private final Hashtable fails; // Long => Integer (IEEE address => number transmission fails)
	private final Vector listeners; // NodeListener
	private int sleepPeriod = DEFAULT_SLEEP_PERIOD;
	private int heartBeatPeriod = sleepPeriod / NUM_HEARTBEATS_PER_CHECK;
	
	static void initialize() {
		instance = new NodeMonitoring();
		new Thread(instance, "Tims Node Monitoring").start();
	}
	
	static NodeMonitoring getInstance() {
		return instance;
	}
	
	private NodeMonitoring() {
		nodes = new Hashtable();
		listeners = new Vector();
		fails = new Hashtable();
	}
	
	/**
	 * If a message has been received from the node.
	 * 
	 * @param nodeId The ID of the node received from
	 */
	void updateSeen(long nodeId) {
		synchronized (nodes) {
			nodes.put(new Long(nodeId), new Long(System.currentTimeMillis()));
			Logger.logDebug("[Node Monitoring] Neighbours: " + getNumberNeighbours());
		}
	}
	
	/**
	 * Add a listener interested in hearing about node removal
	 * 
	 * @param listener The listener to add
	 */
	void addListener(NodeListener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener))
				listeners.addElement(listener);
		}
	}
	
	/**
	 * Return the number of nodes that this node can hear from.
	 * 
	 * @return The number of nodes that this node can hear from.
	 */
	public int getNumberNeighbours() {
		synchronized (nodes) {
			return nodes.size();
		}
	}
	
	/**
	 * Thread that checks for node-removal
	 */
	public void run() {
		// main loop
		final Vector removed = new Vector();
		while (true) {
			// sleep for a while 
			Logger.logDebug("[Node Monitoring] going to sleep");
			try {
				Thread.sleep(sleepPeriod);
			}
			catch (InterruptedException e) {
			}
			Logger.logDebug("[Node Monitoring] woken up");
			
			// find nodes which are no longer active
			synchronized (nodes) {
				for (Enumeration keys = nodes.keys(); keys.hasMoreElements();) {
					Long node = (Long) keys.nextElement();
					long timeSinceLastSeen = System.currentTimeMillis() - ((Long) nodes.get(node)).longValue();
					Logger.logDebug("[Node Monitoring] timeSinceLastSeen is " + timeSinceLastSeen + " for " + IEEEAddress.toDottedHex(node.longValue()));
					if (timeSinceLastSeen >= sleepPeriod) {
						nodes.remove(node);
						Logger.logDebug("[Node Monitoring] Neighbours: " + getNumberNeighbours());
						removed.addElement(node);
					}
				}
			}
			
			// notify the dead nodes listeners
			removeNodes(removed);
			removed.removeAllElements();
		}
	}
	
	private void removeNodes(Vector nodesToRemove) {
		for (Enumeration e = nodesToRemove.elements(); e.hasMoreElements();) {
			long nodeId = ((Long) e.nextElement()).longValue();
			Logger.logDebug("[Node Monitoring] removing node " + IEEEAddress.toDottedHex(nodeId));
			synchronized (listeners) {
				for (Enumeration e2 = listeners.elements(); e2.hasMoreElements();) {
					((NodeListener) e2.nextElement()).nodeDisconnected(nodeId);
				}
			}
		}
	}
	
	/**
	 * Register that we could transmit to a node and reset its failure count
	 * 
	 * @param nodeId The id of the node transmitted to
	 */
	void handleSent(long nodeId) {
		if (nodeId == Network.SEND_MODE_BROADCAST)
			return;
		synchronized (fails) {
			fails.remove(new Long(nodeId));
		}
	}
	
	/**
	 * Handle a no-ack exception. This adds to the failure count of the node and
	 * removes the node if the count reaches a threshhold.
	 * 
	 * @param nodeId The node we could not transmit to
	 */
	void handleNoAck(long nodeId) {
		if (nodeId == Network.SEND_MODE_BROADCAST)
			return;
		Long node = new Long(nodeId);
		synchronized (fails) {
			int numFails = 0;
			if (fails.containsKey(node))
				numFails = ((Integer) fails.get(node)).intValue();
			numFails++;
			if (numFails > MAX_NUM_FAILS) {
				fails.remove(node);
				synchronized (nodes) {
					nodes.remove(new Long(nodeId));
				}
				Vector v = new Vector(1);
				v.addElement(new Long(nodeId));
				removeNodes(v);
			}
			else {
				fails.put(node, new Integer(numFails));
			}
		}
		
	}
	
	/**
	 * Set the sleep period of NodeMonitoring.
	 * 
	 * @param sleepPeriod the period at which NodeMonitoring should check for
	 * disconnected nodes
	 */
	void setSleepPeriod(int sleepPeriod) {
		this.sleepPeriod = sleepPeriod;
		this.heartBeatPeriod = sleepPeriod / NUM_HEARTBEATS_PER_CHECK;
	}
	
	/**
	 * Get the period at which heartbeat messages should be transmitted
	 * 
	 * @return the period at which heartbeat messages should be sent
	 */
	long getHeartBeatPeriod() {
		return heartBeatPeriod;
	}
	
	/**
	 * This interface allows an object to be notified of the disconnection of a
	 * node and respond appropriately
	 * 
	 */
	interface NodeListener {
		public void nodeDisconnected(long nodeId);
	}
	
	/**
	 * Return whether this node can hear from a given node
	 * 
	 * @param toAddress The node to check that we can hear from
	 * @return true if the node can be heard from, false otherwise
	 */
	boolean hasNeighbour(long toAddress) {
		synchronized (nodes) {
			return nodes.containsKey(new Long(toAddress));
		}
	}
}
