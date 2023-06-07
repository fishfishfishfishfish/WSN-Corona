package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;

/**
 * This class represents a SchedulableTask which handles the stopping of a given
 * Query in the scheduler and the removal of tasks related to it
 * 
 * @author Raymes Khoury
 */
public class KillTask extends SchedulableTask {
	private int killID;
	
	/**
	 * Create a new default KillTask. Called when instantiating from a received
	 * task
	 */
	public KillTask() {
		super();
	}
	
	/**
	 * Create a new KillTask. Called when creating a KillTask on the basestation
	 * 
	 * @param taskID The ID of this task
	 * @param killID The ID of the query to kill
	 */
	public KillTask(TaskID taskID, int killID) {
		super(taskID);
		this.killID = killID;
	}
	
	protected void _execute() {
		// Remove it from the scheduler
		Scheduler.getInstance().killQuery(killID);
	}
	
	protected void _deconstruct() {
	}
	
	protected void _reschedule() {
	}
	
	public int getKillID() {
		return killID;
	}
	
	protected void _decode(DataInput data) throws IOException {
		this.killID = data.readInt();
	}
	
	protected void _encode(DataOutput data) throws IOException {
		data.writeInt(killID);
	}
	
	public void baseInit() throws IOException {
		Network.getInstance().sendToDescendants(encode());
	}
	
	public void nodeInit() {
	}
}
