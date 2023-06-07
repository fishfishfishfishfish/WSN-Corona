package au.edu.usyd.corona.middleLayer;


import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;

/**
 * This class provides a simple layer over the LowPan send methods to allow for
 * error checking.
 * 
 * @author Raymes Khoury
 */
class Sender {
	// constants
	private static final int MAX_ATTEMPTS_FRAG = 4;
	private static final int MAX_ATTEMPTS_NO_FRAG = 3;
	
	// singleton instance
	private static final Sender instance = new Sender();
	
	public static Sender getInstance() {
		return instance;
	}
	
	private Sender() {
		// hidden constructor
	}
	
	boolean send(byte protocolFamilyNumber, byte protocolNumber, long toAddress, byte[] payload) {
		boolean res = false;
		int attempts = 0;
		Logger.logDebug("[Sender-send] Trying to send packet to " + IEEEAddress.toDottedHex(toAddress) + " power: " + RadioFactory.getRadioPolicyManager().getOutputPower());
		while (res == false && NodeMonitoring.getInstance().hasNeighbour(toAddress) && attempts < MAX_ATTEMPTS_FRAG) {
			try {
				LowPan.getInstance().send(protocolFamilyNumber, protocolNumber, toAddress, payload, 0, payload.length);
				NodeMonitoring.getInstance().handleSent(toAddress);
				Logger.logDebug("[Sender-send] SEND END to: " + toAddress);
				res = true;
			}
			catch (ChannelBusyException e) {
				Logger.logError("[Sender-send] Channel Busy: could not send to: " + toAddress);
			}
			catch (NoRouteException e) {
				Logger.logError("[Sender-send] No Route: could not send to" + toAddress);
				NodeMonitoring.getInstance().handleNoAck(toAddress);
			}
			attempts++;
		}
		return res;
	}
	
	boolean sendBroadcast(byte protocolNumber, byte[] payload) {
		boolean res = false;
		int attempts = 0;
		Logger.logDebug("[Sender-broadcast] Trying to send packet to Broadcast");
		while (res == false && attempts < MAX_ATTEMPTS_NO_FRAG) {
			try {
				
				LowPan.getInstance().sendBroadcast(protocolNumber, payload, 0, payload.length, 1);
				Logger.logDebug("[Sender-broadcast] SEND END");
				res = true;
			}
			catch (ChannelBusyException e) {
				Logger.logError("[Sender-broadcast] Channel Busy: could not send");
			}
			attempts++;
		}
		return res;
	}
	
	boolean sendWithoutMeshingOrFragmentation(byte protocolNumber, long toAddress, byte[] payload) {
		boolean res = false;
		int attempts = 0;
		Logger.logDebug("[Sender-nomesh] Trying to send packet to " + IEEEAddress.toDottedHex(toAddress) + "(no mesh)");
		while (res == false && attempts < MAX_ATTEMPTS_NO_FRAG) {
			try {
				LowPan.getInstance().sendWithoutMeshingOrFragmentation(protocolNumber, toAddress, payload, 0, payload.length);
				NodeMonitoring.getInstance().handleSent(toAddress);
				Logger.logDebug("[Sender-nomesh] SEND END");
				res = true;
			}
			catch (ChannelBusyException e) {
				Logger.logError("[Sender-nomesh] Channel Busy: could not send to: " + toAddress);
			}
			catch (NoAckException e) {
				Logger.logError("[Sender-nomesh] No Ack: could not send to: " + toAddress);
				NodeMonitoring.getInstance().handleNoAck(toAddress);
			}
			attempts++;
		}
		
		return res;
	}
	
}
