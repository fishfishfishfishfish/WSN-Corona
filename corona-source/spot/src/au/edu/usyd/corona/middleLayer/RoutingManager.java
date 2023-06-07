package au.edu.usyd.corona.middleLayer;


import au.edu.usyd.corona.middleLayer.NodeMonitoring.NodeListener;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

import com.sun.spot.peripheral.radio.IProtocolManager;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

/**
 * This class is responsible for coordinating routing and clustering operations
 * to properly form a routing tree.
 * 
 * In short, the class uses the HEED clustering (see {@link Clustering})
 * algorithm to produce clusters of nodes. Each cluster of nodes has a single
 * cluster head. Once this is complete, a minimum height routing tree is formed
 * using the cluster heads. Cluster members use a lower power level for
 * transmission, which reduces consumption.
 * 
 * There are 2 types of routing that can occur:
 * 
 * (1) Forced routing - occurs when a ROUTE command is initiated from the
 * basestation or periodically through the life of the network. A forced route
 * results in a full re-cluster and re-route of the network tree. This results
 * in all old state regarding the network tree being cleared and a new tree
 * being generated. Note that this operation can obviously be relatively
 * expensive and will result in a short period where application-level packets
 * will be ignored.
 * 
 * (2) Add routing - occurs when a node has been somehow disconnected from the
 * network (or turned on in the presence of an existing network). It results in
 * the node being added to the network by first performing clustering and then
 * performing routing.
 * 
 * In order to make debugging simple, the RoutingManager has a state based,
 * single threaded design. The routing manager can be in one of 5 states (see
 * {@link RoutingConstants}). The routing manager changes states by executing
 * RoutingAction's. A scheduler is used to execute routing actions in a single
 * thread. When a routing action is received by radio, or generated locally, it
 * is added to the scheduler to be executed in the Routing thread. This design
 * means that we can be sure of the current state of the RoutingManager when we
 * receive a routing message. Hence, we can act appropriately to the message
 * based on our state. For example, if we are an unrouted node and receive a
 * request from another unrouted node for a parent, we can be sure not to
 * respond, based on our state.
 * 
 * This design also means that we can treat all routing events (including those
 * generated locally and those received from other nodes) in the same way, by
 * adding them to the scheduler. For example, we can generate a timeout event
 * which is added to the scheduler to be executed at a future time.
 * 
 * @author Raymes Khoury
 */
class RoutingManager implements IProtocolManager, Runnable, NodeListener, RoutingConstants {
	// lowpan constants
	private static final byte PROTOCOL_FAMILY_NUMBER = 114;
	private static final byte PROTOCOL_NUMBER = 115;
	
	// constants
	// The time after a forced re-route begins where new re-route requests are ignored (to prevent broadcast echo's)
	private static final long MIN_FORCED_REROUTE_DELAY = 2 * 1000;
	// The time to wait for a parent to reply to you and make you their child, before becoming unrouted
	private static final long PARENT_WAIT_TIMEOUT = 1 * 1000;
	// The time to wait to hear from a child, before declaring yourself a leaf node
	private static final long CHILD_WAIT_DELAY = PARENT_WAIT_TIMEOUT + 500;
	// The full network tree re-route period
	private static final long DEFAULT_FORCE_ROUTE_EPOCH = 30 * 60 * 1000;
	// The delay the basestation waits after it starts a recluster, before it starts the routing (to ensure clustering complete)
	private static final long BASE_POST_CLUSTER_DELAY = 4 * 1000;
	// The time to wait for a parent in a full re-route before going to no-route
	private static final long PARENT_WAIT_FINAL_TIMEOUT = 10 * 1000;
	// The time before the first heartbeat message
	private static final long INITIAL_HEARTBEAT_DELAY = 10 * 1000;
	// The multiplier of time waited in NO_ROUTE before a re-route attempt is made
	private static final long REROUTE_ATTEMPT_TIME = 4 * 1000;
	// The max time waited in NO_ROUTE before a re-route attempt is made
	private static final long MAX_REROUTE_ATTEMPT_TIME = 60 * 1000;
	
	// The minimum value of signal strength in order to accept a parent
	static final int MIN_RSSI = -40;
	
	// singleton instance
	private static RoutingManager instance = null;
	
	// instance variables
	private final RoutingScheduler scheduler; // The scheduler used for routing actions
	private final Clustering clustering; // Contains clustering related state and the HEED clustering algorithm
	private final Routing routing; // Contains routing-tree state information (e.g. routing children, routing parent, etc.)
	private long lastForcedReRoute; // The time of the last forced re-route  
	private long currentRoute; // An ID for the current route
	private long epochTime = DEFAULT_FORCE_ROUTE_EPOCH; // The period between forced re-routes
	private byte currentState; // The current state of the RoutingManager
	private byte lastState; // The last state of the RoutingManager (for setting LEDs)
	private int reRouteAttempts; // The number of re-route attempts while in the NO_ROUTE state
	
	public static void initialize() {
		instance = new RoutingManager();
		new Thread(instance, "RoutingManager").start();
		Logger.logDebug("RoutingManager initialized");
	}
	
