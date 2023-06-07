package au.edu.usyd.corona.middleLayer;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

/**
 * This class implements the HEED clustering protocol as described in <a
 * href="http://www.cs.purdue.edu/homes/fahmy/papers/tmc04.pdf"
 * target="_blank">http://www.cs.purdue.edu/homes/fahmy/papers/tmc04.pdf</a>. It
 * also stores information about this nodes cluster head/cluster children.
 * 
 * @author Khaled Almi'ani
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class Clustering implements RoutingConstants {
	// lowpan constants
	static final int DEFAULT_INTRA_CLUSTER_POWER_LEVEL = -22;
	static final int DEFAULT_INTER_CLUSTER_POWER_LEVEL = 2;
	
	// constants used in HEED
	private static final float C_PROB = 0.10f; // 10 %
	private static final float P_MIN = 1e-4f; // inversely proportional to E_MAX
	private static final int E_MAX = 100; // maximum battery level
	private static final int NETWORK_DELAY_TIME = 100; // 100 ms
	
	// instance variables
	private final Vector finalClusterHeads; // Long (IEEE address)
	private final Vector tentativeClusterHeads; // Long (IEEE address)
	private final Hashtable costs; // Long => Integer (IEEE address => cost)
	private final Vector children; // Long (IEEE address)
	
	private final Random rand;
	
	private final Vector[] _tentatives;
	private final Vector[] _finals;
	
	private long myClusterHead;
	private int intraClusterPowerLevel;
	private int interClusterPowerLevel;
	
	public Clustering() {
		finalClusterHeads = new Vector();
		tentativeClusterHeads = new Vector();
		costs = new Hashtable();
		children = new Vector();
		
		rand = new Random();
		
		_tentatives = new Vector[]{finalClusterHeads, tentativeClusterHeads};
		_finals = new Vector[]{finalClusterHeads};
		
		myClusterHead = -1;
		intraClusterPowerLevel = DEFAULT_INTRA_CLUSTER_POWER_LEVEL;
		interClusterPowerLevel = DEFAULT_INTER_CLUSTER_POWER_LEVEL;
	}
	
	/**
	 * Remove all cluster heads and children. Used during a force re-route
	 */
	void resetState() {
		finalClusterHeads.removeAllElements();
		tentativeClusterHeads.removeAllElements();
		children.removeAllElements();
	}
	
	/**
	 * Receive a final cluster head message from a cluster head. Register the
	 * cluster head.
	 * 
	 * @param action The routing message
	 */
	void receiveCHFinal(RoutingAction action) {
		if (action.getHeaderInfo().rssi < RoutingManager.MIN_RSSI)
			return;
		
		byte[] payload = action.getPayload();
		// update our cost and mark as a final cluster head
		int cost = Utils.readBigEndInt(payload, 1);
		Long fromO = new Long(action.getHeaderInfo().originator);
		if (!finalClusterHeads.contains(fromO))
			finalClusterHeads.addElement(fromO);
		tentativeClusterHeads.removeElement(fromO);
		costs.put(fromO, new Integer(cost));
	}
	
	/**
	 * Receive a tentative cluster head message from a tentative cluster head.
	 * Register the tentative cluster head.
	 * 
	 * @param action The routing message
	 */
	void receiveCHTentative(RoutingAction action) {
		if (action.getHeaderInfo().rssi < RoutingManager.MIN_RSSI)
			return;
		
		byte[] payload = action.getPayload();
		int cost = Utils.readBigEndInt(payload, 1);
		Long fromO = new Long(action.getHeaderInfo().originator);
		if (!tentativeClusterHeads.contains(fromO))
			tentativeClusterHeads.addElement(fromO);
		finalClusterHeads.removeElement(fromO);
		costs.put(fromO, new Integer(cost));
	}
	
	/**
	 * Run the HEED clustering algorithm on this node. Note that a basestation
	 * can never be a cluster head.
	 */
	void runHEED() {
		// reset state
		myClusterHead = Network.MY_ADDRESS;
		if (Network.NETWORK_MODE == Network.MODE_BASESTATION)
			return;
		
		// ---- STEP 1 ----
		float chProb = Math.max((C_PROB * getBatteryLevel()) / E_MAX, P_MIN);
		float chPrev = 0.0f;
		boolean isFinalCH = false;
		
		if (rand.nextFloat() <= chProb) {
			Long myAdd = new Long(Network.MY_ADDRESS);
			if (!tentativeClusterHeads.contains(myAdd))
				tentativeClusterHeads.addElement(myAdd);
			costs.put(new Long(Network.MY_ADDRESS), new Integer(getCost()));
			sendClusterHeadMessage(ACTION_CLUSTERING_CH_TENTATIVE);
		}
		
		Logger.logDebug("[Cluster] step 1 finished");
		
		doStep2(isFinalCH, chProb, chPrev);
	}
	
	/**
	 * Run one iteration of step 2 of the clustering algorithm
	 * 
	 * @param action A routing message with the HEED values from the previous
	 * iteration of the algorithm
	 */
	public void doStep2(RoutingAction action) {
		Object[] args = action.getArguments();
		doStep2(((Boolean) args[0]).booleanValue(), ((Float) args[1]).floatValue(), ((Float) args[2]).floatValue());
	}
	
	private void doStep2(boolean isFinalCH, float chProb, float chPrev) {
		// ---- STEP 2 ----
		for (Enumeration e = tentativeClusterHeads.elements(); e.hasMoreElements();) {
			Long node = (Long) e.nextElement();
			Logger.logDebug("[Cluster] tent cluster head is " + IEEEAddress.toDottedHex(node.longValue()) + "cost: " + costs.get(node));
		}
		for (Enumeration e = finalClusterHeads.elements(); e.hasMoreElements();) {
			Long node = (Long) e.nextElement();
			Logger.logDebug("[Cluster] final cluster head is " + IEEEAddress.toDottedHex(node.longValue()) + "cost: " + costs.get(node));
		}
		if (!tentativeClusterHeads.isEmpty() || !finalClusterHeads.isEmpty()) {
			long clusterHead = getKeyWithMinValue(_tentatives).longValue();
			if (clusterHead == Network.MY_ADDRESS) {
				if (chProb == 1.0f) {
					isFinalCH = true;
					sendClusterHeadMessage(ACTION_CLUSTERING_CH_FINAL);
				}
				else {
					sendClusterHeadMessage(ACTION_CLUSTERING_CH_TENTATIVE);
				}
			}
		}
		else if (chProb == 1.0f) {
			isFinalCH = true;
			sendClusterHeadMessage(ACTION_CLUSTERING_CH_FINAL);
		}
		else if (rand.nextFloat() <= chProb) {
			tentativeClusterHeads.addElement(new Long(Network.MY_ADDRESS));
			costs.put(new Long(Network.MY_ADDRESS), new Integer(getCost()));
			sendClusterHeadMessage(ACTION_CLUSTERING_CH_TENTATIVE);
		}
		
		chPrev = chProb;
		chProb = Math.min(2 * chProb, 1.0f);
		
		if (chPrev == 1.0f) {
			RoutingManager.getInstance().addAction(new RoutingAction(ACTION_CLUSTERING_STEP3, new Object[]{new Boolean(isFinalCH)}, System.currentTimeMillis() + NETWORK_DELAY_TIME));
			Logger.logDebug("[Cluster] step 2 finished");
		}
		else {
			Object[] arguments = new Object[]{new Boolean(isFinalCH), new Float(chProb), new Float(chProb)};
			// Schedule the next iteration of the algorithm, with a delay to allow for nearby neighbours to respond
			RoutingManager.getInstance().addAction(new RoutingAction(ACTION_CLUSTERING_STEP2, arguments, System.currentTimeMillis() + NETWORK_DELAY_TIME));
		}
		
	}
	
	void doStep3(RoutingAction action) {
		boolean isFinalCH = ((Boolean) action.getArguments()[0]).booleanValue();
		// ---- STEP 3 ----
		if (!isFinalCH && !finalClusterHeads.isEmpty()) {
			myClusterHead = getKeyWithMinValue(_finals).longValue();
			sendJoinClusterMessage(myClusterHead);
			children.removeAllElements();
		}
		else {
			myClusterHead = Network.MY_ADDRESS;
			sendClusterHeadMessage(ACTION_CLUSTERING_CH_FINAL);
		}
		
		Logger.logDebug("[Cluster] step 3 finished");
		RoutingManager.getInstance().addAction(new RoutingAction(ACTION_CLUSTERING_FINISHED, null, System.currentTimeMillis()));
	}
	
	private int getBatteryLevel() {
		if (Network.NETWORK_MODE == Network.MODE_SPOT)
			return Spot.getInstance().getPowerController().getBattery().getBatteryLevel();
		else
			return E_MAX;
	}
	
	/**
	 * Given a map mapping Long's to Integers, this method returns the key to the
	 * item in the map with the smallest value. This method assumes that map is
	 * not null. If the given map is empty, null is returned.
	 * 
	 * @param map the map to traverse
	 * @return the key with the smallest value in the given map, or null for an
	 * empty map
	 */
	private Long getKeyWithMinValue(final Vector[] allNodes) {
		Vector minKeys = new Vector();
		int minValue = 0;
		for (int i = 0; i != allNodes.length; i++) {
			for (Enumeration nodes = allNodes[i].elements(); nodes.hasMoreElements();) {
				Long key = (Long) nodes.nextElement();
				Integer cost = (Integer) costs.get(key);
				if (cost == null)
					continue;
				else if ((minKeys.size() == 0) || (cost.intValue() < minValue)) {
					minKeys.removeAllElements();
					minKeys.addElement(key);
					minValue = cost.intValue();
				}
				else if (cost.intValue() == minValue) {
					minKeys.addElement(key);
				}
			}
		}
		
		return (Long) minKeys.elementAt(rand.nextInt(minKeys.size()));
	}
	
	/**
	 * Broadcast a cluster head message
	 * 
	 * @param type Tentative of Final cluster head
	 */
	void sendClusterHeadMessage(byte type) {
		sendClusterHeadMessage(type, Network.SEND_MODE_BROADCAST);
	}
	
	/**
	 * Send a cluster head message to a particular node
	 * 
	 * @param type Tentative of Final cluster head
	 * @param node The node to send to
	 */
	void sendClusterHeadMessage(byte type, long node) {
		byte[] data = new byte[1 + Utils.SIZE_OF_INT]; // type, final step, cost
		data[0] = type;
		Utils.writeBigEndInt(data, 1, getCost());
		RoutingManager.getInstance().send(data, node);
	}
	
	/**
	 * Send a message to a cluster head to join its cluster
	 * 
	 * @param headId The id of the cluster head
	 */
	private void sendJoinClusterMessage(long headId) {
		final byte[] data = new byte[1]; // type, the id of the node we are becoming a member for
		data[0] = ACTION_CLUSTERING_CH_JOIN;
		boolean sent = RoutingManager.getInstance().send(data, headId);
		if (!sent)
			RoutingManager.getInstance().enterNoRoute();
	}
	
	/**
	 * Get the cost of this node to determine which cluster head to join
	 * 
	 * @return a cost function of this node
	 */
	int getCost() {
		Logger.logDebug("[Cluster] My cost is: " + NodeMonitoring.getInstance().getNumberNeighbours());
		return NodeMonitoring.getInstance().getNumberNeighbours();
	}
	
	/**
	 * Return this nodes cluster head
	 * 
	 * @return This nodes cluster head
	 */
	long getClusterHead() {
		return myClusterHead;
	}
	
	/**
	 * Return the number of children this cluster head has
	 * 
	 * @return The number of children this cluster head has
	 */
	int getNumberChildren() {
		return children.size();
	}
	
	/**
	 * Return an array of children of this cluster head
	 * 
	 * @return an array of child addresses of the cluster head
	 */
	Long[] getChildren() {
		final Long[] c = new Long[children.size()];
		children.copyInto(c);
		return c;
	}
	
	/**
	 * True if this node is a cluster head, otherwise false
	 * 
	 * @return True if this node is a cluster head, otherwise false
	 */
	boolean isClusterHead() {
		return myClusterHead == Network.MY_ADDRESS;
	}
	
	/**
	 * Return whether this cluster head has a child with the given ID
	 * 
	 * @param node the node id of a potential cluster child
	 * @return true if the node is a cluster child, false otherwise
	 */
	boolean hasChild(Long node) {
		return children.contains(node);
	}
	
	/**
	 * @param powerLevel the intracluster power level
	 */
	void setIntraClusterPowerLevel(int powerLevel) {
		intraClusterPowerLevel = powerLevel;
	}
	
	/**
	 * @param powerLevel the intercluster power level
	 */
	void setInterClusterPowerLevel(int powerLevel) {
		interClusterPowerLevel = powerLevel;
	}
	
	/**
	 * Return the power level to be used for intra-cluster communications
	 * 
	 * @return the power level for intra-cluster communications
	 */
	int getIntraClusterPowerLevel() {
		return intraClusterPowerLevel;
	}
	
	/**
	 * Return the power level for inter-cluster communications
	 * 
	 * @return the power level for inter-cluster communications
	 */
	int getInterClusterPowerLevel() {
		return interClusterPowerLevel;
	}
	
	/**
	 * Remove a potential cluster head
	 * 
	 * @param node The cluster head to remove
	 */
	void removeHead(long node) {
		Long fromO = new Long(node);
		finalClusterHeads.removeElement(fromO);
		tentativeClusterHeads.removeElement(fromO);
	}
	
	/**
	 * Remove a cluster child
	 * 
	 * @param node The cluster child to remove
	 */
	void removeChild(long node) {
		Long fromO = new Long(node);
		children.removeElement(fromO);
	}
	
	/**
	 * Update the cost of a node
	 * 
	 * @param node The node to update
	 * @param cost The cost of the node
	 */
	void updateCost(long node, int cost) {
		costs.put(new Long(node), new Integer(cost));
	}
	
	/**
	 * Add a child node
	 * 
	 * @param node The ID of the node to add as a child
	 */
	void addChild(long node) {
		if (!children.contains(new Long(node)))
			children.addElement(new Long(node));
	}
	
	/**
	 * Remove all cluster children
	 */
	void clearChildren() {
		children.removeAllElements();
	}
	
	/**
	 * Return whether this node has any cluster children
	 * 
	 * @return true if this node has any cluster children, false otherwise
	 */
	boolean hasChildren() {
		return !children.isEmpty();
	}
	
	/**
	 * Remove the cost of a node, registered in the costs table
	 * 
	 * @param node The cost of the node to remove
	 */
	void removeCost(long node) {
		costs.remove(new Long(node));
		
	}
}
