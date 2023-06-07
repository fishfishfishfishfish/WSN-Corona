package au.edu.usyd.corona.middleLayer;


/**
 * This interface exposes the methods required by the application layer for time
 * synchronisation
 * 
 */
public interface TimeSyncInterface {
	
	/**
	 * This method sets the regular sync period of the network
	 * 
	 * @param syncPeriod the period between time synchronisations
	 */
	public void setSyncPeriod(long syncPeriod);
	
	/**
	 * Initializes the synchronization process with one particular node. This is
	 * used by the routing when a new child has been discovered and added as a
	 * routing child.
	 * 
	 * @param nodeId the IEEE address of the node to sync with
	 */
	public void syncWithNode(long nodeId);
	
	/**
	 * Set the time sync epoch
	 * 
	 * @param epochTime how often the time synchronization should be conducted
	 */
	public void setSyncEpoch(long epochTime);
	
	/**
	 * Get the time
	 * 
	 * @return the time after adding the clock drift
	 */
	public long getTime();
	
	/**
	 * Forces the time synchronization subsystem to resync the network. This
	 * method can only be called from the basestation.
	 */
	public void forceTimeSync();
	
}
