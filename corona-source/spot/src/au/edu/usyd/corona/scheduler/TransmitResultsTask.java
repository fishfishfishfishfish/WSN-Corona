package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.util.Logger;

/**
 * A task which transmits a result table to a parent and makes it available to
 * be processed.
 * 
 * @author Raymes Khoury
 */
public class TransmitResultsTask extends SchedulableTask {
	private Table t;
	private int epoch;
	
	public TransmitResultsTask() {
		super();
	}
	
	public TransmitResultsTask(TaskID taskID, Table t, int epoch) {
		super(taskID);
		this.t = t;
		this.epoch = epoch;
	}
	
	protected void _execute() {
		
		try {
			// If no data, then nothing needs to be done
			if (t != null) {
				ChildResultStore results = ((QueryTask) Scheduler.getInstance().getTask(t.getTaskID())).getChildResults();
				results.addResult(t, epoch, taskID.getNodeID());
			}
		}
		catch (TaskNotFoundException e) {
			Logger.logError("Could not store recieved task: " + e);
		}
	}
	
	protected void _deconstruct() {
	}
	
	protected void _reschedule() {
	}
	
	protected void _decode(DataInput data) throws IOException {
		taskID = new TaskID(data);
		t = new Table();
		t.decode(data);
		epoch = data.readInt();
	}
	
	protected void _encode(DataOutput data) throws IOException {
		taskID.encode(data);
		t.encode(data);
		data.writeInt(epoch);
	}
	
	public void baseInit() throws IOException {
	}
	
	public void nodeInit() {
	}
}
