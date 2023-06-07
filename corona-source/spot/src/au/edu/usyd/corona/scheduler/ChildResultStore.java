package au.edu.usyd.corona.scheduler;


import java.util.Hashtable;
import java.util.Vector;

import au.edu.usyd.corona.srdb.Table;

/**
 * This class stores results from child nodes
 * 
 * @author Raymes Khoury
 */
public class ChildResultStore {
	private final Hashtable childResults; // Hashtable of results from children.  Maps epoch to vector of results for that epoch.
	private final Hashtable nodesReceivedFrom;
	
	public ChildResultStore() {
		childResults = new Hashtable();
		nodesReceivedFrom = new Hashtable();
	}
	
	/**
	 * Add a result table from a child
	 * 
	 * @param t The table to add
	 * @param epoch The epoch which the table was from
	 * @param node the node received from
	 */
	public synchronized void addResult(Table t, int epoch, long node) {
		Integer epochObj = new Integer(epoch);
		if (childResults.containsKey(epochObj))
			((Vector) childResults.get(epochObj)).addElement(t);
		else {
			Vector newEpoch = new Vector();
			newEpoch.addElement(t);
			childResults.put(epochObj, newEpoch);
		}
		
		Long nodeObj = new Long(node);
		if (nodesReceivedFrom.containsKey(epochObj))
			((Vector) nodesReceivedFrom.get(epochObj)).addElement(nodeObj);
		else {
			Vector newEpoch = new Vector();
			newEpoch.addElement(nodeObj);
			nodesReceivedFrom.put(epochObj, newEpoch);
		}
	}
	
	public synchronized Vector getNodesReceivedFrom(int epoch) {
		return (Vector) nodesReceivedFrom.get(new Integer(epoch));
	}
	
	/**
	 * The number of tables that are available from a given epoch
	 * 
	 * @param epoch The epoch to check the number of tables available from
	 * @return The number of tables available from this epoch
	 */
	public synchronized int numResults(int epoch) {
		Vector res = (Vector) childResults.get(new Integer(epoch));
		return res == null ? 0 : res.size();
	}
	
	/**
	 * Return a Vector of tables from a given epoch
	 * 
	 * @param epoch The epoch to retrieve tables from
	 * @return A vector of tables from that epoch
	 */
	public synchronized Vector getResults(int epoch) {
		return (Vector) childResults.get(new Integer(epoch));
	}
	
	/**
	 * Remove all child results for the given epoch
	 * 
	 * @param epoch the epoch to remove results from
	 */
	public synchronized void removeResults(int epoch) {
		Integer key = new Integer(epoch);
		if (childResults.containsKey(key))
			childResults.remove(key);
		if (nodesReceivedFrom.containsKey(key))
			nodesReceivedFrom.remove(key);
	}
	
}