	public static RoutingManager getInstance() {
		return instance;
	}
	
	private RoutingManager() {
		// register with lowpan
		LowPan.getInstance().registerProtocolFamily(PROTOCOL_FAMILY_NUMBER, this);
		LowPan.getInstance().registerProtocol(PROTOCOL_NUMBER, this);
		
		NodeMonitoring.getInstance().addListener(this);
		
		// Set initial state
		scheduler = new RoutingScheduler();
		clustering = new Clustering();
		routing = new Routing();
		lastForcedReRoute = 0;
		currentRoute = 0;
		reRouteAttempts = 0;
		scheduler.addAction(new RoutingAction(ACTION_REGULAR_HEARTBEAT, null, System.currentTimeMillis() + INITIAL_HEARTBEAT_DELAY));
		
		if (Network.NETWORK_MODE == Network.MODE_BASESTATION)
			currentState = STATE_ROUTED_HEAD;
		else
			currentState = STATE_NO_ROUTE;
		lastState = -1;
		
		setRadio(true);
		
		if (Network.NETWORK_MODE != Network.MODE_SPOT)
			scheduler.addAction(new RoutingAction(ACTION_BASE_REGULAR_RE_ROUTE, null, System.currentTimeMillis()));
		else
			enterNoRoute();
	}
	
	public void processIncomingData(byte[] payload, LowPanHeaderInfo header) {
		try {
			NodeMonitoring.getInstance().updateSeen(header.originator);
			// any message we receive becomes a routing action
			addAction(new RoutingAction(header, payload));
		}
		catch (Exception e) {
			SPOTTools.reportError("Received invalid RoutingAction: " + e);
		}
		
	}
	
	public void run() {
		while (true) {
			setLED();
			try {
				// Execute the next RoutingAction in the queue
				executeAction(scheduler.getNextAction());
			}
			catch (Exception e) {
				SPOTTools.reportError(e);
				enterNoRoute();
			}
			setLED();
		}
	}
	
	private void executeAction(RoutingAction action) {
		Logger.logDebug("[Routing] Executing action: " + action);
		Logger.logDebug("[Routing] Current State: " + currentState);
		
		// Actions to be executed regardless of state
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_FINAL:
			clustering.receiveCHFinal(action);
			return;
		case ACTION_CLUSTERING_CH_TENTATIVE:
			clustering.receiveCHTentative(action);
			return;
		case ACTION_NODE_STATUS:
			updateNodeStatus(action);
			return;
		case ACTION_DEAD_NODE:
			deadNode(action);
			return;
		case ACTION_REGULAR_HEARTBEAT:
			regularHeartbeat(action);
			return;
		}
		
		// Actions to be executed based on current state
		switch (currentState) {
		case STATE_NO_ROUTE:
			noRouteState(action);
			return;
		case STATE_ROUTED_HEAD:
			routedHeadState(action);
			return;
		case STATE_ROUTED_MEMBER:
			routedMemberState(action);
			return;
		case STATE_FORCED_CLUSTERING:
			forcedClusteringState(action);
			return;
		case STATE_FORCED_WAITING_FOR_PARENTS:
			forcedWaitingForParentsState(action);
			return;
		case STATE_ADD_CLUSTERING:
			addClusteringState(action);
			return;
		case STATE_ADD_WAITING_FOR_PARENTS:
			addWaitingForParentsState(action);
			return;
		}
		
