package au.edu.usyd.corona.middleLayer;


/**
 * This interface stores routing states and actions used by several classes. See
 * {@link RoutingAction} and {@link RoutingManager} for further details.
 * 
 * @author Raymes Khoury
 */
interface RoutingConstants {
	// States
	public static final byte STATE_NODE_DEAD = 0; // Fake state for a dead node (i.e. one that can no longer be heard by this node)
	public static final byte STATE_NO_ROUTE = 1; // A node in this state is not a part of the routing tree and is waiting for some action to cause it to be routed
	public static final byte STATE_ROUTED_HEAD = 2; // A node in this state is a part of the routing tree (i.e. has a routing parent) and has potential cluster children and routing children
	public static final byte STATE_ROUTED_MEMBER = 3; // A node in this state is a cluster member of a routed cluster head
	public static final byte STATE_FORCED_CLUSTERING = 4; // A node in this state is performing a forced re-cluster
	public static final byte STATE_FORCED_WAITING_FOR_PARENTS = 5; // A node in this state is waiting for a routing parent, after a forced re-cluster
	public static final byte STATE_ADD_CLUSTERING = 6; // A node in this state is performing an add re-cluster
	public static final byte STATE_ADD_WAITING_FOR_PARENTS = 7; // A node in this state is waiting for a routing parent after an add re-cluster
	
	// Actions
	public static final byte ACTION_FORCED_CLUSTERING_START = 1; // This action causes a node to start forced clustering
	public static final byte ACTION_CLUSTERING_FINISHED = 2; // This action indicates the clustering process is complete and routing can begin
	public static final byte ACTION_CLUSTERING_STEP2 = 3; // This action indicates that one iteration of step 2 of the clustering algorithm should be executed
	public static final byte ACTION_CLUSTERING_STEP3 = 4; // This action indicates that step 3 of the clustering algorithm should be executed 
	public static final byte ACTION_FORCED_ROUTING_START = 5; // This action is transmitted by a routed cluster head during a forced route, indicating that a non-routed node can select it as a parent
	public static final byte ACTION_FORCED_WAITING_FOR_PARENTS_TIMEOUT = 6; // This action indicates that the timeout for selecting a parent has been reached (in forced routing) and the node should choose a parent
	public static final byte ACTION_FORCED_WAITING_FOR_PARENTS_FINAL_TIMEOUT = 7; // This action indicates that timeout for selecting a parent has been reached (in forced routing) and the node should enter no-route if it does not have a parent
	public static final byte ACTION_FORCED_ROUTING_MAKE_CHILD = 8; // This action is transmitted from a routing child to a parent, indicating that a child has selected the parent as its parent
	
	public static final byte ACTION_ADD_START_CLUSTERING = 9; // This action causes a node to start add-clustering if it does not have a routing parent
	public static final byte ACTION_ADD_START = 10; // This action is transmitted to indicate that an unrouted node is in need of a parent
	public static final byte ACTION_ADD_REPLY = 11; // This action is transmitted by a routed cluster head, indicating that it is a suitable parent for an unrouted node
	public static final byte ACTION_ADD_WAITING_FOR_PARENTS_TIMEOUT = 13; // This action indicates that the timeout has been reached for finding a parent, and the node should again become unrouted
	public static final byte ACTION_SEND_HEIGHT = 14; // This action indicates that the timeout has been reached for adding a child during a forced route, and so the height of the node should be sent up the tree
	
	public static final byte ACTION_CLUSTERING_CH_FINAL = 15; // This action indicates that a node is a cluster head
	public static final byte ACTION_CLUSTERING_CH_TENTATIVE = 16; // This action indicates that a node is a tentative cluster head
	public static final byte ACTION_CLUSTERING_CH_JOIN = 17; // This action is transmitted from an unrouted node to a routed cluster head, indicating that it has been selected as the nodes cluster head
	
	public static final byte ACTION_NODE_STATUS = 18; // This action is transmitted as a heartbeat message containing data which is used to maintain the routing tree
	public static final byte ACTION_REGULAR_HEARTBEAT = 19; // This action indicates that a heartbeat message (ACTION_NODE_STATUS) should be sent
	public static final byte ACTION_BASE_REGULAR_RE_ROUTE = 20; // This action indicates that a regular (periodic) force re-route of the routing tree should be performed 
	public static final byte ACTION_BASE_CLUSTERING_START = 21; // This action indicates that a force re-route of the routing tree should be performed immediately
	public static final byte ACTION_BASE_ROUTING_START = 22; // This action indicates that clustering has finished in-network (during a forced re-route) and so the routing process can begin.  This is necessary because the clustering process is instantaneous on the basestation.
	public static final byte ACTION_NOT_YOUR_PARENT = 23; // This action is transmitted from a routed node to another routed node, who incorrectly believes the node is its parent
	public static final byte ACTION_DEAD_NODE = 24; // This action is generated when we lose connectivty to a node (by NodeMonitoring) 
	
}
