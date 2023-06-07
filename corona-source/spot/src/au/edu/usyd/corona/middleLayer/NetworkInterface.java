package au.edu.usyd.corona.middleLayer;


/**
 * This interface specifies the required methods for a tree routing network
 * protocol. A default implementation is provided in the {@link Network} class.
 * 
 * @author Raymes Khoury
 */
public interface NetworkInterface {
	// Node modes
	public static final byte MODE_SPOT = 0;
	public static final byte MODE_BASESTATION = 1;
	public static final byte MODE_UNITTEST = 2;
	
	/**
	 * Registers a listener to respond to Network events
	 * 
	 * @param l The listener to register
	 */
	public void registerListener(NetworkListener l);
	
	/**
	 * De-registers a listener to respond to Network events
	 * 
	 * @param l The listener to de-register
	 */
	public void deRegisterListener(NetworkListener l);
	
	/**
	 * Return the mode the spot is operating in. This is used to indicate whether
	 * the code is running on a regular node (spot), a basestation, or whether
	 * unit-tests are running the code.
	 * 
	 * @return the mode that the spot is operating in; one of
	 * {@link #MODE_BASESTATION}, {@link #MODE_SPOT}, or {@link #MODE_UNITTEST}
	 */
	public byte getMode();
	
	/**
	 * Send a byte buffer to our parent node
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sendToParent(byte[] payload);
	
	/**
	 * Send a byte buffer to all our child nodes
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sendToChild(byte[] payload);
	
	/**
	 * Send a byte buffer to all our ancestors
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sentToAncestors(byte[] payload);
	
	/**
	 * Send a byte buffer to our parent descendants
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sendToDescendants(byte[] payload);
	
	/**
	 * Send a byte buffer to the root node
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sentToRoot(byte[] payload);
	
	/**
	 * Broadcast the byte buffer
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sendBroadcast(byte[] payload);
	
	/**
	 * Send a byte buffer to a specific child node
	 * 
	 * @param payload The byte buffer to transmit
	 * @param node The child node to transmit to
	 */
	public void sendToChild(byte[] payload, long node);
	
	/**
	 * Returns the IEEE address of the current node.
	 * 
	 * @return the IEEE address of the current node
	 */
	public long getMyAddress();
	
	/**
	 * Returns the IEEE address of the parent of this node
	 * 
	 * @return the IEEE address of the parent of this node
	 */
	public long getParentAddress();
	
	/**
	 * Returns the number of children this node has
	 * 
	 * @return the number of children this node has
	 */
	public int getNumChildren();
	
	/**
	 * Returns an array of the addresses of children this node has
	 * 
	 * @return an array of addresses of children this node has
	 */
	public Long[] getChildren();
	
	/**
	 * Return the height of the routing subtree rooted at this node.
	 * 
	 * @return The height of the subtree rooted at this node
	 */
	public byte getHeight();
	
	/**
	 * Set the communication power levels
	 * 
	 * @param intraClusterLevel transmission level inside cluster
	 */
	public void setIntraClusterTrasmissionLevel(int intraClusterLevel);
	
	/**
	 * Set the communication power levels
	 * 
	 * @param interClusterLevel transmission level inside cluster
	 */
	public void setInterClusterTrasmissionLevel(int interClusterLevel);
	
	/**
	 * Set the period between full network-tree re-routes
	 * 
	 * @param epochTime the time between full network-tree re-routes
	 */
	public void setReRouteEpoch(int epochTime);
	
	/**
	 * Set the period that node-monitoring checks for disconnected node
	 * 
	 * @param epochTime The time between checks
	 */
	public void setNodeMonitoringEpoch(int epochTime);
	
	/**
	 * This method does a full re-route of the network tree. May take upto 15
	 * seconds to complete.
	 */
	public void reClusterAndReRoute();
}
