package au.edu.usyd.corona.middleLayer;


import java.util.Enumeration;
import java.util.Vector;

import au.edu.usyd.corona.compression.Compressor;
import au.edu.usyd.corona.compression.JZlibCompressor;
import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingManager;

/**
 * This class provides an implementation of a Tree Networking protocol. It uses
 * the HEED protocol to form node clusters which use a lower transmission
 * power-level to conserve energy. A minimum-height tree is formed.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 * @author Khaled Almi'ani
 */
public class Network implements RoutingConstants, NetworkInterface {
	public static final short PAN_ID = 4;
	public static final int DEFAULT_POWER_LEVEL = 20;
	public static final int CHANNEL_NUMBER = 26;
	
	// Send modes
	public static final byte SEND_MODE_BROADCAST = 0; // Only for routing use
	public static final byte SEND_MODE_PARENT = 1;
	public static final byte SEND_MODE_CHILDREN = 2;
	public static final byte SEND_MODE_CHILDREN_BROADCAST = 3;
	
	public static long MY_ADDRESS; // My address for internal use in the package
	public static byte NETWORK_MODE; // The network mode for internal use in the package
	
	private static Network instance;
	
	private final Compressor compressor;
	private final RoutingManager routingManager;
	private final Vector listeners;
	
	/**
	 * Initialize the singleton instance of the network class with the mode of
	 * network, which is one of {@link #MODE_BASESTATION}, {@link #MODE_SPOT}, or
	 * {@link #MODE_UNITTEST}
	 * 
	 * @param mode the mode of network to initialize
	 */
	public static void initialize(byte mode) {
		MY_ADDRESS = mode == MODE_UNITTEST ? 0 : RadioFactory.getRadioPolicyManager().getIEEEAddress();
		NETWORK_MODE = mode;
		instance = new Network();
	}
	
	public static NetworkInterface getInstance() {
		return instance;
	}
	
	private Network() {
		// if we are not in unit test mode, init the subsystem
		if (NETWORK_MODE != MODE_UNITTEST) {
			// init the radio
			RadioFactory.getRadioPolicyManager().setChannelNumber(CHANNEL_NUMBER);
			RadioFactory.getRadioPolicyManager().setPanId(PAN_ID);
			RadioFactory.getRadioPolicyManager().setOutputPower(DEFAULT_POWER_LEVEL);
			RadioFactory.getRadioPolicyManager().setRxOn(true);
			
			// Set routing manager.  The default mesh networking (AODVManager) is disabled and a single-hop manager enabled.
			LowPan.getInstance().getRoutingManager().stop();
			IRoutingManager single = new SingleHopManager();
			single.initialize(MY_ADDRESS, LowPan.getInstance());
			single.start();
			LowPan.getInstance().setRoutingManager(single);
			
			// init the networking components
			NodeMonitoring.initialize();
			RoutingManager.initialize();
			PacketDispatcher.initialize();
		}
		
		compressor = new JZlibCompressor();
		routingManager = RoutingManager.getInstance();
		listeners = new Vector();
	}
	
	public void registerListener(NetworkListener l) {
		listeners.addElement(l);
	}
	
	public void deRegisterListener(NetworkListener l) {
		listeners.removeElement(l);
	}
	
	public byte getMode() {
		return NETWORK_MODE;
	}
	
	private byte[] compress(byte[] payload) {
		final byte[] bytes;
		synchronized (compressor) {
			bytes = compressor.compress(payload);
		}
		return bytes;
	}
	
	public void sendToParent(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_PARENT, true, false);
	}
	
	public void sendToChild(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_CHILDREN, true, false);
	}
	
	public void sentToAncestors(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_PARENT, true, true);
	}
	
	public void sendToDescendants(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_CHILDREN, true, true);
	}
	
	/**
	 * Send a byte buffer to the root node
	 * 
	 * @param payload The byte buffer to transmit
	 */
	public void sentToRoot(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_PARENT, false, true);
	}
	
	public void sendBroadcast(byte[] payload) {
		PacketDispatcher.getInstance().send(compress(payload), SEND_MODE_BROADCAST, false, false);
	}
	
	public void sendToChild(byte[] payload, long node) {
		PacketDispatcher.getInstance().send(compress(payload), node, false);
	}
	
	/**
	 * Receives a message in the form of a byte array and calls all registered
	 * Network listeners.
	 * 
	 * @param bytes The byte array of the message
	 * @param source the node id of the source of the message
	 */
	public void receive(byte[] bytes, long source) {
		byte[] decompressed;
		synchronized (compressor) {
			decompressed = compressor.decompress(bytes);
		}
		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
			NetworkListener l = (NetworkListener) e.nextElement();
			l.receive(decompressed, source);
		}
	}
	
	public long getMyAddress() {
		return MY_ADDRESS;
	}
	
	public long getParentAddress() {
		if (NETWORK_MODE != MODE_SPOT)
			return -1;
		return routingManager.getParent();
	}
	
	private int getRoutingChildren() {
		if (NETWORK_MODE == MODE_UNITTEST)
			return 0;
		int res = routingManager.getNumRoutingChildren();
		Logger.logDebug("Routing children: " + res);
		return res;
	}
	
	private int getClusterChildren() {
		if (NETWORK_MODE == MODE_UNITTEST)
			return 0;
		int res = routingManager.getNumClusterChildren();
		Logger.logDebug("Cluster children: " + res);
		return res;
	}
	
	public int getNumChildren() {
		return getRoutingChildren() + getClusterChildren();
	}
	
	public Long[] getChildren() {
		return routingManager.getChildren();
	}
	
	public byte getHeight() {
		if (NETWORK_MODE == MODE_UNITTEST)
			return 0;
		byte res = routingManager.getHeight();
		Logger.logDebug("Tree height: " + res);
		return res;
	}
	
	public void setIntraClusterTrasmissionLevel(int intraClusterLevel) {
		if (NETWORK_MODE == MODE_UNITTEST)
			return;
		routingManager.setIntraClusterPowerLevel(intraClusterLevel);
	}
	
	public void setInterClusterTrasmissionLevel(int interClusterLevel) {
		if (NETWORK_MODE == MODE_UNITTEST)
			return;
		routingManager.setInterClusterPowerLevel(interClusterLevel);
	}
	
	public void setReRouteEpoch(int epochTime) {
		if (NETWORK_MODE == MODE_UNITTEST)
			return;
		routingManager.setReRouteEpoch(epochTime);
	}
	
	public void setNodeMonitoringEpoch(int epochTime) {
		if (NETWORK_MODE == MODE_UNITTEST)
			return;
		NodeMonitoring.getInstance().setSleepPeriod(epochTime);
	}
	
	public void reClusterAndReRoute() {
		if (NETWORK_MODE == MODE_UNITTEST)
			return;
		routingManager.doReclusterAndReRoute();
	}
}