		// Should not get here
		Logger.logError("[Routing] Action: " + action.getType() + " was not executed");
	}
	
	private void noRouteState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			// Let child know that we are not good
			sendNodeStatus(action);
			break;
		case ACTION_FORCED_CLUSTERING_START:
			doForcedClustering(action);
			break;
		case ACTION_ADD_START_CLUSTERING:
			doAddClustering(action);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void routedHeadState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			clusterHeadJoinRouted(action);
			break;
		case ACTION_FORCED_CLUSTERING_START:
			doForcedClustering(action);
			break;
		case ACTION_ADD_START:
			addStartRouted(action);
			break;
		case ACTION_SEND_HEIGHT:
			if (!routing.hasChildren() && !(Network.NETWORK_MODE == Network.MODE_BASESTATION))
				sendNodeStatus(getParent());
			break;
		case ACTION_BASE_ROUTING_START:
			send(new byte[]{ACTION_FORCED_ROUTING_START, routing.getDepth()}, Network.SEND_MODE_BROADCAST);
			break;
		case ACTION_BASE_REGULAR_RE_ROUTE:
			doForcedClusteringBase(true);
			break;
		case ACTION_NOT_YOUR_PARENT:
			notYourParentRouted(action);
			break;
		case ACTION_BASE_CLUSTERING_START:
			doForcedClusteringBase(false);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouted(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void routedMemberState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			sendNodeStatus(action);
			break;
		case ACTION_FORCED_CLUSTERING_START:
			doForcedClustering(action);
			break;
		case ACTION_NOT_YOUR_PARENT:
			notYourParentRouted(action);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void forcedClusteringState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			clusterHeadJoinRouting(action);
			break;
		case ACTION_CLUSTERING_FINISHED:
			finishedClustering(action);
			break;
		case ACTION_CLUSTERING_STEP2:
			clustering.doStep2(action);
			break;
		case ACTION_CLUSTERING_STEP3:
			clustering.doStep3(action);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void forcedWaitingForParentsState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			clusterHeadJoinRouting(action);
			break;
		case ACTION_FORCED_WAITING_FOR_PARENTS_TIMEOUT:
			forcedWaitingForParentsTimeout(action);
			break;
		case ACTION_FORCED_WAITING_FOR_PARENTS_FINAL_TIMEOUT:
			forcedWaitingForParentsTimeoutFinal(action);
			break;
		case ACTION_FORCED_ROUTING_START:
			addRoutingParent(action, true);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void addClusteringState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			clusterHeadJoinRouting(action);
			break;
		case ACTION_FORCED_CLUSTERING_START:
			doForcedClustering(action);
			break;
		case ACTION_CLUSTERING_FINISHED:
			addFinishedClustering(action);
			break;
		case ACTION_CLUSTERING_STEP2:
			clustering.doStep2(action);
			break;
		case ACTION_CLUSTERING_STEP3:
			clustering.doStep3(action);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	private void addWaitingForParentsState(RoutingAction action) {
		switch (action.getType()) {
		case ACTION_CLUSTERING_CH_JOIN:
			clusterHeadJoinRouting(action);
			break;
		case ACTION_FORCED_CLUSTERING_START:
			doForcedClustering(action);
			break;
		case ACTION_ADD_WAITING_FOR_PARENTS_TIMEOUT:
			addWaitingForParentsTimeout(action);
			break;
		case ACTION_ADD_REPLY:
			addRoutingParent(action, false);
			break;
		case ACTION_FORCED_ROUTING_MAKE_CHILD:
			doForcedMakeChildRouting(action);
			break;
		default:
			Logger.logDebug("In state: " + currentState + " and got message: " + action + " but did not deal with.");
			break;
		}
	}
	
	/*
	 * Update this nodes state based on a status message received
	 */
	private void updateNodeStatus(RoutingAction action) {
		// At the very least we can update the cost of the node we've heard from
		byte[] payload = action.getPayload();
		byte state = payload[1];
		byte height = payload[2];
		long parent = Utils.readBigEndLong(payload, 3);
		int cost = Utils.readBigEndInt(payload, 3 + Utils.SIZE_OF_LONG);
		long node = action.getHeaderInfo().originator;
		updateNodeStatus(state, height, parent, cost, node);
	}
	
	/*
	 * Update this nodes state based on a status message received
	 */
	private void updateNodeStatus(byte state, byte height, long parent, int cost, long node) {
		Logger.logDebug("[Routing] Status message from: " + IEEEAddress.toDottedHex(node) + " state: " + state);
		
		byte myStartHeight = getHeight();
		
		// we can always update its routing height if it is a child
		if (routing.hasChild(new Long(node)))
			routing.addChild(node, height);
		// if it is routed, we can ALWAYS add it as a potential cluster head otherwise we can at least update its cost
		clustering.updateCost(node, cost);
		
		switch (currentState) {
		case STATE_NO_ROUTE:
			nodeStatusNotRouted(state, parent, cost, node, height);
			break;
		case STATE_ROUTED_HEAD:
			nodeStatusRoutedHead(state, parent, cost, node, height);
			break;
		case STATE_ROUTED_MEMBER:
			nodeStatusRoutedMember(state, parent, cost, node, height);
			break;
		}
		
		// if my height has changed, and im routed, update my parents height
		if (currentState == STATE_ROUTED_HEAD && myStartHeight != getHeight()) {
			if (Network.NETWORK_MODE != Network.MODE_BASESTATION)
				sendNodeStatus(getParent());
		}
	}
	
	/*
	 * Update my state based on a received routing message, when im an unrouted
	 * node
	 */
	private void nodeStatusNotRouted(byte state, long parent, int cost, long node, byte height) {
		switch (state) {
		case STATE_NODE_DEAD:
			clustering.removeHead(node);
			clustering.removeChild(node);
			clustering.removeCost(node);
			routing.removeChild(node);
			
			break;
		case STATE_NO_ROUTE:
			clustering.removeHead(node);
			clustering.removeChild(node);
			routing.removeChild(node);
			break;
		case STATE_ROUTED_HEAD:
			clustering.removeHead(node);
			clustering.removeChild(node);
			if (parent != Network.MY_ADDRESS) {
				// if the node isnt our child, remove it
				routing.removeChild(node);
			}
			else {
				// if the node thinks its our child but it isnt add it as a child
				if (!routing.hasChild(new Long(node))) {
					routing.addChild(node, height);
				}
			}
			break;
		case STATE_ROUTED_MEMBER:
			routing.removeChild(node);
			if (parent != Network.MY_ADDRESS) {
				clustering.removeHead(node);
			}
			else {
				// if the node thinks its our child but it isnt add it as a child
				if (!routing.hasChild(new Long(node))) {
					clustering.addChild(node);
				}
			}
			break;
		// Otherwise the node is still routing, we dont know what to do with the heartbeat
		}
	}
	
	/*
	 * Update my state based on a received routing message, when im a routed
	 * cluster head
	 */
	private void nodeStatusRoutedHead(byte state, long parent, int cost, long node, byte height) {
		switch (state) {
		case STATE_NODE_DEAD:
			// Fix children
			if (clustering.hasChild(new Long(node)) || routing.hasChild(new Long(node)))
				send(new byte[]{ACTION_NOT_YOUR_PARENT}, node);
			
			clustering.removeHead(node);
			clustering.removeChild(node);
			clustering.removeCost(node);
			routing.removeChild(node);
			
			// Fix parent
			if (routing.getRoutingParent() == node) {
				enterNoRoute();
			}
			
			break;
		case STATE_NO_ROUTE:
			// Fix children
			clustering.removeHead(node);
			clustering.removeChild(node);
			routing.removeChild(node);
			
			// Fix parent
			if (routing.getRoutingParent() == node) {
				enterNoRoute();
			}
			
			// Send a status message to it, so it knows we're here
			Logger.logDebug("[Routing] Sending reply status. Neighbours are: " + clustering.getCost());
			if (Network.NETWORK_MODE != Network.MODE_BASESTATION) {
				setRadio(false);
				clustering.sendClusterHeadMessage(ACTION_CLUSTERING_CH_FINAL);
				setRadio(true);
			}
			break;
		case STATE_ROUTED_HEAD:
			// Fix children
			clustering.removeHead(node);
			clustering.removeChild(node);
			
			if (parent != Network.MY_ADDRESS) {
				// if the node isnt our child, remove it
				routing.removeChild(node);
			}
			else {
				// if the node thinks its our child but it isnt add it as a child
				if (!routing.hasChild(new Long(node))) {
					routing.addChild(node, height);
					// if the node is also our parent, no_route
					if (routing.getRoutingParent() == node)
						enterNoRoute();
					else {
						//sync with it
						TimeSync.getInstance().syncWithNode(node);
					}
				}
			}
			
			break;
		case STATE_ROUTED_MEMBER:
			// Fix children
			routing.removeChild(node);
			if (parent != Network.MY_ADDRESS) {
				clustering.removeChild(node);
				clustering.removeHead(node);
			}
			else {
				// if the node thinks its our child but it isnt add it as a child
				if (!clustering.hasChild(new Long(node))) {
					clustering.addChild(node);
				}
			}
			
			// Fix parent
			if (routing.getRoutingParent() == node) {
				enterNoRoute();
			}
			break;
		// Otherwise the node is still routing, we dont know what to do with the heartbeat
		}
		
	}
	
	/*
	 * Update my state based on a received routing message, when im a routed
	 * cluster member
	 */
	private void nodeStatusRoutedMember(byte state, long parent, int cost, long node, byte height) {
		
		switch (state) {
		case STATE_NODE_DEAD:
			// Fix children
			clustering.removeHead(node);
			clustering.removeChild(node);
			clustering.removeCost(node);
			routing.removeChild(node);
			
			// Fix parent
			if (clustering.getClusterHead() == node) {
				enterNoRoute();
			}
			break;
		case STATE_NO_ROUTE:
			// Fix children
			clustering.removeHead(node);
			clustering.removeChild(node);
			routing.removeChild(node);
			
			// Fix parent
			if (clustering.getClusterHead() == node) {
				enterNoRoute();
			}
			break;
		case STATE_ROUTED_HEAD:
			// Fix children
			clustering.removeHead(node);
			clustering.removeChild(node);
			routing.removeChild(node);
			
			if (parent == Network.MY_ADDRESS) {
				// if the node thinks we're its parent, it's way off
				sendNodeStatus(node);
			}
			
			break;
		case STATE_ROUTED_MEMBER:
			// Fix children
			routing.removeChild(node);
			clustering.removeHead(node);
			clustering.removeChild(node);
			
			if (clustering.getClusterHead() == node)
				enterNoRoute();
			
			if (parent == Network.MY_ADDRESS) {
				// if the node thinks we're its parent, it's way off
				sendNodeStatus(node);
			}
			break;
		// Otherwise the node is still routing, we dont know what to do with the heartbeat
		}
		
	}
	
	/*
	 * Executed when we hear from node monitoring that we have lost connection to
	 * a node
	 */
	private void deadNode(RoutingAction action) {
		long id = ((Long) action.getArguments()[0]).longValue();
		updateNodeStatus(STATE_NODE_DEAD, (byte) 0, 0, 0, id);
	}
	
	public void nodeDisconnected(long nodeId) {
		addAction(new RoutingAction(ACTION_DEAD_NODE, new Object[]{new Long(nodeId)}, System.currentTimeMillis()));
	}
	
	private void doForcedMakeChildRouting(RoutingAction action) {
		long node = action.getHeaderInfo().originator;
		doForcedMakeChildRouted(action);
		sendNodeStatus(node);
	}
	
	private void doForcedMakeChildRouted(RoutingAction action) {
		long node = action.getHeaderInfo().originator;
		// Fix children
		clustering.removeHead(node);
		clustering.removeChild(node);
		routing.addChild(node, (byte) 0);
	}
	
	private void doAddClustering(RoutingAction action) {
		currentState = STATE_ADD_CLUSTERING;
		setRadio(false);
		clustering.runHEED();
	}
	
	private void addStartRouted(RoutingAction action) {
		send(new byte[]{ACTION_ADD_REPLY, routing.getDepth()}, action.getHeaderInfo().originator);
	}
	
	private void clusterHeadJoinRouting(RoutingAction action) {
		long node = action.getHeaderInfo().originator;
		routing.removeChild(node);
		clustering.removeHead(node);
		clustering.removeChild(node);
		clustering.addChild(node);
	}
	
	private void clusterHeadJoinRouted(RoutingAction action) {
		long node = action.getHeaderInfo().originator;
		routing.removeChild(node);
		clustering.removeHead(node);
		clustering.addChild(node);
		
		TimeSync.getInstance().syncWithNode(node);
		
		// Fix parent
		if (routing.getRoutingParent() == node) {
			enterNoRoute();
		}
	}
	
	private void forcedWaitingForParentsTimeout(RoutingAction action) {
		if (((Long) action.getArguments()[0]).longValue() != currentRoute)
			return;
		currentState = STATE_ROUTED_HEAD;
		setRadio(true);
		boolean sent = send(new byte[]{ACTION_FORCED_ROUTING_MAKE_CHILD}, routing.getRoutingParent());
		if (!sent)
			enterNoRoute();
		else {
			scheduler.addAction(new RoutingAction(ACTION_SEND_HEIGHT, null, System.currentTimeMillis() + CHILD_WAIT_DELAY));
			send(new byte[]{ACTION_FORCED_ROUTING_START, routing.getDepth()}, Network.SEND_MODE_BROADCAST);
			send(new byte[]{ACTION_FORCED_ROUTING_START, routing.getDepth()}, Network.SEND_MODE_BROADCAST);
		}
	}
	
	private void forcedWaitingForParentsTimeoutFinal(RoutingAction action) {
		if (((Long) action.getArguments()[0]).longValue() != currentRoute)
			return;
		if (!routing.hasParent())
			enterNoRoute();
	}
	
	private void addWaitingForParentsTimeout(RoutingAction action) {
		if (routing.hasParent()) {
			currentState = STATE_ROUTED_HEAD;
			setRadio(true);
			boolean sent = sendNodeStatus(routing.getRoutingParent());
			if (!sent) {
				enterNoRoute();
			}
			else {
				sendNodeStatus();
			}
		}
		else {
			reRouteAttempts++;
			long nextAttempt = Math.min(reRouteAttempts * REROUTE_ATTEMPT_TIME, MAX_REROUTE_ATTEMPT_TIME);
			enterNoRoute(nextAttempt);
		}
		
	}
	
	private void doForcedClustering(RoutingAction action) {
		setRadio(true);
		if (action.getHeaderInfo().rssi < MIN_RSSI)
			return;
		
		if (System.currentTimeMillis() - lastForcedReRoute < MIN_FORCED_REROUTE_DELAY || Network.NETWORK_MODE != Network.MODE_SPOT)
			return;
		lastForcedReRoute = System.currentTimeMillis();
		currentState = STATE_FORCED_CLUSTERING;
		
		// make sure people have an updated version of my cost
		sendNodeStatus();
		
		// clear all children and list of cluster heads
		routing.clearChildren();
		clustering.resetState();
		
		byte tempDepth = action.getPayload()[1];
		currentRoute++;
		
		send(new byte[]{ACTION_FORCED_CLUSTERING_START, (byte) (tempDepth + 1)}, Network.SEND_MODE_BROADCAST);
		setRadio(false);
		clustering.runHEED();
	}
	
	private void doForcedClusteringBase(boolean regular) {
		if (System.currentTimeMillis() - lastForcedReRoute < MIN_FORCED_REROUTE_DELAY)
			return;
		lastForcedReRoute = System.currentTimeMillis();
		
		// make sure people have an updated version of my cost
		sendNodeStatus();
		
		// clear all children and list of cluster heads
		routing.clearChildren();
		clustering.resetState();
		
		send(new byte[]{ACTION_FORCED_CLUSTERING_START, (byte) 0}, Network.SEND_MODE_BROADCAST);
		scheduler.addAction(new RoutingAction(ACTION_BASE_ROUTING_START, null, System.currentTimeMillis() + BASE_POST_CLUSTER_DELAY));
		scheduler.addAction(new RoutingAction(ACTION_BASE_ROUTING_START, null, System.currentTimeMillis() + BASE_POST_CLUSTER_DELAY + 500));
		if (regular)
			scheduler.addAction(new RoutingAction(ACTION_BASE_REGULAR_RE_ROUTE, null, System.currentTimeMillis() + epochTime));
		clustering.runHEED();
	}
	
	private void finishedClustering(RoutingAction action) {
		if (!clustering.isClusterHead()) {
			currentState = STATE_ROUTED_MEMBER;
			setRadio(false);
		}
		else {
			currentState = STATE_FORCED_WAITING_FOR_PARENTS;
			scheduler.addAction(new RoutingAction(ACTION_FORCED_WAITING_FOR_PARENTS_FINAL_TIMEOUT, new Object[]{new Long(currentRoute)}, System.currentTimeMillis() + PARENT_WAIT_FINAL_TIMEOUT));
			setRadio(true);
			routing.lostParent();
		}
	}
	
	private void notYourParentRouted(RoutingAction action) {
		if (action.getHeaderInfo().originator == getParent())
			enterNoRoute();
	}
	
	private void sendNodeStatus(RoutingAction action) {
		sendNodeStatus(action.getHeaderInfo().originator);
	}
	
	private void enterNoRoute(long timeTillNextRoute) {
		Logger.logDebug("[Routing] Entering noRoute");
		currentState = STATE_NO_ROUTE;
		setRadio(true);
		clustering.clearChildren();
		routing.clearChildren();
		sendNodeStatus();
		scheduler.addAction(new RoutingAction(ACTION_ADD_START_CLUSTERING, null, System.currentTimeMillis() + timeTillNextRoute));
	}
	
	void enterNoRoute() {
		reRouteAttempts = 0;
		enterNoRoute(0);
	}
	
	private void addRoutingParent(RoutingAction action, boolean forced) {
		if (action.getHeaderInfo().rssi < MIN_RSSI)
			return;
		Logger.logDebug("[Routing] parent rssi: " + action.getHeaderInfo().rssi);
		if (!routing.hasParent() && forced)
			scheduler.addAction(new RoutingAction(ACTION_FORCED_WAITING_FOR_PARENTS_TIMEOUT, new Object[]{new Long(currentRoute)}, System.currentTimeMillis() + PARENT_WAIT_TIMEOUT));
		
		byte depth = action.getPayload()[1];
		if (!routing.hasParent() || routing.getDepth() > depth + 1) {
			routing.newParent(action.getHeaderInfo().originator, depth);
		}
	}
	
	private void addFinishedClustering(RoutingAction action) {
		if (!clustering.isClusterHead()) {
			currentState = STATE_ROUTED_MEMBER;
			setRadio(false);
			sendNodeStatus();
		}
		else {
			currentState = STATE_ADD_WAITING_FOR_PARENTS;
			setRadio(true);
			send(new byte[]{ACTION_ADD_START}, Network.SEND_MODE_BROADCAST);
			routing.lostParent();
			scheduler.addAction(new RoutingAction(ACTION_ADD_WAITING_FOR_PARENTS_TIMEOUT, null, System.currentTimeMillis() + PARENT_WAIT_TIMEOUT));
		}
	}
	
	private void regularHeartbeat(RoutingAction action) {
		if (currentState == STATE_ROUTED_MEMBER)
			sendNodeStatus(clustering.getClusterHead());
		else
			sendNodeStatus();
		scheduler.addAction(new RoutingAction(ACTION_REGULAR_HEARTBEAT, null, System.currentTimeMillis() + NodeMonitoring.getInstance().getHeartBeatPeriod()));
	}
	
	/**
	 * Add a routing action to the routing scheduler
	 * 
	 * @param action the action to add
	 */
	void addAction(RoutingAction action) {
		scheduler.addAction(action);
	}
	
	/**
	 * Do a full re-cluster and re-route
	 */
	void doReclusterAndReRoute() {
		scheduler.addAction(new RoutingAction(ACTION_BASE_CLUSTERING_START, null, System.currentTimeMillis()));
	}
	
	/**
	 * Send the status of this node to a given node
	 * 
	 * @param address the address to send your status to
	 * @return true if the status was successfully sent, false otherwise
	 */
	boolean sendNodeStatus(long address) {
		Logger.logDebug("[Routing] parent is " + IEEEAddress.toDottedHex(getParent()));
		Long[] children = routing.getChildren();
		for (int i = 0; i != children.length; i++)
			Logger.logDebug("[Routing] routing child is " + IEEEAddress.toDottedHex(children[i].longValue()));
		children = clustering.getChildren();
		for (int i = 0; i != children.length; i++)
			Logger.logDebug("[Routing] cluster child is " + IEEEAddress.toDottedHex(children[i].longValue()));
		children = null;
		
		Logger.logDebug("[Routing] Sending Node Status to " + IEEEAddress.toDottedHex(address));
		byte[] payload = new byte[3 + Utils.SIZE_OF_LONG + Utils.SIZE_OF_INT];
		payload[0] = ACTION_NODE_STATUS;
		payload[1] = currentState;
		payload[2] = getHeight();
		Utils.writeBigEndLong(payload, 3, getParent());
		Utils.writeBigEndInt(payload, 3 + Utils.SIZE_OF_LONG, clustering.getCost());
		return send(payload, address);
	}
	
	/**
	 * Broadcast the status of the node
	 */
	void sendNodeStatus() {
		sendNodeStatus(Network.SEND_MODE_BROADCAST);
	}
	
	/**
	 * Send a parent indicating a node isn't your parent
	 * 
	 * @param address the node to send to
	 */
	void sendNotYourParent(long address) {
		send(new byte[]{ACTION_NOT_YOUR_PARENT}, address);
	}
	
	/**
	 * Get the current intra-cluster power level
	 * 
	 * @return the current intra-cluster power level
	 */
	int getIntraClusterPowerLevel() {
		return clustering.getIntraClusterPowerLevel();
	}
	
	/**
	 * Set the power levels
	 * 
	 * @param val power level
	 */
	void setIntraClusterPowerLevel(int val) {
		clustering.setIntraClusterPowerLevel(val);
	}
	
	void setInterClusterPowerLevel(int val) {
		clustering.setInterClusterPowerLevel(val);
	}
	
	/**
	 * Get the inter-cluster power level
	 * 
	 * @return the inter-cluster power level
	 */
	int getInterClusterPowerLevel() {
		return clustering.getInterClusterPowerLevel();
	}
	
	/**
	 * Return whether a given node is a cluster child of the current node
	 * 
	 * @param node the potential cluster child
	 * @return true if the child is a cluster member, false otherwise
	 */
	boolean hasClusterChild(long node) {
		if (currentState != STATE_ROUTED_HEAD)
			return false;
		return clustering.hasChild(new Long(node));
	}
	
	/**
	 * Return an array of cluster children
	 * 
	 * @return an array of the IDs of cluster children
	 */
	Long[] getClusterChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return new Long[0];
		return clustering.getChildren();
	}
	
	/**
	 * Return whether the node has cluster children
	 * 
	 * @return true if the node has a cluster child, false otherwise
	 */
	boolean hasClusterChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return false;
		return clustering.hasChildren();
	}
	
	/**
	 * Return whether the given node is a routing child of this node
	 * 
	 * @param node the potential routing child
	 * @return true if the node has a routing child
	 */
	boolean hasRoutingChild(long node) {
		if (currentState != STATE_ROUTED_HEAD)
			return false;
		return routing.hasChild(new Long(node));
	}
	
	/**
	 * Return true if the node has any routing children, false otherwise
	 * 
	 * @return true if the node has any routing children, false otherwise
	 */
	boolean hasRoutingChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return false;
		return routing.hasChildren();
	}
	
	/**
	 * Get the routing children of this node
	 * 
	 * @return an array of IDs of this nodes routing children
	 */
	Long[] getRoutingChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return new Long[0];
		return routing.getChildren();
	}
	
	/**
	 * Return whether the node has any children
	 * 
	 * @return true if the node has children, false otherwise
	 */
	boolean hasChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return false;
		return hasRoutingChildren() || hasClusterChildren();
	}
	
	/**
	 * Return all of the nodes children (cluster and routing)
	 * 
	 * @return an array of IDs of this nodes children
	 */
	Long[] getChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return new Long[0];
		Long[] routingChildren = getRoutingChildren();
		Long[] clusterChildren = getClusterChildren();
		Long[] result = new Long[routingChildren.length + clusterChildren.length];
		System.arraycopy(routingChildren, 0, result, 0, routingChildren.length);
		System.arraycopy(clusterChildren, 0, result, routingChildren.length, clusterChildren.length);
		return result;
	}
	
	/**
	 * Return whether a given node is a routing or clustering child
	 * 
	 * @param node a potential child node
	 * @return true if the node is a child, false otherwise
	 */
	boolean hasChild(long node) {
		if (currentState == STATE_ROUTED_HEAD)
			return hasClusterChild(node) || hasRoutingChild(node);
		return false;
	}
	
	/**
	 * The current state of the RoutingManager
	 * 
	 * @return the current state of the RoutingManager
	 */
	byte getState() {
		return currentState;
	}
	
	/**
	 * Return the parent of this node, or -1 if this node does not have a parent
	 * 
	 * @return the parent ID of this node, or -1 if this node does not have a
	 * parent
	 */
	long getParent() {
		if (Network.NETWORK_MODE == Network.MODE_BASESTATION)
			return -1;
		if (currentState == STATE_ROUTED_HEAD)
			return routing.getRoutingParent();
		else if (currentState == STATE_ROUTED_MEMBER)
			return clustering.getClusterHead();
		else
			return -1;
	}
	
	/**
	 * Return whether this node is routed or not
	 * 
	 * @return true if the node is routed, false otherwise
	 */
	boolean isRouted() {
		return currentState == STATE_ROUTED_HEAD || currentState == STATE_ROUTED_MEMBER;
	}
	
	/**
	 * Set the periodic forced re-route epoch
	 * 
	 * @param period the period between forced re-routes
	 */
	void setReRouteEpoch(long period) {
		epochTime = period;
	}
	
	/**
	 * Return the height of the subtree rooted at this node
	 * 
	 * @return the height of the subtree rooted at this node
	 */
	byte getHeight() {
		if (currentState != STATE_ROUTED_HEAD)
			return 0;
		byte routingHeight = routing.getHeight();
		if (routingHeight == 0 && clustering.hasChildren())
			return 1;
		else
			return routingHeight;
	}
	
	/**
	 * Return the number of cluster children this node has
	 * 
	 * @return the number of cluster children this node has
	 */
	int getNumClusterChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return 0;
		return clustering.getNumberChildren();
	}
	
	/**
	 * Return the number of routing children this node has
	 * 
	 * @return the number of routing children this node has
	 */
	int getNumRoutingChildren() {
		if (currentState != STATE_ROUTED_HEAD)
			return 0;
		return routing.getNumberChildren();
	}
	
	private void setLED() {
		if (Network.NETWORK_MODE != Network.MODE_SPOT)
			return;
		
		if (currentState == lastState) {
			if (currentState == STATE_ROUTED_HEAD) {
				if (routing.hasChildren()) {
					EDemoBoard.getInstance().getLEDs()[6].setOn();
					EDemoBoard.getInstance().getLEDs()[6].setColor(LEDColor.MAGENTA);
				}
				else {
					EDemoBoard.getInstance().getLEDs()[6].setOff();
				}
				if (clustering.hasChildren()) {
					EDemoBoard.getInstance().getLEDs()[7].setOn();
					EDemoBoard.getInstance().getLEDs()[7].setColor(LEDColor.TURQUOISE);
				}
				else {
					EDemoBoard.getInstance().getLEDs()[7].setOff();
				}
			}
			return;
		}
		
		lastState = currentState;
		
		EDemoBoard.getInstance().getLEDs()[0].setOff();
		EDemoBoard.getInstance().getLEDs()[1].setOff();
		EDemoBoard.getInstance().getLEDs()[2].setOff();
		EDemoBoard.getInstance().getLEDs()[6].setOff();
		EDemoBoard.getInstance().getLEDs()[7].setOff();
		
		switch (currentState) {
		case STATE_NO_ROUTE:
			EDemoBoard.getInstance().getLEDs()[0].setOn();
			EDemoBoard.getInstance().getLEDs()[0].setColor(LEDColor.RED);
			break;
		case STATE_ROUTED_HEAD:
			EDemoBoard.getInstance().getLEDs()[0].setOn();
			EDemoBoard.getInstance().getLEDs()[0].setColor(LEDColor.BLUE);
			if (routing.hasChildren()) {
				EDemoBoard.getInstance().getLEDs()[6].setOn();
				EDemoBoard.getInstance().getLEDs()[6].setColor(LEDColor.MAGENTA);
			}
			if (clustering.hasChildren()) {
				EDemoBoard.getInstance().getLEDs()[7].setOn();
				EDemoBoard.getInstance().getLEDs()[7].setColor(LEDColor.TURQUOISE);
			}
			break;
		case STATE_ROUTED_MEMBER:
			EDemoBoard.getInstance().getLEDs()[0].setOn();
			EDemoBoard.getInstance().getLEDs()[0].setColor(LEDColor.GREEN);
			break;
		// Clustering lights
		case STATE_FORCED_CLUSTERING:
			EDemoBoard.getInstance().getLEDs()[1].setOn();
			EDemoBoard.getInstance().getLEDs()[1].setColor(LEDColor.YELLOW);
			break;
		case STATE_FORCED_WAITING_FOR_PARENTS:
			EDemoBoard.getInstance().getLEDs()[1].setOn();
			EDemoBoard.getInstance().getLEDs()[1].setColor(LEDColor.RED);
			break;
		case STATE_ADD_CLUSTERING:
			EDemoBoard.getInstance().getLEDs()[2].setOn();
			EDemoBoard.getInstance().getLEDs()[2].setColor(LEDColor.CYAN);
			break;
		case STATE_ADD_WAITING_FOR_PARENTS:
			EDemoBoard.getInstance().getLEDs()[2].setOn();
			EDemoBoard.getInstance().getLEDs()[2].setColor(LEDColor.BLUE);
			break;
		}
	}
	
	private void setRadio(boolean inter) {
		if (RadioFactory.getRadioPolicyManager() == null)
			return;
		
		if (Network.NETWORK_MODE == Network.MODE_BASESTATION)
			RadioFactory.getRadioPolicyManager().setOutputPower(getInterClusterPowerLevel());
		else if (inter)
			RadioFactory.getRadioPolicyManager().setOutputPower(getInterClusterPowerLevel());
		else
			RadioFactory.getRadioPolicyManager().setOutputPower(getIntraClusterPowerLevel());
	}
	
	boolean send(byte[] payload, long toAddress) {
		if (toAddress == Network.SEND_MODE_BROADCAST)
			return Sender.getInstance().sendBroadcast(PROTOCOL_NUMBER, payload);
		else
			return Sender.getInstance().sendWithoutMeshingOrFragmentation(PROTOCOL_NUMBER, toAddress, payload);
	}
}
