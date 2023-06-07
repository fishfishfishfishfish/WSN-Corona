package au.edu.usyd.corona.middleLayer;


import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.radio.IProtocolManager;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.util.IEEEAddress;

/**
 * This class is responsible for sending and receiving application-layer packets
 * in the Tree-Networking protocol.
 * 
 * @author Khaled Almi'ani
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class PacketDispatcher implements IProtocolManager {
	// lowpan constants
	private static final byte PROTOCOL_FAMILY_NUMBER = 116;
	private static final byte PROTOCOL_NUMBER = 117;
	
	// singleton instance
	private static PacketDispatcher instance;
	
	// duplicate checking variables
	private static final byte LAST_SEEN_SIZE = 10; // Number of previously received packets to record
	private final int[] lastSeenArray; // An array of hash-values of last seen packets, in order to eliminate duplicates
	private final Hashtable lastSeenTable; // A hash set of last seen packets, in order to efficiently check for duplicates
	private int lastSeen;
	
	public static void initialize() {
		instance = new PacketDispatcher();
	}
	
	public static PacketDispatcher getInstance() {
		return instance;
	}
	
	private PacketDispatcher() {
		LowPan.getInstance().registerProtocolFamily(PROTOCOL_FAMILY_NUMBER, this);
		LowPan.getInstance().registerProtocol(PROTOCOL_NUMBER, this);
		
		lastSeenArray = new int[LAST_SEEN_SIZE];
		lastSeenTable = new Hashtable();
		lastSeen = 0;
	}
	
	private boolean isDuplicate(byte[] payload) {
		int hash = 0;
		try {
			// use a tricky method to hash the byte array, which is essentially a string
			hash = new String(payload, "US-ASCII").hashCode();
		}
		catch (UnsupportedEncodingException e) {
			Logger.logError("[PD] Unsupported char encoding.");
		}
		
		synchronized (lastSeenTable) {
			// If we've seen the hashed value, ignore it
			if (lastSeenTable.containsKey(new Integer(hash))) {
				return true;
			}
			else {
				// Otherwise we remove the oldest hashed value and store the most recent one in the table
				int remove = lastSeenArray[lastSeen];
				lastSeenArray[lastSeen] = hash;
				lastSeenTable.remove(new Integer(remove));
				lastSeenTable.put(new Integer(hash), new Boolean(true));
				lastSeen = (lastSeen + 1) % LAST_SEEN_SIZE;
				
			}
		}
		return false;
	}
	
	public void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
		Logger.logDebug("[PD] rssi: " + headerInfo.rssi + " linkQuality: " + headerInfo.linkQuality);
		try {
			Logger.logDebug("[PD] Received at" + headerInfo.timestamp + " originalAddress=" + IEEEAddress.toDottedHex(headerInfo.originator) + " sourceAddress" + IEEEAddress.toDottedHex(headerInfo.sourceAddress) + " linkquality=" + headerInfo.linkQuality);
			
			// Eliminate duplicates
			if (isDuplicate(payload)) {
				Logger.logError("[PD] Duplicate packet received from: " + IEEEAddress.toDottedHex(headerInfo.originator) + " - ignoring. (");
				return;
			}
			
			// Parse header info
			final byte sendMode = getSendMode(payload);
			final boolean passToApplicationLayer = isPassToApplicationLayer(payload);
			final boolean forwardImmediately = isForwardImmediately(payload);
			final boolean isSink = Network.NETWORK_MODE == Network.MODE_BASESTATION;
			final long node = headerInfo.originator;
			
			// update the seen information about the packets source
			NodeMonitoring.getInstance().updateSeen(headerInfo.originator);
			Logger.logDebug("[PD] got packet from " + IEEEAddress.toDottedHex(headerInfo.originator));
			
			// ignore the packet if we dont have a parent
			if (!RoutingManager.getInstance().isRouted()) {
				RoutingManager.getInstance().sendNodeStatus(node);
				Logger.logDebug("[PD] dropping packet as we are not routed");
				return;
			}
			
			// act according to the packet type
			switch (sendMode) {
			case Network.SEND_MODE_BROADCAST:
				((Network) Network.getInstance()).receive(removeFlags(payload), headerInfo.sourceAddress);
				break;
			case Network.SEND_MODE_CHILDREN_BROADCAST:
				// check to see if it was from our parent
				long parent = RoutingManager.getInstance().getParent();
				Logger.logDebug("[PD] parent is " + IEEEAddress.toDottedHex(parent));
				if (parent == headerInfo.sourceAddress) {
					Logger.logDebug("[PD] " + forwardImmediately + " " + passToApplicationLayer);
					if (forwardImmediately)
						sendPostFlagged(payload, Network.SEND_MODE_CHILDREN_BROADCAST);
					if (passToApplicationLayer)
						((Network) Network.getInstance()).receive(removeFlags(payload), headerInfo.sourceAddress);
				}
				break;
			
			case Network.SEND_MODE_CHILDREN:
				if (node == RoutingManager.getInstance().getParent()) {
					if (forwardImmediately)
						sendPostFlagged(payload, Network.SEND_MODE_CHILDREN);
					if (passToApplicationLayer)
						((Network) Network.getInstance()).receive(removeFlags(payload), headerInfo.sourceAddress);
				}
				else {
					// If the node thinks its our parent, but its not, send status to let it know
					Logger.logError("Received packet from " + IEEEAddress.toDottedHex(node) + " which is not our parent, dropping");
					RoutingManager.getInstance().sendNodeStatus(node);
				}
				break;
			
			case Network.SEND_MODE_PARENT:
				if (!RoutingManager.getInstance().hasChild(node)) {
					RoutingManager.getInstance().sendNotYourParent(headerInfo.sourceAddress);
					Logger.logError("Received packet from " + IEEEAddress.toDottedHex(node) + " which is not our child, dropping");
					break;
				}
				if (forwardImmediately && !isSink)
					sendPostFlagged(payload, Network.SEND_MODE_PARENT);
				if (passToApplicationLayer || isSink) // sinks should always get the data
					((Network) Network.getInstance()).receive(removeFlags(payload), headerInfo.sourceAddress);
				break;
			
			default:
				Logger.logError("[PD] Received packet with invalid send mode");
				break;
			}
		}
		catch (Exception e) {
			Logger.logError("[PD] Could not process packet: " + e);
		}
	}
	
	/**
	 * Send a byte buffer to nodes in the tree.
	 * 
	 * @param payload The byte array to transmit
	 * @param sendMode The send mode to transmit to (parent, child, broadcast)
	 * @param passToApplicationLayer Whether the node should pass the value to
	 * the application layer, or just forward it
	 * @param forwardImmediately Whether the node should forward the packet up or
	 * down the tree
	 */
	void send(byte[] payload, byte sendMode, boolean passToApplicationLayer, boolean forwardImmediately) {
		// Add the appropriate header info to the packet
		payload = addFlags(payload, sendMode, passToApplicationLayer, forwardImmediately);
		sendPostFlagged(payload, sendMode);
	}
	
	private void sendPostFlagged(byte[] payload, byte sendMode) {
		// Transmit the packet
		if (!RoutingManager.getInstance().isRouted()) {
			Logger.logDebug("[PD] not sending packet as we are not routed");
			return;
		}
		
		long toAddress = 0;
		switch (sendMode) {
		case Network.SEND_MODE_BROADCAST:
			Sender.getInstance().sendBroadcast(PROTOCOL_NUMBER, payload);
			break;
		case Network.SEND_MODE_CHILDREN:
			if (RoutingManager.getInstance().getState() == RoutingConstants.STATE_ROUTED_HEAD) {
				Long[] children = RoutingManager.getInstance().getRoutingChildren();
				for (int i = 0; i != children.length; i++) {
					toAddress = children[i].longValue();
					new SenderThread(payload, toAddress).start();
				}
				children = RoutingManager.getInstance().getClusterChildren();
				for (int i = 0; i != children.length; i++) {
					toAddress = children[i].longValue();
					new SenderThread(payload, toAddress).start();
				}
			}
			break;
		
		case Network.SEND_MODE_CHILDREN_BROADCAST:
			if (RoutingManager.getInstance().getState() == RoutingConstants.STATE_ROUTED_HEAD) {
				Sender.getInstance().sendBroadcast(PROTOCOL_NUMBER, payload);
			}
			
			break;
		
		case Network.SEND_MODE_PARENT:
			toAddress = RoutingManager.getInstance().getParent();
			if (RoutingManager.getInstance().getState() == RoutingConstants.STATE_ROUTED_HEAD)
				new SenderThread(payload, toAddress).start();
			else
				new SenderThread(payload, toAddress).start();
			break;
		
		default:
			Logger.logError("[PD] Sending packet with invalid send mode");
			break;
		}
		
	}
	
	/**
	 * Add header information to the packet
	 * 
	 * @param payload The packet to add header info to
	 * @param sendMode The transmission mode (parent, child, broadcast)
	 * @param passToApplicationLayer Whether the packet should be passed to the
	 * application layer
	 * @param forwardImmediately Whether the node should foward to packet
	 * @return The byte array, preceded by header information
	 */
	private byte[] addFlags(byte[] payload, byte sendMode, boolean passToApplicationLayer, boolean forwardImmediately) {
		byte[] out = new byte[payload.length + 3];
		out[0] = sendMode;
		out[1] = passToApplicationLayer ? (byte) 1 : 0;
		out[2] = forwardImmediately ? (byte) 1 : 0;
		System.arraycopy(payload, 0, out, 3, payload.length);
		return out;
	}
	
	/**
	 * Remove flags from a byte buffer
	 * 
	 * @param payload The byte array to remove flags from
	 * @return An array of header information
	 */
	private byte[] removeFlags(byte[] payload) {
		byte[] out = new byte[payload.length - 3];
		System.arraycopy(payload, 3, out, 0, out.length);
		return out;
	}
	
	private boolean isForwardImmediately(byte[] payload) {
		return payload[2] == (byte) 1;
	}
	
	private boolean isPassToApplicationLayer(byte[] payload) {
		return payload[1] == (byte) 1;
	}
	
	private byte getSendMode(byte[] payload) {
		return payload[0];
	}
	
	/**
	 * This class is used to transmit an application layer packet in a new
	 * thread. This means that the application will not be blocked for
	 * transmission, however it also means the application layer will not receive
	 * errors about transmission. This is okay for our purposes, as we always
	 * assume packets do not have guaranteed transmission.
	 * 
	 */
	private class SenderThread extends Thread {
		private final byte[] payload;
		private final long toAddress;
		
		public SenderThread(byte[] payload, long toAddress) {
			this.payload = payload;
			this.toAddress = toAddress;
		}
		
		public void run() {
			Sender.getInstance().send(PROTOCOL_FAMILY_NUMBER, PROTOCOL_NUMBER, toAddress, payload);
		}
	}
	
	void send(byte[] payload, long node, boolean forwardImmediately) {
		payload = addFlags(payload, Network.SEND_MODE_CHILDREN, true, forwardImmediately);
		sendPreFlagged(payload, node);
	}
	
	private void sendPreFlagged(byte[] payload, long node) {
		if (!RoutingManager.getInstance().isRouted()) {
			Logger.logDebug("[PD] not sending packet as we are not routed");
			return;
		}
		
		new SenderThread(payload, node).start();
	}
	
}
