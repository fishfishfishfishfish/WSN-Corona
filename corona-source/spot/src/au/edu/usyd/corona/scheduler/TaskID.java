package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;

import com.sun.spot.util.IEEEAddress;

/**
 * This class represents a Task ID which uniquely identifies a Task in the
 * system.
 * 
 * @author Raymes Khoury
 */
public class TaskID implements Transmittable {
	private int queryID; // The query ID the task is related to
	private long nodeID; // The node that the task originated at
	private int localTaskID; // An ID local to the node that the task originated at
	
	private static int localIDCounter = 0;
	private static Object localIDMutex = new Object();
	
	public TaskID() {
	}
	
	public TaskID(int localTaskID, long nodeID, int queryID) {
		this.localTaskID = localTaskID;
		this.nodeID = nodeID;
		this.queryID = queryID;
	}
	
	/**
	 * Construct a new TaskID for the given query
	 * 
	 * @param queryID The ID of the query the task will be related to
	 */
	public TaskID(int queryID) {
		synchronized (localIDMutex) {
			this.localTaskID = localIDCounter++;
		}
		this.nodeID = Network.getInstance().getMyAddress();
		this.queryID = queryID;
	}
	
	public TaskID(DataInput data) throws IOException {
		decode(data);
	}
	
	public int getQueryID() {
		return queryID;
	}
	
	public long getNodeID() {
		return nodeID;
	}
	
	public int getLocalTaskID() {
		return localTaskID;
	}
	
	public void decode(DataInput data) throws IOException {
		queryID = data.readInt();
		nodeID = data.readLong();
		localTaskID = data.readInt();
	}
	
	public void encode(DataOutput data) throws IOException {
		data.writeInt(queryID);
		data.writeLong(nodeID);
		data.writeInt(localTaskID);
	}
	
	public boolean equals(Object o) {
		if (o instanceof TaskID) {
			TaskID t = (TaskID) o;
			return this.queryID == t.queryID && this.nodeID == t.nodeID && this.localTaskID == t.localTaskID;
		}
		return false;
	}
	
	public int hashCode() {
		return queryID ^ (int) nodeID ^ localTaskID;
	}
	
	public String toString() {
		return "(QID: " + queryID + ", NODEID: " + IEEEAddress.toDottedHex(nodeID) + ", LOCALID: " + localTaskID + ")";
	}
	
	public static void setLocalTaskID(int id) {
		localIDCounter = id;
	}
}
