package au.edu.usyd.corona.server.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.scheduler.TaskID;

public class TimeSyncTask extends SchedulableTask {
	public TimeSyncTask() {
		super();
	}
	
	public TimeSyncTask(TaskID taskID) {
		super(taskID);
	}
	
	@Override
	protected void _decode(DataInput data) throws IOException {
	}
	
	@Override
	protected void _deconstruct() {
	}
	
	@Override
	protected void _encode(DataOutput data) throws IOException {
	}
	
	@Override
	protected void _execute() {
		TimeSync.getInstance().forceTimeSync();
	}
	
	@Override
	protected void _reschedule() {
	}
	
	@Override
	public void baseInit() throws IOException {
	}
	
	@Override
	public void nodeInit() {
	}
}
