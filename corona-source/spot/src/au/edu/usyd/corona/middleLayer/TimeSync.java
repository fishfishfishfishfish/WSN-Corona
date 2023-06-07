package au.edu.usyd.corona.middleLayer;


import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.radio.IProtocolManager;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

/**
 * This class is based on the description of the single reference protocol given
 * in TSync: a lightweight bidirectional time synchronization service for
 * wireless sensor networks (see <a
 * href="http://mantis.cs.colorado.edu/media/tsync.pdf"
 * target="_blank">http://mantis.cs.colorado.edu/media/tsync.pdf</a>)
 * 
 * @author Tim Dawborn
 * @author Khaled Almi'ani
 */
public class TimeSync implements IProtocolManager, Runnable, RoutingConstants, TimeSyncInterface {
	// constants
	private static final int DEFAULT_SYNC_PERIOD = 15 * 60 * 1000; // 15 minutes
	private static final byte PROTOCOL_NUMBER = 102;
	private static final byte PROTOCOL_FAMILY_NUMBER = 101;
	
	// packet headers
	private static final byte TYPE_SYNC_BEGIN = 1;
	private static final byte TYPE_SYNC_REPLY = 2;
	private static final byte TYPE_BROADCAST_T2_D2 = 3;
	private static final byte TYPE_TARGETED_BEGIN = 4;
	private static final byte TYPE_TARGETED_REPLY = 5;
	private static final byte TYPE_TARGETED_T2_D2 = 6;
	
	// singleton instance
	private static final TimeSync instance = new TimeSync();
	
	// instance variables
	private InitiateSyncWorker initiateSync;
	private boolean isSink;
	private long T1, MY_T2;
	private long syncPeriod = DEFAULT_SYNC_PERIOD;
	private boolean syncWithMe = false;
	private long delta = 0;
	
	public static void initialize() {
		getInstance();
	}
	
	public static TimeSyncInterface getInstance() {
		return instance;
	}
	
	private TimeSync() {
		if (Network.NETWORK_MODE != Network.MODE_UNITTEST) {
			// register with lowpan
			LowPan.getInstance().registerProtocolFamily(PROTOCOL_FAMILY_NUMBER, this);
			LowPan.getInstance().registerProtocol(PROTOCOL_NUMBER, this);
			
			initiateSync = new InitiateSyncWorker();
			new Thread(initiateSync, "TimeSync Worker").start();
			
			isSink = Network.NETWORK_MODE == Network.MODE_BASESTATION;
			if (isSink)
				new Thread(this, "TimeSync").start();
		}
	}
	
	private long getDelta() {
		if (Network.NETWORK_MODE != Network.MODE_SPOT)
			return 0;
		return delta;
	}
	
	public void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
		final long from = headerInfo.originator;
		
		// update seen information
		NodeMonitoring.getInstance().updateSeen(from);
		
