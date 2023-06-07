package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.TimeSync;

/**
 * This class is a task created to execute the SET syntax in the query language.
 * It is used to set a user changable value in the system, such as sleep epochs
 * for threads.
 * 
 * @author Tim Dawborn
 */
public class SetPropertyTask extends SchedulableTask {
	public static final byte SET_REROUTE_EPOCH = 0;
	public static final byte SET_MONITORING_EPOCH = 1;
	public static final byte SET_SYNC_EPOCH = 2;
	public static final byte SET_INTERCLUSTER_POWER = 3;
	public static final byte SET_INTRACLUSTER_POWER = 4;
	
	private byte property;
	private int value;
	
	public SetPropertyTask() {
		super();
	}
	
	public SetPropertyTask(TaskID taskID, byte property, int value) {
		super(taskID);
		this.property = property;
		this.value = value;
	}
	
	protected void _execute() {
		switch (property) {
		case SET_REROUTE_EPOCH:
			Network.getInstance().setReRouteEpoch(value);
			break;
		
		case SET_MONITORING_EPOCH:
			Network.getInstance().setNodeMonitoringEpoch(value);
			break;
		
		case SET_SYNC_EPOCH:
			TimeSync.getInstance().setSyncEpoch(value);
			break;
		
		case SET_INTERCLUSTER_POWER:
			Network.getInstance().setInterClusterTrasmissionLevel(value);
			break;
		
		case SET_INTRACLUSTER_POWER:
			Network.getInstance().setIntraClusterTrasmissionLevel(value);
			break;
		}
	}
	
	protected void _deconstruct() {
	}
	
	protected void _reschedule() {
	}
	
	protected void _decode(DataInput data) throws IOException {
		property = data.readByte();
		value = data.readInt();
	}
	
	protected void _encode(DataOutput data) throws IOException {
		data.writeByte(property);
		data.writeInt(value);
	}
	
	public void baseInit() throws IOException {
		Network.getInstance().sendToDescendants(encode());
	}
	
	public void nodeInit() {
	}
}
