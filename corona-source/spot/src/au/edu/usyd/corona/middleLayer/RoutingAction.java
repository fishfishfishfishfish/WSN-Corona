package au.edu.usyd.corona.middleLayer;


import au.edu.usyd.corona.collections.Comparable;

import com.sun.spot.peripheral.radio.LowPanHeaderInfo;

/**
 * This class represents a single Routing action to be performed. Each routing
 * action is executed by the RoutingManager and performs a certain function,
 * such as causing a re-route or adding a child node. All routing messages are
 * transmitted in the form of a RoutingAction which is then executed and may
 * change the state of the RoutingManager. Each RoutingAction has a time of
 * execution, so it can be set to execute in the future (for example, timeouts),
 * however this time is usually set to the current time. Any additional
 * information about the RoutingAction (for example, the node to add when adding
 * a routing child) is stored in the LowPanHeaderInfo object (headerInfo) and
 * byte array (payload), for a transmitted routing action, or in the Object
 * array (arguments) for a locally generated action (such as a timeout).
 * 
 * See the {@link RoutingManager} for further details. See
 * {@link RoutingConstants} for the types of RoutingAction available.
 * 
 * @author Raymes Khoury
 */
class RoutingAction implements Comparable, RoutingConstants {
	private final byte type; // The type of the routing action (in RoutingConstants)
	private final LowPanHeaderInfo headerInfo; // The lowpan header of a transmitted routing action
	private final byte[] payload; // The byte array of a transmitted routing action
	private final Object[] arguments; // an array of arguments for a locally generated routing action
	private final long timeExecution; // the time that the routing action should be executed
	
	/**
	 * Create a routing action from transmitted routing data. The first byte of
	 * the byte array is assumed to be the type of the RoutingAction.
	 * 
	 * @param headerInfo the lowpan header of the transmitted data
	 * @param payload the byte array of the transmitted data
	 */
	RoutingAction(LowPanHeaderInfo headerInfo, byte[] payload) {
		this.type = payload[0];
		this.headerInfo = headerInfo;
		this.payload = payload;
		this.arguments = null;
		this.timeExecution = System.currentTimeMillis();
	}
	
	/**
	 * Create a locally generated routing action
	 * 
	 * @param type the type of the RoutingAction
	 * @param arguments any additional information associated with the
	 * RoutingAction
	 * @param timeExecution the time at which the RoutingAction should be
	 * executed
	 */
	RoutingAction(byte type, Object[] arguments, long timeExecution) {
		this.type = type;
		this.headerInfo = null;
		this.payload = null;
		this.arguments = arguments;
		this.timeExecution = timeExecution;
	}
	
	byte getType() {
		return type;
	}
	
	LowPanHeaderInfo getHeaderInfo() {
		return headerInfo;
	}
	
	byte[] getPayload() {
		return payload;
	}
	
	long getTimeExecution() {
		return timeExecution;
	}
	
	Object[] getArguments() {
		return arguments;
	}
	
	public int compareTo(Object o) {
		return (int) (getTimeExecution() - ((RoutingAction) o).getTimeExecution());
	}
	
	public String toString() {
		// It's annoying not having enums in squawk so we settle for this ugly thing
		switch (type) {
		case ACTION_FORCED_CLUSTERING_START:
			return "ACTION_FORCED_CLUSTERING_START";
		case ACTION_CLUSTERING_FINISHED:
			return "ACTION_CLUSTERING_FINISHED";
		case ACTION_CLUSTERING_STEP2:
			return "ACTION_CLUSTERING_STEP2";
		case ACTION_FORCED_ROUTING_START:
			return "ACTION_FORCED_ROUTING_START";
		case ACTION_FORCED_WAITING_FOR_PARENTS_TIMEOUT:
			return "ACTION_FORCED_WAITING_FOR_PARENTS_TIMEOUT";
		case ACTION_FORCED_WAITING_FOR_PARENTS_FINAL_TIMEOUT:
			return "ACTION_FORCED_WAITING_FOR_PARENTS_FINAL_TIMEOUT";
		case ACTION_ADD_START_CLUSTERING:
			return "ACTION_ADD_START_CLUSTERING";
		case ACTION_ADD_START:
			return "ACTION_ADD_START";
		case ACTION_ADD_REPLY:
			return "ACTION_ADD_REPLY";
		case ACTION_ADD_WAITING_FOR_PARENTS_TIMEOUT:
			return "ACTION_ADD_WAITING_FOR_PARENTS_TIMEOUT";
		case ACTION_SEND_HEIGHT:
			return "ACTION_SEND_HEIGHT";
		case ACTION_CLUSTERING_CH_FINAL:
			return "ACTION_CLUSTERING_CH_FINAL";
		case ACTION_CLUSTERING_CH_TENTATIVE:
			return "ACTION_CLUSTERING_CH_TENTATIVE";
		case ACTION_CLUSTERING_CH_JOIN:
			return "ACTION_CLUSTERING_CH_JOIN";
		case ACTION_NODE_STATUS:
			return "ACTION_NODE_STATUS";
		case ACTION_REGULAR_HEARTBEAT:
			return "ACTION_REGULAR_HEARTBEAT";
		case ACTION_BASE_REGULAR_RE_ROUTE:
			return "ACTION_BASE_REGULAR_RE_ROUTE";
		case ACTION_BASE_CLUSTERING_START:
			return "ACTION_BASE_CLUSTERING_START";
		case ACTION_BASE_ROUTING_START:
			return "ACTION_BASE_ROUTING_START";
		case ACTION_NOT_YOUR_PARENT:
			return "ACTION_NOT_YOUR_PARENT";
		case ACTION_DEAD_NODE:
			return "ACTION_DEAD_NODE";
		default:
			return type + "";
		}
	}
}