		switch (payload[0]) {
		case TYPE_SYNC_BEGIN:
		case TYPE_TARGETED_BEGIN:
			if (isSink)
				break;
			else if (RoutingManager.getInstance().getParent() != from) {
				Logger.logDebug("[TimeSync] dropping packet as not from parent");
				break;
			}
			MY_T2 = headerInfo.timestamp;
			
			// if the sync was aimed at me directly
			if (Utils.readBigEndLong(payload, 1) == Network.MY_ADDRESS) {
				Logger.logDebug("[TimeSync] begin direct sync from " + IEEEAddress.toDottedHex(from));
				syncWithMe = true;
				if (payload[0] == TYPE_SYNC_BEGIN)
					sendSyncReply(TYPE_SYNC_REPLY, from);
				else
					sendSyncReply(TYPE_TARGETED_REPLY, from);
			}
			break;
		
		case TYPE_SYNC_REPLY:
		case TYPE_TARGETED_REPLY:
			final long T2 = Utils.readBigEndLong(payload, 1);
			final long T3 = Utils.readBigEndLong(payload, 9);
			final long T4 = System.currentTimeMillis() + delta;
			final long D2 = ((T2 - T1) - (T4 - T3)) / 2;
			initiateSync.setGotReply(true);
			if (payload[0] == TYPE_SYNC_REPLY)
				sendBroadcastT2D2(T2, D2);
			else
				sendTargetedT2D2(from, T2, D2);
			break;
		
		case TYPE_BROADCAST_T2_D2:
		case TYPE_TARGETED_T2_D2:
			if (Network.NETWORK_MODE == Network.MODE_SPOT) {
				RoutingManager routing = RoutingManager.getInstance();
				boolean fromParent = routing.getParent() == from && routing.isRouted();
				
				if (syncWithMe) {
					syncWithMe = false;
					delta = -Utils.readBigEndLong(payload, 9);
					Logger.logDebug("[TimeSync] delta is " + delta);
				}
				else if (fromParent) {
					delta = -(Utils.readBigEndLong(payload, 9) + MY_T2 - Utils.readBigEndLong(payload, 1));
					Logger.logDebug("[TimeSync] delta is " + delta);
				}
				
				if (fromParent) {
					
					if (routing.getState() == RoutingConstants.STATE_ROUTED_HEAD && routing.hasChildren()) {
						new Thread("HRTS notifier") {
							public void run() {
								initiateSync.signalMutex();
							}
						}.start();
					}
				}
			}
			break;
		}
		
	}
	
	private void resync() {
		if (Network.NETWORK_MODE == Network.MODE_BASESTATION)
			initiateSync.signalMutex();
	}
	
	public void run() {
		while (true) {
			// sleep for the desired sleep period
			try {
				Thread.sleep(syncPeriod);
			}
			catch (InterruptedException e) {
			}
			
			// do a resync
			resync();
		}
	}
	
	public void setSyncPeriod(long syncPeriod) {
		this.syncPeriod = syncPeriod;
	}
	
	public void syncWithNode(long nodeId) {
		Logger.logDebug("[Time Sync] syncing directly with node " + IEEEAddress.toDottedHex(nodeId));
		sendSyncBegin(TYPE_TARGETED_BEGIN, nodeId);
	}
	
	private void sendSyncBegin(byte type, long targetNodeId) {
		T1 = System.currentTimeMillis() + delta;
		final byte[] data = new byte[1 + 8];
		data[0] = type;
		Utils.writeBigEndLong(data, 1, targetNodeId);
		send(data, Network.SEND_MODE_BROADCAST);
	}
	
	private void sendSyncReply(byte type, long nodeId) {
		final byte[] data = new byte[1 + 8 + 8];
		data[0] = type;
		Utils.writeBigEndLong(data, 1, MY_T2);
		Utils.writeBigEndLong(data, 9, System.currentTimeMillis());
		send(data, nodeId);
	}
	
	private void sendBroadcastT2D2(long T2, long D2) {
		final byte[] data = new byte[1 + 8 + 8];
		data[0] = TYPE_BROADCAST_T2_D2;
		Utils.writeBigEndLong(data, 1, T2);
		Utils.writeBigEndLong(data, 9, D2);
		send(data, Network.SEND_MODE_BROADCAST);
	}
	
	private void sendTargetedT2D2(long nodeId, long T2, long D2) {
		final byte[] data = new byte[1 + 8 + 8];
		data[0] = TYPE_TARGETED_T2_D2;
		Utils.writeBigEndLong(data, 1, T2);
		Utils.writeBigEndLong(data, 9, D2);
		send(data, nodeId);
	}
	
	public void setSyncEpoch(long epochTime) {
		if (Network.NETWORK_MODE == Network.MODE_UNITTEST)
			return;
		setSyncPeriod(epochTime);
	}
	
	public long getTime() {
		return System.currentTimeMillis() + getDelta();
	}
	
	public void forceTimeSync() {
		if (Network.NETWORK_MODE == Network.MODE_UNITTEST)
			return;
		resync();
	}
	
	private boolean send(byte[] payload, long toAddress) {
		if (toAddress == Network.SEND_MODE_BROADCAST)
			return Sender.getInstance().sendBroadcast(PROTOCOL_NUMBER, payload);
		else
			return Sender.getInstance().sendWithoutMeshingOrFragmentation(PROTOCOL_NUMBER, toAddress, payload);
	}
	
	private class InitiateSyncWorker implements Runnable {
		private boolean gotReply = false;
		private int index = 0;
		private Long[] allChildren;
		private final Object mutex = new Object();
		
		public final void run() {
			while (true) {
				// sleep until we need to start
				synchronized (mutex) {
					try {
						mutex.wait();
					}
					catch (InterruptedException e) {
					}
				}
				
				// run what is needed to be run
				synchronized (mutex) {
					_run();
				}
				
			}
		}
		
		/**
		 * Invoking this method wakes up the sleeping thread, and calls its
		 * {@link #_run()} method. If the thread is already inside this method,
		 * the call is ignored.
		 */
		public void signalMutex() {
			synchronized (mutex) {
				mutex.notifyAll();
			}
		}
		
		public void _run() {
			gotReply = false;
			
			// runs the normal execution
			Logger.logDebug("[TimeSync] starting init time sync");
			// update our knowledge of children
			allChildren = RoutingManager.getInstance().getChildren();
			if (allChildren.length == 0)
				return;
			
			// try to start time sync with one child, and if it does not respond, try the next, one up to 10 times
			for (int i = 0; (i != 10) && (!gotReply); i++) {
				long syncWith = getSyncNode();
				sendSyncBegin(TYPE_SYNC_BEGIN, syncWith);
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
				}
			}
			
			Logger.logDebug("[TimeSync] finished init time sync");
		}
		
		private long getSyncNode() {
			if (index >= allChildren.length)
				index = 0;
			return allChildren[index++].longValue();
		}
		
		public void setGotReply(boolean gotReply) {
			this.gotReply = gotReply;
		}
	}
}
